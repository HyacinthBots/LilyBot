@file:Suppress("DuplicatedCode")

package org.hyacinthbots.lilybot.extensions.config.utils

import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.rest.builder.message.EmbedBuilder
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.extensions.logging.config.LoggingArgs
import org.hyacinthbots.lilybot.extensions.moderation.config.ModerationArgs
import org.hyacinthbots.lilybot.extensions.utility.config.UtilityArgs
import org.hyacinthbots.lilybot.utils.interval
import org.hyacinthbots.lilybot.utils.trimmedContents

suspend fun EmbedBuilder.utilityEmbed(arguments: UtilityArgs, user: UserBehavior) {
    val obj = Translations.Config.Utility.Embed
    title = obj.title.translate()
    field {
        name = obj.utilityFieldName.translate()
        value = if (arguments.utilityLogChannel != null) {
            "${arguments.utilityLogChannel!!.mention} ${arguments.utilityLogChannel!!.data.name.value}"
        } else {
            Translations.Basic.disabled.translate()
        }
    }
    field {
        name = obj.channelUpdates.translate()
        value =
            if (arguments.logChannelUpdates) Translations.Basic.yes.translate() else Translations.Basic.no.translate()
    }
    field {
        name = obj.eventUpdates.translate()
        value = if (arguments.logEventUpdates) Translations.Basic.yes.translate() else Translations.Basic.no.translate()
    }
    field {
        name = obj.inviteUpdates.translate()
        value =
            if (arguments.logInviteUpdates) Translations.Basic.yes.translate() else Translations.Basic.no.translate()
    }
    field {
        name = obj.roleUpdates.translate()
        value = if (arguments.logRoleUpdates) Translations.Basic.yes.translate() else Translations.Basic.no.translate()
    }

    footer {
        text = Translations.Config.configuredBy.translate(user.asUserOrNull()?.username)
        icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
    }
}

suspend fun EmbedBuilder.moderationEmbed(arguments: ModerationArgs, user: UserBehavior) {
    val obj = Translations.Config.Moderation.Embed
    title = obj.title.translate()
    field {
        name = obj.moderatorsFieldName.translate()
        value = arguments.moderatorRole?.mention ?: Translations.Basic.disabled.translate()
    }
    field {
        name = obj.actionLogFieldName.translate()
        value = arguments.modActionLog?.mention ?: Translations.Basic.disabled.translate()
    }
    field {
        name = obj.logPubliclyFieldName.translate()
        value = when (arguments.logPublicly) {
            true -> Translations.Basic.enabled
            false -> Translations.Basic.disabled
            null -> Translations.Basic.disabled
        }.translate()
    }
    field {
        name = Translations.Config.Moderation.Embed.QuickTimeoutLength.name.translate()
        value = arguments.quickTimeoutLength.interval()
            ?: Translations.Config.Moderation.Embed.QuickTimeoutLength.disabled.translate()
    }
    field {
        name = obj.warningAutoPunishmentsName.translate()
        value = when (arguments.warnAutoPunishments) {
            true -> Translations.Basic.enabled
            false -> Translations.Basic.disabled
            null -> Translations.Basic.disabled
        }.translate()
    }
    field {
        name = Translations.Config.Moderation.Embed.DmDefault.name.translate()
        value = when (arguments.dmDefault) {
            true -> Translations.Config.Moderation.Embed.DmDefault.`true`
            false -> Translations.Config.Moderation.Embed.DmDefault.`false`
            null -> Translations.Config.Moderation.Embed.DmDefault.`false`
        }.translate()
    }
    field {
        name = Translations.Config.Moderation.Embed.BanDmMessage.name.translate()
        value = arguments.banDmMessage ?: Translations.Config.Moderation.Embed.BanDmMessage.disabled.translate()
    }
    field {
        name = obj.autoInviteRoleName.translate()
        value = when (arguments.autoInviteModeratorRole) {
            true -> Translations.Basic.enabled
            false -> Translations.Basic.disabled
            null -> Translations.Basic.disabled
        }.translate()
    }
    field {
        name = obj.memberRoleChangesName.translate()
        value = when (arguments.logMemberRoleChanges) {
            true -> Translations.Basic.enabled
            false -> Translations.Basic.disabled
            null -> Translations.Basic.disabled
        }.translate()
    }
    footer {
        text = Translations.Config.configuredBy.translate(user.asUserOrNull()?.username)
        icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
    }
}

suspend fun EmbedBuilder.loggingEmbed(arguments: LoggingArgs, guild: GuildBehavior?, user: UserBehavior) {
    val obj = Translations.Config.Logging.Embed
    title = obj.title.translate()
    field {
        name = obj.messageDeleteFieldName.translate()
        value = if (arguments.enableMessageDeleteLogs && arguments.messageLogs != null) {
            arguments.messageLogs!!.mention
        } else {
            Translations.Basic.disabled.translate()
        }
    }
    field {
        name = obj.messageEditFieldName.translate()
        value = if (arguments.enableMessageEditLogs && arguments.messageLogs != null) {
            arguments.messageLogs!!.mention
        } else {
            Translations.Basic.disabled.translate()
        }
    }
    field {
        name = obj.memberFieldName.translate()
        value = if (arguments.enableMemberLogging && arguments.memberLog != null) {
            arguments.memberLog!!.mention
        } else {
            Translations.Basic.disabled.translate()
        }
    }

    field {
        name = Translations.Config.Logging.Embed.PublicMemberField.name.translate()
        value = if (arguments.enablePublicMemberLogging && arguments.publicMemberLog != null) {
            arguments.publicMemberLog!!.mention
        } else {
            Translations.Basic.disabled.translate()
        }
    }
    if (arguments.enableMemberLogging && arguments.publicMemberLog != null) {
        val config = LoggingConfigCollection().getConfig(guild!!.id)
        if (config != null) {
            field {
                name = Translations.Config.Logging.Embed.PublicMemberField.joinMessage.translate()
                value = config.publicMemberLogData?.joinMessage.trimmedContents(256)!!
            }
            field {
                name = Translations.Config.Logging.Embed.PublicMemberField.leaveMessage.translate()
                value = config.publicMemberLogData?.leaveMessage.trimmedContents(256)!!
            }
            field {
                name = Translations.Config.Logging.Embed.PublicMemberField.pingOnJoin.translate()
                value = config.publicMemberLogData?.pingNewUsers.toString()
            }
        }
    }

    footer {
        text = Translations.Config.configuredBy.translate(user.asUserOrNull()?.username)
        icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
    }
}
