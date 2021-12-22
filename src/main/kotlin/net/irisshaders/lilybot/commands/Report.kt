@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import kotlinx.datetime.Clock
import kotlinx.serialization.json.JsonNull.content
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
                val messageAuthor = event.interaction.getTarget().getAuthorAsMember()
                val reportedMessage = event.interaction.getTarget()

                respond {
                    content = "Message reported to staff"
                }
                createReport(user, actionLog, messageAuthor, reportedMessage)
            }

        }
        // Fuck this thing ngl
        ephemeralSlashCommand(::ManualReportArgs) {
            name = "manual-report"
            description = "Manually report a message"

            action {
                val actionLog = guild?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior

                respond {
                    content = "Message reported to staff"
                }
//                createReport(user, actionLog, messageAuthor, reportedMessage)
            }
        }
    }

    inner class ManualReportArgs : Arguments() {
        val message by string("message", "Link to the message to report")
    }

    private suspend fun createReport(user: UserBehavior, actionLog: GuildMessageChannelBehavior, messageAuthor: Member?, reportedMessage: Message) {
        println("Test")
        actionLog.createMessage {
            content = "<@&${MODERATORS.value}>"
        }

        actionLog.createEmbed {
            color = DISCORD_RED
            title = "Message reported!"

            field {
                value = "**Messaged Content:** ${reportedMessage.content}"
                inline = true
            }
            field {
                value =
                    "**Message Link:** ${reportedMessage.getJumpUrl()}"
                inline = false
            }
            field {
                value = "**Reported by:** ${user.asUser().tag}"
                inline = false
            }
            timestamp = Clock.System.now()
        }.edit {
            components {
                ephemeralButton(row = 0) {
                    label = "Delete the reported message"
                    style = ButtonStyle.Danger

                    action {
                        reportedMessage.delete(reason = "Deleted via report.")
                    }
                }

                ephemeralSelectMenu(row = 1) {
                    option(
                        label = "10-Minute Timeout",
                        value = "10-timeout",
                    ) {
                        description = "Timeout the user for ten minutes."
                    }
                    option(
                        label = "20-Minute Timeout",
                        value = "20-timeout",
                    ) {
                        description = "Timeout the user for 20 minutes."
                    }
                    option(
                        label = "30-Minute Timeout",
                        value = "30-timeout",
                    )
                    {
                        description = "Timeout the user for 30 minutes."
                    }
                    option(
                        label = "Kick the user.",
                        value = "kick-user",
                    )
                    {
                        description = "Kick the user from the server."
                    }
                    option(
                        label = "Softban the user.",
                        value = "softban-user",
                    )
                    {
                        description = "Softban the user and delete all their messages."
                    }
                    option(
                        label = "Ban the user.",
                        value = "ban-user",
                    )
                    {
                        description = "Ban the user and delete their messages."
                    }
                    action {
                        when (this.selected[0]) {
                            "10-timeout" -> println("10")
                            "20-timeout" -> println("20")
                            "30-timeout" -> println("30")
                            "kick-user" -> messageAuthor?.kick(reason = "Kicked via report")
                            "softban-user" -> {
                                messageAuthor?.ban {
                                    this.reason = "Banned via report."
                                    this.deleteMessagesDays = 1
                                }
                                reportedMessage.getGuild().unban(messageAuthor!!.id, reason = "Softban")
                            }
                            "ban-user" -> messageAuthor?.ban {
                                this.reason = "Banned via report"
                                this.deleteMessagesDays = 1
                            }
                        }
                    }
                }
            }
        }
    }
}