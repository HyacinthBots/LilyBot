@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.ONLINE_STATUS_CHANNEL
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.getFromConfigPrivateResponse
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import kotlin.time.ExperimentalTime

class Utilities : Extension() {
	override val name = "utilities"

	override suspend fun setup() {
		val onlineLog = kord.getGuild(TEST_GUILD_ID)?.getChannel(ONLINE_STATUS_CHANNEL) as GuildMessageChannelBehavior

		/**
		 * Online notification
		 * @author IMS212
		 */
		responseEmbedInChannel(
			onlineLog, "Lily is now online!", null,
			DISCORD_GREEN, null
		)

		/**
		 * Ping Command
		 * @author IMS212
		 * @author NoComment1105
		 */
		publicSlashCommand {  // Public slash commands have public responses
			name = "ping"
			description = "Am I alive?"

			action {
				val averagePing = this@Utilities.kord.gateway.averagePing

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
		 * Say Command
		 * @author NoComment1105
		 */
		ephemeralSlashCommand(::SayArgs) {
			name = "say"
			description = "Say something through Lily."

			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = getFromConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val targetChannel = if (arguments.targetChannel == null) {
					channel
				} else {
					guild?.getChannel(arguments.targetChannel!!.id) as MessageChannelBehavior
				}

				try {
					if (arguments.embedMessage) {
						targetChannel.createEmbed {
							color = DISCORD_BLURPLE
							description = arguments.messageArgument
							timestamp = Clock.System.now()
						}
					} else {
						targetChannel.createMessage {
							content = arguments.messageArgument
						}
					}
				} catch (e: KtorRequestException) {
					respond { content = "Lily does not have permission to send messages in this channel." }
					return@action
				}

				respond { content = "Message sent." }

				responseEmbedInChannel(
					actionLog,
					"Message Sent",
					"/say has been used to " +
							"say ${arguments.messageArgument} in ${targetChannel.mention}",
					DISCORD_BLACK,
					user.asUser()
				)
			}
		}

		/**
		 * Presence Command
		 * @author IMS
		 */
		ephemeralSlashCommand(::PresenceArgs) {
			name = "set-status"
			description = "Set Lily's current presence/status."

			check { hasPermission(Permission.Administrator) }

			action {
				// lock this command to the testing guild
				if (guild?.id != TEST_GUILD_ID) {
					respond { content = "**Error:** This command can only be run in Lily's testing guild." }
					return@action
				}

				val actionLogId = getFromConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior

				this@ephemeralSlashCommand.kord.editPresence {
					status = PresenceStatus.Online
					playing(arguments.presenceArgument)
				}

				DatabaseHelper.putInStatus(arguments.presenceArgument)

				respond { content = "Presence set to `${arguments.presenceArgument}`" }

				responseEmbedInChannel(
					actionLog,
					"Presence Changed",
					"Lily's presence has been set to `${arguments.presenceArgument}`",
					DISCORD_BLACK,
					user.asUser()
				)
			}
		}
	}

	inner class SayArgs : Arguments() {
		val messageArgument by string {
			name = "message"
			description = "Message contents"
		}
		val targetChannel by optionalChannel {
			name = "channel"
			description = "The channel you want to send the message in"
		}
		val embedMessage by defaultingBoolean {
			name = "embed"
			description = "Would you like to send as embed"
			defaultValue = false
		}
	}

	inner class PresenceArgs : Arguments() {
		val presenceArgument by string {
			name = "presence"
			description = "Lily's presence"
		}
	}
}
