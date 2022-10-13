package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PKMessage
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageDeleteEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageDeleteEvent
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
		val guild = message.getGuild()

		if (message.content.startsWith("pk;e", 0, true)) {
			return
		}

		val messageLog = getLoggingChannelWithPerms(ConfigOptions.MESSAGE_LOG, guild) ?: return

		messageLog.createEmbed {
			author {
				name = "Message deleted"
				icon = proxiedMessage?.member?.avatarUrl ?: message.author?.avatar?.url
			}
			description =
				"Location: ${message.channel.mention} " +
						"(${message.channel.asChannelOf<GuildMessageChannel>().name})"
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
