package org.hyacinthbots.lilybot.extensions.moderation.config

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.utils.botHasPermissions
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.utils.moderationEmbed
import org.hyacinthbots.lilybot.utils.canPingRole
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

suspend fun SlashCommand<*, *, *>.moderationCommand() =
    ephemeralSubCommand(::ModerationArgs) {
        name = Translations.Config.Moderation.name
        description = Translations.Config.Moderation.description

        requirePermission(Permission.ManageGuild)

        check {
            anyGuild()
            hasPermission(Permission.ManageGuild)
        }

        action {
            val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)
            if (moderationConfig != null) {
                respond {
                    content = Translations.Config.configAlreadyExists.translate("moderation")
                }
                return@action
            }

            if (!arguments.enabled) {
                ModerationConfigCollection().setConfig(
                    ModerationConfigData(
                        guild!!.id,
                        false,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                    )
                )
                respond {
                    content = Translations.Config.Moderation.systemDisabled.translate()
                }
                return@action
            }

            if (
                arguments.moderatorRole != null && arguments.modActionLog == null ||
                arguments.moderatorRole == null && arguments.modActionLog != null
            ) {
                respond {
                    content = Translations.Config.Moderation.roleAndChannelRequired.translate()
                }
                return@action
            }

            if (!canPingRole(arguments.moderatorRole, guild!!.id, this@moderationCommand.kord)) {
                respond {
                    content =
                        Translations.Config.Moderation.roleNotPingable.translate(arguments.moderatorRole!!.mention)
                }
                return@action
            }

            val modActionLog: TextChannel?
            if (arguments.enabled && arguments.modActionLog != null) {
                modActionLog = guild!!.getChannelOfOrNull(arguments.modActionLog!!.id)
                if (modActionLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
                    respond {
                        content = Translations.Config.invalidChannel.translate("mod-action log")
                    }
                    return@action
                }
            }

            respond {
                embed {
                    moderationEmbed(arguments, user)
                }
            }

            ModerationConfigCollection().setConfig(
                ModerationConfigData(
                    guild!!.id,
                    arguments.enabled,
                    arguments.modActionLog?.id,
                    arguments.moderatorRole?.id,
                    arguments.quickTimeoutLength,
                    arguments.warnAutoPunishments,
                    arguments.logPublicly,
                    arguments.dmDefault,
                    arguments.banDmMessage,
                    arguments.autoInviteModeratorRole,
                    arguments.logMemberRoleChanges
                )
            )

            val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

            if (utilityLog == null) {
                respond {
                    content = Translations.Config.considerUtility.translate()
                }
                return@action
            }

            utilityLog.createMessage {
                embed {
                    moderationEmbed(arguments, user)
                }
            }
        }
    }
