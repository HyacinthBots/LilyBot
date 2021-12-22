@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.entity.channel.Channel
import dev.kord.core.entity.channel.GuildChannel
import dev.kord.core.entity.channel.MessageChannel
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
                }

                actionLog.createEmbed {
                    color = DISCORD_RED
                    title = "Message reported!"
                    description = event.interaction.getChannel().mention

                    field {
                        name  = "Messaged Content:"
                        value = event.interaction.getTarget().content
                        inline = true
                    }
                    field {
                        name = "Message Link:"
                        value = event.interaction.getTarget().getJumpUrl()
                        inline = false
                    }
                    footer {
                        text = "Requested by " + user.asUser().tag
                        icon = user.asUser().avatar?.url
                    }
                    timestamp = Clock.System.now()
                }
            }

        }
        ephemeralSlashCommand(::ManualReportArgs) {
            name = "manual-report"
            description = "Manually report a message"
            locking = true // To prevent the command from being run more than once concurrently

            action {
                val actionLog = guild?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior
                val messageID = Snowflake(arguments.message.split("/")[6])
                val messageChannelID = Snowflake(arguments.message.split("/")[5])
                val messageChannel = (guild?.getChannel(messageChannelID) as MessageChannel).getMessage(messageID).content

                respond {
                    content = "Message reported to staff"
                }

                actionLog.createMessage {
                    content = "<@&${MODERATORS.value}>"
                }

                actionLog.createEmbed {
                    color = DISCORD_RED
                    title = "Message Manually Reported!"
                    description = event.interaction.getChannel().mention

                    field {
                        name = "Message content:"
                        value = messageChannel
                        inline = false
                    }
                    field {
                        name = "Message Link:"
                        value = arguments.message
                        inline = true
                    }
                    footer {
                        text = "Requested by " + user.asUser().tag
                        icon = user.asUser().avatar?.url
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