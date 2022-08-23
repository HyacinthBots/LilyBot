package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.api.pluralkit.PK_API_DELAY
import net.irisshaders.lilybot.api.pluralkit.PluralKit
import net.irisshaders.lilybot.database.collections.LoggingConfigCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.configPresent

/**
 * The class for logging deletion of messages to the guild message log.
 *
 * @since 2.0
 */
class MessageDelete : Extension() {
	override val name = "message-delete"

	override suspend fun setup() {
		/**
		 * Logs deleted messages in a guild to the message log channel designated in the config for that guild
		 * @author NoComment1105
		 * @since 2.0
		 */
		event<MessageDeleteEvent> {
			check {
				anyGuild()
				configPresent(ConfigType.LOGGING)
				failIf {
					event.message?.author?.id == kord.selfId ||
					!event.message?.author?.isBot!!
				}
			}

			action {
				val config = LoggingConfigCollection().getConfig(event.getGuild()!!.id) ?: return@action
				delay(PK_API_DELAY) // Allow the PK API to catch up
				if (event.message?.author?.isBot == true) return@action
				val eventMessage = event.message ?: return@action
				if (PluralKit.containsPkChatCommandPrefix(eventMessage)) return@action

				var messageLog: GuildMessageChannel? = null
				try {
					messageLog = kord.getChannelOf(config.messageChannel!!)
				} catch (e: EntityNotFoundException) {
					LoggingConfigCollection().clearConfig(event.getGuild()!!.id) // Clear the config to make the user fix it
				}

				val messageContent = if (eventMessage.asMessageOrNull().content.length > 1024) {
					eventMessage.asMessageOrNull().content.substring(0, 1020) + " ..."
				} else {
					eventMessage.asMessageOrNull().content
				}
				val messageLocation = event.channel.id.value

				// Avoid logging messages proxied by PluralKit, since these messages aren't "actually deleted"
				if (PluralKit.isProxied(eventMessage.id)) {
					return@action
				}

				messageLog?.createEmbed {
					color = DISCORD_PINK
					author {
						name = "Message Deleted"
						icon = eventMessage.author?.avatar?.url
					}
					description = "Location: <#$messageLocation>"
					timestamp = Clock.System.now()

					field {
						name = "Message Contents:"
						value = messageContent.ifEmpty { "Failed to get content of message" }
						inline = false
					}
					// If the message has an attachment, add the link to it to the embed
					if (eventMessage.attachments.isNotEmpty()) {
						val attachmentUrls = StringBuilder()
						for (attachment in eventMessage.attachments) {
							attachmentUrls.append(
								"https://media.discordapp.net/attachments/" +
										"${attachment.data.url.split("/")[4]}/" + // Get the channel
										"${attachment.data.url.split("/")[5]}/" + // Get the message ID
										attachment.data.filename + "\n"
							)
						}
						field {
							name = "Attachments:"
							value = attachmentUrls.trim().toString()
							inline = false
						}
					}
					field {
						name = "Message Author:"
						value = "${eventMessage.author?.mention ?: "Failed to get author of message"} " +
								"(${eventMessage.author?.tag ?: "Failed to get author tag"})"
						inline = true
					}
					field {
						name = "Author ID:"
						value = eventMessage.author?.id.toString()
						inline = true
					}
				}
			}
		}
	}
}
