package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.MemberJoinEvent
import dev.kord.core.event.guild.MemberLeaveEvent
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.getModerationChannelWithPerms

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
				val config = DatabaseHelper.getConfig(event.guildId)!!
				val joinChannel = getModerationChannelWithPerms(event.getGuild(), config.joinChannel) ?: return@action
				val guildMemberCount =
					event.getGuild().withStrategy(EntitySupplyStrategy.cacheWithRestFallback).memberCount

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
				val config = DatabaseHelper.getConfig(event.guildId)!!
				val leaveChannel = getModerationChannelWithPerms(event.getGuild(), config.joinChannel) ?: return@action
				val guildMemberCount =
					event.getGuild().withStrategy(EntitySupplyStrategy.cacheWithRestFallback).memberCount

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
}
