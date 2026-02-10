package org.hyacinthbots.lilybot.extensions.logging.events

import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.components.components
import dev.kordex.core.components.linkButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.getJumpUrl
import dev.kordex.core.utils.isNullOrBot
import dev.kordex.modules.pluralkit.api.PKMessage
import dev.kordex.modules.pluralkit.events.ProxiedMessageUpdateEvent
import dev.kordex.modules.pluralkit.events.UnProxiedMessageUpdateEvent
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.*
import kotlin.time.Clock

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
                failIf(event.message.asMessageOrNull()?.author.isNullOrBot())
                failIf(event.old?.content == event.message.asMessageOrNull()?.content)
                failIf(event.old == null)
            }
            action {
                onMessageEdit(event.getMessageOrNull(), event.old, null)
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
                failIf(event.old?.content == event.message.asMessageOrNull()?.content)
                failIf(event.old == null)
            }
            action {
                onMessageEdit(event.getMessageOrNull(), event.old, event.pkMessage)
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
    private suspend fun onMessageEdit(message: Message?, old: Message?, proxiedMessage: PKMessage?) {
        message ?: return
        val guild = message.getGuildOrNull() ?: return

        val messageLog = getLoggingChannelWithPerms(ConfigOptions.MESSAGE_LOG, guild) ?: return

        messageLog.createMessage {
            embed {
                color = DISCORD_YELLOW
                author {
                    name = Translations.Events.MessageEdit.embedAuthor.translate()
                    icon = proxiedMessage?.member?.avatarUrl ?: message.author?.avatar?.cdnUrl?.toUrl()
                }
                description = Translations.Events.MessageEvent.location.translate(
                    message.channel.mention,
                    message.channel.asChannelOfOrNull<GuildMessageChannel>()?.name
                        ?: Translations.Events.MessageDelete.noChannelName.translate()
                )
                timestamp = Clock.System.now()

                field {
                    name = Translations.Events.MessageEdit.embedPrevious.translate()
                    value = old?.trimmedContents()
                        .ifNullOrEmpty { Translations.Events.MessageEvent.failedContents.translate() }
                    inline = false
                }
                field {
                    name = Translations.Events.MessageEdit.embedNew.translate()
                    value = message.trimmedContents()
                        .ifNullOrEmpty { Translations.Events.MessageEdit.embedNewFail.translate() }
                    inline = false
                }
                attachmentsAndProxiedMessageInfo(guild, message, proxiedMessage)
            }
            components {
                linkButton {
                    label = Translations.Events.MessageEdit.embedButton
                    url = message.getJumpUrl()
                }
            }
        }
    }
}
