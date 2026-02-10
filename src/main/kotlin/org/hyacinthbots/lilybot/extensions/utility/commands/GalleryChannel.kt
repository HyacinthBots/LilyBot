package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.entity.MessageType
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.delete
import dev.kordex.core.utils.permissionsForMember
import dev.kordex.core.utils.respond
import kotlinx.coroutines.delay
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import kotlin.time.Duration.Companion.seconds

/**
 * The class the holds the systems that allow a guild to set a channel as a gallery channel.
 *
 * @since 3.3.0
 */
class GalleryChannel : Extension() {
    override val name = "gallery-channel"

    override suspend fun setup() {
        /**
         * Gallery channel commands.
         * @author NoComment1105
         * @since 3.3.0
         */
        ephemeralSlashCommand {
            name = Translations.Utility.GalleryChannel.name
            description = Translations.Utility.GalleryChannel.description

            /**
             * The command that sets the gallery channel.
             */
            ephemeralSubCommand {
                name = Translations.Utility.GalleryChannel.Set.name
                description = Translations.Utility.GalleryChannel.Set.description

                requirePermission(Permission.ManageGuild)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageGuild)
                    requireBotPermissions(Permission.ManageChannels, Permission.ManageMessages)
                    botHasChannelPerms(Permissions(Permission.ManageChannels, Permission.ManageMessages))
                }

                action {
                    val translations = Translations.Utility.GalleryChannel.Set
                    GalleryChannelCollection().getChannels(guildFor(event)!!.id).forEach {
                        if (channel.asChannelOrNull()?.id == it.channelId) {
                            respond {
                                content = translations.already.translate()
                            }
                            return@action
                        }
                    }

                    GalleryChannelCollection().setChannel(guild!!.id, channel.asChannelOrNull()!!.id)

                    respond {
                        content = translations.response.translate()
                    }

                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                        ?: return@action
                    utilityLog.createEmbed {
                        title = translations.embedTitle.translate()
                        description = translations.embedDesc.translate(channel.mention)
                        footer {
                            text = translations.embedRequested.translate(user.asUserOrNull()?.username)
                            icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                        }
                        color = DISCORD_GREEN
                    }
                }
            }

            /**
             * The command that unsets the gallery channel.
             */
            ephemeralSubCommand {
                name = Translations.Utility.GalleryChannel.Unset.name
                description = Translations.Utility.GalleryChannel.Unset.description

                requirePermission(Permission.ManageGuild)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageGuild)
                    requireBotPermissions(Permission.ManageChannels)
                    botHasChannelPerms(Permissions(Permission.ManageChannels))
                }

                action {
                    val translations = Translations.Utility.GalleryChannel.Unset
                    var channelFound = false

                    GalleryChannelCollection().getChannels(guildFor(event)!!.id).forEach {
                        if (channel.asChannelOrNull()?.id == it.channelId) {
                            GalleryChannelCollection().removeChannel(guild!!.id, channel.asChannelOrNull()!!.id)
                            channelFound = true
                        }
                    }

                    if (channelFound) {
                        respond {
                            content = translations.response.translate()
                        }

                        val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                            ?: return@action
                        utilityLog.createEmbed {
                            title = translations.embedTitle.translate()
                            description = translations.embedDesc.translate(channel.mention)
                            footer {
                                text = translations.embedRequested.translate(user.asUserOrNull()?.username)
                                icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                            }
                            color = DISCORD_RED
                        }
                    } else {
                        respond {
                            content = translations.notGallery.translate()
                        }
                    }
                }
            }

            /**
             * The command that returns a list of all image channels for a particular guild.
             */
            ephemeralSubCommand {
                name = Translations.Utility.GalleryChannel.List.name
                description = Translations.Utility.GalleryChannel.List.description

                check {
                    anyGuild()
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(
                        Permissions(Permission.SendMessages, Permission.EmbedLinks)
                    )
                }

                action {
                    val translations = Translations.Utility.GalleryChannel.List
                    var channels = ""

                    GalleryChannelCollection().getChannels(guildFor(event)!!.id).forEach {
                        channels += "<#${it.channelId}> "
                    }

                    respond {
                        embed {
                            title = translations.embedTitle.translate()
                            description = translations.embedDesc.translate()
                            field {
                                name = translations.embedChannelsField.translate()
                                value = if (channels != "") {
                                    channels.replace(
                                        " ",
                                        "\n"
                                    )
                                } else {
                                    translations.noneFound.translate()
                                }
                            }
                        }
                    }
                }
            }
        }

        /**
         * The event for checking a channel.
         * @since 3.3.0
         */
        event<MessageCreateEvent> {
            check {
                anyGuild()
                failIf { event.message.author?.id == kord.selfId }
            }

            action {
                GalleryChannelCollection().getChannels(event.guildId!!).forEach {
                    // If there are no attachments to the message and the channel we're in is an image channel
                    if (event.message.channelId == it.channelId && event.message.attachments.isEmpty()) {
                        if (!event.message.channel.asChannelOf<GuildMessageChannel>().permissionsForMember(kord.selfId)
                                .contains(Permission.ManageMessages)
                        ) {
                            event.message.channel.createMessage {
                                content = Translations.Utility.GalleryChannel.noPerms.translate()
                            }
                            return@forEach
                        }
                        // Delay to give the message a chance to populate with an embed, if it is a link to imgur etc.
                        delay(0.25.seconds.inWholeMilliseconds)
                        if (event.message.embeds.isEmpty()) { // If there is still no embed, we delete the message
                            // and explain why
                            if (event.message.type != MessageType.Default && event.message.type != MessageType.Reply) {
                                event.message.delete()
                                return@action
                            }

                            val response = event.message.respond {
                                content = Translations.Utility.GalleryChannel.images.translate()
                            }

                            event.message.delete()

                            try {
                                // Delete the explanation after 3 seconds. If an exception is thrown, the
                                // message has already been deleted
                                response.delete(2.5.seconds.inWholeMilliseconds)
                            } catch (_: EntityNotFoundException) {
                                // The message that we're attempting to delete has already been deleted.
                            }
                        }
                    }
                }
            }
        }
    }
}
