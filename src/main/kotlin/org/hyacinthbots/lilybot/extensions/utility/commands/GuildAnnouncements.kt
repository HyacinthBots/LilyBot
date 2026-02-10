package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.Color
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.optionalSnowflake
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import kotlinx.coroutines.flow.toList
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import org.hyacinthbots.lilybot.utils.getFirstUsableChannel
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.getSystemChannelWithPerms

class GuildAnnouncements : Extension() {
    override val name = "guild-announcements"

    override suspend fun setup() {
        ephemeralSlashCommand(::GuildAnnouncementArgs, ::GuildAnnouncementModal) {
            name = Translations.Utility.GuildAnnouncements.name
            description = Translations.Utility.GuildAnnouncements.description

            guild(TEST_GUILD_ID)
            requirePermission(Permission.Administrator)

            check {
                hasPermission(Permission.Administrator)
            }

            action { modal ->
                val translations = Translations.Utility.GuildAnnouncements
                var response: EphemeralFollowupMessage? = null
                response = respond {
                    content = translations.sendConfirm.translate() +
                        if (arguments.targetGuild == null) {
                            translations.deliverAll
                        } else {
                            translations.deliverSpecific
                        }.translate(arguments.targetGuild)
                    components {
                        ephemeralButton {
                            label = Translations.Basic.yes
                            style = ButtonStyle.Success

                            action ButtonAction@{
                                response?.edit {
                                    content = translations.sent.translate()
                                    components { removeAll() }
                                }

                                if (arguments.targetGuild != null) {
                                    val guild = event.kord.getGuildOrNull(arguments.targetGuild!!)
                                    if (guild == null) {
                                        respond { content = translations.targetNotFound.translate() }
                                        return@ButtonAction
                                    }

                                    val channel = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)
                                        ?: getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild)
                                        ?: getSystemChannelWithPerms(guild)
                                        ?: getFirstUsableChannel(guild)

                                    if (channel == null) {
                                        respond { content = translations.noAvailableChannel.translate() }
                                        return@ButtonAction
                                    }

                                    channel.createEmbed {
                                        title = modal?.header?.value
                                        description = modal?.body?.value
                                        color = Color(0x7B52AE)
                                        footer {
                                            text = translations.deliveredToOne.translate()
                                        }
                                    }
                                } else {
                                    event.kord.guilds.toList().chunked(15).forEach { chunk ->
                                        for (i in chunk) {
                                            val channel =
                                                getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, i)
                                                    ?: getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, i)
                                                    ?: getSystemChannelWithPerms(i)
                                                    ?: getFirstUsableChannel(i)
                                                    ?: continue

                                            try {
                                                channel.createEmbed {
                                                    title = modal?.header?.value
                                                    description = modal?.body?.value
                                                    color = Color(0x7B52AE)
                                                    footer {
                                                        text =
                                                            translations.sentBy.translate(user.asUserOrNull()?.username)
                                                    }
                                                }
                                            } catch (_: KtorRequestException) {
                                                continue
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        ephemeralButton {
                            label = Translations.Basic.no
                            style = ButtonStyle.Danger

                            action {
                                response?.edit {
                                    content = translations.notSent.translate()
                                    components { removeAll() }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    inner class GuildAnnouncementArgs : Arguments() {
        val targetGuild by optionalSnowflake {
            name = Translations.Utility.GuildAnnouncements.Arguments.Target.name
            description = Translations.Utility.GuildAnnouncements.Arguments.Target.description
        }
    }

    inner class GuildAnnouncementModal : ModalForm() {
        override var title = Translations.Utility.GuildAnnouncements.Modal.title

        val header = lineText {
            label = Translations.Utility.GuildAnnouncements.Modal.Header.label
            placeholder = Translations.Utility.GuildAnnouncements.Modal.Header.placeholder
            maxLength = 250
            required = false
        }

        val body = paragraphText {
            label = Translations.Utility.GuildAnnouncements.Modal.Body.label
            placeholder = Translations.Utility.GuildAnnouncements.Modal.Body.placeholder
            maxLength = 1750
            required = true
        }
    }
}
