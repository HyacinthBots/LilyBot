package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageDeleteEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper

/**
 * The class for logging deletion of messages to the guild action log.
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
			// Don't try to create if the message is in DMs
			check { anyGuild() }

			action {
				// Fail if the user is a bot
				if (event.message?.author?.isBot == true) return@action

				// Try to get the message logs channel, return@action if null
				val config = DatabaseHelper.getConfig(event.guild!!.id) ?: return@action

				val guild = kord.getGuild(event.guildId!!)
				val messageLog = guild?.getChannel(config.messageLogs) as GuildMessageChannelBehavior
				val eventMessage = event.message
				val messageContent = eventMessage?.asMessageOrNull()?.content.toString()
				val messageLocation = event.channel.id.value

				messageLog.createEmbed {
					color = DISCORD_PINK
					title = "Message Deleted"
					description = "Location: <#$messageLocation>"
					timestamp = Clock.System.now()

					field {
						name = "Message Contents:"
						value = messageContent.ifEmpty { "Failed to get content of message" }
						inline = false
					}
					// If the message has an attachment, add the link to it to the embed
					if (eventMessage?.attachments != null && eventMessage.attachments.isNotEmpty()) {
						val attachmentUrls = StringBuilder()
						for (attachment in eventMessage.attachments) {
							attachmentUrls.append(attachment.data.url + "\n")
						}
						field {
							name = "Attachments:"
							value = attachmentUrls.trim().toString()
							inline = false
						}
					}
					field {
						name = "Message Author:"
						value = eventMessage?.author?.tag.toString()
						inline = true
					}
					field {
						name = "Author ID:"
						value = eventMessage?.author?.id.toString()
						inline = true
					}
				}
			}
		}
	}
}
