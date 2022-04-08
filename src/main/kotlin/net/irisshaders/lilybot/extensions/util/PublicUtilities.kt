@file:OptIn(DoNotChain::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.annotations.DoNotChain
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.setNickname
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ONLINE_STATUS_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.clearComponents
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

		// TODO: Permission stuff? Accept returns a permission error
		// FIXME: This code is cursed, clean it up a little
		ephemeralSlashCommand(::NickRequestArgs) {
			name = "request-nickname"
			description = "This command allows you to request a new nickname for the server."

			action {
				val actionLogId = getConfigPublicResponse("modActionLog") ?: return@action
				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val requester = user.asUser()
				var responseEmbed: Message? = null

				respond { content = "Nickname request sent!" }

				responseEmbed = actionLog.createEmbed {
					color = DISCORD_YELLOW
					title = "Nickname Request"
					timestamp = Clock.System.now()

					field {
						name = "User:"
						value = "${user.mention}\n${user.asUser().tag}\n${user.id}"
						inline = false
					}
					field {
						name = "Requested Nickname:"
						value = "${user.asMember(guild!!.id).nickname} -> ${arguments.newNick}"
						inline = false
					}
				}.edit {
					components {
						ephemeralButton(row = 0) {
							label = "Accept Nickname"
							style = ButtonStyle.Success

							action {
								user.asMember(guild!!.id).setNickname(arguments.newNick)

								userDMEmbed(
									user.asUser(),
									"Nickname Change accepted",
									null,
									DISCORD_GREEN
								)
								clearComponents(responseEmbed)

								actionLog.createMessage("Nickname Accepted")
							}
						}
						ephemeralButton(row = 0) {
							label = "Deny Nickname"
							style = ButtonStyle.Danger

							var reason: String? = null
							var reasonEmbed: Message? = null

							action {
								reasonEmbed = actionLog.createEmbed {
									description = "Why are you denying this username?"
								}.edit {
									components {
										ephemeralSelectMenu(row = 1) {
											placeholder = "Select reason"
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
														reason = "This nickname is inappropriate for this server."
													}
													"impersonation" -> {
														reason = "This nickname impersonates another user."
													}
													"hoisting" -> {
														reason =
															"This nickname deliberately hoists you up the user ladder. " +
																	"Which is not allowed."
													}
												}

												userDMEmbed(
													requester.asUser(),
													"Nickname Change Denied",
													"Staff have review your nickname request and decided to reject it " +
															"for reason:\n\n${reason} Please choose another nickname",
													DISCORD_RED
												)

												clearComponents(responseEmbed)
												clearComponents(reasonEmbed)
												reasonEmbed!!.delete()

												actionLog.createMessage("Nickname Denied. Reason:\n$reason")
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

	inner class NickRequestArgs : Arguments() {
		val newNick by string {
			name = "newNick"
			description = "The new nickname you would like"
		}
	}
}
