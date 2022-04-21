package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.hasPermission
import dev.kord.common.entity.Permission
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.getConfigPrivateResponse
import net.irisshaders.lilybot.utils.responseEmbedInChannel

class ModUtilities : Extension() {
	override val name = "mod-utilities"

	override suspend fun setup() {

		/**
		 * Say Command
		 * @author NoComment1105
		 */
		ephemeralSlashCommand(::SayArgs) {
			name = "say"
			description = "Say something through Lily."

			action {
				if (guild != null) {
					if (!user.asMember(guild!!.id).hasPermission(Permission.ModerateMembers)) {
						respond { content = "**Error:** You do not have the `Moderate Members` permission" }
						return@action
					}
					val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

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

					actionLog.createEmbed {
						title = "Say command used"
						description = "`${arguments.messageArgument}`"
						color = DISCORD_BLACK
						timestamp = Clock.System.now()
						field {
							name = "Channel:"
							value = targetChannel.mention
							inline = true
						}
						field {
							name = "Type:"
							value = if (arguments.embedMessage) "Embed" else "Message"
							inline = true
						}
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
					}
				} else {
					if (arguments.embedMessage) {
						channel.createEmbed {
							color = DISCORD_BLURPLE
							description = arguments.messageArgument
							timestamp = Clock.System.now()
						}
					} else {
						channel.createMessage {
							content = arguments.messageArgument
						}
					}
					respond { content = "Message sent!" }
				}
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

				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior

				this@ephemeralSlashCommand.kord.editPresence {
					status = PresenceStatus.Online
					playing(arguments.presenceArgument)
				}

				DatabaseHelper.setStatus(arguments.presenceArgument)

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
			description = "The text of the message to be sent"
		}
		val targetChannel by optionalChannel {
			name = "channel"
			description = "The channel the message should be sent in"
		}
		val embedMessage by defaultingBoolean {
			name = "embed"
			description = "If the message should be sent as an embed"
			defaultValue = false
		}
	}

	inner class PresenceArgs : Arguments() {
		val presenceArgument by string {
			name = "presence"
			description = "The new value Lily's presence should be set to"
		}
	}
}
