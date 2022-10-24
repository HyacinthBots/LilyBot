package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.types.CheckContextWithCache
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalAttachment
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import io.github.nocomment1105.discordmoderationactions.builder.ban
import io.github.nocomment1105.discordmoderationactions.builder.kick
import io.github.nocomment1105.discordmoderationactions.builder.removeTimeout
import io.github.nocomment1105.discordmoderationactions.builder.softban
import io.github.nocomment1105.discordmoderationactions.builder.timeout
import io.github.nocomment1105.discordmoderationactions.builder.unban
import io.github.nocomment1105.discordmoderationactions.enums.DmResult
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.dmNotificationStatusEmbedField
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.isBotOrModerator
import org.hyacinthbots.lilybot.utils.requiredConfigs
import kotlin.math.min
import kotlin.time.Duration

class ModerationCommands : Extension() {
	override val name = "moderation"

	private val warnSuffix = "Please consider your actions carefully.\n\n" +
			"For more information about the warn system, please see [this document]" +
			"(https://github.com/HyacinthBots/LilyBot/blob/main/docs/commands.md)"

	override suspend fun setup() {
		ephemeralMessageCommand {
			name = "moderate"
			locking = true

			action {
				respond {
					content = "How would you like to moderate this message?"
					components {
						ephemeralSelectMenu {
							placeholder = "Select action..."
							maximumChoices = 1 // Prevent selecting multiple options at once

							option(label = "Ban user", ModerationActions.BAN.name) {
								description = "Ban the user that sent this message"
							}

							option("Soft-ban", ModerationActions.SOFT_BAN.name) {
								description = "Soft-ban the user that sent this message"
							}

							option("Kick", ModerationActions.KICK.name) {
								description = "Kick the user that sent this message"
							}

							option("Timeout", ModerationActions.TIMEOUT.name) {
								description = "Timeout the user that sent this message"
							}

							option("Warn", ModerationActions.WARN.name) {
								description = "Warn the user that sent this message"
							}

							action SelectMenu@{
								// Get the first because there can only be one
								val option = event.interaction.values.firstOrNull()
								if (option == null) {
									respond { content = "You did not select an option!" }
								}

								when (option) {
									ModerationActions.BAN.name -> {
										// TODO ban function and functions in said ban function that do the other stuff
									}

									ModerationActions.SOFT_BAN.name -> {
									}

									ModerationActions.KICK.name -> {
									}

									ModerationActions.TIMEOUT.name -> {
									}

									ModerationActions.WARN.name -> {
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

			check {
				// TODO make a function for all the general checks?
				modCommandChecks(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers)
			}

			action {
				// TODO ban function and functions in said ban function that do the other stuff

				isBotOrModerator(arguments.userArgument, "ban") ?: return@action

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				ban(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					sendActionLog = true
					sendDm = arguments.dm
					removeTimeout = true
					deleteMessageDuration = DateTimePeriod(days = arguments.messages)
					this.loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Banned a user"
						description = "${arguments.userArgument.mention} has been banned!"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Days of messages deleted:"
							value = arguments.messages.toString()
							inline = false
						}
					}

					publicActionEmbed {
						title = "Banned a user"
						description = "${arguments.userArgument.mention} has been banned!"
						color = DISCORD_BLACK
					}

					dmEmbed {
						title = "You have been banned from ${guild?.asGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}"
					}
				}

				respond {
					content = "Banned a user"
				}
			}
		}

		ephemeralSlashCommand(::SoftBanArgs) {
			name = "soft-ban"
			description = "Soft-bans a user."

			check {
				modCommandChecks(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers)
			}

			action {
				isBotOrModerator(arguments.userArgument, "ban") ?: return@action

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages != null && (arguments.messages!! > 7 || arguments.messages!! < 0)) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				softban(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					sendActionLog = true
					sendDm = arguments.dm
					removeTimeout = true
					if (arguments.messages != null) deleteMessageDuration = DateTimePeriod(days = arguments.messages!!)
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Soft-Banned a user"
						description = "${arguments.userArgument.mention} has been soft-banned!"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Days of messages deleted"
							value = arguments.messages.toString()
							inline = false
						}
					}

					publicActionEmbed {
						title = "Soft-Banned a user"
						description = "${arguments.userArgument.mention} has been soft-banned!"
					}

					dmEmbed {
						title = "You have been soft-banned from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}\n\n" +
								"You are free to rejoin without the need to be unbanned"
					}
				}

				respond {
					content = "Soft-banned user"
				}
			}
		}

		ephemeralSlashCommand(::UnbanArgs) {
			name = "unban"
			description = "Unbans a user."

			check {
				modCommandChecks(Permission.BanMembers)
				requireBotPermissions(Permission.BanMembers)
			}

			action {
				unban(arguments.userArgument) {
					reason = arguments.reason
					sendActionLog = true
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)

					actionEmbed {
						title = "Unbanned a user"
						description = "${arguments.userArgument.mention} has been unbanned!\n${
							arguments.userArgument.id
						} (${arguments.userArgument.tag})"
						field {
							name = "Reason:"
							value = arguments.reason
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
				}

				respond {
					content = "Unbanned user."
				}
			}
		}

		ephemeralSlashCommand(::KickArgs) {
			name = "kick"
			description = "Kicks a user."

			check {
				modCommandChecks(Permission.KickMembers)
				requireBotPermissions(Permission.KickMembers)
			}

			action {
				isBotOrModerator(arguments.userArgument, "kick") ?: return@action

				kick(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					sendActionLog = true
					sendDm = arguments.dm
					removeTimeout = true
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)

					actionEmbed {
						title = "Kicked a user"
						description = "${arguments.userArgument.mention} has been kicked!"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
					}

					publicActionEmbed {
						title = "Kicked a user"
						description = "${arguments.userArgument.mention} has been kicked!"
					}

					dmEmbed {
						title = "You have been kicked from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}"
					}
				}

				respond {
					content = "Kicked user."
				}
			}
		}

		ephemeralSlashCommand(::ClearArgs) {
			name = "clear"
			description = "Clears messages from a channel."

			check {
				modCommandChecks(Permission.ManageMessages)
				requireBotPermissions(Permission.ManageMessages)
				botHasChannelPerms(Permissions(Permission.ManageMessages))
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val messageAmount = arguments.messages
				val textChannel = channel.asChannelOf<GuildMessageChannel>()

				// Get the specified amount of messages into an array list of Snowflakes and delete them
				val messages = channel.withStrategy(EntitySupplyStrategy.rest).getMessagesBefore(
					Snowflake.max, min(messageAmount, 100)
				).map { it.id }.toList()

				textChannel.bulkDelete(messages)

				respond {
					content = "Messages cleared."
				}

				if (config.publicLogging != null && config.publicLogging == true) {
					channel.createEmbed {
						title = "$messageAmount messages have been cleared."
						color = DISCORD_BLACK
					}
				}

				val actionLog = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
				actionLog.createEmbed {
					title = "$messageAmount messages have been cleared."
					description = "Action occurred in ${textChannel.mention}"
					footer {
						text = user.asUser().tag
						icon = user.asUser().avatar?.url
					}
					color = DISCORD_BLACK
				}
			}
		}

		ephemeralSlashCommand(::TimeoutArgs) {
			name = "timeout"
			description = "Times out a user."

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.UTC)

				isBotOrModerator(arguments.userArgument, "timeout") ?: return@action

				timeout(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					timeoutDuration = duration
					sendDm = arguments.dm
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Timeout"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Duration:"
							value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
								.replace("PT", "") + ")"
							inline = false
						}
					}
					publicActionEmbed {
						title = "Timeout"
						description = "${arguments.userArgument.mention} was timed out by a moderator"
						color = DISCORD_BLACK
						field {
							name = "Duration:"
							value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
								.replace("PT", "") + ")"
							inline = false
						}
					}
					dmEmbed {
						title = "You have been timed out in ${guild?.fetchGuild()?.name}"
						description = "**Duration:**\n${
							duration.toDiscord(TimestampType.Default) + "(" + arguments.duration.toString()
								.replace("PT", "") + ")"
						}\n**Reason:**\n${arguments.reason}"
					}
				}

				respond {
					content = "Timed-out user."
				}
			}
		}

		ephemeralSlashCommand(::RemoveTimeoutArgs) {
			name = "remove-timeout"
			description = "Removes a timeout from a user"

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				removeTimeout(arguments.userArgument) {
					sendDm = arguments.dm
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Timeout Removed"
						dmNotificationStatusEmbedField(dmResult)
						field {
							name = "User:"
							value = "${arguments.userArgument.tag} \n${arguments.userArgument.id}"
							inline = false
						}
						footer {
							text = "Requested by ${user.asUser().tag}"
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_BLACK
					}
					dmEmbed {
						title = "Timeout removed in ${guild!!.asGuild().name}"
						description = "Your timeout has been manually removed in this guild."
					}
				}

				respond {
					content = "Removed timeout from user."
				}
			}
		}

		ephemeralSlashCommand(::WarnArgs) {
			name = "warn"
			description = "Warns a user."

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
				val config = ModerationConfigCollection().getConfig(guild!!.id)!!
				val actionLog = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
				val guildName = guild?.asGuild()?.name

				isBotOrModerator(arguments.userArgument, "warn")

				WarnCollection().setWarn(arguments.userArgument.id, guild!!.id, false)
				val strikes = WarnCollection().getWarn(arguments.userArgument.id, guild!!.id)?.strikes

				respond {
					content = "Warned user."
				}

				var dm: Message? = null

				when (strikes) {
					1 -> if (arguments.dm) {
						dm = arguments.userArgument.dm {
							embed {
								title = "1st warning in $guildName"
								description = "**Reason:** ${arguments.reason}\n\n" +
										"No moderation action has been take. $warnSuffix"
							}
						}
					}

					2 -> {
						if (arguments.dm) {
							dm = arguments.userArgument.dm {
								embed {
									title = "2nd warning in $guildName"
									description = "**Reason:** ${arguments.reason}\n\n" +
											"You have been timed out for 3 hours. $warnSuffix"
								}
							}
						}

						guild?.getMemberOrNull(arguments.userArgument.id)?.edit {
							timeoutUntil = Clock.System.now().plus(Duration.parse("PT3H"))
						}
					}

					3 -> {
						if (arguments.dm) {
							dm = arguments.userArgument.dm {
								embed {
									title = "3rd warning in $guildName"
									description = "**Reason:** ${arguments.reason}\n\n" +
											"You have been timed-out for 12 hours. $warnSuffix"
									color = DISCORD_RED
								}
							}
						}

						guild?.getMemberOrNull(arguments.userArgument.id)?.edit {
							timeoutUntil = Clock.System.now().plus(Duration.parse("PT12H"))
						}
					}

					else -> {
						if (arguments.dm) {
							dm = arguments.userArgument.dm {
								embed {
									title = "Warning $strikes in $guildName"
									description = "**Reason:** ${arguments.reason}\n\n" +
											"You have been timed-out for 3 days. $warnSuffix"
								}
							}
						}

						guild?.getMemberOrNull(arguments.userArgument.id)?.edit {
							timeoutUntil = Clock.System.now().plus(Duration.parse("PT3D"))
						}
					}
				}

				val dmResult = if (arguments.dm && dm != null) {
					DmResult.DM_SUCCESS
				} else if (arguments.dm && dm == null) {
					DmResult.DM_FAIL
				} else {
					DmResult.DM_NOT_SENT
				}

				actionLog.createMessage {
					embed {
						title = "Warning"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Total strikes"
							value = strikes.toString()
						}
					}
					embed {
						warnTimeoutLog(strikes!!, user.asUser(), arguments.userArgument, arguments.reason)
					}
				}

				if (config.publicLogging != null && config.publicLogging == true) {
					channel.createEmbed {
						title = "Warning"
						description = "${arguments.userArgument.mention} has been warned by a moderator"
						color = DISCORD_BLACK
					}
				}
			}
		}

		ephemeralSlashCommand(::RemoveWarnArgs) {
			name = "remove-warn"
			description = "Removes a user's warnings"

			check {
				modCommandChecks(Permission.ModerateMembers)
				requireBotPermissions(Permission.ModerateMembers)
			}

			action {
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

				var dm: Message? = null
				if (arguments.dm) {
					dm = targetUser.dm {
						embed {
							title = "Warn strike removal in ${guild?.fetchGuild()?.name}"
							description = "You have had a warn strike removed. You now have $userStrikes strikes."
							color = DISCORD_GREEN
						}
					}
				}

				val dmResult = if (arguments.dm && dm != null) {
					DmResult.DM_SUCCESS
				} else if (arguments.dm && dm == null) {
					DmResult.DM_FAIL
				} else {
					DmResult.DM_NOT_SENT
				}

				val actionLog = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
				actionLog.createEmbed {
					title = "Warning Removal"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()
					baseModerationEmbed(null, targetUser, user)
					dmNotificationStatusEmbedField(dmResult)
					field {
						name = "Total Strikes:"
						value = userStrikes.toString()
						inline = false
					}
				}
			}
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
			name = "messages"
			description = "Messages"
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
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the ban. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class SoftBanArgs : Arguments() {
		/** The user to soft-ban. */
		val userArgument by user {
			name = "user"
			description = "Person to Soft ban"
		}

		/** The number of days worth of messages to delete, defaults to 3 days. */
		val messages by optionalInt {
			name = "messages"
			description = "Messages"
		}

		/** The reason for the soft-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the soft-ban. */
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
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class ClearArgs : Arguments() {
		/** The number of messages the user wants to remove. */
		val messages by int {
			name = "messages"
			description = "Number of messages to delete"
		}
	}

	inner class TimeoutArgs : Arguments() {
		/** The requested user to timeout. */
		val userArgument by user {
			name = "user"
			description = "Person to timeout"
		}

		/** The time the timeout should last for. */
		val duration by coalescingDefaultingDuration {
			name = "duration"
			description = "Duration of timeout"
			defaultValue = DateTimePeriod(0, 0, 0, 6, 0, 0, 0)
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
			description = "Whether to send a direct message to the user about the warn"
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
			description = "Reason for warn"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
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
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}
	}
}

/**
 * Performs the common checks for a command.
 *
 * @param actionPermission The permission to check the user has.
 * @author NoComment1105
 * @since 4.4.0
 */
private suspend fun CheckContextWithCache<*>.modCommandChecks(actionPermission: Permission) {
	anyGuild()
	requiredConfigs(ConfigOptions.MODERATION_ENABLED)
	hasPermission(actionPermission)
}

private fun EmbedBuilder.warnTimeoutLog(timeoutNumber: Int, moderator: User, targetUser: User, reason: String) {
	when (timeoutNumber) {
		1 -> {}
		2 ->
		    description = "${targetUser.mention} has been timed-out for 3 hours due to 2 warn strikes\n" +
				"${targetUser.id} (${targetUser.tag})\nReason: $reason"

		3 ->
		    description = "${targetUser.mention} has been timed-out for 12 hours due to 3 warn strikes\n" +
				"${targetUser.id} (${targetUser.tag}\nReason: $reason"

		else ->
		    description = "${targetUser.mention} has been timed-out for 3 days due to $targetUser warn " +
				"strike\n${targetUser.id} (${targetUser.tag})\nIt might be time to consider other " +
				"action. Reason: $reason"
	}

	if (timeoutNumber != 1) {
		title = "Timeout"
		footer {
			text = moderator.tag
			icon = moderator.avatar?.url
		}
		color = DISCORD_BLACK
	}
}
