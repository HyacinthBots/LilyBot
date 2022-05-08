package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper

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
			action {
				val config = DatabaseHelper.getConfig(event.guildId) ?: return@action

				val eventMember = event.member
				val guildMemberCount = event.getGuild().members.count()

				val joinChannel = event.getGuild().getChannel(config.joinChannel) as GuildMessageChannelBehavior

				joinChannel.createEmbed {
					color = DISCORD_GREEN
					title = "User joined the server!"
					timestamp = Clock.System.now()

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
				}
			}
		}

		/** Create an embed in the join channel on user leave */
		event<MemberLeaveEvent> {
			action {
				// If it's Lily leaving, return the action, otherwise the log will fill with errors
				if (event.user.id == kord.selfId) return@action
				val config = DatabaseHelper.getConfig(event.guildId) ?: return@action

				val eventUser = event.user
				val guildMemberCount = event.getGuild().members.count()

				val joinChannel = event.getGuild().getChannel(config.joinChannel) as GuildMessageChannelBehavior

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
