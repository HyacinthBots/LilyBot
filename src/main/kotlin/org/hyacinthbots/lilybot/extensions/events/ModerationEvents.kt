package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import dev.kord.core.behavior.UserBehavior
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
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
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
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.collections.ModerationActionCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.moderation.ModerationAction
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.dmNotificationStatusEmbedField
import org.hyacinthbots.lilybot.utils.formatPermissionSet
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.interval

class ModerationEvents : Extension() {
	override val name: String = "moderation-events"

	private val logger = KotlinLogging.logger {}

	override suspend fun setup() {
		event<BanAddEvent> {
			check { anyGuild() }
			action {
				// If the ban doesn't exist then... ????
				event.getBanOrNull() ?: return@action
				var existingAction = ModerationActionCollection().getAction(
					ModerationAction.BAN, event.guildId, event.user.id
				)
				if (existingAction == null) {
					existingAction = ModerationActionCollection().getAction(
						ModerationAction.SOFT_BAN, event.guildId, event.user.id
					)
				}

				if (existingAction == null) {
					existingAction = ModerationActionCollection().getAction(
						ModerationAction.TEMP_BAN, event.guildId, event.user.id
					)
				}

				if (existingAction != null && existingAction.targetUserId != event.user.id) {
					// If this happens I will eat my hat
					logger.warn("It's hat eating time from the ban command")
					return@action
				}

				if (existingAction != null) {
					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.getGuild())?.createEmbed {
						title = when (existingAction.actionType) {
							ModerationAction.BAN -> "Banned a user"
							ModerationAction.SOFT_BAN -> "Soft-banned a user"
							ModerationAction.TEMP_BAN -> "Temp-banned a user"
							else -> null // Theoretically this should never occur but the compiler cries otherwise
						}

						description = "${event.user.mention} has been ${
							when (existingAction.actionType) {
								ModerationAction.BAN -> ""
								ModerationAction.SOFT_BAN -> "soft-"
								ModerationAction.TEMP_BAN -> "temporarily"
								else -> null // Again should theoretically never occur, but compiler
							}
						}banned"
						baseModerationEmbed(
							existingAction.data.reason,
							event.user,
							UserBehavior(existingAction.data.actioner!!, kord)
						)
						image = existingAction.data.imageUrl
						dmNotificationStatusEmbedField(existingAction.data.dmOutcome, existingAction.data.dmOverride)
						timestamp = Clock.System.now()
						field {
							name = "Days of messages deleted"
							value =
								if (existingAction.actionType == ModerationAction.SOFT_BAN &&
									existingAction.data.deletedMessages == 0
								) {
									"3"
								} else {
									existingAction.data.deletedMessages.toString()
								}
							inline = false
						}
						if (existingAction.data.timeData != null) {
							field {
								name = "Duration:"
								value =
									existingAction.data.timeData!!.durationInst?.toDiscord(TimestampType.Default) + " (${
										existingAction.data.timeData!!.durationDtp?.interval()
									})"
							}
						}
					}

					ModerationActionCollection().removeAction(
						existingAction.actionType,
						event.guildId,
						event.user.id
					)
				} else {
					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.getGuild())?.createEmbed {
						title = "Banned a User"
						description = "${event.user.mention} has been banned!"
						baseModerationEmbed(event.getBan().reason, event.user, null)
						timestamp = Clock.System.now()
					}
				}
			}
		}
		event<BanRemoveEvent> {
			check { anyGuild() }
			action {
				val ignore = ModerationActionCollection().shouldIgnoreAction(
					ModerationAction.UNBAN,
					event.guildId,
					event.user.id
				)
				if (ignore != null && ignore) {
					return@action
				}

				val existingAction =
					ModerationActionCollection().getAction(ModerationAction.UNBAN, event.guildId, event.user.id)
				if (existingAction != null && existingAction.targetUserId != event.user.id) {
					// If this happens I will eat my hat
					logger.warn("It's hat eating time from the unban command")
					return@action
				}

				if (existingAction != null) {
					val isTempUnban = existingAction.data.reason?.contains("**temporary-ban**", true) == true

					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.getGuild())?.createEmbed {
						title = if (isTempUnban) "Temporary ban removed" else "Unbanned a user"
						description =
							"${event.user.mention} has ${if (isTempUnban) "had their temporary ban removed!" else "been unbanned!"}\n${
								event.user.id
							}  (${event.user.username})"
						if (existingAction.data.reason?.contains("**temporary-ban-expire**") == false) {
							field {
								name = "Reason:"
								value = existingAction.data.reason.toString()
							}
						} else {
							field {
								name = "Initial Ban date:"
								value = existingAction.data.timeData?.start?.toDiscord(TimestampType.ShortDateTime)
									.toString()
							}
						}

						if (existingAction.data.actioner != null) {
							val user = UserBehavior(existingAction.data.actioner, kord).asUserOrNull()
							footer {
								text = user?.username ?: "Unable to get username"
								icon = user?.avatar?.cdnUrl?.toUrl()
							}
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
					ModerationActionCollection().removeAction(
						existingAction.actionType,
						event.guildId,
						event.user.id
					)
				}
			}
		}
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