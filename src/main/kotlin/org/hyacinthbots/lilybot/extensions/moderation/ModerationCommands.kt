package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.annotations.DoNotChain
import com.kotlindiscord.kord.extensions.checks.hasPermissions
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingOptionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalAttachment
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralStringSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PluralKit
import com.kotlindiscord.kord.extensions.pagination.EphemeralResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import com.kotlindiscord.kord.extensions.utils.kordExUserAgent
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import com.kotlindiscord.kord.extensions.utils.timeout
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import com.kotlindiscord.kord.extensions.utils.toDuration
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.hyacinthbots.lilybot.database.collections.ModerationActionCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.TemporaryBanCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.entities.ActionData
import org.hyacinthbots.lilybot.database.entities.TemporaryBanData
import org.hyacinthbots.lilybot.database.entities.TimeData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.HYACINTH_GITHUB
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.dmNotificationStatusEmbedField
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.interval
import org.hyacinthbots.lilybot.utils.isBotOrModerator
import org.hyacinthbots.lilybot.utils.modCommandChecks
import kotlin.time.Duration

class ModerationCommands : Extension() {
	override val name = "moderation"

	private val warnSuffix = "Please consider your actions carefully.\n\n" +
		"For more information about the warn system, please see [this document]" +
		"($HYACINTH_GITHUB/LilyBot/blob/main/docs/commands.md#name-warn)"

	/** The scheduler that will track the time for un-banning in temp bans. */
	private val tempBanScheduler = Scheduler()

	/** The task that will run the [tempBanScheduler]. */
	private lateinit var tempBanTask: Task

	@OptIn(DoNotChain::class)
	override suspend fun setup() {
		tempBanTask = tempBanScheduler.schedule(120, repeat = true, callback = ::removeTempBans)
		ephemeralMessageCommand {
			name = "Moderate"
			locking = true

			requirePermission(Permission.BanMembers, Permission.KickMembers, Permission.ModerateMembers)

			check {
				hasPermissions(Permissions(Permission.BanMembers, Permission.KickMembers, Permission.ModerateMembers))
				requireBotPermissions(Permission.BanMembers, Permission.KickMembers, Permission.ModerateMembers)
			}

			action {
				val messageEvent = event
				val loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!) ?: return@action
				var menuMessage: EphemeralFollowupMessage? = null
				val targetMessage = messageEvent.interaction.getTargetOrNull()
				if (targetMessage == null) {
					respond {
						content = "The message this command was run on cannot be found! It may have been deleted."
					}
					return@action
				}
				val senderId: Snowflake
				if (targetMessage.author.isNullOrBot()) {
					val proxiedMessage =
						PluralKit(userAgent = this@ephemeralMessageCommand.kord.kordExUserAgent()).getMessageOrNull(
							targetMessage.id
						)
					proxiedMessage ?: run {
						respond { content = "Unable to find user" }
						return@action
					}
					senderId = proxiedMessage.sender
				} else {
					senderId = targetMessage.author!!.id
				}
				val sender = guild!!.getMemberOrNull(senderId)
					?: run {
						respond { content = "Unable to find user" }
						return@action
					}

				isBotOrModerator(event.kord, sender.asUserOrNull(), guild, "moderate") ?: return@action

				menuMessage = respond {
					content = "How would you like to moderate this message?"
					components {
						ephemeralStringSelectMenu {
							placeholder = "Select action..."
							maximumChoices = 1 // Prevent selecting multiple options at once

							option("Ban user", ModerationAction.BAN.name) {
								description = "Ban the user that sent this message"
							}

							option("Soft-ban", ModerationAction.SOFT_BAN.name) {
								description = "Soft-ban the user that sent this message"
							}

							option("Kick", ModerationAction.KICK.name) {
								description = "Kick the user that sent this message"
							}

							option("Timeout", ModerationAction.TIMEOUT.name) {
								description = "Timeout the user that sent this message"
							}

							option("Warn", ModerationAction.WARN.name) {
								description = "Warn the user that sent this message"
							}

							action SelectMenu@{
								// Get the first because there can only be one
								val option = event.interaction.values.firstOrNull()
								if (option == null) {
									respond { content = "You did not select an option!" }
									return@SelectMenu
								}

								val reasonSuffix = "for sending the following message: `${targetMessage.content}`"
								val modConfig = ModerationConfigCollection().getConfig(guild!!.id)

								when (option) {
									ModerationAction.BAN.name -> {
										val dm = sender.dm {
											embed {
												title = "You have been banned from ${guild?.asGuildOrNull()?.name}"
												description = modConfig?.banDmMessage ?: "Quick banned $reasonSuffix"

												color = DISCORD_GREEN
											}
										}
										ModerationActionCollection().addAction(
											ModerationAction.BAN, guild!!.id, senderId,
											ActionData(
												user.id,
												null,
												null,
												"Quick banned $reasonSuffix",
												dm != null,
												true,
												null
											)
										)

										sender.ban {
											reason =
												"Quick banned $reasonSuffix"
										}

										if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
											try {
												targetMessage.reply {
													embed {
														title = "Banned."
														description = "${sender.mention} user was banned " +
															"for sending this message."
													}
												}
											} catch (_: KtorRequestException) {
												channel.createEmbed {
													title = "Banned."
													description = "${sender.mention} user was banned " +
														"for sending a deleted message."
												}
											}
										}

										menuMessage?.edit {
											content = "Banned a user."
											components { removeAll() }
										}
									}

									ModerationAction.SOFT_BAN.name -> {
										val dm = sender.dm {
											embed {
												title = "You have been soft-banned from ${guild?.asGuildOrNull()?.name}"
												description =
													"Quick soft-banned $reasonSuffix. This is a soft-ban, you are " +
														"free to rejoin at any time"
											}
										}

										ModerationActionCollection().addAction(
											ModerationAction.SOFT_BAN, guild!!.id, senderId,
											ActionData(
												user.id,
												null,
												null,
												"Quick banned $reasonSuffix",
												dm != null,
												true,
												null
											)
										)

										sender.ban {
											reason =
												"Quick soft-banned $reasonSuffix"
										}

										ModerationActionCollection().shouldIgnoreAction(
											ModerationAction.SOFT_BAN, guild!!.id, senderId
										)

										guild!!.unban(senderId, "Quick soft-ban unban")

										if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
											try {
												targetMessage.reply {
													embed {
														title = "Soft-banned."
														description = "${sender.mention} user was soft-banned " +
															"for sending this message."
													}
												}
											} catch (_: KtorRequestException) {
												channel.createEmbed {
													title = "Soft-Banned."
													description = "${sender.mention} user was soft-banned " +
														"for sending a deleted message."
												}
											}
										}

										menuMessage?.edit {
											content = "Soft-Banned a user."
											components { removeAll() }
										}
									}

									ModerationAction.KICK.name -> {
										val dm = sender.dm {
											embed {
												title = "You have been kicked from ${guild?.asGuildOrNull()?.name}"
												description = "Quick kicked $reasonSuffix."
											}
										}

										guild!!.kick(senderId, "Quick kicked ")

										if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
											try {
												targetMessage.reply {
													embed {
														title = "Kicked."
														description = "${sender.mention} user was kicked " +
															"for sending this message."
													}
												}
											} catch (_: KtorRequestException) {
												channel.createEmbed {
													title = "Kicked."
													description = "${sender.mention} user was kicked " +
														"for sending a deleted message."
												}
											}
										}

										ModerationActionCollection().addAction(
											ModerationAction.KICK, guild!!.id, senderId,
											ActionData(
												user.id,
												null,
												null,
												"Quick kicked via moderate menu $reasonSuffix",
												dm != null,
												true,
												null
											)
										)

										menuMessage?.edit {
											content = "Kicked a user."
											components { removeAll() }
										}
									}

									ModerationAction.TIMEOUT.name -> {
										val timeoutTime =
											ModerationConfigCollection().getConfig(guild!!.id)?.quickTimeoutLength
										if (timeoutTime == null) {
											menuMessage?.edit {
												content =
													"No timeout length has been set in the moderation config, please set a length."
												components { removeAll() }
											}
											return@SelectMenu
										}

										val dm = sender.dm {
											embed {
												title = "You have been timed-out in ${guild?.asGuildOrNull()?.name}"
												description =
													"Quick timed out for ${timeoutTime.interval()} $reasonSuffix."
											}
										}

										sender.timeout(timeoutTime, reason = "Quick timed-out $reasonSuffix")

										if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
											try {
												targetMessage.reply {
													embed {
														title = "Timed-out."
														description = "${sender.mention} user was timed-out for " +
															"${timeoutTime.interval()} for sending this message."
													}
												}
											} catch (_: KtorRequestException) {
												channel.createEmbed {
													title = "Timed-out."
													description = "${sender.mention} user was timed-out for " +
														"${timeoutTime.interval()} for sending a deleted message."
												}
											}
										}

										ModerationActionCollection().addAction(
											ModerationAction.TIMEOUT, guild!!.id, senderId,
											ActionData(
												user.id,
												null,
												TimeData(timeoutTime, null, null, null),
												"Quick timed-out via moderate menu $reasonSuffix",
												dm != null,
												null,
												null
											)
										)

										menuMessage?.edit {
											content = "Timed-out a user."
											components { removeAll() }
										}
									}

									ModerationAction.WARN.name -> {
										WarnCollection().setWarn(senderId, guild!!.id, false)
										val strikes = WarnCollection().getWarn(senderId, guild!!.id)?.strikes

										val dm = sender.dm {
											embed {
												title = "Warning $strikes in ${guild?.asGuildOrNull()?.name}"
												description =
													"Quick warned $reasonSuffix\n $warnSuffix"
											}
										}

										if (modConfig?.autoPunishOnWarn == true && strikes!! > 1) {
											val duration = when (strikes) {
												2 -> "PT3H"
												3 -> "PT12H"
												else -> "P3D"
											}
											guild?.getMemberOrNull(senderId)?.edit {
												timeoutUntil = Clock.System.now().plus(Duration.parse(duration))
											}
										}

										loggingChannel.createMessage {
											embed {
												title = "Warning"
												baseModerationEmbed(
													"Quick warned via moderate menu $reasonSuffix",
													sender,
													user
												)
												dmNotificationStatusEmbedField(dm, true)
												timestamp = Clock.System.now()
												field {
													name = "Total strikes"
													value = strikes.toString()
												}
											}
											if (modConfig?.autoPunishOnWarn == true && strikes != 1) {
												embed {
													warnTimeoutLog(
														strikes!!,
														event.interaction.user.asUserOrNull(),
														sender.asUserOrNull(),
														"Quick warned via moderate menu $reasonSuffix"
													)
												}
											}
										}

										menuMessage?.edit {
											content = "Warned a user."
											components { removeAll() }
										}

										if (modConfig?.publicLogging == true) {
											try {
												targetMessage.reply {
													embed {
														title = "Warned."
														description = "${sender.mention} user was warned " +
															"for sending this message."
													}
												}
											} catch (_: KtorRequestException) {
												channel.createEmbed {
													title = "Warned."
													description = "${sender.mention} user was warned " +
														"for sending a deleted message."
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}

		ephemeralSlashCommand(::BanArgs) {
			name = "ban"
			description = "Bans a user."

			requirePermission(Permission.BanMembers)

			check {
				modCommandChecks(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers)
			}

			action {
				isBotOrModerator(event.kord, arguments.userArgument, guild, "ban") ?: return@action

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
				var dmStatus: Message? = null
				if (arguments.dm) {
					dmStatus = arguments.userArgument.dm {
						embed {
							title = "You have been banned from ${guild?.asGuildOrNull()?.name}"
							description = "**Reason:**\n${
								if (modConfig?.banDmMessage != null && arguments.reason == "No reason provided") {
									modConfig.banDmMessage
								} else if (modConfig?.banDmMessage != null && arguments.reason != "No reason provided") {
									"${arguments.reason}\n${modConfig.banDmMessage}"
								} else {
									arguments.reason
								}
							}\n${
								if (arguments.softBan) {
									"You were soft-banned. You are free to rejoin without the need to be unbanned"
								} else {
									""
								}
							}"
						}
					}
				}

				ModerationActionCollection().addAction(
					if (arguments.softBan) ModerationAction.SOFT_BAN else ModerationAction.BAN,
					guild!!.id, arguments.userArgument.id,
					ActionData(
						user.id,
						arguments.messages,
						null,
						arguments.reason,
						dmStatus != null,
						arguments.dm,
						arguments.image?.url
					)
				)

				if (modConfig?.publicLogging == true) {
					event.interaction.channel.createEmbed {
						if (arguments.softBan) {
							title = "Soft-Banned a user"
							description = "${arguments.userArgument.mention} has been soft-banned!"
						} else {
							title = "Banned a user"
							description = "${arguments.userArgument.mention} has been banned!"
						}
						color = DISCORD_BLACK
					}
				}

				guild?.ban(arguments.userArgument.id) {
					reason = arguments.reason + if (arguments.softBan) " **SOFT-BAN**" else ""
					deleteMessageDuration = if (arguments.softBan && arguments.messages == 0) {
						DateTimePeriod(days = 3).toDuration(TimeZone.UTC)
					} else {
						DateTimePeriod(days = arguments.messages).toDuration(TimeZone.UTC)
					}
				}

				if (arguments.softBan) {
					ModerationActionCollection().declareActionToIgnore(
						ModerationAction.UNBAN, guild?.id!!, arguments.userArgument.id
					)
					guild?.unban(arguments.userArgument.id, "User was soft-banned. **SOFT-BAN**")
				}

				respond {
					content = if (arguments.softBan) "Soft-banned " else "Banned " + arguments.userArgument.mention
				}
			}
		}

		ephemeralSlashCommand {
			name = "temp-ban"
			description = "The parent command for temporary ban commands"

			ephemeralSubCommand(::TempBanArgs) {
				name = "add"
				description = "Temporarily bans a user"

				requirePermission(Permission.BanMembers)
				check {
					modCommandChecks(Permission.BanMembers)
					requireBotPermissions(Permission.BanMembers)
				}

				action {
					isBotOrModerator(event.kord, arguments.userArgument, guild, "temp-ban add")
					val now = Clock.System.now()
					val duration = now.plus(arguments.duration, TimeZone.UTC)
					val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
					var dmStatus: Message? = null
					if (arguments.dm) {
						dmStatus = arguments.userArgument.dm {
							embed {
								title = "You have been temporarily banned from ${guild?.fetchGuild()?.name}"
								description = "**Reason:**\n${arguments.reason}\n\n" +
									"You are banned until $duration"
							}
						}
					}

					ModerationActionCollection().addAction(
						ModerationAction.TEMP_BAN, guild!!.id, arguments.userArgument.id,
						ActionData(
							user.id,
							arguments.messages,
							TimeData(arguments.duration, duration),
							arguments.reason,
							dmStatus != null,
							arguments.dm,
							arguments.image?.url
						)
					)

					if (modConfig?.publicLogging == true) {
						event.interaction.channel.createEmbed {
							title = "Temp Banned a user"
							description = "${arguments.userArgument.mention} has been Temp Banned!"
							color = DISCORD_BLACK
						}
					}

					TemporaryBanCollection().setTempBan(
						TemporaryBanData(guild!!.id, arguments.userArgument.id, user.id, now, duration)
					)

					guild?.ban(arguments.userArgument.id) {
						reason = arguments.reason + " **TEMPORARY-BAN**"
						deleteMessageDuration = DateTimePeriod(days = arguments.messages).toDuration(TimeZone.UTC)
					}

					respond {
						content = "Temporarily banned ${arguments.userArgument.mention}"
					}
				}
			}

			ephemeralSubCommand {
				name = "view-all"
				description = "View all temporary bans for this guild"

				requirePermission(Permission.BanMembers)

				check {
					modCommandChecks(Permission.BanMembers)
					requireBotPermissions(Permission.BanMembers)
				}

				action {
					val pagesObj = Pages()
					val tempBans = TemporaryBanCollection().getTempBansForGuild(guild!!.id)
					if (tempBans.isEmpty()) {
						pagesObj.addPage(
							Page {
								description = "There are no temporary bans in this guild."
							}
						)
					} else {
						tempBans.chunked(4).forEach { tempBan ->
							var content = ""
							tempBan.forEach {
								content = """
									User: ${this@ephemeralSubCommand.kord.getUser(it.bannedUserId)?.username}
									Moderator: ${guild?.getMemberOrNull(it.moderatorUserId)?.username}
									Start Time: ${it.startTime.toDiscord(TimestampType.ShortDateTime)} (${
									it.startTime.toDiscord(TimestampType.RelativeTime)
								})
									End Time: ${it.endTime.toDiscord(TimestampType.ShortDateTime)} (${
									it.endTime.toDiscord(TimestampType.RelativeTime)
								})
									---
								""".trimIndent()
							}

							pagesObj.addPage(
								Page {
									title = "Temporary bans for ${guild?.asGuildOrNull()?.name ?: "this guild"}"
									description = content
								}
							)
						}
					}

					val paginator = EphemeralResponsePaginator(
						pages = pagesObj,
						owner = event.interaction.user,
						timeoutSeconds = 300,
						interaction = interactionResponse
					)

					paginator.send()
				}
			}
		}

		ephemeralSlashCommand(::UnbanArgs) {
			name = "unban"
			description = "Unbans a user."

			requirePermission(Permission.BanMembers)

			check {
				modCommandChecks(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers)
			}

			action {
				val tempBan = TemporaryBanCollection().getUserTempBan(this.getGuild()!!.id, arguments.userArgument.id)
				if (tempBan == null) {
					ModerationActionCollection().addAction(
						ModerationAction.UNBAN, guild!!.id, arguments.userArgument.id,
						ActionData(
							user.id, null, null, arguments.reason, null, null, null
						)
					)
					guild?.unban(arguments.userArgument.id, arguments.reason)
				} else {
					ModerationActionCollection().addAction(
						ModerationAction.UNBAN, guild!!.id, arguments.userArgument.id,
						ActionData(
							user.id, null, null, arguments.reason + "**TEMPORARY-BAN", null, null, null
						)
					)
					guild?.unban(arguments.userArgument.id, arguments.reason + "**TEMPORARY-BAN**")
					TemporaryBanCollection().removeTempBan(guild!!.id, arguments.userArgument.id)
				}
				respond {
					content = "Unbanned user."
				}
			}
		}

		ephemeralSlashCommand(::KickArgs) {
			name = "kick"
			description = "Kicks a user."

			requirePermission(Permission.KickMembers)

			check {
				modCommandChecks(Permission.KickMembers)
				requireBotPermissions(Permission.KickMembers)
			}

			action {
				isBotOrModerator(event.kord, arguments.userArgument, guild, "kick") ?: return@action

				val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
				var dmStatus: Message? = null
				if (arguments.dm) {
					dmStatus = arguments.userArgument.dm {
						embed {
							title = "You have been kicked from ${guild?.fetchGuild()?.name}"
							description = "**Reason:**\n${arguments.reason}"
						}
					}
				}
				ModerationActionCollection().addAction(
					ModerationAction.KICK, guild!!.id, arguments.userArgument.id,
					ActionData(
						user.id, null, null, arguments.reason, dmStatus != null, arguments.dm, arguments.image?.url
					)
				)

				if (modConfig?.publicLogging == true) {
					event.interaction.channel.createEmbed {
						title = "Kicked a user"
						description = "${arguments.userArgument.mention} has been kicked!"
						color = DISCORD_BLACK
					}
				}

				guild?.kick(arguments.userArgument.id, arguments.reason)

				respond {
					content = "Kicked ${arguments.userArgument.mention}"
				}
			}
		}

		ephemeralSlashCommand(::TimeoutArgs) {
			name = "timeout"
			description = "Times out a user."

			requirePermission(Permission.ModerateMembers)

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
				val durationArg = arguments.duration ?: modConfig?.quickTimeoutLength ?: DateTimePeriod(hours = 6)
				val duration = Clock.System.now().plus(durationArg, TimeZone.UTC)

				isBotOrModerator(event.kord, arguments.userArgument, guild, "timeout") ?: return@action

				var dmStatus: Message? = null
				if (arguments.dm) {
					dmStatus = arguments.userArgument.dm {
						embed {
							title = "You have been timed out in ${guild?.fetchGuild()?.name}"
							description = "**Duration:**\n${
								duration.toDiscord(TimestampType.Default) + " (${durationArg.interval()})"
							}\n**Reason:**\n${arguments.reason}"
						}
					}
				}

				ModerationActionCollection().addAction(
					ModerationAction.TIMEOUT, guild!!.id, arguments.userArgument.id,
					    ActionData(
						user.id,
						null,
						TimeData(durationArg, duration, Clock.System.now(), duration),
						arguments.reason,
						dmStatus != null,
						arguments.dm,
						arguments.image?.url
					)
				)

				if (modConfig?.publicLogging == true) {
					event.interaction.channel.createEmbed {
						title = "Timeout"
						description = "${arguments.userArgument.mention} was timed out by a moderator"
						color = DISCORD_BLACK
						field {
							name = "Duration:"
							value = duration.toDiscord(TimestampType.Default) + " (${durationArg.interval()})"
							inline = false
						}
					}
				}

				arguments.userArgument.asMemberOrNull(guild!!.id)?.edit {
					timeoutUntil = duration
				}

				respond {
					content = "Timed-out ${arguments.userArgument.mention}."
				}
			}
		}

		ephemeralSlashCommand(::RemoveTimeoutArgs) {
			name = "remove-timeout"
			description = "Removes a timeout from a user"

			requirePermission(Permission.ModerateMembers)

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				var dmStatus: Message? = null
				if (arguments.dm) {
					dmStatus = arguments.userArgument.dm {
						embed {
							title = "Timeout removed in ${guild!!.asGuildOrNull()?.name}"
							description = "Your timeout has been manually removed in this guild."
						}
					}
				}

				ModerationActionCollection().addAction(
					ModerationAction.REMOVE_TIMEOUT, guild!!.id, arguments.userArgument.id,
					    ActionData(
						user.id, null, null, null, dmStatus != null, arguments.dm, null
					)
				)

				arguments.userArgument.asMemberOrNull(guild!!.id)?.edit {
					timeoutUntil = null
				}

				respond {
					content = "Removed timeout from user."
				}
			}
		}

		ephemeralSlashCommand(::WarnArgs) {
			name = "warn"
			description = "Warns a user."

			requirePermission(Permission.ModerateMembers)

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog =
					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
				val guildName = guild?.asGuildOrNull()?.name

				isBotOrModerator(event.kord, arguments.userArgument, guild, "warn") ?: return@action

				WarnCollection().setWarn(arguments.userArgument.id, guild!!.id, false)
				val strikes = WarnCollection().getWarn(arguments.userArgument.id, guild!!.id)?.strikes

				respond {
					content = "Warned user."
				}

				var dmStatus: Message? = null

				if (arguments.dm) {
					val warnText = if (config.autoPunishOnWarn == false) {
						"No moderation action has been taken.\n $warnSuffix"
					} else {
						when (strikes) {
							1 -> "No moderation action has been taken.\n $warnSuffix"
							2 -> "You have been timed out for 3 hours.\n $warnSuffix"
							3 -> "You have been timed out for 12 hours.\n $warnSuffix"
							else -> "You have been timed out for 3 days.\n $warnSuffix"
						}
					}

					dmStatus = arguments.userArgument.dm {
						embed {
							title = "Warning $strikes in $guildName"
							description = "**Reason:** ${arguments.reason}\n\n$warnText"
						}
					}
				}

				if (config.autoPunishOnWarn == true && strikes!! > 1) {
					val duration = when (strikes) {
						2 -> "PT3H"
						3 -> "PT12H"
						else -> "P3D"
					}
					guild?.getMemberOrNull(arguments.userArgument.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse(duration))
					}
				}

				actionLog.createMessage {
					embed {
						title = "Warning"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmStatus, arguments.dm)
						timestamp = Clock.System.now()
						field {
							name = "Total strikes"
							value = strikes.toString()
						}
						color = DISCORD_RED
					}
					if (config.autoPunishOnWarn == true && strikes != 1) {
						embed {
							warnTimeoutLog(
								strikes!!,
								event.interaction.user.asUserOrNull(),
								arguments.userArgument,
								arguments.reason
							)
						}
					}
				}

				if (config.publicLogging != null && config.publicLogging == true) {
					channel.createEmbed {
						title = "Warning"
						description = "${arguments.userArgument.mention} has been warned by a moderator"
						color = DISCORD_RED
					}
				}
			}
		}

		ephemeralSlashCommand(::RemoveWarnArgs) {
			name = "remove-warn"
			description = "Removes a user's warnings"

			requirePermission(Permission.ModerateMembers)

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val targetUser = guild?.getMemberOrNull(arguments.userArgument.id) ?: run {
					respond {
						content = "I was unable to find the member in this guild! Please try again!"
					}
					return@action
				}

				var userStrikes = WarnCollection().getWarn(targetUser.id, guild!!.id)?.strikes
				if (userStrikes == 0 || userStrikes == null) {
					respond {
						content = "This user does not have any warning strikes!"
					}
					return@action
				}

				WarnCollection().setWarn(targetUser.id, guild!!.id, true)
				userStrikes = WarnCollection().getWarn(targetUser.id, guild!!.id)?.strikes

				respond {
					content = "Removed strike from user"
				}

				var dmStatus: Message? = null
				if (arguments.dm) {
					dmStatus = targetUser.dm {
						embed {
							title = "Warn strike removal in ${guild?.fetchGuild()?.name}"
							description = "You have had a warn strike removed. You now have $userStrikes strikes."
							color = DISCORD_GREEN
						}
					}
				}

				val actionLog =
					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
				actionLog.createEmbed {
					title = "Warning Removal"
					color = DISCORD_GREEN
					timestamp = Clock.System.now()
					baseModerationEmbed(null, targetUser, user)
					dmNotificationStatusEmbedField(dmStatus, arguments.dm)
					field {
						name = "Total Strikes:"
						value = userStrikes.toString()
						inline = false
					}
				}

				if (config.publicLogging != null && config.publicLogging == true) {
					channel.createEmbed {
						title = "Warning Removal"
						description = "${arguments.userArgument.mention} had a warn strike removed by a moderator."
						color = DISCORD_GREEN
					}
				}
			}
		}
	}

	private suspend fun removeTempBans() {
		val tempBans = TemporaryBanCollection().getAllTempBans()
		val dueTempBans =
			tempBans.filter { it.endTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0 }

		for (it in dueTempBans) {
			var guild: Guild?
			try {
				guild = kord.getGuildOrNull(it.guildId)
			} catch (_: KtorRequestException) {
				TemporaryBanCollection().removeTempBan(it.guildId, it.bannedUserId)
				continue
			}

			if (guild == null) {
				TemporaryBanCollection().removeTempBan(it.guildId, it.bannedUserId)
				continue
			}

			ModerationActionCollection().addAction(
				ModerationAction.UNBAN, guild.id, it.bannedUserId,
				ActionData(
					it.moderatorUserId,
					null,
					TimeData(null, null, it.startTime, it.endTime),
					"**temporary-ban-expire**",
					null,
					null,
					null
				)
			)

			guild.unban(it.bannedUserId, "Temporary Ban expired")
			TemporaryBanCollection().removeTempBan(it.guildId, it.bannedUserId)
		}
	}

	inner class BanArgs : Arguments() {
		/** The user to ban. */
		val userArgument by user {
			name = "user"
			description = "Person to ban"
		}

		/** The number of days worth of messages to delete. */
		val messages by int {
			name = "delete-message-days"
			description = "The number of days worth of messages to delete"
		}

		/** The reason for the ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No reason provided"
		}

		/** Weather to softban this user or not. */
		val softBan by defaultingBoolean {
			name = "soft-ban"
			description = "Weather to soft-ban this user (unban them once messages are deleted)"
			defaultValue = false
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the ban"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the ban. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class TempBanArgs : Arguments() {
		/** The user to ban. */
		val userArgument by user {
			name = "user"
			description = "Person to ban"
		}

		/** The number of days worth of messages to delete. */
		val messages by int {
			name = "delete-message-days"
			description = "The number of days worth of messages to delete"
		}

		/** The duration of the temporary ban. */
		val duration by coalescingDuration {
			name = "duration"
			description = "The duration of the temporary ban."
		}

		/** The reason for the ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the ban"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the ban. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class UnbanArgs : Arguments() {
		/** The ID of the user to unban. */
		val userArgument by user {
			name = "user"
			description = "Person to un-ban"
		}

		/** The reason for the un-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the un-ban"
			defaultValue = "No reason provided"
		}
	}

	inner class KickArgs : Arguments() {
		/** The user to kick. */
		val userArgument by user {
			name = "user"
			description = "Person to kick"
		}

		/** The reason for the kick. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the Kick"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the kick"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class TimeoutArgs : Arguments() {
		/** The requested user to timeout. */
		val userArgument by user {
			name = "user"
			description = "Person to timeout"
		}

		/** The time the timeout should last for. */
		val duration by coalescingOptionalDuration {
			name = "duration"
			description = "Duration of timeout"
		}

		/** The reason for the timeout. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for timeout"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the timeout"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class RemoveTimeoutArgs : Arguments() {
		/** The requested user to remove the timeout from. */
		val userArgument by user {
			name = "user"
			description = "Person to remove timeout from"
		}

		/** Whether to DM the user about the timeout removal or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to dm the user about this or not"
			defaultValue = true
		}
	}

	inner class WarnArgs : Arguments() {
		/** The requested user to warn. */
		val userArgument by user {
			name = "user"
			description = "Person to warn"
		}

		/** The reason for the warning. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for warning"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warning"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class RemoveWarnArgs : Arguments() {
		/** The requested user to remove the warning from. */
		val userArgument by user {
			name = "user"
			description = "Person to remove warn from"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warning"
			defaultValue = true
		}
	}
}

/**
 * Creates a log for timeouts produced by a number of warnings.
 *
 * @param warningNumber The number of warning strikes the user has
 * @param moderator The moderator that actioned the warning
 * @param targetUser The User that was warned
 * @param reason The reason for the warning
 * @author NoComment1105
 * @since 4.4.0
 */
private fun EmbedBuilder.warnTimeoutLog(warningNumber: Int, moderator: User, targetUser: User, reason: String) {
	when (warningNumber) {
		1 -> {}
		2 -> description = "${targetUser.mention} has been timed-out for 3 hours due to 2 warn strikes"

		3 -> description = "${targetUser.mention} has been timed-out for 12 hours due to 3 warn strikes"

		else ->
			description = "${targetUser.mention} has been timed-out for 3 days due to $warningNumber warn " +
				"strikes\nIt might be time to consider other action."
	}

	if (warningNumber != 1) {
		title = "Timeout"
		field {
			name = "User"
			value = "${targetUser.id} (${targetUser.username})"
		}
		field {
			name = "Reason"
			value = reason
		}
		footer {
			text = moderator.username
			icon = moderator.avatar?.cdnUrl?.toUrl()
		}
		color = DISCORD_BLACK
		timestamp = Clock.System.now()
	}
}
