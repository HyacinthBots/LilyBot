@file:OptIn(ExperimentalTime::class)


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
import kotlin.time.ExperimentalTime

class MessageDelete : Extension() {
	override val name = "message-delete"

	override suspend fun setup() {
		/**
		 * Logs deleted messages in a guild to the message log channel designated in the config for that guild
		 * @author NoComment1105
		 */
		event<MessageDeleteEvent> {
			// Don't try to create if the message is in DMs
			check { anyGuild() }
			action {
				if (event.message?.author?.isBot == true || event.message?.author?.id == kord.selfId) return@action

				// Try to get the message logs channel, return@action if null
				val messageLogId = DatabaseHelper.getConfig(event.guild!!.id, "messageLogs") ?: return@action

				val guild = kord.getGuild(event.guildId!!)
				val messageLogChannel = guild?.getChannel(messageLogId) as GuildMessageChannelBehavior
				val messageContent = event.message?.asMessageOrNull()?.content.toString()
				val eventMessage = event.message
				val messageLocation = event.channel.id.value

				messageLogChannel.createEmbed {
					color = DISCORD_PINK
					title = "Message Deleted"
					description = "Location: <#$messageLocation>"
					timestamp = Clock.System.now()

					field {
						name = "Message Contents:"
						value = messageContent.ifEmpty { "Failed to get content of message" }
						inline = false
					}
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
