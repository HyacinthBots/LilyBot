package org.hyacinthbots.lilybot.extensions.moderation.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.DISCORD_BLACK
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.annotations.DoNotChain
import dev.kordex.core.checks.hasPermissions
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.*
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralStringSelectMenu
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralMessageCommand
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.pagination.EphemeralResponsePaginator
import dev.kordex.core.pagination.pages.Page
import dev.kordex.core.pagination.pages.Pages
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import dev.kordex.core.utils.*
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import dev.kordex.modules.pluralkit.api.PluralKit
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.ModerationActionCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.TemporaryBanCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.entities.ActionData
import org.hyacinthbots.lilybot.database.entities.TemporaryBanData
import org.hyacinthbots.lilybot.database.entities.TimeData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.moderation.utils.ModerationAction
import org.hyacinthbots.lilybot.utils.*
import kotlin.time.Clock
import kotlin.time.Duration

class ModerationCommands : Extension() {
    override val name = "moderation"

    private val warnSuffix = Translations.Moderation.ModCommands.warnSuffix.translate() +
        "($HYACINTH_GITHUB/LilyBot/blob/main/docs/commands.md#name-warn)"

    /** The scheduler that will track the time for un-banning in temp bans. */
    private val tempBanScheduler = Scheduler()

    /** The task that will run the [tempBanScheduler]. */
    private lateinit var tempBanTask: Task

    @OptIn(DoNotChain::class)
    override suspend fun setup() {
        tempBanTask = tempBanScheduler.schedule(120, repeat = true, callback = ::removeTempBans, name = "Temp ban task")
        ephemeralMessageCommand {
            name = Translations.Moderation.ModCommands.MessageCommand.name
            locking = true

            requirePermission(Permission.BanMembers, Permission.KickMembers, Permission.ModerateMembers)

            check {
                hasPermissions(Permissions(Permission.BanMembers, Permission.KickMembers, Permission.ModerateMembers))
                requireBotPermissions(Permission.BanMembers, Permission.KickMembers, Permission.ModerateMembers)
            }

            action {
                val messageTranslations = Translations.Moderation.ModCommands.MessageCommand
                val messageEvent = event
                val loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!) ?: return@action
                var menuMessage: EphemeralFollowupMessage? = null
                val targetMessage = messageEvent.interaction.getTargetOrNull()
                if (targetMessage == null) {
                    respond {
                        content = messageTranslations.notFound.translate()
                    }
                    return@action
                }
                val senderId: Snowflake
                if (targetMessage.author.isNullOrBot()) {
                    val proxiedMessage =
                        PluralKit(userAgent = this@ephemeralMessageCommand.kord.kordExUserAgent()).getMessageOrNull(
                            targetMessage.id
                        )
                    proxiedMessage ?: run {
                        respond { content = Translations.Basic.UnableTo.findUser.translate() }
                        return@action
                    }
                    senderId = proxiedMessage.sender
                } else {
                    senderId = targetMessage.author!!.id
                }
                val sender = guild!!.getMemberOrNull(senderId)
                    ?: run {
                        respond { content = Translations.Basic.UnableTo.findUser.translate() }
                        return@action
                    }

                isBotOrModerator(event.kord, sender.asUserOrNull(), guild, "moderate") ?: return@action

                menuMessage = respond {
                    content = messageTranslations.howMod.translate()
                    components {
                        ephemeralStringSelectMenu {
                            val selectTranslations = Translations.Moderation.ModCommands.MessageCommand.SelectMenu
                            placeholder = selectTranslations.placeholder
                            maximumChoices = 1 // Prevent selecting multiple options at once

                            option(selectTranslations.ban, ModerationAction.BAN.name)
                            option(selectTranslations.softBan, ModerationAction.SOFT_BAN.name)
                            option(selectTranslations.kick, ModerationAction.KICK.name)
                            option(selectTranslations.timeout, ModerationAction.TIMEOUT.name)
                            option(selectTranslations.warn, ModerationAction.WARN.name)

                            action SelectMenu@{
                                // Get the first because there can only be one
                                val option = event.interaction.values.firstOrNull()
                                if (option == null) {
                                    respond {
                                        content = selectTranslations.noOption.translate()
                                    }
                                    return@SelectMenu
                                }

                                val reasonSuffix =
                                    messageTranslations.reasonSuffix.translate(
                                        targetMessage.content
                                    )
                                val modConfig = ModerationConfigCollection().getConfig(guild!!.id)

                                when (option) {
                                    ModerationAction.BAN.name -> {
                                        val banTranslations = Translations.Moderation.ModCommands.Ban
                                        val quickTranslations = Translations.Moderation.ModCommands.Ban.Quick
                                        val dm = sender.dm {
                                            embed {
                                                title = banTranslations.dmTitle.translate(guild?.asGuildOrNull()?.name)
                                                description =
                                                    modConfig?.banDmMessage ?: quickTranslations.defaultDesc.translate(
                                                        reasonSuffix
                                                    )

                                                color = DISCORD_GREEN
                                            }
                                        }
                                        ModerationActionCollection().addAction(
                                            ModerationAction.BAN, guild!!.id, senderId,
                                            ActionData(
                                                user.id,
                                                null,
                                                null,
                                                quickTranslations.defaultDesc.translate(reasonSuffix),
                                                dm != null,
                                                true,
                                                null
                                            )
                                        )

                                        sender.ban {
                                            reason = quickTranslations.defaultDesc.translate(reasonSuffix)
                                        }

                                        if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
                                            try {
                                                targetMessage.reply {
                                                    embed {
                                                        title = quickTranslations.embedTitle.translate()
                                                        description =
                                                            quickTranslations.embedDescMessage.translate(sender.mention)
                                                    }
                                                }
                                            } catch (_: KtorRequestException) {
                                                channel.createEmbed {
                                                    title = quickTranslations.embedTitle.translate()
                                                    description =
                                                        quickTranslations.embedDescDeleted.translate(sender.mention)
                                                }
                                            }
                                        }

                                        menuMessage?.edit {
                                            content = banTranslations.response.translate()
                                            components { removeAll() }
                                        }
                                    }

                                    ModerationAction.SOFT_BAN.name -> {
                                        val softBanTranslations = Translations.Moderation.ModCommands.SoftBan
                                        val quickTranslations = Translations.Moderation.ModCommands.SoftBan.Quick
                                        val dm = sender.dm {
                                            embed {
                                                title =
                                                    softBanTranslations.dmTitle.translate(guild?.asGuildOrNull()?.name)
                                                description = softBanTranslations.dmDesc.translate(reasonSuffix)
                                            }
                                        }

                                        ModerationActionCollection().addAction(
                                            ModerationAction.SOFT_BAN, guild!!.id, senderId,
                                            ActionData(
                                                user.id,
                                                null,
                                                null,
                                                Translations.Moderation.ModCommands.Ban.Quick.defaultDesc.translate(
                                                    reasonSuffix
                                                ),
                                                dm != null,
                                                true,
                                                null
                                            )
                                        )

                                        sender.ban {
                                            reason =
                                                Translations.Moderation.ModCommands.Ban.Quick.defaultDesc.translate(
                                                    reasonSuffix
                                                )
                                        }

                                        ModerationActionCollection().shouldIgnoreAction(
                                            ModerationAction.SOFT_BAN, guild!!.id, senderId
                                        )

                                        guild!!.unban(senderId, "Quick soft-ban unban")

                                        if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
                                            try {
                                                targetMessage.reply {
                                                    embed {
                                                        title = quickTranslations.embedTitle.translate()
                                                        description =
                                                            quickTranslations.embedDescMessage.translate(sender.mention)
                                                    }
                                                }
                                            } catch (_: KtorRequestException) {
                                                channel.createEmbed {
                                                    title = quickTranslations.embedTitle.translate()
                                                    description =
                                                        quickTranslations.embedDescDeleted.translate(sender.mention)
                                                }
                                            }
                                        }

                                        menuMessage?.edit {
                                            content = softBanTranslations.response.translate()
                                            components { removeAll() }
                                        }
                                    }

                                    ModerationAction.KICK.name -> {
                                        val kickTranslations = Translations.Moderation.ModCommands.Kick
                                        val quickTranslations = Translations.Moderation.ModCommands.Kick.Quick
                                        val dm = sender.dm {
                                            embed {
                                                title = kickTranslations.dmTitle.translate(guild?.asGuildOrNull()?.name)
                                                description = quickTranslations.dmDesc.translate(reasonSuffix)
                                            }
                                        }

                                        guild!!.kick(senderId, "Quick kicked ")

                                        if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
                                            try {
                                                targetMessage.reply {
                                                    embed {
                                                        title = quickTranslations.embedTitle.translate()
                                                        description =
                                                            quickTranslations.embedDescMessage.translate(sender.mention)
                                                    }
                                                }
                                            } catch (_: KtorRequestException) {
                                                channel.createEmbed {
                                                    title = quickTranslations.embedTitle.translate()
                                                    description =
                                                        quickTranslations.embedDescDeleted.translate(sender.mention)
                                                }
                                            }
                                        }

                                        ModerationActionCollection().addAction(
                                            ModerationAction.KICK, guild!!.id, senderId,
                                            ActionData(
                                                user.id,
                                                null,
                                                null,
                                                quickTranslations.actionDesc.translate(reasonSuffix),
                                                dm != null,
                                                true,
                                                null
                                            )
                                        )

                                        menuMessage?.edit {
                                            content = kickTranslations.response.translate()
                                            components { removeAll() }
                                        }
                                    }

                                    ModerationAction.TIMEOUT.name -> {
                                        val timeoutTranslations = Translations.Moderation.ModCommands.Timeout
                                        val quickTranslations = Translations.Moderation.ModCommands.Timeout.Quick
                                        val timeoutTime =
                                            ModerationConfigCollection().getConfig(guild!!.id)?.quickTimeoutLength
                                        if (timeoutTime == null) {
                                            menuMessage?.edit {
                                                content = timeoutTranslations.noLength.translate()
                                                components { removeAll() }
                                            }
                                            return@SelectMenu
                                        }

                                        val dm = sender.dm {
                                            embed {
                                                title =
                                                    timeoutTranslations.dmTitle.translate(guild?.asGuildOrNull()?.name)
                                                description = quickTranslations.dmDesc.translate(
                                                    timeoutTime.interval(),
                                                    reasonSuffix
                                                )
                                            }
                                        }

                                        sender.timeout(timeoutTime, reason = "Quick timed-out $reasonSuffix")

                                        if (modConfig?.publicLogging != null && modConfig.publicLogging == true) {
                                            try {
                                                targetMessage.reply {
                                                    embed {
                                                        title = quickTranslations.embedTitle.translate()
                                                        description = quickTranslations.embedDescMessage.translate(
                                                            sender.mention,
                                                            timeoutTime.interval()
                                                        )
                                                    }
                                                }
                                            } catch (_: KtorRequestException) {
                                                channel.createEmbed {
                                                    title = quickTranslations.embedTitle.translate()
                                                    description = quickTranslations.embedDescDeleted.translate(
                                                        sender.mention,
                                                        timeoutTime.interval()
                                                    )
                                                }
                                            }
                                        }

                                        ModerationActionCollection().addAction(
                                            ModerationAction.TIMEOUT, guild!!.id, senderId,
                                            ActionData(
                                                user.id,
                                                null,
                                                TimeData(timeoutTime, null, null, null),
                                                quickTranslations.actionDesc.translate(reasonSuffix),
                                                dm != null,
                                                null,
                                                null
                                            )
                                        )

                                        menuMessage?.edit {
                                            content = timeoutTranslations.response.translate()
                                            components { removeAll() }
                                        }
                                    }

                                    ModerationAction.WARN.name -> {
                                        val warnTranslations = Translations.Moderation.ModCommands.Warning
                                        WarnCollection().setWarn(senderId, guild!!.id, false)
                                        val strikes = WarnCollection().getWarn(senderId, guild!!.id)?.strikes

                                        val dm = sender.dm {
                                            embed {
                                                title = warnTranslations.dmTitle.translate(
                                                    strikes, guild?.asGuildOrNull()?.name
                                                )
                                                description =
                                                    warnTranslations.quickDmDesc.translate(reasonSuffix, warnSuffix)
                                            }
                                        }

                                        if (modConfig?.autoPunishOnWarn == true && strikes!! > 1) {
                                            val duration = when (strikes) {
                                                2 -> "PT3H"
                                                3 -> "PT12H"
                                                else -> "P3D"
                                            }
                                            guild?.getMemberOrNull(senderId)?.edit {
                                                timeoutUntil = Clock.System.now().plus(Duration.parse(duration))
                                            }
                                        }

                                        loggingChannel.createMessage {
                                            embed {
                                                title = warnTranslations.embedTitle.translate()
                                                baseModerationEmbed(
                                                    warnTranslations.quickWarned.translate(),
                                                    sender,
                                                    user
                                                )
                                                dmNotificationStatusEmbedField(dm, true)
                                                timestamp = Clock.System.now()
                                                field {
                                                    name = warnTranslations.embedStrikeTot.translate()
                                                    value = strikes.toString()
                                                }
                                            }
                                            if (modConfig?.autoPunishOnWarn == true && strikes != 1) {
                                                embed {
                                                    warnTimeoutLog(
                                                        strikes!!,
                                                        event.interaction.user.asUserOrNull(),
                                                        sender.asUserOrNull(),
                                                        warnTranslations.quickWarned.translate()
                                                    )
                                                }
                                            }
                                        }

                                        menuMessage?.edit {
                                            content = warnTranslations.response.translate()
                                            components { removeAll() }
                                        }

                                        if (modConfig?.publicLogging == true) {
                                            try {
                                                targetMessage.reply {
                                                    embed {
                                                        title = warnTranslations.embedTitle.translate()
                                                        description =
                                                            warnTranslations.embedDescMessage.translate(sender.mention)
                                                    }
                                                }
                                            } catch (_: KtorRequestException) {
                                                channel.createEmbed {
                                                    title = warnTranslations.embedTitle.translate()
                                                    description =
                                                        warnTranslations.embedDescDeleted.translate(sender.mention)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        ephemeralSlashCommand(::BanArgs) {
            name = Translations.Moderation.ModCommands.Ban.name
            description = Translations.Moderation.ModCommands.Ban.description

            requirePermission(Permission.BanMembers)

            check {
                modCommandChecks(Permission.BanMembers)
                requireBotPermissions(Permission.BanMembers)
            }

            action {
                val translations = Translations.Moderation.ModCommands.Ban
                isBotOrModerator(event.kord, arguments.userArgument, guild, "ban") ?: return@action

                // The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
                if (arguments.messages > 7 || arguments.messages < 0) {
                    respond { content = translations.invalidMessages.translate() }
                    return@action
                }

                val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
                var dmStatus: Message? = null
                val dmControl = if (arguments.dm != null) arguments.dm!! else modConfig?.dmDefault == true
                if (dmControl) {
                    dmStatus = arguments.userArgument.dm {
                        embed {
                            title = translations.dmTitle.translate(guild?.asGuildOrNull()?.name)
                            description = "${translations.dmDesc.translate()}\n${
                                if (modConfig?.banDmMessage != null &&
                                    arguments.reason == Translations.Basic.noReason.translate()
                                ) {
                                    modConfig.banDmMessage
                                } else if (modConfig?.banDmMessage != null &&
                                    arguments.reason != Translations.Basic.noReason.translate()
                                ) {
                                    "${arguments.reason}\n${modConfig.banDmMessage}"
                                } else {
                                    arguments.reason
                                }
                            }\n${
                                if (arguments.softBan) {
                                    translations.wasSoft.translate()
                                } else {
                                    ""
                                }
                            }"
                        }
                    }
                }

                ModerationActionCollection().addAction(
                    if (arguments.softBan) ModerationAction.SOFT_BAN else ModerationAction.BAN,
                    guild!!.id, arguments.userArgument.id,
                    ActionData(
                        user.id,
                        arguments.messages,
                        null,
                        arguments.reason,
                        dmStatus != null,
                        arguments.dm,
                        arguments.image?.url
                    )
                )

                if (modConfig?.publicLogging == true) {
                    event.interaction.channel.createEmbed {
                        if (arguments.softBan) {
                            title = Translations.Moderation.ModCommands.SoftBan.response.translate()
                            description = translations.publicSoftDesc.translate()
                        } else {
                            title = translations.response.translate()
                            description = translations.publicDesc.translate()
                        }
                        color = DISCORD_BLACK
                    }
                }

                guild?.ban(arguments.userArgument.id) {
                    reason = arguments.reason + if (arguments.softBan) " **SOFT-BAN**" else ""
                    deleteMessageDuration = if (arguments.softBan && arguments.messages == 0) {
                        DateTimePeriod(days = 3).toDuration(TimeZone.UTC)
                    } else {
                        DateTimePeriod(days = arguments.messages).toDuration(TimeZone.UTC)
                    }
                }

                if (arguments.softBan) {
                    ModerationActionCollection().declareActionToIgnore(
                        ModerationAction.UNBAN, guild?.id!!, arguments.userArgument.id
                    )
                    guild?.unban(arguments.userArgument.id, "User was soft-banned. **SOFT-BAN**")
                }

                respond {
                    content =
                        if (arguments.softBan) {
                            Translations.Moderation.ModCommands.SoftBan.Quick.embedTitle.translate()
                        } else {
                            Translations.Moderation.ModCommands.Ban.Quick.embedTitle.translate()
                        } + " " + arguments.userArgument.mention
                }
            }
        }

        ephemeralSlashCommand {
            name = Translations.Moderation.ModCommands.TempBan.name
            description = Translations.Moderation.ModCommands.TempBan.description

            ephemeralSubCommand(::TempBanArgs) {
                name = Translations.Moderation.ModCommands.TempBan.Add.name
                description = Translations.Moderation.ModCommands.TempBan.Add.description

                requirePermission(Permission.BanMembers)
                check {
                    modCommandChecks(Permission.BanMembers)
                    requireBotPermissions(Permission.BanMembers)
                }

                action {
                    val translations = Translations.Moderation.ModCommands.TempBan.Add
                    isBotOrModerator(event.kord, arguments.userArgument, guild, "temp-ban add")
                    val now = Clock.System.now()
                    val duration = now.plus(arguments.duration, TimeZone.UTC)
                    val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
                    var dmStatus: Message? = null
                    val dmControl = if (arguments.dm != null) arguments.dm!! else modConfig?.dmDefault == true
                    if (dmControl) {
                        dmStatus = arguments.userArgument.dm {
                            embed {
                                title = translations.dmTitle.translate(guild?.asGuildOrNull()?.name)
                                description = translations.dmDesc.translate(arguments.reason, duration)
                            }
                        }
                    }

                    ModerationActionCollection().addAction(
                        ModerationAction.TEMP_BAN, guild!!.id, arguments.userArgument.id,
                        ActionData(
                            user.id,
                            arguments.messages,
                            TimeData(arguments.duration, duration),
                            arguments.reason,
                            dmStatus != null,
                            arguments.dm,
                            arguments.image?.url
                        )
                    )

                    if (modConfig?.publicLogging == true) {
                        event.interaction.channel.createEmbed {
                            title = translations.publicEmbedTitle.translate()
                            description = translations.publicEmbedDesc.translate(arguments.userArgument.mention)
                            color = DISCORD_BLACK
                        }
                    }

                    TemporaryBanCollection().setTempBan(
                        TemporaryBanData(guild!!.id, arguments.userArgument.id, user.id, now, duration)
                    )

                    guild?.ban(arguments.userArgument.id) {
                        reason = arguments.reason + " **TEMPORARY-BAN**"
                        deleteMessageDuration = DateTimePeriod(days = arguments.messages).toDuration(TimeZone.UTC)
                    }

                    respond {
                        content = translations.response.translate(arguments.userArgument.mention)
                    }
                }
            }

            ephemeralSubCommand {
                name = Translations.Moderation.ModCommands.TempBan.View.name
                description = Translations.Moderation.ModCommands.TempBan.View.description

                requirePermission(Permission.BanMembers)

                check {
                    modCommandChecks(Permission.BanMembers)
                    requireBotPermissions(Permission.BanMembers)
                }

                action {
                    val translations = Translations.Moderation.ModCommands.TempBan.View
                    val pageTranslations = Translations.Moderation.ModCommands.TempBan.View.Page
                    val pagesObj = Pages()
                    val tempBans = TemporaryBanCollection().getTempBansForGuild(guild!!.id)
                    if (tempBans.isEmpty()) {
                        pagesObj.addPage(
                            Page {
                                description = translations.none.translate()
                            }
                        )
                    } else {
                        tempBans.chunked(4).forEach { tempBan ->
                            var content = ""
                            tempBan.forEach {
                                content = """
									${pageTranslations.user.translate(this@ephemeralSubCommand.kord.getUser(it.bannedUserId)?.username)}
									${pageTranslations.mod.translate(guild?.getMemberOrNull(it.moderatorUserId)?.username)}
									${pageTranslations.start.translate(it.startTime.toDiscord(TimestampType.ShortDateTime))} (${
                                    it.startTime.toDiscord(TimestampType.RelativeTime)
                                })
									${pageTranslations.end.translate(it.endTime.toDiscord(TimestampType.ShortDateTime))} (${
                                    it.endTime.toDiscord(TimestampType.RelativeTime)
                                })
									---
								""".trimIndent()
                            }

                            pagesObj.addPage(
                                Page {
                                    title = pageTranslations.title.translate(guild?.asGuildOrNull()?.name)
                                    description = content
                                }
                            )
                        }
                    }

                    val paginator = EphemeralResponsePaginator(
                        pages = pagesObj,
                        owner = event.interaction.user,
                        timeoutSeconds = 300,
                        interaction = interactionResponse
                    )

                    paginator.send()
                }
            }
        }

        ephemeralSlashCommand(::UnbanArgs) {
            name = Translations.Moderation.ModCommands.Unban.name
            description = Translations.Moderation.ModCommands.Unban.description

            requirePermission(Permission.BanMembers)

            check {
                modCommandChecks(Permission.BanMembers)
                requireBotPermissions(Permission.BanMembers)
            }

            action {
                val tempBan = TemporaryBanCollection().getUserTempBan(this.getGuild()!!.id, arguments.userArgument.id)
                if (tempBan == null) {
                    ModerationActionCollection().addAction(
                        ModerationAction.UNBAN, guild!!.id, arguments.userArgument.id,
                        ActionData(
                            user.id, null, null, arguments.reason, null, null, null
                        )
                    )
                    guild?.unban(arguments.userArgument.id, arguments.reason)
                } else {
                    ModerationActionCollection().addAction(
                        ModerationAction.UNBAN, guild!!.id, arguments.userArgument.id,
                        ActionData(
                            user.id, null, null, arguments.reason + "**TEMPORARY-BAN", null, null, null
                        )
                    )
                    guild?.unban(arguments.userArgument.id, arguments.reason + "**TEMPORARY-BAN**")
                    TemporaryBanCollection().removeTempBan(guild!!.id, arguments.userArgument.id)
                }
                respond {
                    content = Translations.Moderation.ModCommands.Unban.response.translate()
                }
            }
        }

        ephemeralSlashCommand(::KickArgs) {
            name = Translations.Moderation.ModCommands.Kick.name
            description = Translations.Moderation.ModCommands.Kick.description

            requirePermission(Permission.KickMembers)

            check {
                modCommandChecks(Permission.KickMembers)
                requireBotPermissions(Permission.KickMembers)
            }

            action {
                isBotOrModerator(event.kord, arguments.userArgument, guild, "kick") ?: return@action

                val translations = Translations.Moderation.ModCommands.Kick
                val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
                var dmStatus: Message? = null
                val dmControl = if (arguments.dm != null) arguments.dm!! else modConfig?.dmDefault == true
                if (dmControl) {
                    dmStatus = arguments.userArgument.dm {
                        embed {
                            title = translations.dmTitle.translate(guild?.fetchGuild()?.name)
                            description = translations.dmDesc.translate(arguments.reason)
                        }
                    }
                }
                ModerationActionCollection().addAction(
                    ModerationAction.KICK, guild!!.id, arguments.userArgument.id,
                    ActionData(
                        user.id, null, null, arguments.reason, dmStatus != null, arguments.dm, arguments.image?.url
                    )
                )

                if (modConfig?.publicLogging == true) {
                    event.interaction.channel.createEmbed {
                        title = translations.response.translate()
                        description = translations.embedDesc.translate(arguments.userArgument.mention)
                        color = DISCORD_BLACK
                    }
                }

                guild?.kick(arguments.userArgument.id, arguments.reason)

                respond {
                    content = translations.response.translate()
                }
            }
        }

        ephemeralSlashCommand(::TimeoutArgs) {
            name = Translations.Moderation.ModCommands.Timeout.name
            description = Translations.Moderation.ModCommands.Timeout.description

            requirePermission(Permission.ModerateMembers)

            check {
                modCommandChecks(Permission.ModerateMembers)
                requireBotPermissions(Permission.ModerateMembers)
            }

            action {
                val translations = Translations.Moderation.ModCommands.Timeout
                val modConfig = ModerationConfigCollection().getConfig(guild!!.id)
                val durationArg = arguments.duration ?: modConfig?.quickTimeoutLength ?: DateTimePeriod(hours = 6)
                val duration = Clock.System.now().plus(durationArg, TimeZone.UTC)

                isBotOrModerator(event.kord, arguments.userArgument, guild, "timeout") ?: return@action

                var dmStatus: Message? = null
                val dmControl = if (arguments.dm != null) arguments.dm!! else modConfig?.dmDefault == true
                if (dmControl) {
                    dmStatus = arguments.userArgument.dm {
                        embed {
                            title = translations.dmTitle.translate(guild?.fetchGuild()?.name)
                            description = translations.dmDesc.translate(
                                duration.toDiscord(TimestampType.Default),
                                durationArg.interval(),
                                arguments.reason
                            )
                        }
                    }
                }

                ModerationActionCollection().addAction(
                    ModerationAction.TIMEOUT, guild!!.id, arguments.userArgument.id,
                    ActionData(
                        user.id,
                        null,
                        TimeData(durationArg, duration, Clock.System.now(), duration),
                        arguments.reason,
                        dmStatus != null,
                        arguments.dm,
                        arguments.image?.url
                    )
                )

                if (modConfig?.publicLogging == true) {
                    event.interaction.channel.createEmbed {
                        title = Translations.Moderation.ModCommands.Timeout.Quick.embedTitle.translate()
                        description = translations.embedDesc.translate(arguments.userArgument.mention)
                        color = DISCORD_BLACK
                        field {
                            name = translations.duration.translate()
                            value = duration.toDiscord(TimestampType.Default) + " (${durationArg.interval()})"
                            inline = false
                        }
                    }
                }

                arguments.userArgument.asMemberOrNull(guild!!.id)?.edit {
                    timeoutUntil = duration
                }

                respond {
                    content = translations.response.translate()
                }
            }
        }

        ephemeralSlashCommand(::RemoveTimeoutArgs) {
            name = Translations.Moderation.ModCommands.RemoveTimeout.name
            description = Translations.Moderation.ModCommands.RemoveTimeout.description

            requirePermission(Permission.ModerateMembers)

            check {
                modCommandChecks(Permission.ModerateMembers)
                requireBotPermissions(Permission.ModerateMembers)
            }

            action {
                val translations = Translations.Moderation.ModCommands.RemoveTimeout
                val config = ModerationConfigCollection().getConfig(guild!!.id)
                var dmStatus: Message? = null
                val dmControl = if (arguments.dm != null) arguments.dm!! else config?.dmDefault == true
                if (dmControl) {
                    dmStatus = arguments.userArgument.dm {
                        embed {
                            title = translations.dmTitle.translate(guild?.fetchGuild()?.name)
                            description = translations.dmDesc.translate()
                        }
                    }
                }

                ModerationActionCollection().addAction(
                    ModerationAction.REMOVE_TIMEOUT, guild!!.id, arguments.userArgument.id,
                    ActionData(
                        user.id, null, null, null, dmStatus != null, arguments.dm, null
                    )
                )

                arguments.userArgument.asMemberOrNull(guild!!.id)?.edit {
                    timeoutUntil = null
                }

                respond {
                    content = translations.response.translate()
                }
            }
        }

        ephemeralSlashCommand(::WarnArgs) {
            name = Translations.Moderation.ModCommands.Warning.name
            description = Translations.Moderation.ModCommands.Warning.description

            requirePermission(Permission.ModerateMembers)

            check {
                modCommandChecks(Permission.ModerateMembers)
                requireBotPermissions(Permission.ModerateMembers)
            }

            action {
                val translations = Translations.Moderation.ModCommands.Warning
                val config = ModerationConfigCollection().getConfig(guild!!.id)!!
                val actionLog =
                    getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
                val guildName = guild?.asGuildOrNull()?.name

                isBotOrModerator(event.kord, arguments.userArgument, guild, "warn") ?: return@action

                WarnCollection().setWarn(arguments.userArgument.id, guild!!.id, false)
                val strikes = WarnCollection().getWarn(arguments.userArgument.id, guild!!.id)?.strikes

                var dmStatus: Message? = null

                val dmControl = if (arguments.dm != null) arguments.dm!! else config.dmDefault == true

                if (dmControl) {
                    val warnText = if (config.autoPunishOnWarn == false) {
                        "${translations.noAction.translate()}\n $warnSuffix"
                    } else {
                        when (strikes) {
                            1 -> "${translations.noAction.translate()}\n $warnSuffix"
                            2 -> "${translations.action3h.translate()}\n $warnSuffix"
                            3 -> "${translations.action12h.translate()}\n $warnSuffix"
                            else -> "${translations.action3d.translate()}\n $warnSuffix"
                        }
                    }

                    dmStatus = arguments.userArgument.dm {
                        embed {
                            title = translations.dmTitle.translate(strikes, guildName)
                            description = translations.dmDesc.translate(arguments.reason, warnText)
                        }
                    }
                }

                if (config.autoPunishOnWarn == true && strikes!! > 1) {
                    val duration = when (strikes) {
                        2 -> "PT3H"
                        3 -> "PT12H"
                        else -> "P3D"
                    }
                    guild?.getMemberOrNull(arguments.userArgument.id)?.edit {
                        timeoutUntil = Clock.System.now().plus(Duration.parse(duration))
                    }
                }

                actionLog.createMessage {
                    embed {
                        title = translations.embedTitle.translate()
                        image = arguments.image?.url
                        baseModerationEmbed(arguments.reason, arguments.userArgument, user)
                        dmNotificationStatusEmbedField(dmStatus, dmControl)
                        timestamp = Clock.System.now()
                        field {
                            name = translations.embedStrikeTot.translate()
                            value = strikes.toString()
                        }
                        color = DISCORD_RED
                    }
                    if (config.autoPunishOnWarn == true && strikes != 1) {
                        embed {
                            warnTimeoutLog(
                                strikes!!,
                                event.interaction.user.asUserOrNull(),
                                arguments.userArgument,
                                arguments.reason
                            )
                        }
                    }
                }

                if (config.publicLogging != null && config.publicLogging == true) {
                    channel.createEmbed {
                        title = translations.embedTitle.translate()
                        description = translations.embedDesc.translate(arguments.userArgument.mention)
                        color = DISCORD_RED
                    }
                }

                respond {
                    content = translations.response.translate()
                }
            }
        }

        ephemeralSlashCommand(::RemoveWarnArgs) {
            name = Translations.Moderation.ModCommands.RemoveWarning.name
            description = Translations.Moderation.ModCommands.RemoveWarning.description

            requirePermission(Permission.ModerateMembers)

            check {
                modCommandChecks(Permission.ModerateMembers)
                requireBotPermissions(Permission.ModerateMembers)
            }

            action {
                val translations = Translations.Moderation.ModCommands.RemoveWarning
                val config = ModerationConfigCollection().getConfig(guild!!.id)!!
                val targetUser = guild?.getMemberOrNull(arguments.userArgument.id) ?: run {
                    respond {
                        content = translations.unableToFind.translate()
                    }
                    return@action
                }

                var userStrikes = WarnCollection().getWarn(targetUser.id, guild!!.id)?.strikes
                if (userStrikes == 0 || userStrikes == null) {
                    respond {
                        content = translations.noStrikes.translate()
                    }
                    return@action
                }

                WarnCollection().setWarn(targetUser.id, guild!!.id, true)
                userStrikes = WarnCollection().getWarn(targetUser.id, guild!!.id)?.strikes

                var dmStatus: Message? = null
                if (arguments.dm) {
                    dmStatus = targetUser.dm {
                        embed {
                            title = translations.dmTitle.translate(guild?.fetchGuild()?.name)
                            description = translations.dmDesc.translate(userStrikes)
                            color = DISCORD_GREEN
                        }
                    }
                }

                val actionLog =
                    getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
                actionLog.createEmbed {
                    title = translations.embedTitle.translate()
                    color = DISCORD_GREEN
                    timestamp = Clock.System.now()
                    baseModerationEmbed(null, targetUser, user)
                    dmNotificationStatusEmbedField(dmStatus, arguments.dm)
                    field {
                        name = Translations.Moderation.ModCommands.Warning.embedStrikeTot.translate()
                        value = userStrikes.toString()
                        inline = false
                    }
                }

                if (config.publicLogging != null && config.publicLogging == true) {
                    channel.createEmbed {
                        title = translations.embedTitle.translate()
                        description = translations.embedDesc.translate(arguments.userArgument.mention)
                        color = DISCORD_GREEN
                    }
                }

                respond {
                    content = translations.response.translate()
                }
            }
        }
    }

    private suspend fun removeTempBans() {
        val tempBans = TemporaryBanCollection().getAllTempBans()
        val dueTempBans =
            tempBans.filter { it.endTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0 }

        for (it in dueTempBans) {
            var guild: Guild?
            try {
                guild = kord.getGuildOrNull(it.guildId)
            } catch (_: KtorRequestException) {
                TemporaryBanCollection().removeTempBan(it.guildId, it.bannedUserId)
                continue
            }

            if (guild == null) {
                TemporaryBanCollection().removeTempBan(it.guildId, it.bannedUserId)
                continue
            }

            ModerationActionCollection().addAction(
                ModerationAction.UNBAN, guild.id, it.bannedUserId,
                ActionData(
                    it.moderatorUserId,
                    null,
                    TimeData(null, null, it.startTime, it.endTime),
                    "**temporary-ban-expire**",
                    null,
                    null,
                    null
                )
            )

            guild.unban(it.bannedUserId, "Temporary Ban expired")
            TemporaryBanCollection().removeTempBan(it.guildId, it.bannedUserId)
        }
    }

    inner class BanArgs : Arguments() {
        /** The user to ban. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** The number of days worth of messages to delete. */
        val messages by int {
            name = Translations.Moderation.ModCommands.Arguments.Messages.name
            description = Translations.Moderation.ModCommands.Arguments.Messages.description
        }

        /** The reason for the ban. */
        val reason by defaultingString {
            name = Translations.Moderation.ModCommands.Arguments.Reason.name
            description = Translations.Moderation.ModCommands.Arguments.Reason.description
            defaultValue = Translations.Moderation.ModCommands.Arguments.Reason.default.translate()
        }

        /** Weather to softban this user or not. */
        val softBan by defaultingBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Soft.name
            description = Translations.Moderation.ModCommands.Arguments.Soft.description
            defaultValue = false
        }

        /** Whether to DM the user or not. */
        val dm by optionalBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
        }

        /** An image that the user wishes to provide for context to the ban. */
        val image by optionalAttachment {
            name = Translations.Moderation.ModCommands.Arguments.Image.name
            description = Translations.Moderation.ModCommands.Arguments.Image.description
        }
    }

    inner class TempBanArgs : Arguments() {
        /** The user to ban. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** The number of days worth of messages to delete. */
        val messages by int {
            name = Translations.Moderation.ModCommands.Arguments.Messages.name
            description = Translations.Moderation.ModCommands.Arguments.Messages.description
        }

        /** The duration of the temporary ban. */
        val duration by coalescingDuration {
            name = Translations.Moderation.ModCommands.TempBan.Arguments.Duration.name
            description = Translations.Moderation.ModCommands.TempBan.Arguments.Duration.description
        }

        /** The reason for the ban. */
        val reason by defaultingString {
            name = Translations.Moderation.ModCommands.Arguments.Reason.name
            description = Translations.Moderation.ModCommands.Arguments.Reason.description
            defaultValue = Translations.Moderation.ModCommands.Arguments.Reason.default.translate()
        }

        /** Whether to DM the user or not. */
        val dm by optionalBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
        }

        /** An image that the user wishes to provide for context to the ban. */
        val image by optionalAttachment {
            name = Translations.Moderation.ModCommands.Arguments.Image.name
            description = Translations.Moderation.ModCommands.Arguments.Image.description
        }
    }

    inner class UnbanArgs : Arguments() {
        /** The ID of the user to unban. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** The reason for the un-ban. */
        val reason by defaultingString {
            name = Translations.Moderation.ModCommands.Arguments.Reason.name
            description = Translations.Moderation.ModCommands.Arguments.Reason.description
            defaultValue = Translations.Moderation.ModCommands.Arguments.Reason.default.translate()
        }
    }

    inner class KickArgs : Arguments() {
        /** The user to kick. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** The reason for the kick. */
        val reason by defaultingString {
            name = Translations.Moderation.ModCommands.Arguments.Reason.name
            description = Translations.Moderation.ModCommands.Arguments.Reason.description
            defaultValue = Translations.Moderation.ModCommands.Arguments.Reason.default.translate()
        }

        /** Whether to DM the user or not. */
        val dm by optionalBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
        }

        /** An image that the user wishes to provide for context to the kick. */
        val image by optionalAttachment {
            name = Translations.Moderation.ModCommands.Arguments.Image.name
            description = Translations.Moderation.ModCommands.Arguments.Image.description
        }
    }

    inner class TimeoutArgs : Arguments() {
        /** The requested user to timeout. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** The time the timeout should last for. */
        val duration by coalescingOptionalDuration {
            name = Translations.Moderation.ModCommands.TempBan.Arguments.Duration.name
            description = Translations.Moderation.ModCommands.Timeout.Arguments.Duration.description
        }

        /** The reason for the timeout. */
        val reason by defaultingString {
            name = Translations.Moderation.ModCommands.Arguments.Reason.name
            description = Translations.Moderation.ModCommands.Arguments.Reason.description
            defaultValue = Translations.Moderation.ModCommands.Arguments.Reason.default.translate()
        }

        /** Whether to DM the user or not. */
        val dm by optionalBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
        }

        /** An image that the user wishes to provide for context to the kick. */
        val image by optionalAttachment {
            name = Translations.Moderation.ModCommands.Arguments.Image.name
            description = Translations.Moderation.ModCommands.Arguments.Image.description
        }
    }

    inner class RemoveTimeoutArgs : Arguments() {
        /** The requested user to remove the timeout from. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** Whether to DM the user about the timeout removal or not. */
        val dm by optionalBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
        }
    }

    inner class WarnArgs : Arguments() {
        /** The requested user to warn. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** The reason for the warning. */
        val reason by defaultingString {
            name = Translations.Moderation.ModCommands.Arguments.Reason.name
            description = Translations.Moderation.ModCommands.Arguments.Reason.description
            defaultValue = Translations.Moderation.ModCommands.Arguments.Reason.default.translate()
        }

        /** Whether to DM the user or not. */
        val dm by optionalBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
        }

        /** An image that the user wishes to provide for context to the kick. */
        val image by optionalAttachment {
            name = Translations.Moderation.ModCommands.Arguments.Image.name
            description = Translations.Moderation.ModCommands.Arguments.Image.description
        }
    }

    inner class RemoveWarnArgs : Arguments() {
        /** The requested user to remove the warning from. */
        val userArgument by user {
            name = Translations.Moderation.ModCommands.Arguments.User.name
            description = Translations.Moderation.ModCommands.Arguments.User.description
        }

        /** Whether to DM the user or not. */
        val dm by defaultingBoolean {
            name = Translations.Moderation.ModCommands.Arguments.Dm.name
            description = Translations.Moderation.ModCommands.Arguments.Dm.description
            defaultValue = true
        }
    }
}

/**
 * Creates a log for timeouts produced by a number of warnings.
 *
 * @param warningNumber The number of warning strikes the user has
 * @param moderator The moderator that actioned the warning
 * @param targetUser The User that was warned
 * @param reason The reason for the warning
 * @author NoComment1105
 * @since 4.4.0
 */
private fun EmbedBuilder.warnTimeoutLog(warningNumber: Int, moderator: User, targetUser: User, reason: String) {
    val translations = Translations.Moderation.ModCommands.Warning.Log
    when (warningNumber) {
        1 -> {}
        2 -> description = translations.action3h.translate(targetUser.mention)

        3 -> description = translations.action12h.translate(targetUser.mention)

        else ->
            description = translations.action3d.translate(targetUser.mention, warningNumber)
    }

    if (warningNumber != 1) {
        title = Translations.Moderation.ModCommands.Timeout.Quick.embedTitle.translate()
        field {
            name = Translations.Basic.userField.translate()
            value = "${targetUser.id} (${targetUser.username})"
        }
        field {
            name = translations.reason.translate()
            value = reason
        }
        footer {
            text = moderator.username
            icon = moderator.avatar?.cdnUrl?.toUrl()
        }
        color = DISCORD_BLACK
        timestamp = Clock.System.now()
    }
}
