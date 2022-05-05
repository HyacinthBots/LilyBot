package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.ONLINE_STATUS_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import net.irisshaders.lilybot.utils.userDMEmbed

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
		 * Online notification, that is printed to the official [TEST_GUILD_ID]'s [ONLINE_STATUS_CHANNEL].
		 * @author IMS212
		 * @since v2.0
		 */
		// The channel specifically for sending online notifications to
		val onlineLog = kord.getGuild(TEST_GUILD_ID)?.getChannel(ONLINE_STATUS_CHANNEL) as GuildMessageChannelBehavior
		responseEmbedInChannel(
			onlineLog, "Lily is now online!", null, DISCORD_GREEN, null
		)

		/**
		 * This function is called to remove any threads in the database that haven't had a message sent in the last
		 * week. It only runs on startup.
		 * @author tempest15
		 * @since 3.2.0
		 */
		DatabaseHelper.cleanupThreadData(kord)

		/**
		 * This function is called to remove any guilds in the database that haven't had Lily in them for more than
		 * a month. It only runs on startup
		 *
		 * @author NoComment1105
		 * @since 3.2.0
		 */
		DatabaseHelper.cleanupGuildData()

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
							name = "Your Ping with Lily is:"
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

				check { anyGuild() }
				check { configPresent() }

				action {
					val config = DatabaseHelper.getConfig(guild!!.id)!!
					val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior

					val requester = user.asUser()
					val requesterAsMember = requester.asMember(guild!!.id)
					// Declare the embed outside the action to allow us to reference it inside the action
					var actionLogEmbed: Message? = null

					respond { content = "Nickname request sent!" }

					actionLogEmbed = actionLog.createEmbed {
						color = DISCORD_YELLOW
						title = "Nickname Request"
						timestamp = Clock.System.now()

						field {
							name = "User:"
							value = "${requester.mention}\n${requester.asUser().tag}\n${requester.id}"
							inline = false
						}

						field {
							name = "Current Nickname:"
							value = "`${requesterAsMember.nickname}`"
							inline = false
						}

						field {
							name = "Requested Nickname:"
							value = "`${arguments.newNick}`"
							inline = false
						}
					}.edit {
						components {
							ephemeralButton(row = 0) {
								label = "Accept"
								style = ButtonStyle.Success

								action {
									requesterAsMember.edit { nickname = arguments.newNick }

									userDMEmbed(
										requester.asUser(),
										"Nickname Change Accepted in ${guild!!.asGuild().name}",
										"Nickname updated from `${requesterAsMember.nickname}` to " +
												"`${arguments.newNick}`",
										DISCORD_GREEN
									)

									actionLogEmbed!!.edit {
										components { removeAll() }

										embed {
											color = DISCORD_GREEN
											title = "Nickname Request Accepted"

											field {
												name = "User:"
												value = "${requester.mention}\n${requester.asUser().tag}\n" +
														"${requester.id}"
												inline = false
											}

											// these two fields should be the same and exist as a sanity check
											field {
												name = "Previous Nickname:"
												value = "`${requesterAsMember.nickname}`"
												inline = false
											}

											field {
												name = "Accepted Nickname:"
												value = "`${arguments.newNick}`"
												inline = false
											}

											footer {
												text = "Nickname accepted by ${user.asUser().tag}"
												icon = user.asUser().avatar?.url
											}

											timestamp = Clock.System.now()
										}
									}
								}
							}

							ephemeralButton(row = 0) {
								label = "Deny"
								style = ButtonStyle.Danger

								// Declare the reason outside the action to allow us to reference it in the action
								var reason: String? = null

								action {
									actionLogEmbed!!.edit {
										components {
											removeAll()

											ephemeralSelectMenu(row = 1) {
												placeholder = "Why are you denying this nickname?"

												option("Inappropriate", "inappropriate") {
													description = "This nickname is inappropriate"
												}
												option("Impersonates Others", "impersonation") {
													description = "This nickname impersonates someone"
												}
												option("Hoisting", "hoisting") {
													description = "This nickname deliberately hoists the user"
												}

												action {
													when (this.selected[0]) {
														"inappropriate" -> reason = "is inappropriate for this server."
														"impersonation" -> reason = "impersonates another user."
														"hoisting" ->
														    reason = "deliberately hoists you up the user " +
																"ladder, which is not allowed."
													}

													userDMEmbed(
														requester.asUser(),
														"Nickname Change Denied in ${guild!!.asGuild().name}",
														"Staff have reviewed your nickname request (" +
																"`${arguments.newNick}`) and rejected it," +
																" because it $reason",
														DISCORD_RED
													)

													actionLogEmbed!!.edit {
														components { removeAll() }
														embed {
															color = DISCORD_RED
															title = "Nickname Request Denied"

															field {
																name = "User:"
																value = "${requester.mention}\n" +
																		"${requester.asUser().tag}\n${requester.id}"
																inline = false
															}

															field {
																name = "Current Nickname:"
																value = "`${requesterAsMember.nickname}`"
																inline = false
															}

															field {
																name = "Rejected Nickname:"
																value = "`${arguments.newNick}`"
																inline = false
															}

															field {
																name = "Reason:"
																value = selected[0]
																inline = false
															}

															footer {
																text = "Nickname denied by ${user.asUser().tag}"
																icon = user.asUser().avatar?.url
															}

															timestamp = Clock.System.now()
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
			}

			ephemeralSubCommand {
				name = "clear"
				description = "Clear your current nickname"

				check { anyGuild() }
				check { configPresent() }

				action {
					val config = DatabaseHelper.getConfig(guild!!.id)!!
					val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior

					// Check the user has a nickname to clear, avoiding errors and useless action-log notifications
					if (user.fetchMember(guild!!.id).nickname == null) {
						respond { content = "You have no nickname to clear!" }
						return@action
					}

					respond { content = "Nickname cleared" }

					actionLog.createEmbed {
						title = "Nickname Cleared"
						color = DISCORD_YELLOW
						timestamp = Clock.System.now()

						field {
							name = "User:"
							value = "${user.mention}\n${user.asUser().tag}\n${user.id}"
							inline = false
						}

						field {
							name = "New Nickname:"
							value = "Nickname changed from `${user.asMember(guild!!.id).nickname}` to `null`"
							inline = false
						}
					}
					user.asMember(guild!!.id).edit { nickname = null }
				}
			}
		}
	}

	inner class NickRequestArgs : Arguments() {
		/** The new nickname that the command user requested. */
		val newNick by string {
			name = "nickname"
			description = "The new nickname you would like"
		}
	}
}
