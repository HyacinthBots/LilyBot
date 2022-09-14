package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PKMessage
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.ProxiedMessageDeleteEvent
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.events.UnProxiedMessageDeleteEvent
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.configPresent
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.ifNullOrEmpty
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
				configPresent(ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
				failIf {
					event.message?.author?.id == kord.selfId
				}
			}

			action {
				onMessageDelete(event.getMessage(), event.pkMessage)
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
				configPresent(ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED, ConfigOptions.MESSAGE_LOG)
				failIf {
					event.message?.author?.id == kord.selfId ||
							event.message?.author?.isBot == true
				}
			}

			action {
				onMessageDelete(event.getMessage(), null)
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
	private suspend fun onMessageDelete(message: Message, proxiedMessage: PKMessage?) {
		val guild = message.getGuild()
		val config = LoggingConfigCollection().getConfig(guild.id) ?: return
		val messageLog =
			getLoggingChannelWithPerms(message.getGuild(), config.messageChannel!!, ConfigType.LOGGING)
				?: return

		messageLog.createMessage {
			embed {
				color = DISCORD_PINK
				author {
					name = "Message deleted"
					icon = proxiedMessage?.member?.avatarUrl ?: message.author?.avatar?.url
				}
				description =
					"Location: ${message.channel.mention} " +
							"(${message.channel.asChannelOf<GuildMessageChannel>().name})"
				timestamp = Clock.System.now()

				field {
					name = "Message contents"
					value = message.trimmedContents().ifNullOrEmpty { "Failed to retrieve previous message contents" }
					inline = false
				}

				if (message.attachments.isNotEmpty()) {
					field {
						name = "Attachments"
						value = message.attachments.map { it.url }.joinToString { "\n" }
						inline = false
					}
				}

				if (proxiedMessage != null) {
					field {
						name = "Message Author:"
						value = "System Member: ${proxiedMessage.member.name}\n" +
								"Account: ${guild.getMember(proxiedMessage.sender).tag} " +
								guild.getMember(proxiedMessage.sender).mention
						inline = true
					}

					field {
						name = "Author ID:"
						value = proxiedMessage.sender.toString()
					}
				} else {
					field {
						name = "Message Author:"
						value =
							"${message.author?.tag ?: "Failed to get author of message"} ${message.author?.mention ?: ""}"
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
