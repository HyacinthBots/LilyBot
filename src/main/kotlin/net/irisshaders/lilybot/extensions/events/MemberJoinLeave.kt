package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.getFirstUsableChannel

/**
 * Logs members joining and leaving a guild to the join messages channel designated in the config for that guild.
 * @author NoComment1105
 * @author tempest15
 * @since 2.0
 */
class MemberJoinLeave : Extension() {
	override val name = "member-join-leave"

	override suspend fun setup() {
		/** Create an embed in the join channel on user join */
		event<MemberJoinEvent> {
			check {
				anyGuild()
				configPresent()
				failIf { event.member.id == kord.selfId }
			}
			action {
				val joinChannel = getJoinLeaveChannelWithPerms(event.getGuild()) ?: return@action
				val guildMemberCount = event.guild.members.count()

				joinChannel.createEmbed {
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
				configPresent()
				failIf { event.user.id == kord.selfId }
			}
			action {
				val leaveChannel = getJoinLeaveChannelWithPerms(event.getGuild()) ?: return@action
				val guildMemberCount = event.guild.members.count()

				leaveChannel.createEmbed {
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

	/**
	 * Check if the bot can send messages in the guild's configured member join/leave logging channel.
	 * If the bot can't, reset a config and send a message in the top usable channel saying that the config was reset.
	 * If the bot can, return the member join/leave logging channel.
	 *
	 * @param inputGuild The guild to check in.
	 * @return The member join/leave logging channel or null if it does not have the correct permissions.
	 * @author tempest15
	 * @since 3.5.4
	 */
	private suspend fun getJoinLeaveChannelWithPerms(inputGuild: Guild): GuildMessageChannel? {
		val config = DatabaseHelper.getConfig(inputGuild.id)!!
		val joinLeaveChannel = inputGuild.getChannelOfOrNull<GuildMessageChannel>(config.joinChannel)

		if (joinLeaveChannel?.botHasPermissions(
				Permission.ViewChannel,
				Permission.SendMessages,
				Permission.EmbedLinks
			) != true
		) {
			val usableChannel = getFirstUsableChannel(inputGuild) ?: return null
			usableChannel.createMessage(
				"Lily cannot send messages in your configured member join/leave logging channel. " +
						"As a result, your config has been reset. " +
						"Please fix the permissions before setting a new config."
			)
			delay(3000) // So that other events may finish firing
			DatabaseHelper.clearConfig(usableChannel.guildId)
			return null
		}
		return joinLeaveChannel
	}
}
