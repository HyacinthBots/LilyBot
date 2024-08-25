package org.hyacinthbots.lilybot.extensions.utils.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.guildFor
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.utils.dm
import dev.kordex.core.utils.getTopRole
import dev.kordex.core.utils.hasPermission
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.requiredConfigs

/**
 * This class contains a few utility commands that can be used by the public in guilds, or that are often seen by the
 * public.
 *
 * @since 3.1.0
 */
class PublicUtilities : Extension() {
	override val name = "public-utilities"

	override suspend fun setup() {
		/**
		 * Ping Command.
		 * @author IMS212
		 * @since 2.0
		 */
		publicSlashCommand {
			name = "ping"
			description = "Am I alive?"

			action {
				val averagePing = this@PublicUtilities.kord.gateway.averagePing

				respond {
					embed {
						color = DISCORD_YELLOW
						title = "Pong!"

						timestamp = Clock.System.now()

						field {
							name = "Lily's Ping to Discord is:"
							value = "**$averagePing**"
							inline = true
						}
					}
				}
			}
		}

		/**
		 * Nickname request command
		 * @author NoComment1105
		 * @since 3.1.0
		 */
		ephemeralSlashCommand {
			name = "nickname"
			description = "The parent command for all nickname commands"

			ephemeralSubCommand(::NickRequestArgs) {
				name = "request"
				description = "Request a new nickname for the server!"

				check {
					anyGuild()
					requiredConfigs(ConfigOptions.UTILITY_LOG)
				}

				action {
					val config = UtilityConfigCollection().getConfig(guildFor(event)!!.id)!!
					val utilityLog = guild?.getChannelOfOrNull<GuildMessageChannel>(config.utilityLogChannel!!)

					val requester = user.asUserOrNull()
					val requesterAsMember = requester?.asMemberOrNull(guild!!.id)
					val self = this@PublicUtilities.kord.getSelf().asMemberOrNull(guild!!.id)

					if (requesterAsMember?.getTopRole()?.getPosition() != null &&
						self?.getTopRole()?.getPosition() == null
					) {
						respond {
							content = "You have a role and Lily does not, so she cannot change your nickname."
						}
						return@action
					} else if ((requesterAsMember?.getTopRole()?.getPosition() ?: 0) >
						(self?.getTopRole()?.getPosition() ?: 0)
					) {
						respond {
							content = "Your highest role is above Lily's, so she cannot change your nickname."
						}
						return@action
					}

					if (requesterAsMember?.hasPermission(Permission.ChangeNickname) == true) {
						requesterAsMember.edit { nickname = arguments.newNick }
						respond {
							content = "You have permission to change your own nickname, so I've just made the change."
						}
						return@action
					}

					// Declare the embed outside the action to allow us to reference it inside the action
					var actionLogEmbed: Message? = null

					respond { content = "Nickname request sent!" }

					try {
						actionLogEmbed =
							utilityLog?.createMessage {
								embed {
									color = DISCORD_YELLOW
									title = "Nickname Request"
									timestamp = Clock.System.now()

									field {
										name = "User:"
										value =
											"${requester?.mention}\n${requester?.asUserOrNull()?.username}\n${requester?.id}"
										inline = false
									}

									field {
										name = "Current Nickname:"
										value = "`${requesterAsMember?.nickname}`"
										inline = false
									}

									field {
										name = "Requested Nickname:"
										value = "`${arguments.newNick}`"
										inline = false
									}
								}
								components {
									ephemeralButton(row = 0) {
										label = "Accept"
										style = ButtonStyle.Success

										action button@{
											if (requesterAsMember?.getTopRole()?.getPosition() != null &&
												self?.getTopRole()?.getPosition() == null
											) {
												respond {
													content = "This user has a role and Lily does not, " +
															"so she cannot change their nickname. " +
															"Please fix Lily's permissions and try again"
												}
												return@button
											} else if ((requesterAsMember?.getTopRole()?.getPosition() ?: 0) >
												(self?.getTopRole()?.getPosition() ?: 0)
											) {
												respond {
													content = "This user's highest role is above Lily's, " +
															"so she cannot change their nickname. " +
															"Please fix Lily's permissions and try again."
												}
												return@button
											}

											requesterAsMember?.edit { nickname = arguments.newNick }

											requester?.dm {
												embed {
													title =
														"Nickname Change Accepted in ${guild!!.asGuildOrNull()?.name}"
													description =
														"Nickname updated from `${requesterAsMember?.nickname}` to " +
																"`${arguments.newNick}`"
													color = DISCORD_GREEN
												}
											}

											actionLogEmbed!!.edit {
												components { removeAll() }

												embed {
													color = DISCORD_GREEN
													title = "Nickname Request Accepted"

													field {
														name = "User:"
														value =
															"${requester?.mention}\n${requester?.asUserOrNull()?.username}\n" +
																	"${requester?.id}"
														inline = false
													}

													// these two fields should be the same and exist as a sanity check
													field {
														name = "Previous Nickname:"
														value = "`${requesterAsMember?.nickname}`"
														inline = false
													}

													field {
														name = "Accepted Nickname:"
														value = "`${arguments.newNick}`"
														inline = false
													}

													footer {
														text = "Nickname accepted by ${user.asUserOrNull()?.username}"
														icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
													}

													timestamp = Clock.System.now()
												}
											}
										}
									}

									ephemeralButton(row = 0) {
										label = "Deny"
										style = ButtonStyle.Danger

										action {
											requester?.dm {
												embed {
													title = "Nickname Request Denied"
													description = "Moderators have reviewed your nickname request (`${
														arguments.newNick
													}`) and rejected it.\nPlease try a different nickname"
												}
											}

											actionLogEmbed!!.edit {
												components { removeAll() }
												embed {
													title = "Nickname Request Denied"

													field {
														name = "User:"
														value = "${requester?.mention}\n" +
																"${requester?.asUserOrNull()?.username}\n${requester?.id}"
														inline = false
													}

													field {
														name = "Current Nickname:"
														value = "`${requesterAsMember?.nickname}`"
														inline = false
													}

													field {
														name = "Rejected Nickname:"
														value = "`${arguments.newNick}`"
														inline = false
													}

													footer {
														text = "Nickname denied by ${user.asUserOrNull()?.username}"
														icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
													}

													timestamp = Clock.System.now()
													color = DISCORD_RED
												}
											}
										}
									}
								}
							}
					} catch (e: KtorRequestException) {
						// Avoid hard failing on permission error, since the public won't know what it means
						respond {
							content = "Error sending message to moderators. Please ask the moderators to check" +
									"the `UTILITY` config."
						}
						return@action
					}
				}
			}

			ephemeralSubCommand {
				name = "clear"
				description = "Clear your current nickname"

				check {
					anyGuild()
					requiredConfigs(ConfigOptions.UTILITY_LOG)
				}

				action {
					val config = UtilityConfigCollection().getConfig(guild!!.id)!!
					val utilityLog = guild?.getChannelOfOrNull<GuildMessageChannel>(config.utilityLogChannel!!)

					// Check the user has a nickname to clear, avoiding errors and useless action-log notifications
					if (user.fetchMember(guild!!.id).nickname == null) {
						respond { content = "You have no nickname to clear!" }
						return@action
					}

					respond { content = "Nickname cleared" }

					try {
						utilityLog?.createEmbed {
							title = "Nickname Cleared"
							color = DISCORD_YELLOW
							timestamp = Clock.System.now()

							field {
								name = "User:"
								value = "${user.mention}\n${user.asUserOrNull()?.username}\n${user.id}"
								inline = false
							}

							field {
								name = "New Nickname:"
								value = "Nickname changed from `${user.asMemberOrNull(guild!!.id)?.nickname}` to `null`"
								inline = false
							}
						}
					} catch (_: KtorRequestException) {
						// Avoid hard failing on permission error, since the public won't know what it means
						respond {
							content = "Error sending message to moderators. Please " +
									"ask the moderators to check the `UTILITY` config."
						}
						return@action
					}
					user.asMemberOrNull(guild!!.id)?.edit { nickname = null }
				}
			}
		}
	}

	inner class NickRequestArgs : Arguments() {
		/** The new nickname that the command user requested. */
		val newNick by string {
			name = "nickname"
			description = "The new nickname you would like"

			minLength = 1
			maxLength = 32
		}
	}
}
