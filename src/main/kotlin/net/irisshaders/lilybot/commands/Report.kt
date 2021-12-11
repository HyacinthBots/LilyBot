@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ACTION_LOG
import net.irisshaders.lilybot.utils.GUILD_ID
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
                val actionLog = guild?.getChannel(ACTION_LOG) as GuildMessageChannelBehavior

                respond {
                    content = "Message reported to staff"
                }

                actionLog.createMessage {
                    content = "<@&${MODERATORS.value}>"
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
                val actionLog = guild!!.getChannelOf<GuildMessageChannel>(ACTION_LOG)

                respond {
                    content = "Message reported to staff"
                }

                actionLog.createMessage {
                    content = "`<@&${MODERATORS.value}>`"
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