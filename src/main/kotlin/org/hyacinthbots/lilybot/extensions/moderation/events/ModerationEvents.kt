package org.hyacinthbots.lilybot.extensions.moderation.events

import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.BanAddEvent
import dev.kord.core.event.guild.BanRemoveEvent
import dev.kord.core.event.guild.MemberUpdateEvent
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.DISCORD_YELLOW
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import dev.kordex.core.utils.timeoutUntil
import kotlinx.datetime.Clock
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.LeftMemberFlagCollection
import org.hyacinthbots.lilybot.database.collections.ModerationActionCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.moderation.utils.ModerationAction
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.dmNotificationStatusEmbedField
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.ifNullOrEmpty
import org.hyacinthbots.lilybot.utils.interval

class ModerationEvents : Extension() {
	override val name: String = "moderation-events"

	override suspend fun setup() {
		event<BanAddEvent> {
			check { anyGuild() }
			action {
				// Do not log if the moderation system is disabled
				if (ModerationConfigCollection().getConfig(event.guildId)?.enabled != true) return@action
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
					return@action
				}

				if (existingAction != null) {
					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.getGuild())?.createEmbed {
						title = when (existingAction.actionType) {
							ModerationAction.BAN -> Translations.Events.Moderation.Ban.banned.translate()
							ModerationAction.SOFT_BAN -> Translations.Events.Moderation.Ban.softBanned.translate()
							ModerationAction.TEMP_BAN -> Translations.Events.Moderation.Ban.tempBanned.translate()
							else -> null // Theoretically this should never occur but the compiler cries otherwise
						} + " " + Translations.Events.Moderation.Ban.aUser.translate()

						description =
							Translations.Events.Moderation.Ban.banDescription.translate(event.user.mention) + " " +
								if (existingAction.data.reason?.contains("quick ban", true) == false) {
									when (existingAction.actionType) {
										ModerationAction.BAN -> Translations.Events.Moderation.Ban.banned.translate()
										ModerationAction.SOFT_BAN -> Translations.Events.Moderation.Ban.softBanned.translate()
										ModerationAction.TEMP_BAN -> Translations.Events.Moderation.Ban.tempBanned.translate()
										else -> null // Again should theoretically never occur, but compiler
									}?.lowercase()
								} else {
									when (existingAction.actionType) {
										ModerationAction.BAN -> existingAction.data.reason
										ModerationAction.SOFT_BAN -> existingAction.data.reason
										else -> null
									}
								}
						baseModerationEmbed(
							existingAction.data.reason,
							event.user,
							UserBehavior(existingAction.data.actioner!!, kord)
						)
						image = existingAction.data.imageUrl
						dmNotificationStatusEmbedField(existingAction.data.dmOutcome, existingAction.data.dmOverride)
						timestamp = Clock.System.now()
						if (existingAction.data.deletedMessages != null) {
							field {
								name = Translations.Events.Moderation.Ban.deleteDays.translate()
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
						}
						if (existingAction.data.timeData != null) {
							field {
								name = Translations.Events.Moderation.Ban.duration.translate()
								value =
									existingAction.data.timeData.durationInst?.toDiscord(TimestampType.Default) +
										" (${existingAction.data.timeData.durationDtp?.interval()})"
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
						title =
							Translations.Events.Moderation.Ban.banned.translate() + " " +
								Translations.Events.Moderation.Ban.aUser.translate()
						description =
							Translations.Events.Moderation.Ban.defaultBanDescription.translate(event.user.mention)
						baseModerationEmbed(event.getBan().reason, event.user, null)
						timestamp = Clock.System.now()
					}
				}
			}
		}
		event<BanRemoveEvent> {
			check { anyGuild() }
			action {
				// Do not log if the moderation system is disabled
				if (ModerationConfigCollection().getConfig(event.guildId)?.enabled != true) return@action
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
					return@action
				}

				if (existingAction != null) {
					val isTempUnban = existingAction.data.reason?.contains("**temporary-ban**", true) == true

					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.getGuild())?.createEmbed {
						title = if (isTempUnban) {
							Translations.Events.Moderation.Unban.tempRemoved
						} else {
							Translations.Events.Moderation.Unban.unbanned
						}.translate()
						description = Translations.Events.Moderation.Unban.description.translate(event.user.mention) +
							"${
								if (isTempUnban) {
									Translations.Events.Moderation.Unban.tempRemovedDesc
								} else {
									Translations.Events.Moderation.Unban.unbannedDesc
								}.translate()
							}\n" +
							"${event.user.id} (${event.user.username})"
						if (existingAction.data.reason?.contains("**temporary-ban-expire**") == false) {
							field {
								name = Translations.Events.Moderation.Unban.reason.translate()
								value = existingAction.data.reason.toString()
							}
						} else {
							field {
								name = Translations.Events.Moderation.Unban.initialDate.translate()
								value = existingAction.data.timeData?.start?.toDiscord(TimestampType.ShortDateTime)
									.toString()
							}
						}

						if (existingAction.data.actioner != null) {
							val user = UserBehavior(existingAction.data.actioner, kord).asUserOrNull()
							footer {
								text = user?.username ?: Translations.Basic.UnableTo.tag.translate()
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
				} else {
					getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.getGuild())?.createEmbed {
						title = Translations.Events.Moderation.Unban.unbanned.translate()
						description = Translations.Events.Moderation.Unban.description.translate(event.user.mention) +
							Translations.Events.Moderation.Unban.unbannedDesc.translate() +
							"\n${event.user.id} (${event.user.username})"
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}
				}
			}
		}
		event<MemberUpdateEvent> {
			check { anyGuild() }
			action {
				if (LeftMemberFlagCollection().getMemberFromTable(event.guildId, event.member.id) != null) {
					LeftMemberFlagCollection().removeMemberFromLeft(event.guildId, event.member.id)
					return@action
				}
				// Do not log if member role changes is not true
				if (ModerationConfigCollection().getConfig(event.guildId)?.logMemberRoleChanges != true) return@action
				val channel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, event.guild)
				if (event.old?.timeoutUntil != event.member.timeoutUntil) {
					var existingAction =
						ModerationActionCollection().getAction(ModerationAction.TIMEOUT, event.guildId, event.member.id)

					if (existingAction == null) {
						existingAction =
							ModerationActionCollection().getAction(
								ModerationAction.REMOVE_TIMEOUT,
								event.guildId,
								event.member.id
							)
					}

					if (existingAction != null) {
						val data = existingAction.data
						val isRemove = existingAction.actionType == ModerationAction.REMOVE_TIMEOUT
						val user = UserBehavior(existingAction.targetUserId, kord).asUserOrNull()
						val actioner = data.actioner?.let { UserBehavior(it, kord) }?.asUserOrNull()
						channel?.createEmbed {
							if (isRemove) {
								title = Translations.Events.Moderation.MemberUpdate.timeoutRemoved.translate()
								dmNotificationStatusEmbedField(data.dmOutcome, data.dmOverride)
								field {
									name = Translations.Basic.userField.translate()
									value = "${user?.username} (${user?.id})"
								}
								footer {
									text = Translations.Basic.requestedBy.translate(actioner?.username)
									icon = user?.avatar?.cdnUrl?.toUrl()
								}
								timestamp = Clock.System.now()
								color = DISCORD_GREEN
							} else {
								title = Translations.Events.Moderation.MemberUpdate.timeoutAdded.translate()
								image = data.imageUrl
								baseModerationEmbed(data.reason, event.member, user)
								dmNotificationStatusEmbedField(data.dmOutcome, data.dmOverride)
								timestamp = data.timeData?.start
								color = DISCORD_RED
								field {
									name = Translations.Events.Moderation.Ban.duration.translate()
									value = data.timeData?.durationInst?.toDiscord(TimestampType.Default) +
										" (${data.timeData?.durationDtp.interval()})"
								}
							}
						}

						ModerationActionCollection().removeAction(
							existingAction.actionType,
							event.guild.id,
							event.member.id
						)
					} else {
						channel?.createEmbed {
							if (event.member.timeoutUntil != null) {
								title = Translations.Events.Moderation.MemberUpdate.timeoutAdded.translate()
								color = DISCORD_RED
								field {
									name = Translations.Events.Moderation.Ban.duration.translate()
									value = event.member.timeoutUntil?.toDiscord(TimestampType.Default) ?: "0"
								}
							} else {
								title = Translations.Events.Moderation.MemberUpdate.timeoutRemoved.translate()
								color = DISCORD_GREEN
							}
							field {
								name = Translations.Basic.userField.translate()
								value = event.member.mention + "(${event.member.id})"
							}
							description = Translations.Events.Moderation.MemberUpdate.viaMenu.translate()
							timestamp = Clock.System.now()
						}
					}
				} else {
					// FIXME I'm gonna be sick please fix this
					if (event.member.nickname == event.old?.nickname &&
						event.member.roleBehaviors == event.old?.roleBehaviors &&
						(
							event.member.data != event.old?.data || event.member.avatar != event.old?.avatar ||
								event.member.premiumSince != event.old?.premiumSince ||
								event.member.isPending != event.old?.isPending || event.member.flags != event.old?.flags ||
								event.member.avatarDecoration != event.old?.avatarDecoration
							)
					) {
						return@action
					}
					val newRoles = mutableListOf<String>()
					val oldRoles = mutableListOf<String>()
					event.member.roleBehaviors.forEach { newRoles.add(it.mention) }
					event.old?.roleBehaviors?.forEach { oldRoles.add(it.mention) }
					channel?.createEmbed {
						title = Translations.Events.Moderation.MemberUpdate.updatedTitle.translate()
						description =
							Translations.Events.Moderation.MemberUpdate.updatedDesc.translate(event.member.username)
						if (event.member.nickname != event.old?.nickname) {
							field {
								name = Translations.Events.Moderation.MemberUpdate.nickChange.translate()
								value = Translations.Events.Moderation.MemberUpdate.nickChangeVal.translateNamed(
									"old" to event.old?.nickname, "new" to event.member.nickname
								)
							}
						}
						if (event.member.roleIds != event.old?.roleIds) {
							field {
								name = Translations.Events.Moderation.MemberUpdate.newRoles.translate()
								value =
									newRoles.joinToString(", ").ifNullOrEmpty { Translations.Basic.none.translate() }
								inline = true
							}
							field {
								name = Translations.Events.Moderation.MemberUpdate.oldRoles.translate()
								value =
									oldRoles.joinToString(", ").ifNullOrEmpty { Translations.Basic.none.translate() }
								inline = true
							}
						}
						color = DISCORD_YELLOW
					}
				}
			}
		}
	}
}
