package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PKMessage
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageUpdateEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageUpdateEvent
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.attachmentsAndProxiedMessageInfo
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.ifNullOrEmpty
import org.hyacinthbots.lilybot.utils.requiredConfigs
import org.hyacinthbots.lilybot.utils.trimmedContents

/**
 * The class for logging editing of messages to the guild message log.
 * @since 4.1.0
 */
class MessageEdit : Extension() {
	override val name = "message-edit"

	override suspend fun setup() {
		/**
		 * Logs edited messages to the message log channel.
		 * @see onMessageEdit
		 * @author trainb0y
		 */
		event<UnProxiedMessageUpdateEvent> {
			check {
				anyGuild()
				requiredConfigs(ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
				failIf {
					event.message.asMessage().author?.id == kord.selfId
				}
			}
			action {
				onMessageEdit(event.getMessage(), event.old, null)
			}
		}

		/**
		 * Logs proxied edited messages to the message log channel.
		 * @see onMessageEdit
		 * @author trainb0y
		 */
		event<ProxiedMessageUpdateEvent> {
			check {
				anyGuild()
				requiredConfigs(ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
				failIf {
					event.message.asMessage().author?.id == kord.selfId
				}
			}
			action {
				onMessageEdit(event.getMessage(), event.old, event.pkMessage)
			}
		}
	}

	/**
	 * If message logging is enabled, sends an embed describing the message edit to the guild's message log channel.
	 *
	 * @param message The current message
	 * @param old The original message
	 * @param proxiedMessage Extra data for PluralKit proxied messages
	 * @author trainb0y
	 */
	private suspend fun onMessageEdit(message: Message, old: Message?, proxiedMessage: PKMessage?) {
		val guild = message.getGuild()

		val messageLog = getLoggingChannelWithPerms(ConfigOptions.MESSAGE_LOG, guild) ?: return

		messageLog.createEmbed {
			color = DISCORD_YELLOW
			author {
				name = "Message Edited"
				icon = proxiedMessage?.member?.avatarUrl ?: message.author?.avatar?.url
			}
			description =
				"Location: ${message.channel.mention} " +
						"(${message.channel.asChannelOf<GuildMessageChannel>().name})"
			timestamp = Clock.System.now()

			field {
				name = "Previous contents"
				value = old?.trimmedContents().ifNullOrEmpty { "Failed to retrieve previous message contents" }
				inline = false
			}
			field {
				name = "New contents"
				value = message.trimmedContents().ifNullOrEmpty { "Failed to retrieve new message contents" }
				inline = false
			}
			attachmentsAndProxiedMessageInfo(guild, message, proxiedMessage)
		}
	}
}
