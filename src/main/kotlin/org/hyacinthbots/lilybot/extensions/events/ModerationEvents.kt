package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.channel.CategoryCreateEvent
import dev.kord.core.event.channel.CategoryDeleteEvent
import dev.kord.core.event.channel.CategoryUpdateEvent
import dev.kord.core.event.channel.ForumChannelCreateEvent
import dev.kord.core.event.channel.ForumChannelDeleteEvent
import dev.kord.core.event.channel.ForumChannelUpdateEvent
import dev.kord.core.event.channel.NewsChannelCreateEvent
import dev.kord.core.event.channel.NewsChannelDeleteEvent
import dev.kord.core.event.channel.NewsChannelUpdateEvent
import dev.kord.core.event.channel.StageChannelCreateEvent
import dev.kord.core.event.channel.StageChannelDeleteEvent
import dev.kord.core.event.channel.StageChannelUpdateEvent
import dev.kord.core.event.channel.TextChannelCreateEvent
import dev.kord.core.event.channel.TextChannelDeleteEvent
import dev.kord.core.event.channel.TextChannelUpdateEvent
import dev.kord.core.event.channel.VoiceChannelCreateEvent
import dev.kord.core.event.channel.VoiceChannelDeleteEvent
import dev.kord.core.event.channel.VoiceChannelUpdateEvent
import dev.kord.core.event.channel.thread.ThreadChannelCreateEvent
import dev.kord.core.event.channel.thread.ThreadChannelDeleteEvent
import dev.kord.core.event.channel.thread.ThreadUpdateEvent
import dev.kord.core.event.guild.GuildScheduledEventCreateEvent
import dev.kord.core.event.guild.GuildScheduledEventDeleteEvent
import dev.kord.core.event.guild.GuildScheduledEventUpdateEvent
import dev.kord.core.event.guild.InviteCreateEvent
import dev.kord.core.event.guild.InviteDeleteEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kord.core.event.role.RoleCreateEvent
import dev.kord.core.event.role.RoleDeleteEvent
import dev.kord.core.event.role.RoleUpdateEvent
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.formatPermissionSet
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

class ModerationEvents : Extension() {
	override val name: String = "moderation-events"

	override suspend fun setup() {
		event<CategoryCreateEvent> {
			check { anyGuild() }
			action {
				var allowed = ""
				var denied = ""
				event.channel.permissionOverwrites.forEach {
					allowed += "${guildFor(event)!!.getRoleOrNull(it.target)?.mention}: ${formatPermissionSet(it.allowed)}\n"
					denied += "${guildFor(event)!!.getRoleOrNull(it.target)?.mention}: ${formatPermissionSet(it.denied)}\n"
				}

				val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guildFor(event)!!)
				utilityLog?.createEmbed {
					title = "Category Created"
					description = "${event.channel.mention} (${event.channel.name}) was created."
					field {
						name = "Allowed Permissions"
						value = allowed
						inline = true
					}
					field {
						name = "Denied Permissions"
						value = denied
						inline = true
					}
					timestamp = Clock.System.now()
					color = DISCORD_GREEN
				}
			}
		}
		event<CategoryDeleteEvent> {
			check { anyGuild() }
			action {
				val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guildFor(event)!!)
				utilityLog?.createEmbed {
					title = "Category Deleted"
					description = "${event.channel.name} was deleted."
					timestamp = Clock.System.now()
					color = DISCORD_RED
				}
			}
		}
		event<CategoryUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<ForumChannelCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<ForumChannelDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<ForumChannelUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<GuildScheduledEventCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<GuildScheduledEventDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<GuildScheduledEventUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<InviteCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<InviteDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<MemberUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<NewsChannelCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<NewsChannelDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<NewsChannelUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<RoleCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<RoleDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<RoleUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<StageChannelCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<StageChannelDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<StageChannelUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<TextChannelCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<TextChannelDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<TextChannelUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<ThreadChannelCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<ThreadChannelDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<ThreadUpdateEvent> {
			check { anyGuild() }
			action { }
		}
		event<VoiceChannelCreateEvent> {
			check { anyGuild() }
			action { }
		}
		event<VoiceChannelDeleteEvent> {
			check { anyGuild() }
			action { }
		}
		event<VoiceChannelUpdateEvent> {
			check { anyGuild() }
			action { }
		}
	}
}
