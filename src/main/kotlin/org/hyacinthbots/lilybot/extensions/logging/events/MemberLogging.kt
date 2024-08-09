package org.hyacinthbots.lilybot.extensions.logging.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.rest.builder.message.embed
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs

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
						name = "User joined the server!"
						icon = event.member.avatar?.cdnUrl?.toUrl()
					}
					field {
						name = "Welcome:"
						value = "${event.member.mention} (${event.member.username})"
						inline = true
					}
					field {
						name = "ID:"
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
								name = "Welcome ${event.member.username}"
								icon = event.member.avatar?.cdnUrl?.toUrl()
							}
							description = if (config.publicMemberLogData?.joinMessage != null) {
								config.publicMemberLogData.joinMessage
							} else {
								"Welcome to the server!"
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
				val memberLog = getLoggingChannelWithPerms(ConfigOptions.MEMBER_LOG, event.guild)
				val config = LoggingConfigCollection().getConfig(event.guildId)

				memberLog?.createEmbed {
					author {
						name = "User left the server!"
						icon = event.user.avatar?.cdnUrl?.toUrl()
					}
					field {
						name = "Goodbye:"
						value = event.user.username
						inline = true
					}
					field {
						name = "ID:"
						value = event.user.id.toString()
					}
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}

				if (config != null && config.enablePublicMemberLogs) {
					var publicLog = guildFor(event)?.getChannelOfOrNull<GuildMessageChannel>(config.publicMemberLog!!)
					val permissions = publicLog?.botHasPermissions(Permission.SendMessages, Permission.EmbedLinks)
					if (permissions == false || permissions == null) {
						publicLog = null
					}

					publicLog?.createEmbed {
						author {
							name = "Goodbye ${event.user.username}"
							icon = event.user.avatar?.cdnUrl?.toUrl()
						}
						description = if (config.publicMemberLogData?.leaveMessage != null) {
							config.publicMemberLogData.leaveMessage
						} else {
							"Farewell!"
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}
				}
			}
		}
	}
}
