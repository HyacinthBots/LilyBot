@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import kotlin.time.ExperimentalTime

/**
 * Logs members joining and leaving a guild to the join messages channel designated in the config for that guild
 * @author NoComment1105
 */
class JoinLeaveEvent : Extension() {
	override val name = "joinleaveevent"

	@Suppress("DuplicatedCode")
	override suspend fun setup() {
		event<MemberJoinEvent> {

			action {
				// Try to get the join channel ID from the database
				val joinChannelId = DatabaseHelper.selectInConfig(event.guildId.toString(), "joinChannel")

				val eventMember = event.member
				val guildMemberCount = event.getGuild().members.count()

				// If the database has nothing in it for join channel, return
				// This should only happen if no config is set, it's not a nullable field
				if (joinChannelId == null) return@action

				val joinChannel = event.getGuild().getChannel(Snowflake(joinChannelId)) as GuildMessageChannelBehavior

				joinChannel.createEmbed {
					color = DISCORD_GREEN
					title = "User joined the server!"
					timestamp = Clock.System.now()

					field {
						name = "Welcome:"
						value = eventMember.tag
						inline = true
					}
					field {
						name = "ID:"
						value = eventMember.id.toString()
						inline = false
					}
					footer {
						text = "Member Count: $guildMemberCount"
					}
				}
			}
		}

		event<MemberLeaveEvent> {

			action {
				// Try to get the join channel ID from the database
				val joinChannelId = DatabaseHelper.selectInConfig(event.guildId.toString(), "joinChannel")

				val eventUser = event.user
				val guildMemberCount = event.getGuild().members.count()

				// If the database has nothing in it for join channel, return
				// This should only happen if no config is set, it's not a nullable field
				if (joinChannelId == null) return@action

				val joinChannel = event.getGuild().getChannel(Snowflake(joinChannelId)) as GuildMessageChannelBehavior

				joinChannel.createEmbed {
					color = DISCORD_RED
					title = "User left the server!"
					timestamp = Clock.System.now()

					field {
						name = "Goodbye:"
						value = eventUser.tag
						inline = true
					}
					field {
						name = "ID:"
						value = eventUser.id.toString()
						inline = false
					}
					footer {
						text = "Member count: $guildMemberCount"
					}
				}
			}
		}
	}
}
