package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageDeleteEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageDeleteEvent
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Attachment
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
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
		event<ProxiedMessageDeleteEvent> {
			check {
				anyGuild()
				configPresent(ConfigType.LOGGING)
				failIf {
					event.message?.author?.id == kord.selfId
				}
			}

			action {
				val config = LoggingConfigCollection().getConfig(event.getGuild().id) ?: return@action
				var messageLog: GuildMessageChannel? = null
				try {
					messageLog = kord.getChannelOf(config.messageChannel!!)
				} catch (e: EntityNotFoundException) {
					LoggingConfigCollection().clearConfig(event.getGuild().id) // Clear the config to make the user fix it
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
							"Location: ${event.getGuild().getChannelOf<GuildMessageChannel>(messageLocation).mention}" +
									"\n${event.getGuild().getChannelOf<GuildMessageChannel>(messageLocation).name}"
						timestamp = Clock.System.now()

						fields(messageContent, attachments)

						field {
							name = "Message Author:"
							value = "System Member: ${proxiedMessage.member.name}\n" +
									"Account: ${event.getGuild().getMember(proxiedMessage.sender).tag}" +
									event.getGuild().getMember(proxiedMessage.sender).mention
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

		event<UnProxiedMessageDeleteEvent> {
			check {
				anyGuild()
				configPresent()
				failIf {
					event.message?.author?.id == kord.selfId ||
					event.message?.author?.isBot == true
				}
			}

			action {
				val config = LoggingConfigCollection().getConfig(event.getGuild().id) ?: return@action

				val message = event.message

				val guild = kord.getGuild(event.guildId!!)!!
				var messageLog: GuildMessageChannel? = null
				try {
					messageLog = guild.getChannelOf(config.messageChannel!!)
				} catch (e: EntityNotFoundException) {
					LoggingConfigCollection().clearConfig(guild.id) // Clear the config to make the user fix it
				}

				val messageContent = if (message?.asMessageOrNull() != null) {
					if (message.asMessageOrNull().content.length > 1024) {
						message.asMessageOrNull().content.substring(0, 1024) + "..."
					} else {
						message.asMessageOrNull().content
					}
				} else {
					null
				}

				message ?: return@action

				val messageLocation = event.channel.asChannelOf<GuildMessageChannel>()
				val attachments = event.message?.attachments
				val images: MutableSet<Attachment> = mutableSetOf()
				attachments?.forEach { if (it.isImage) images += it }

				messageLog?.createMessage {
					embed {
						color = DISCORD_PINK
						author {
							name = "Message deleted"
							icon = message.author?.avatar?.url
						}
						description =
							"Location: ${messageLocation.mention}" +
									"\n${messageLocation.name}"
						timestamp = Clock.System.now()

						fields(messageContent, attachments)

						field {
							name = "Message Author:"
							value = "${message.author?.id ?: "Failed to get author of message"} ${message.author?.mention ?: ""}"
							inline = true
						}

						field {
							name = "Author ID:"
							value = message.author?.id.toString()
						}
					}
				}
			}
		}
	}
}

/**
 * Adds the common fields to a deleted message embed.
 *
 * @param messageContent The content of the message.
 * @param attachments The attachments of the message.
 *
 * @author NoComment1105
 * @since 3.6.0
 */
private fun EmbedBuilder.fields(messageContent: String?, attachments: Set<Attachment>?) {
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
				it.url + "\n"
			)
		}
		field {
			name = "Attachments"
			value = attachmentUrls.trim().toString()
			inline = false
		}
	}
}
