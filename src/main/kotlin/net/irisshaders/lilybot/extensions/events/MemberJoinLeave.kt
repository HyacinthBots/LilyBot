package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.collections.LoggingConfigCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.configPresent

/**
 * Logs members joining and leaving a guild to the join messages channel designated in the config for that guild.
 * @author NoComment1105
 * @since 2.0
 */
class MemberJoinLeave : Extension() {
	override val name = "member-join-leave"

	override suspend fun setup() {
		/** Create an embed in the join channel on user join */
		event<MemberJoinEvent> {
			check { configPresent(ConfigType.LOGGING) }
			action {
				val config = LoggingConfigCollection().getConfig(guildFor(event)!!.id)!!

				// If it's Lily joining, don't try to log since a channel won't be set
				if (event.member.id == kord.selfId) return@action

				val eventMember = event.member
				val guildMemberCount = event.getGuild().members.count()

				val joinChannel = event.getGuild().getChannelOf<GuildMessageChannel>(config.joinChannel)

				joinChannel.createEmbed {
					author {
						name = "User joined the server!"
						icon = eventMember.avatar!!.url
					}
					field {
						name = "Welcome:"
						value = "${eventMember.mention} (${eventMember.tag})"
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
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}

		/** Create an embed in the join channel on user leave */
		event<MemberLeaveEvent> {
			check { configPresent(ConfigType.LOGGING) }
			action {
				// If it's Lily leaving, return the action, otherwise the log will fill with errors
				if (event.user.id == kord.selfId) return@action
				val config = LoggingConfigCollection().getConfig(guildFor(event)!!.id)!!

				val joinChannel = event.getGuild().getChannelOf<GuildMessageChannel>(config.joinChannel)

				val eventUser = event.user
				val guildMemberCount = event.getGuild().members.count()

				joinChannel.createEmbed {
					author {
						name = "User left the server!"
						icon = eventUser.avatar!!.url
					}
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
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}
			}
		}
	}
}
