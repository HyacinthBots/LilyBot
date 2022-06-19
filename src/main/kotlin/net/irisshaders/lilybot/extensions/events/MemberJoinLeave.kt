package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import mu.KotlinLogging
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent

/**
 * Logs members joining and leaving a guild to the join messages channel designated in the config for that guild.
 * @author NoComment1105
 * @since 2.0
 */
class MemberJoinLeave : Extension() {
	override val name = "member-join-leave"

	private val joinLeaveLogger = KotlinLogging.logger("Join Leave logger")

	override suspend fun setup() {
		/** Create an embed in the join channel on user join */
		event<MemberJoinEvent> {
			check { configPresent() }
			action {
				val config = DatabaseHelper.getConfig(event.guildId)!!

				// If it's Lily joining, don't try to log since a channel won't be set
				if (event.member.id == kord.selfId) return@action

				val eventMember = event.member
				val guildMemberCount = event.getGuild().members.count()

				val joinChannel = event.getGuild().getChannelOf<GuildMessageChannel>(config.joinChannel)

				try {
					joinChannel.createEmbed {
						title = "User joined the server!"
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
				} catch (e: KtorRequestException) {
					if (e.httpResponse.status.value == 400) {
						return@action
					} else {
						joinLeaveLogger.warn("Join embed failed to send. This was not due to a permission error!")
					}
				}
			}
		}

		/** Create an embed in the join channel on user leave */
		event<MemberLeaveEvent> {
			check { configPresent() }
			action {
				// If it's Lily leaving, return the action, otherwise the log will fill with errors
				if (event.user.id == kord.selfId) return@action
				val config = DatabaseHelper.getConfig(event.guildId)!!

				val eventUser = event.user
				val guildMemberCount = event.getGuild().members.count()

				val joinChannel = event.getGuild().getChannelOf<GuildMessageChannel>(config.joinChannel)

				try {
					joinChannel.createEmbed {
						title = "User left the server!"
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
				} catch (e: KtorRequestException) {
					if (e.httpResponse.status.value == 400) {
						return@action
					} else {
						joinLeaveLogger.warn("Leave embed failed to send. This was not due to a permission error!")
					}
				}
			}
		}
	}
}
