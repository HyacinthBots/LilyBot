package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.buttons.EphemeralInteractionButton
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
import net.irisshaders.lilybot.utils.ONLINE_STATUS_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.getConfigPublicResponse
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import net.irisshaders.lilybot.utils.userDMEmbed

class PublicUtilities : Extension() {
	override val name = "public-utilities"


	override suspend fun setup() {
		val onlineLog = kord.getGuild(TEST_GUILD_ID)?.getChannel(ONLINE_STATUS_CHANNEL) as GuildMessageChannelBehavior

		/**
		 * Online notification
		 * @author IMS212
		 */
		responseEmbedInChannel(
			onlineLog, "Lily is now online!", null, DISCORD_GREEN, null
		)

		/**
		 * Ping Command
		 * @author IMS212
		 * @author NoComment1105
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
		 */
		ephemeralSlashCommand {
			name = "nickname"
			description = "Nickname related commands"

			ephemeralSubCommand(::NickRequestArgs) {
				name = "request"
				description = "This command allows you to request a new nickname for the server."

				action {
					val actionLogId = getConfigPublicResponse("modActionLog") ?: return@action
					val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior

					val requester = user.asUser()
					val requesterMember = requester.asMember(guild!!.id)
					var responseEmbed: Message? = null

					var denyButton: EphemeralInteractionButton? = null
					var acceptButton: EphemeralInteractionButton

					respond { content = "Nickname request sent!" }

					responseEmbed = actionLog.createEmbed {
						color = DISCORD_YELLOW
						title = "Nickname Request"
						timestamp = Clock.System.now()

						field {
							name = "User:"
							value = "${requester.mention}\n${requester.asUser().tag}\n${requester.id}"
							inline = false
						}
						field {
							name = "Requested Nickname:"
							value = "${requesterMember.nickname} -> ${arguments.newNick}"
							inline = false
						}
					}.edit {
						components {
							acceptButton = ephemeralButton(row = 0) {
								label = "Accept Nickname"
								style = ButtonStyle.Success

								action {
									requesterMember.edit { nickname = arguments.newNick }

									userDMEmbed(
										requester.asUser(),
										"Nickname Change accepted in ${guild!!.asGuild().name}",
										"Nickname updated from `${requesterMember.nickname}` to " +
												"`${arguments.newNick}`",
										DISCORD_GREEN
									)

									responseEmbed!!.edit { components { removeAll() } }

									responseEmbed!!.edit {
										embed {
											color = DISCORD_GREEN
											title = "Nickname request accepted"
											description = "Nickname accepted by ${user.asUser().mention}"

											field {
												name = "User:"
												value = "${requester.mention}\n${requester.asUser().tag}\n" +
														"${requester.id}"
												inline = false
											}

											field {
												name = "Requested Nickname"
												value = "`${arguments.newNick}`"
												inline = false
											}

											timestamp = Clock.System.now()
										}
									}
								}
							}

							denyButton = ephemeralButton(row = 0) {
								label = "Deny Nickname"
								style = ButtonStyle.Danger

								var reason: String? = null

								action {
									responseEmbed!!.edit {
										embed {
											title = "Why are you denying this nickname?"
										}
										components {
											remove(denyButton!!)
											remove(acceptButton)
											ephemeralSelectMenu(row = 1) {
												placeholder = "Select Reason"
												option(
													label = "Inappropriate Nickname",
													value = "inappropriate"
												) {
													description = "This nickname is deemed inappropriate"
												}
												option(
													label = "Impersonates Others",
													value = "impersonation"
												) {
													description = "This nickname impersonates someone"
												}
												option(
													label = "Hoisting",
													value = "hoisting"
												) {
													description = "This nickname deliberately hoists the user"
												}

												action {
													when (this.selected[0]) {
														"inappropriate" -> {
															reason = "is inappropriate for this server."
														}
														"impersonation" -> {
															reason = "impersonates another user."
														}
														"hoisting" -> {
															reason = "deliberately hoists you up the user ladder, " +
																	"which is not allowed."
														}
													}

													userDMEmbed(
														requester.asUser(),
														"Nickname Change Denied in ${guild!!.asGuild().name}",
														"Staff have review your nickname request (" +
																"`${arguments.newNick}`) and rejected it," +
																" because it $reason",
														DISCORD_RED
													)

													responseEmbed!!.edit { components { removeAll() } }
													//reasonMessage!!.delete()

													responseEmbed!!.edit {
														embed {
															color = DISCORD_RED
															title = "Nickname Request Denied"
															description = "Nickname denied by ${user.asUser().mention}"

															field {
																name = "User:"
																value = "${requester.mention}\n" +
																		"${requester.asUser().tag}\n${requester.id}"
																inline = false
															}

															field {
																name = "Requested Nickname"
																value = "`${arguments.newNick}`"
																inline = false
															}

															field {
																name = "Reason:"
																value = selected[0]
																inline = false
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

				action {
					val actionLogId = getConfigPublicResponse("modActionLog") ?: return@action
					val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior

					if (user.fetchMember(guild!!.id).nickname == null) {
						respond { content = "You have no nickname to clear!" }
						return@action
					}

					respond { content = "Nickname cleared" }

					actionLog.createEmbed {
						title = "Nickname cleared"
						color = DISCORD_YELLOW
						timestamp = Clock.System.now()

						field {
							name = "User:"
							value = "${user.mention}\n${user.asUser().tag}\n${user.id}"
							inline = false
						}
						field {
							name = "Nickname changed from"
							value = "${user.asMember(guild!!.id).nickname} -> null"
							inline = false
						}
					}

					user.asMember(guild!!.id).edit { nickname = null }
				}
			}
		}
	}

	inner class NickRequestArgs : Arguments() {
		val newNick by string {
			name = "newNick"
			description = "The new nickname you would like"
		}
	}
}
