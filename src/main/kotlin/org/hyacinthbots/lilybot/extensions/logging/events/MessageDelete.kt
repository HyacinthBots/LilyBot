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
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.attachmentsAndProxiedMessageInfo
import org.hyacinthbots.lilybot.utils.generateBulkDeleteFile
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.ifNullOrEmpty
import org.hyacinthbots.lilybot.utils.requiredConfigs
import org.hyacinthbots.lilybot.utils.trimmedContents
import kotlin.time.Clock

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
				if (event.messages.isEmpty()) return@action

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
			title = Translations.Events.MessageDelete.Bulk.embedTitle.translate()
			description = Translations.Events.MessageDelete.Bulk.embedDescription.translate()
			field {
				name = Translations.Events.MessageDelete.Bulk.embedLocation.translate()
				value = "${event.channel.mention} " +
					"(${
						event.channel.asChannelOfOrNull<GuildMessageChannel>()?.name
							?: Translations.Events.MessageEvent.failedContents.translate()
					})"
			}
			field {
				name = Translations.Events.MessageDelete.Bulk.embedNumber.translate()
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
			content = Translations.Events.MessageDelete.Bulk.embedFailedContent.translate()
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
				name = Translations.Events.MessageDelete.Single.embedAuthor.translate()
				icon = proxiedMessage?.member?.avatarUrl ?: message.author?.avatar?.cdnUrl?.toUrl()
			}
			description = Translations.Events.MessageEvent.location.translate(
				message.channel.mention,
				message.channel.asChannelOfOrNull<GuildMessageChannel>()?.name
					?: Translations.Events.MessageDelete.noChannelName.translate()
			)
			color = DISCORD_PINK
			timestamp = Clock.System.now()

			field {
				name = Translations.Events.MessageDelete.Single.embedContents.translate()
				value = message.trimmedContents().ifNullOrEmpty { Translations.Events.MessageEvent.failedContents.translate() }
				inline = false
			}
			attachmentsAndProxiedMessageInfo(guild, message, proxiedMessage)
		}
	}
}
