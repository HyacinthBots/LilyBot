package org.hyacinthbots.lilybot.extensions.logging.events

import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageBulkDeleteEvent
import dev.kord.rest.builder.message.create.UserMessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_PINK
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.modules.pluralkit.api.PKMessage
import dev.kordex.modules.pluralkit.events.ProxiedMessageDeleteEvent
import dev.kordex.modules.pluralkit.events.UnProxiedMessageDeleteEvent
import io.ktor.client.request.forms.ChannelProvider
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.attachmentsAndProxiedMessageInfo
import org.hyacinthbots.lilybot.utils.generateBulkDeleteFile
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.ifNullOrEmpty
import org.hyacinthbots.lilybot.utils.requiredConfigs
import org.hyacinthbots.lilybot.utils.trimmedContents

/**
 * The class for logging deletion of messages to the guild message log.
 *
 * @since 2.0
 */
class MessageDelete : Extension() {
	override val name = "message-delete"

	override suspend fun setup() {
		/**
		 * Logs proxied deleted messages in a guild to the message log channel designated in the config for that guild
		 * @author NoComment1105
		 * @see onMessageDelete
		 */
		event<ProxiedMessageDeleteEvent> {
			check {
				anyGuild()
				requiredConfigs(ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
				failIf {
					event.message?.author?.id == kord.selfId
				}
			}

			action {
				onMessageDelete(event.getMessageOrNull(), event.pkMessage)
			}
		}

		/**
		 * Logs unproxied deleted messages in a guild to the message log channel designated in the config for that guild.
		 * @author NoComment1105
		 * @see onMessageDelete
		 */
		event<UnProxiedMessageDeleteEvent> {
			check {
				anyGuild()
				requiredConfigs(ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
				failIf {
					event.message?.author?.id == kord.selfId ||
							event.message?.author?.isBot == true
				}
			}

			action {
				onMessageDelete(event.getMessageOrNull(), null)
			}
		}

		event<MessageBulkDeleteEvent> {
			check {
				anyGuild()
				requiredConfigs(ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
			}

			action {
				val messageLog =
						getLoggingChannelWithPerms(ConfigOptions.MESSAGE_LOG, event.getGuildOrNull()!!) ?: return@action

				val messages = generateBulkDeleteFile(event.messages)

				messageLog.createMessage {
					bulkDeleteEmbed(event, messages)
				}
			}
		}
	}

	/**
	 * Builds the embed for the bulk delete event.
	 *
	 * @param event The [MessageBulkDeleteEvent] for the event
	 * @param messages The messages that were deleted
	 */
	private suspend fun UserMessageCreateBuilder.bulkDeleteEmbed(event: MessageBulkDeleteEvent, messages: String?) {
		embed {
			title = "Bulk Message Delete"
			description = "A Bulk delete of messages occurred"
			field {
				name = "Location"
				value = "${event.channel.mention} " +
						"(${event.channel.asChannelOfOrNull<GuildMessageChannel>()?.name
							?: "Could not get channel name"})"
			}
			field {
				name = "Number of messages"
				value = event.messages.size.toString()
			}
			color = DISCORD_PINK
			timestamp = Clock.System.now()
		}
		if (messages != null) {
			addFile(
				"messages.md",
				ChannelProvider { messages.byteInputStream().toByteReadChannel() }
			)
		} else {
			content = "The messages from this event could not be gathered and logged."
		}
	}

	/**
	 * If message logging is enabled, sends an embed describing the message deletion to the guild's message log channel.
	 *
	 * @param message The deleted message
	 * @param proxiedMessage Extra data for PluralKit proxied messages
	 * @author trainb0y
	 */
	private suspend fun onMessageDelete(message: Message?, proxiedMessage: PKMessage?) {
		message ?: return
		val guild = message.getGuildOrNull() ?: return
		val messageLog = getLoggingChannelWithPerms(ConfigOptions.MESSAGE_LOG, guild) ?: return

		if (message.content.startsWith("pk;e", 0, true)) {
			return
		}

		messageLog.createEmbed {
			author {
				name = "Message deleted"
				icon = proxiedMessage?.member?.avatarUrl ?: message.author?.avatar?.cdnUrl?.toUrl()
			}
			description =
				"Location: ${message.channel.mention} " +
						"(${message.channel.asChannelOfOrNull<GuildMessageChannel>()?.name ?: "Could not get channel name"})"
			color = DISCORD_PINK
			timestamp = Clock.System.now()

			field {
				name = "Message contents"
				value = message.trimmedContents().ifNullOrEmpty { "Failed to retrieve previous message contents" }
				inline = false
			}
			attachmentsAndProxiedMessageInfo(guild, message, proxiedMessage)
		}
	}
}
