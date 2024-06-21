package org.hyacinthbots.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.common.entity.AuditLogEvent
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.guild.GuildAuditLogEntryCreateEvent
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

class ModerationEvents : Extension() {
	override val name: String = "moderation-events"

	override suspend fun setup() {
		event<GuildAuditLogEntryCreateEvent> {
			check { anyGuild() }
			action {
				val moderationLog = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guildFor(event)!!) ?: return@action

				@Suppress("UnusedPrivateProperty")
				val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guildFor(event)!!) ?: return@action
				val entry = event.auditLogEntry
				val target = entry.targetId?.let { kord.getUser(it) }
				val actioner = entry.userId?.let { UserBehavior(it, kord) }
				val time = Clock.System.now()
				when (event.auditLogEntry.actionType) {
					AuditLogEvent.ApplicationCommandPermissionUpdate -> { /* No */ }
					AuditLogEvent.AutoModerationBlockMessage -> { /* No */ }
					AuditLogEvent.AutoModerationFlagToChannel -> { /* No */ }
					AuditLogEvent.AutoModerationRuleCreate -> { /* No */ }
					AuditLogEvent.AutoModerationRuleDelete -> { /* No */ }
					AuditLogEvent.AutoModerationRuleUpdate -> { /* No */ }
					AuditLogEvent.AutoModerationUserCommunicationDisabled -> { /* No */ }
					AuditLogEvent.BotAdd -> { /* Mayhaps? */ }
					AuditLogEvent.ChannelCreate -> { /* Yes */ }
					AuditLogEvent.ChannelDelete -> { /* Yes */ }
					AuditLogEvent.ChannelOverwriteCreate -> { /* No */ }
					AuditLogEvent.ChannelOverwriteDelete -> { /* No */ }
					AuditLogEvent.ChannelOverwriteUpdate -> { /* No */ }
					AuditLogEvent.ChannelUpdate -> { /* Mayhaps? */ }
					AuditLogEvent.CreatorMonetizationRequestCreated -> { /* No */ }
					AuditLogEvent.CreatorMonetizationTermsAccepted -> { /* No */ }
					AuditLogEvent.EmojiCreate -> { /* No */ }
					AuditLogEvent.EmojiDelete -> { /* No */ }
					AuditLogEvent.EmojiUpdate -> { /* No */ }
					AuditLogEvent.GuildScheduledEventCreate -> { /* Mayhaps? */ }
					AuditLogEvent.GuildScheduledEventDelete -> { /* Mayhaps? */ }
					AuditLogEvent.GuildScheduledEventUpdate -> { /* Mayhaps? */ }
					AuditLogEvent.GuildUpdate -> { /* What does this entail? */ }
					AuditLogEvent.IntegrationCreate -> { /* No */ }
					AuditLogEvent.IntegrationDelete -> { /* No */ }
					AuditLogEvent.IntegrationUpdate -> { /* No */ }
					AuditLogEvent.InviteCreate -> { /* Mayhaps? */ }
					AuditLogEvent.InviteDelete -> { /* Mayhaps? */ }
					AuditLogEvent.InviteUpdate -> { /* Mayhaps? */ }
					AuditLogEvent.MemberBanAdd -> {
						if (entry.reason?.contains("temporary-ban", true) == true) {
							// We loose too much information by running temporary bans here, the command will post it instead for us
							return@action
						}

						moderationLog.createEmbed {
							title = if (entry.reason?.contains("soft-ban", true) == true) {
								"Soft Banned user"
							} else {
								"Banned User"
							}
							baseModerationEmbed(entry.reason ?: "No reason provided", target, actioner)
							timestamp = time
							color = DISCORD_BLACK
							footer {
								text = actioner?.asUserOrNull()?.username ?: "Unable to get user username"
								icon = actioner?.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
							}
						}
					}
					AuditLogEvent.MemberBanRemove -> {
						if (entry.reason?.contains("soft-ban", true) == true || entry.reason?.contains("temporary-ban", true) == true) {
							// Soft-banning implies an unban so do not do any logging
							// Temporary banning loses too much information here as well, the scheduler can do it for us
							return@action
						}

						moderationLog.createEmbed {
							title = "Unbanned user"
							description = "${target?.mention} has been unbanned!\n${
								target?.id
							} (${target?.username})"
							if (entry.reason != null) {
								field {
									name = "Reason:"
									value = entry.reason!!
								}
							}
							timestamp = time
							color = DISCORD_GREEN
							footer {
								text = actioner?.asUserOrNull()?.username ?: "Unable to get user username"
								icon = actioner?.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
							}
						}
					}
					AuditLogEvent.MemberDisconnect -> { /* No */ }
					AuditLogEvent.MemberKick -> { /* Yes */ }
					AuditLogEvent.MemberMove -> { /* No */ }
					AuditLogEvent.MemberPrune -> { /* Mayhaps? */ }
					AuditLogEvent.MemberRoleUpdate -> { /* Mayhaps? */ }
					AuditLogEvent.MemberUpdate -> { /* No */ }
					AuditLogEvent.MessageBulkDelete -> { /* Compare with current */ }
					AuditLogEvent.MessageDelete -> { /* Compare with current */ }
					AuditLogEvent.MessagePin -> { /* Mayhaps? */ }
					AuditLogEvent.MessageUnpin -> { /* Mayhaps? */ }
					AuditLogEvent.RoleCreate -> { /* Yes */ }
					AuditLogEvent.RoleDelete -> { /* Yes */ }
					AuditLogEvent.RoleUpdate -> { /* Mayhaps? */ }
					AuditLogEvent.StageInstanceCreate -> { /* What does this entail */ }
					AuditLogEvent.StageInstanceDelete -> { /* What does this entail */ }
					AuditLogEvent.StageInstanceUpdate -> { /* What does this entail */ }
					AuditLogEvent.StickerCreate -> { /* No */ }
					AuditLogEvent.StickerDelete -> { /* No */ }
					AuditLogEvent.StickerUpdate -> { /* No */ }
					AuditLogEvent.ThreadCreate -> { /* Mayhaps? */ }
					AuditLogEvent.ThreadDelete -> { /* Mayhaps? */ }
					AuditLogEvent.ThreadUpdate -> { /* Mayhaps? */ }
					AuditLogEvent.WebhookCreate -> { /* No */ }
					AuditLogEvent.WebhookDelete -> { /* No */ }
					AuditLogEvent.WebhookUpdate -> { /* No */ }
					is AuditLogEvent.Unknown -> {
						// Silently stop? Log that *something* occurred?
					}
				}
			}
		}
	}
}
