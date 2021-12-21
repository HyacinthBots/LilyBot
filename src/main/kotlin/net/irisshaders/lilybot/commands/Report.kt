@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.cache.api.data.description
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.MESSAGE_LOGS
import net.irisshaders.lilybot.utils.MODERATORS
import kotlin.time.ExperimentalTime

/**
 * The message reporting feature in the bot
 * @author NoComment1105
 */
class Report : Extension() {
    override val name = "report"

    override suspend fun setup() {
        ephemeralMessageCommand {
            name = "Report"
            locking = true // To prevent the command from being run more than once concurrently

            action {
                val actionLog = guild?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior

                respond {
                    content = "Message reported to staff"
                }

                actionLog.createMessage {
                    content = "<@&${MODERATORS.value}>"
                    components {
                        linkButton(row = 0) {
                            label = "Jump to reported message"
                            url = event.interaction.getTarget().getJumpUrl()
                        }
                        ephemeralButton(row = 1) {
                            label = "Delete the report"
                            style = ButtonStyle.Danger

                            action {
                                this.message?.delete()
                            }
                        }

                        ephemeralButton(row = 1) {
                            label = "Delete the reported message"
                            style = ButtonStyle.Danger

                            action {
                                // Delete the reported messahe here
                            }
                        }

                        ephemeralSelectMenu(row = 2) {
                            option(
                                label = "10-Minute Timeout",
                                value = "10-timeout",
                            )
                            option(
                                label = "20-Minute Timeout",
                                value = "20-timeout",
                            )
                            option(
                                label = "30-Minute Timeout",
                                value = "30-timeout",
                            )
                            action {
                                // IDK how to do selection menus
                            }
                        }
                    }
                }

                actionLog.createEmbed {
                    color = DISCORD_RED
                    title = "Message reported!"

                    field {
                        value = "**Messaged Content:** ${event.interaction.getTarget().content}"
                        inline = true
                    }
                    field {
                        value = "**Message Link:** https://discord.com/channels/${GUILD_ID.value}/${event.interaction.channelId.value}/${event.interaction.targetId.value}"
                        inline = false
                    }
                    field {
                        value = "**Reported by:** ${user.asUser().tag}"
                        inline = false
                    }
                    timestamp = Clock.System.now()
                }
            }

        }
        ephemeralSlashCommand(::ManualReportArgs) {
            name = "manual-report"
            description = "Manually report a message"

            action {
                val actionLog = guild?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior

                respond {
                    content = "Message reported to staff"
                }

                actionLog.createMessage {
                    content = "`<@&${MODERATORS.value}>`"
                    components {
                        linkButton(row = 0) {
                            label = "Jump to reported message"
                            url = arguments.message
                        }
                        ephemeralButton(row = 1) {
                            label = "Delete the report"
                            style = ButtonStyle.Danger

                            action {
                                this.message?.delete()
                            }
                        }

                        ephemeralButton(row = 1) {
                            label = "Delete the reported message"
                            style = ButtonStyle.Danger

                            action {
                                // Delete the reported messahe here
                            }
                        }

                        ephemeralSelectMenu(row = 2) {
                            option(
                                label = "10-Minute Timeout",
                                value = "10-timeout",
                            )
                            option(
                                label = "20-Minute Timeout",
                                value = "20-timeout",
                            )
                            option(
                                label = "30-Minute Timeout",
                                value = "30-timeout",
                            )
                            action {
                                // IDK how to do selection menus
                            }
                        }
                    }
                }

                actionLog.createEmbed {
                    color = DISCORD_RED
                    title = "Message Manually Reported!"

                    field {
                        value = "**Message Link:** ${arguments.message}"
                        inline = true
                    }
                    field {
                        value = "**Reported by:** ${user.asUser().tag}"
                        inline = false
                    }
                    timestamp = Clock.System.now()
                }
            }
        }
    }

    inner class ManualReportArgs : Arguments() {
        val message by string("message", "Link to the message to report")
    }
}