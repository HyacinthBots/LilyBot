@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.*
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.*
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.replace
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.system.exitProcess
import kotlin.time.ExperimentalTime

@Suppress("DuplicatedCode")
class Moderation : Extension() {
    override val name = "moderation"

    override suspend fun setup() {
        /**
         * Clear Command
         * @author IMS212
         */
        ephemeralSlashCommand(::ClearArgs) {
            name = "clear"
            description = "Clears messages."

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val messageAmount = arguments.messages
                val messageHolder = arrayListOf<Snowflake>()
                val textChannel = channel as GuildMessageChannelBehavior

                channel.getMessagesBefore(channel.messages.last().id, Integer.min(messageAmount, 100)).filterNotNull()
                    .onEach {
                        messageHolder.add(it.id)
                    }.catch {
                        it.printStackTrace()
                        println("error")
                    }.collect()

                textChannel.bulkDelete(messageHolder)

                respond {
                    content = "Messages Cleared"
                }

                ResponseHelper.responseEmbedInChannel(actionLog, "$messageAmount messages have been cleared.", "Action occured in ${textChannel.mention}", DISCORD_BLACK, user.asUser())
            }
        }


        /**
         * Ban command
         * @author IMS212
         */
        ephemeralSlashCommand(::BanArgs) {
            name = "ban"
            description = "Bans a user."

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                guild?.ban(arguments.userArgument.id, builder = {
                    this.reason = "Requested by " + user.asUser().username
                    this.deleteMessagesDays = arguments.messages
                })

                respond {
                    content = "Banned a user"
                }

                actionLog.createEmbed {
                    color = DISCORD_BLACK
                    title = "Banned a user"
                    description = "${arguments.userArgument.mention} has been banned!"

                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    field {
                        name = "Days of messages deleted"
                        value = arguments.messages.toString()
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

        /**
         *  Unban command
         *  @author NoComment1105
         */
        ephemeralSlashCommand(::UnbanArgs) {
            name = "unban"
            description = "Unbans a user"

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                guild?.unban(arguments.userArgument.id)

                respond {
                    content = "Unbanned User"
                }

                ResponseHelper.responseEmbedInChannel(actionLog, "Unbanned a user", "${arguments.userArgument.mention} has been unbanned!", DISCORD_GREEN, user.asUser())

            }
        }

        /**
         * Soft ban command
         * @author NoComment1105
         */
        ephemeralSlashCommand(::SoftBanArgs) {
            name = "softban"
            description = "Softbans a user"

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                guild?.ban(arguments.userArgument.id, builder = {
                    this.reason = "Requested by ${user.asUser().username}"
                    this.deleteMessagesDays = arguments.messages
                })

                respond {
                    content = "Soft-Banned User"
                }

                actionLog.createEmbed {
                    color = DISCORD_BLACK
                    title = "Soft-banned a user"
                    description = "${arguments.userArgument.mention} has been soft banned."

                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    field {
                        name = "Days of messages deleted"
                        value = arguments.messages.toString()
                        inline = false
                    }

                    footer {
                        text = "Requested by " + user.asUser().tag
                        icon = user.asUser().avatar?.url
                    }

                    timestamp = Clock.System.now()
                }

                guild?.unban(arguments.userArgument.id)
            }
        }

        /**
         * Kick command
         * @author IMS212
         */
        ephemeralSlashCommand(::KickArgs) {
            name = "kick"
            description = "Kicks a user."

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                guild?.kick(arguments.userArgument.id, "Requested by " + user.asUser().username)

                respond {
                    content = "Kicked User"
                }

                ResponseHelper.responseEmbedInChannel(actionLog, "Kicked a user", "Kicked ${arguments.userArgument.mention}!", DISCORD_BLACK, user.asUser())
            }
        }

        /**
         * Say Command
         * @author NoComment1105
         */
        ephemeralSlashCommand(::SayArgs) {
            name = "say"
            description = "Say something through Lily."

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                if (arguments.embedMessage) {
                    channel.createEmbed {
                        color = DISCORD_BLURPLE
                        description = arguments.messageArgument
                        timestamp = Clock.System.now()
                    }
                } else {
                    channel.createMessage {
                        content = arguments.messageArgument
                    }
                }

                respond { content = "Command used" }

                ResponseHelper.responseEmbedInChannel(
                    actionLog,
                    "Message Sent",
                    "/say has been used to say ${arguments.messageArgument}.",
                    DISCORD_BLACK,
                    user.asUser()
                )
            }
        }

        /**
         * Presence Command
         * @author IMS
         */
        ephemeralSlashCommand(::PresenceArgs) {
            name = "set-status"
            description = "Set Lily's current presence/status."

            allowRole(MODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                this@ephemeralSlashCommand.kord.editPresence {
                    status = PresenceStatus.Online
                    playing(arguments.presenceArgument)
                }

                respond { content = "Presence set to `${arguments.presenceArgument}`" }

                ResponseHelper.responseEmbedInChannel(actionLog, "Presence Changed", "Lily's presence has been set to ${arguments.presenceArgument}", DISCORD_BLACK, user.asUser())
            }
        }

        /**
         * Shutdown command
         * @author IMS212
         */
        ephemeralSlashCommand {
            name = "shutdown"
            description = "Shuts down the bot."

            allowRole(ADMIN)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                respond {
                    embed {
                        title = "Shutdown"
                        description = "Are you sure you would like to shut down?"
                    }

                    components {
                        ephemeralButton {
                            label = "Yes"
                            style = ButtonStyle.Success

                            action {
                                respond { content = "Shutting down..." }
                                ResponseHelper.responseEmbedInChannel(actionLog, "Shutting Down!", null, DISCORD_RED, user.asUser())
                                kord.shutdown()
                                exitProcess(0)
                            }
                        }

                        ephemeralButton {
                            label = "No"
                            style = ButtonStyle.Danger

                            action {
                                respond { content = "Shutdown aborted." }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Warn Command
         * @author chalkyjeans
         */
        ephemeralSlashCommand(::WarnArgs) {
            name = "warn"
            description = "Warn a member for any infractions."

            allowRole(MODERATORS)
            allowRole(TRIALMODERATORS)

            action {
                val userId = arguments.userArgument.id.asString
                val userTag = arguments.userArgument.tag
                val warnPoints = arguments.warnPoints
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                var databasePoints: String? = null

                newSuspendedTransaction {
                    DatabaseManager.Warn.insertIgnore {
                        it[id] = userId
                        it[points] = "0"
                    }

                    databasePoints = DatabaseManager.Warn.select {
                        DatabaseManager.Warn.id eq userId
                    }.single()[DatabaseManager.Warn.points]

                    DatabaseManager.Warn.replace {
                        it[id] = id
                        it[points] = (warnPoints + Integer.parseInt(databasePoints)).toString()
                    }
                }

                respond {
                    content = "Warned User"
                }
                actionLog.createEmbed {
                    title = "Warning"
                    color = DISCORD_BLACK
                    timestamp = Clock.System.now()

                    field {
                        name = "User:"
                        value = "$userTag \n $userId"
                        inline = false
                    }
                    field {
                        name = "Total Points:"
                        value = databasePoints.toString()
                        inline = false
                    }
                    field {
                        name = "Points added:"
                        value = warnPoints.toString()
                        inline = false
                    }
                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    footer {
                        text = "Requested by " + user.asUser().tag
                        icon = user.asUser().avatar?.url
                    }
                }
            }
        }

        /**
         * Timeout command
         *
         * @author
         */
        ephemeralSlashCommand(::TimeoutArgs) {
            name = "timeout"
            description = "Timeout a user"

            allowRole(MODERATORS)
            allowRole(TRIALMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userID = arguments.userArgument.id.toString()
                val userTag = arguments.userArgument.tag
                val duration = arguments.duration.toString()

                respond {
                    content = "Timed out $userID"
                }

                actionLog.createEmbed {
                    title = "Timeout"
                    color = DISCORD_BLACK
                    timestamp = Clock.System.now()

                    field {
                        name = "User:"
                        value = "$userTag \n $userID"
                        inline = false
                    }
                    field {
                        name = "Duration:"
                        value = duration
                        inline = false
                    }
                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    footer {
                        text = "Requested by " + user.asUser().tag
                        icon = user.asUser().avatar?.url
                    }
                }
            }
        }
    }

    inner class ClearArgs : Arguments() {
        val messages by int("messages", "Messages")
    }

    inner class KickArgs : Arguments() {
        val userArgument by user("kickUser", "Person to kick")
    }

    inner class BanArgs : Arguments() {
        val userArgument by user("banUser", "Person to ban")
        val messages by int("messages", "Messages")
        val reason by defaultingString("reason", "The reason for the ban", "No Reason Provided")
    }

    inner class UnbanArgs : Arguments() {
        val userArgument by user("unbanUserId", "Person Unbanned")
    }

    inner class SoftBanArgs : Arguments() {
        val userArgument by user("softBanUser", "Person to Soft ban")
        val messages by defaultingInt("messages", "Messages", 3)
        val reason by defaultingString("reason", "The reason for the ban", "No Reason Provided")
    }

    inner class SayArgs : Arguments() {
        val messageArgument by string("message", "Message contents")
        val embedMessage by boolean("embed", "Would you like to send as embed")
    }

    inner class PresenceArgs : Arguments() {
        val presenceArgument by string("presence", "Lily's presence")
    }

    inner class WarnArgs : Arguments() {
        val userArgument by user("warnUser", "Person to Warn")
        val warnPoints by defaultingInt("points", "Amount of points to add", 10)
        val reason by defaultingString("reason", "Reason for Warn", "No Reason Provided")
    }

    inner class TimeoutArgs : Arguments() {
        val userArgument by user("timeoutUser", "Person to timeout")
        val duration by defaultingCoalescingDuration("duration", "Duration of timeout", DateTimePeriod(0, 0, 0, 6, 0, 0, 0))
        val reason by defaultingString("reason", "Reason for timeout", "No reason provided")
    }
}
