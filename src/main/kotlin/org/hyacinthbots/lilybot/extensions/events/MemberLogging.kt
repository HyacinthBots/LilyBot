package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.configPresent
import org.hyacinthbots.lilybot.utils.getModerationChannelWithPerms

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
				configPresent(ConfigOptions.MEMBER_LOGGING_ENABLED, ConfigOptions.MEMBER_LOG)
				failIf { event.member.id == kord.selfId }
			}
			action {
				val config = LoggingConfigCollection().getConfig(event.guildId)!!
				val memberLog = getModerationChannelWithPerms(event.getGuild(), config.memberLog!!, ConfigType.LOGGING)
					?: return@action
				val guildMemberCount = event.guild.members.count()

				memberLog.createEmbed {
					author {
						name = "User joined the server!"
						icon = event.member.avatar?.url
					}
					field {
						name = "Welcome:"
						value = "${event.member.mention} (${event.member.tag})"
						inline = true
					}
					field {
						name = "ID:"
						value = event.member.id.toString()
						inline = false
					}
					footer {
						text = "Member Count: $guildMemberCount"
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}

		/** Create an embed in the join channel on user leave */
		event<MemberLeaveEvent> {
			check {
				anyGuild()
				configPresent(ConfigOptions.MEMBER_LOGGING_ENABLED, ConfigOptions.MEMBER_LOG)
				failIf { event.user.id == kord.selfId }
			}
			action {
				val config = LoggingConfigCollection().getConfig(event.guildId)!!
				val memberLog = getModerationChannelWithPerms(event.getGuild(), config.memberLog!!, ConfigType.LOGGING)
					?: return@action
				val guildMemberCount = event.guild.members.count()

				memberLog.createEmbed {
					author {
						name = "User left the server!"
						icon = event.user.avatar?.url
					}
					field {
						name = "Goodbye:"
						value = event.user.tag
						inline = true
					}
					field {
						name = "ID:"
						value = event.user.id.toString()
						inline = false
					}
					footer {
						text = "Member count: $guildMemberCount"
					}
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}
			}
		}
	}
}
