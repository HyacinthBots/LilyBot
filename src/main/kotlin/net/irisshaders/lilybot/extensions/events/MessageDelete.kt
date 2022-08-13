package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageDeleteEvent
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
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
		event<ProxiedMessageDeleteEvent> {
			check {
				anyGuild()
				configPresent()
				failIf {
					event.message?.author?.id == kord.selfId ||
					!event.message?.author?.isBot!!
				}
			}

			action {
				val config = DatabaseHelper.getConfig(event.getGuild().id) ?: return@action

				var messageLog: GuildMessageChannel? = null
				try {
					messageLog = kord.getChannelOf(config.messageLogs)
				} catch (e: EntityNotFoundException) {
					DatabaseHelper.clearConfig(event.getGuild().id) // Clear the config to make the user fix it
				}

				val originalMessage = event.message
				val proxiedMessage = event.pkMessage
				val messageContent = if (originalMessage?.asMessageOrNull() != null) {
					if (originalMessage.asMessageOrNull().content.length > 1024) {
						originalMessage.asMessageOrNull().content.substring(0, 1024) + "..."
					} else {
						originalMessage.asMessageOrNull().content
					}
				} else {
					null
				}
				val messageLocation = event.pkMessage.channel
				val attachments = event.message?.attachments
				val images: MutableSet<Attachment> = mutableSetOf()
				attachments?.forEach { if (it.isImage) images += it }

				messageLog?.createMessage {
					embed {
						color = DISCORD_PINK
						author {
							name = "Message deleted"
							icon = proxiedMessage.member.avatarUrl
						}
						description =
							"Location: <#${event.getGuild().getChannelOf<GuildMessageChannel>(messageLocation)}"
						timestamp = Clock.System.now()

						field {
							name = "Message contents"
							value =
								if (messageContent.isNullOrEmpty()) {
									"Failed to retrieve message contents"
								} else {
									messageContent
								}
							inline = false
						}
						if (!attachments.isNullOrEmpty()) {
							val attachmentUrls = StringBuilder()
							attachments.forEach {
								attachmentUrls.append(
									"https://media.discordapp.net/attachments/$messageLocation/" +
											"${proxiedMessage.id}/${it.filename}\n"
								)
							}
							field {
								name = "Attachments"
								value = attachmentUrls.trim().toString()
								inline = false
							}
						}

						if (images.isNotEmpty()) {
							image = images.first().url
						}

						field {
							name = "Message Author:"
							value = "${proxiedMessage.member.name}\n" +
									"(${event.getGuild().getMember(proxiedMessage.sender).tag}\n" +
									"${event.getGuild().getMember(proxiedMessage.sender).mention})"
							inline = true
						}

						field {
							name = "Author ID:"
							value = proxiedMessage.sender.toString()
						}
					}
				}
			}
		}
	}
}
