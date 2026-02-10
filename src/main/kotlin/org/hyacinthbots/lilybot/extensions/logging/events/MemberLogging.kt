package org.hyacinthbots.lilybot.extensions.logging.events

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.guildFor
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.botHasPermissions
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.LeftMemberFlagCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationActionCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.moderation.utils.ModerationAction
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.dmNotificationStatusEmbedField
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs
import kotlin.time.Clock

/**
 * Logs members joining and leaving a guild to the member log channel designated in the config for that guild.
 * @author NoComment1105
 * @author tempest15
 * @since 2.0
 */
class MemberLogging : Extension() {
    override val name = "member-logging"

    override suspend fun setup() {
        /** Create an embed in the join channel on user join */
        event<MemberJoinEvent> {
            check {
                anyGuild()
                requiredConfigs(ConfigOptions.MEMBER_LOGGING_ENABLED, ConfigOptions.MEMBER_LOG)
                failIf { event.member.id == kord.selfId }
            }
            action {
                val memberLog = getLoggingChannelWithPerms(ConfigOptions.MEMBER_LOG, event.guild)
                val config = LoggingConfigCollection().getConfig(event.guildId)

                memberLog?.createEmbed {
                    author {
                        name = Translations.Events.MemberLogging.MemberJoin.embedAuthor.translate()
                        icon = event.member.avatar?.cdnUrl?.toUrl()
                    }
                    field {
                        name = Translations.Events.MemberLogging.MemberJoin.embedWelcome.translate()
                        value = "${event.member.mention} (${event.member.username})"
                        inline = true
                    }
                    field {
                        name = Translations.Events.MemberLogging.MemberEvent.embedId.translate()
                        value = event.member.id.toString()
                        inline = false
                    }
                    timestamp = Clock.System.now()
                    color = DISCORD_GREEN
                }

                if (config != null && config.enablePublicMemberLogs) {
                    var publicLog = guildFor(event)?.getChannelOfOrNull<GuildMessageChannel>(config.publicMemberLog!!)
                    val permissions = publicLog?.botHasPermissions(Permission.SendMessages, Permission.EmbedLinks)
                    if (permissions == false || permissions == null) {
                        publicLog = null
                    }

                    publicLog?.createMessage {
                        if (config.publicMemberLogData?.pingNewUsers == true) content = event.member.mention
                        embed {
                            author {
                                name = Translations.Events.MemberLogging.MemberJoin.publicEmbedAuthor
                                    .translate(event.member.username)
                                icon = event.member.avatar?.cdnUrl?.toUrl()
                            }
                            description = if (config.publicMemberLogData?.joinMessage != null) {
                                config.publicMemberLogData.joinMessage
                            } else {
                                Translations.Events.MemberLogging.MemberJoin.publicEmbedWelcomeMessage.translate()
                            }
                            timestamp = Clock.System.now()
                            color = DISCORD_GREEN
                        }
                    }
                }
            }
        }

        /** Create an embed in the join channel on user leave */
        event<MemberLeaveEvent> {
            check {
                anyGuild()
                requiredConfigs(ConfigOptions.MEMBER_LOGGING_ENABLED, ConfigOptions.MEMBER_LOG)
                failIf { event.user.id == kord.selfId }
            }
            action {
                LeftMemberFlagCollection().addMemberToLeft(event.guildId, event.user.id)
                val memberLog = getLoggingChannelWithPerms(ConfigOptions.MEMBER_LOG, event.guild)
                val config = LoggingConfigCollection().getConfig(event.guildId)

                memberLog?.createEmbed {
                    author {
                        name = Translations.Events.MemberLogging.MemberLeave.embedAuthor.translate()
                        icon = event.user.avatar?.cdnUrl?.toUrl()
                    }
                    field {
                        name = Translations.Events.MemberLogging.MemberLeave.embedGoodbye.translate()
                        value = event.user.username
                        inline = true
                    }
                    field {
                        name = Translations.Events.MemberLogging.MemberEvent.embedId.translate()
                        value = event.user.id.toString()
                    }
                    timestamp = Clock.System.now()
                    color = DISCORD_RED
                }

                // Check if the member was kicked, and send a log if they were.
                val kickData =
                    ModerationActionCollection().getAction(ModerationAction.KICK, event.guildId, event.user.id)
                if (kickData != null) {
                    val targetUser = event.kord.getUser(kickData.targetUserId)!!
                    val actioner = kickData.data.actioner?.let { event.kord.getUser(it) }!!
                    getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.guild)?.createEmbed {
                        title = Translations.Moderation.ModCommands.Kick.response.translate()
                        description = Translations.Moderation.ModCommands.Kick.embedDesc.translate(targetUser.mention)
                        image = kickData.data.imageUrl
                        baseModerationEmbed(kickData.data.reason, targetUser, actioner)
                        dmNotificationStatusEmbedField(kickData.data.dmOutcome != null, kickData.data.dmOutcome)
                        timestamp = Clock.System.now()
                    }
                }

                if (config != null && config.enablePublicMemberLogs) {
                    var publicLog = guildFor(event)?.getChannelOfOrNull<GuildMessageChannel>(config.publicMemberLog!!)
                    val permissions = publicLog?.botHasPermissions(Permission.SendMessages, Permission.EmbedLinks)
                    if (permissions == false || permissions == null) {
                        publicLog = null
                    }

                    publicLog?.createEmbed {
                        author {
                            name = Translations.Events.MemberLogging.MemberLeave.publicEmbedAuthor
                                .translate(event.user.username)
                            icon = event.user.avatar?.cdnUrl?.toUrl()
                        }
                        description = if (config.publicMemberLogData?.leaveMessage != null) {
                            config.publicMemberLogData.leaveMessage
                        } else {
                            Translations.Events.MemberLogging.MemberLeave.publicEmbedGoodbyeMessage.translate()
                        }
                        timestamp = Clock.System.now()
                        color = DISCORD_RED
                    }
                }

                if (LeftMemberFlagCollection().getMemberFromTable(event.guildId, event.user.id) == null) {
                    LeftMemberFlagCollection().removeMemberFromLeft(event.guildId, event.user.id)
                }
            }
        }
    }
}
