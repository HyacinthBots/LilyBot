@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.commands.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingCoalescingDuration
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.hasRole
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.FULLMODERATORS
import net.irisshaders.lilybot.utils.MODERATORS
import net.irisshaders.lilybot.utils.MOD_ACTION_LOG
import net.irisshaders.lilybot.utils.ADMIN
import net.irisshaders.lilybot.utils.TRIALMODERATORS
import net.irisshaders.lilybot.utils.ResponseHelper
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

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val messageAmount = arguments.messages
                val messageHolder = arrayListOf<Snowflake>()
                val textChannel = channel as GuildMessageChannelBehavior

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.clear.getMessages"
                    message = "Gathering messages"
                    data["amount"] = messageAmount
                }

                channel.getMessagesBefore(channel.messages.last().id, Integer.min(messageAmount, 100)).filterNotNull()
                    .onEach {
                        messageHolder.add(it.id)
                    }.catch {
                        it.printStackTrace()
                        println("error")
                        sentry.breadcrumb(BreadcrumbType.Error) {
                            category = "commands.moderation.clear.getMessages"
                            message = "Error gathering message"
                        }
                    }.collect()

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.clear.getMessages"
                    message = "Messages gathered"
                    data["amount"] = messageHolder.size
                }


                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.clear.deleteMessages"
                    message = "Message Deleting Starting"
                }

                textChannel.bulkDelete(messageHolder)

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.clear.deleteMessages"
                    message = "Message Deleting finishing"
                    data["quantity"] = messageAmount
                }

                respond {
                    content = "Messages Cleared"
                }

                ResponseHelper.responseEmbedInChannel(
                    actionLog,
                    "$messageAmount messages have been cleared.",
                    "Action occurred in ${textChannel.mention}",
                    DISCORD_BLACK,
                    user.asUser()
                )
            }
        }


        /**
         * Ban command
         * @author IMS212
         */
        ephemeralSlashCommand(::BanArgs) {
            name = "ban"
            description = "Bans a user."

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userArg = arguments.userArgument

                if (guild?.getMember(userArg.id)?.isBot == true) {
                    sentry.breadcrumb(BreadcrumbType.Error) {
                        category = "commands.moderation.ban.checkIsBot"
                        message = "Lmao someone tried to ban a bot"
                        data["banTarget"] = userArg.tag
                    }
                    respond {
                        content = "Lol you can't ban me or other bots"
                    }
                    return@action
                } else if (guild?.getRole(MODERATORS) ?.let { guild?.getMember(arguments.userArgument.id)?.hasRole(it.asRole()) } == true) {
                    sentry.breadcrumb(BreadcrumbType.Error) {
                        category = "commands.moderation.ban.checkIsMod"
                        message = "Lmao someone tried to ban a mod"
                        data["banTarget"] = userArg.tag
                    }
                    respond {
                        content = "Bruh don't try to ban a moderator"
                    }
                    return@action
                }

                val dm = ResponseHelper.userDMEmbed(
                    userArg,
                    "You have been banned from ${guild?.fetchGuild()?.name}",
                    "**Reason:**\n${arguments.reason}",
                    null
                )

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.ban.banTask"
                    message = "Running ban task"
                    data["banTarget"] = userArg.tag
                }

                guild?.ban(userArg.id, builder = {
                    this.reason = "Requested by " + user.asUser().username
                    this.deleteMessagesDays = arguments.messages
                })

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.ban.banTast"
                    message = "Finished ban task"
                    data["banTarget"] = userArg.tag
                }

                respond {
                    content = "Banned a user"
                }

                actionLog.createEmbed {
                    color = DISCORD_BLACK
                    title = "Banned a user"
                    description = "${userArg.mention} has been banned!"

                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    field {
                        name = "Days of messages deleted:"
                        value = arguments.messages.toString()
                        inline = false
                    }
                    field {
                        name = "User Notification:"
                        value =
                            if (dm != null) {
                                "User notified with a direct message"
                            } else {
                                "Failed to notify user with a direct message"
                            }
                        inline = false
                    }

                    footer {
                        text = "Requested by ${user.asUser().tag}"
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

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userArg = arguments.userArgument

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.unban.unbanTask"
                    message = "Unbanning user"
                    data["unbanTarget"] = userArg.tag
                }

                guild?.unban(userArg.id)

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.unban.unbanTask"
                    message = "Unban task complete"
                    data["unbanTarget"] = userArg.tag
                }

                respond {
                    content = "Unbanned User"
                }

                ResponseHelper.responseEmbedInChannel(
                    actionLog,
                    "Unbanned a user",
                    "${userArg.mention} has been unbanned!",
                    DISCORD_GREEN,
                    user.asUser()
                )

            }
        }

        /**
         * Soft ban command
         * @author NoComment1105
         */
        ephemeralSlashCommand(::SoftBanArgs) {
            name = "softban"
            description = "Softbans a user"

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userArg = arguments.userArgument

                if (guild?.getMember(userArg.id)?.isBot == true) {
                    sentry.breadcrumb(BreadcrumbType.Error) {
                        category = "commands.moderation.soft-ban.checkIsBot"
                        message = "Lmao someone tried to ban a bot"
                        data["banTarget"] = userArg.tag
                    }
                    respond {
                        content = "Lol you can't ban me or other bots"
                    }
                    return@action
                } else if (guild?.getRole(MODERATORS) ?.let { guild?.getMember(arguments.userArgument.id)?.hasRole(it.asRole()) } == true) {
                    sentry.breadcrumb(BreadcrumbType.Error) {
                        category = "commands.moderation.soft-ban.checkIsMod"
                        message = "Lmao someone tried to ban a mod"
                        data["banTarget"] = userArg.tag
                    }
                    respond {
                        content = "Bruh don't try to ban a moderator"
                    }
                    return@action
                }

                val dm = ResponseHelper.userDMEmbed(
                    userArg,
                    "You have been soft-banned from ${guild?.fetchGuild()?.name}",
                    "**Reason:**\n${arguments.reason}\n\nYou are free to rejoin without the need to be unbanned",
                    null
                )

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.soft-ban.banTask"
                    message = "Running ban task"
                    data["soft-banTarget"] = userArg.tag
                }

                guild?.ban(userArg.id, builder = {
                    this.reason = "Requested by ${user.asUser().username}"
                    this.deleteMessagesDays = arguments.messages
                })

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.soft-ban.banTask"
                    message = "Completed ban task"
                    data["soft-banTarget"] = userArg.tag
                }

                respond {
                    content = "Soft-Banned User"
                }

                actionLog.createEmbed {
                    color = DISCORD_BLACK
                    title = "Soft-banned a user"
                    description = "${userArg.mention} has been soft banned."

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
                    field {
                        name = "User Notification:"
                        value =
                            if (dm != null) {
                                "User notified with a direct message"
                            } else {
                                "Failed to notify user with a direct message"
                            }
                        inline = false
                    }

                    footer {
                        text = "Requested by ${user.asUser().tag}"
                        icon = user.asUser().avatar?.url
                    }

                    timestamp = Clock.System.now()
                }

                guild?.unban(userArg.id)
            }
        }

        /**
         * Kick command
         * @author IMS212
         */
        ephemeralSlashCommand(::KickArgs) {
            name = "kick"
            description = "Kicks a user."

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userArg = arguments.userArgument

                if (guild?.getMember(userArg.id)?.isBot == true) {
                    sentry.breadcrumb(BreadcrumbType.Error) {
                        category = "commands.moderation.kick.checkIsBot"
                        message = "Lmao someone tried to kick a bot"
                        data["banTarget"] = userArg.tag
                    }
                    respond {
                        content = "Lol you can't kick me or other bots"
                    }
                    return@action
                } else if (guild?.getRole(MODERATORS) ?.let { guild?.getMember(arguments.userArgument.id)?.hasRole(it.asRole()) } == true) {
                    sentry.breadcrumb(BreadcrumbType.Error) {
                        category = "commands.moderation.kick.checkIsMod"
                        message = "Lmao someone tried to kick a mod"
                        data["banTarget"] = userArg.tag
                    }
                    respond {
                        content = "Bruh don't try to kick a moderator"
                    }
                    return@action
                }

                val dm = ResponseHelper.userDMEmbed(
                    userArg,
                    "You have been kicked from ${guild?.fetchGuild()?.name}",
                    "**Reason:**\n${arguments.reason}",
                    null
                )

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.kick.kickTask"
                    message = "Running kick task"
                    data["kickTarget"] = userArg.tag
                }

                guild?.kick(userArg.id, "Requested by " + user.asUser().username)

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.kick.kickTask"
                    message = "Completed kick task"
                    data["kickTarget"] = userArg.tag
                }

                respond {
                    content = "Kicked User"
                }

                actionLog.createEmbed {
                    color = DISCORD_BLACK
                    title = "Kicked User"
                    description = "Kicked ${arguments.userArgument.tag} from the Server"

                    field {
                        name = "Reason"
                        value = arguments.reason
                        inline = false
                    }
                    field {
                        name = "User Notification:"
                        value =
                            if (dm != null) {
                                "User notified with a direct message"
                            } else {
                                "Failed to notify user with a direct message"
                            }
                        inline = false
                    }
                    footer {
                        text = "Requested By ${user.asUser().tag}"
                        icon = user.asUser().avatar?.url
                    }
                }
            }
        }

        /**
         * Say Command
         * @author NoComment1105
         */
        ephemeralSlashCommand(::SayArgs) {
            name = "say"
            description = "Say something through Lily."

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                if (arguments.embedMessage) {
                    channel.createEmbed {
                        color = DISCORD_BLURPLE
                        description = arguments.messageArgument
                        timestamp = Clock.System.now()
                    }
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "commands.moderation.say.isEmbed"
                        message = "Say was used to create an embed"
                        data["content"] = arguments.messageArgument
                    }
                } else {
                    channel.createMessage {
                        content = arguments.messageArgument
                    }
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "commands.moderation.say.isEmbed"
                        message = "Say was used to create an message"
                        data["content"] = arguments.messageArgument
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

            allowRole(FULLMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior

                this@ephemeralSlashCommand.kord.editPresence {
                    status = PresenceStatus.Online
                    playing(arguments.presenceArgument)
                }

                respond { content = "Presence set to `${arguments.presenceArgument}`" }

                ResponseHelper.responseEmbedInChannel(
                    actionLog,
                    "Presence Changed",
                    "Lily's presence has been set to ${arguments.presenceArgument}",
                    DISCORD_BLACK,
                    user.asUser()
                )

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.presence.updatedPresence"
                    message = "Presence update"
                    data["newPresence"] = arguments.presenceArgument
                }
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
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "commands.moderation.shutdown.shutdownTask"
                        message = "${user.asUser().tag} shutdown the bot"
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
        // FIXME Fix this where points arent written to the correct place in the database table
        ephemeralSlashCommand(::WarnArgs) {
            name = "warn"
            description = "Warn a member for any infractions."

            allowRole(FULLMODERATORS)
            allowRole(TRIALMODERATORS)

            action {
                val userArg = arguments.userArgument
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                var databasePoints: String? = null

                newSuspendedTransaction {
                    DatabaseManager.Warn.insertIgnore {
                        it[id] = userArg.id.toString()
                        it[points] = "0"
                    }

                    databasePoints = DatabaseManager.Warn.select {
                        DatabaseManager.Warn.id eq userArg.id.toString()
                    }.single()[DatabaseManager.Warn.points]

                    DatabaseManager.Warn.replace {
                        it[id] = id
                        it[points] = (arguments.warnPoints + Integer.parseInt(databasePoints)).toString()
                    }
                }

                val dm = ResponseHelper.userDMEmbed(
                    userArg,
                    "You have been warned in ${guild?.fetchGuild()?.name}",
                    "You were given ${arguments.warnPoints} points\nYour total is now ${Integer.parseInt(databasePoints)}\n\n**Reason:**\n${arguments.reason}",
                    null
                )

                respond {
                    content = "Warned User"
                }
                actionLog.createEmbed {
                    title = "Warning"
                    color = DISCORD_BLACK
                    timestamp = Clock.System.now()

                    field {
                        name = "User:"
                        value = "${userArg.tag} \n${userArg.id}"
                        inline = false
                    }
                    field {
                        name = "Total Points:"
                        value = databasePoints.toString()
                        inline = false
                    }
                    field {
                        name = "Points added:"
                        value = arguments.warnPoints.toString()
                        inline = false
                    }
                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    field {
                        name = "User notification"
                        value =
                            if (dm != null) {
                                "User notified with a direct message"
                            } else {
                                "Failed to notify user with a direct message"
                            }
                        inline = false
                    }
                    footer {
                        text = "Requested by ${user.asUser().tag}"
                        icon = user.asUser().avatar?.url
                    }
                }
            }
        }

        /**
         * Timeout command
         *
         * @author NoComment/IMS
         */
        ephemeralSlashCommand(::TimeoutArgs) {
            name = "timeout"
            description = "Timeout a user"

            allowRole(FULLMODERATORS)
            allowRole(TRIALMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userArg = arguments.userArgument
                val duration = Clock.System.now().plus(arguments.duration, TimeZone.currentSystemDefault())

                if (guild?.getMember(userArg.id)?.isBot == true || guild?.getRole(MODERATORS)
                        ?.let { guild?.getMember(userArg.id)?.hasRole(it.asRole()) } == true
                ) {
                    respond {
                        content = "You cannot timeout a moderator/bot!"
                    }
                    sentry.breadcrumb(BreadcrumbType.Info) {
                        category = "commands.moderation.timeout.isModOrBot"
                        message = "Lmao someone tried to timeout a bot/moderator"
                        data["timeoutTarget"] = userArg.tag
                    }
                    return@action
                }

                val dm = ResponseHelper.userDMEmbed(
                    userArg,
                    "You have been timed out in ${guild?.fetchGuild()?.name}",
                    "**Duration:**\n${duration.toDiscord(TimestampType.Default) +  "(" + arguments.duration.toString().replace("PT", "") + ")"}\n**Reason:**\n${arguments.reason}",
                    null
                )

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.timeout.timeoutTask"
                    message = "Running Timeout task"
                    data["timeoutTarget"] = userArg.tag
                }

                guild?.getMember(userArg.id)?.edit {
                    timeoutUntil = duration
                }

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.timeout.timeoutTask"
                    message = "Completed Timeout task"
                    data["timeoutTarget"] = userArg.tag
                }

                respond {
                    content = "Timed out ${userArg.id}"
                }

                actionLog.createEmbed {
                    title = "Timeout"
                    color = DISCORD_BLACK
                    timestamp = Clock.System.now()

                    field {
                        name = "User:"
                        value = "${userArg.tag} \n${userArg.id}"
                        inline = false
                    }
                    field {
                        name = "Duration:"
                        value = duration.toDiscord(TimestampType.Default) +  " (" + arguments.duration.toString().replace("PT", "") + ")"
                        inline = false
                    }
                    field {
                        name = "Reason:"
                        value = arguments.reason
                        inline = false
                    }
                    field {
                        name = "User notification"
                        value =
                            if (dm != null) {
                                "User notified with a direct message"
                            } else {
                                "Failed to notify user with a direct message "
                            }
                        inline = false
                    }
                    footer {
                        text = "Requested by ${user.asUser().tag}"
                        icon = user.asUser().avatar?.url
                    }
                }
            }
        }

        /**
         * Timeout removal command
         *
         * @author IMS
         */
        ephemeralSlashCommand(::UnbanArgs) {
            name = "remove-timeout"
            description = "Remove timeout on a user"

            allowRole(FULLMODERATORS)
            allowRole(TRIALMODERATORS)

            action {
                val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
                val userArg = arguments.userArgument

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.remove-timeout.remove-timeoutTask"
                    message = "Running Remove Timeout task"
                    data["removeTimeoutTarget"] = userArg.tag
                }

                guild?.getMember(userArg.id)?.edit {
                    timeoutUntil = null
                }

                sentry.breadcrumb(BreadcrumbType.Info) {
                    category = "commands.moderation.remove-timeout.remove-timeoutTask"
                    message = "Completed Remove Timeout task"
                    data["removeTimeoutTarget"] = userArg.tag
                }

                respond {
                    content = "Removed timeout on ${userArg.id}"
                }

                actionLog.createEmbed {
                    title = "Remove Timeout"
                    color = DISCORD_BLACK
                    timestamp = Clock.System.now()

                    field {
                        name = "User:"
                        value = "${userArg.tag} \n${userArg.id}"
                        inline = false
                    }
                    footer {
                        text = "Requested by ${user.asUser().tag}"
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
        val reason by defaultingString("reason", "The reason for the Kick", "No Reason Provided")
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
