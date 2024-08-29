@file:Suppress("DuplicatedCode")

package org.hyacinthbots.lilybot.extensions.config.utils

import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.UserBehavior
import dev.kord.rest.builder.message.EmbedBuilder
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.extensions.logging.config.LoggingArgs
import org.hyacinthbots.lilybot.extensions.moderation.config.ModerationArgs
import org.hyacinthbots.lilybot.extensions.utility.config.UtilityArgs
import org.hyacinthbots.lilybot.utils.interval
import org.hyacinthbots.lilybot.utils.trimmedContents

suspend fun EmbedBuilder.utilityEmbed(arguments: UtilityArgs, user: UserBehavior) {
	title = "Configuration: Utility"
	field {
		name = "Utility Log"
		value = if (arguments.utilityLogChannel != null) {
			"${arguments.utilityLogChannel!!.mention} ${arguments.utilityLogChannel!!.data.name.value}"
		} else {
			"Disabled"
		}
	}
	field {
		name = "Log Channel updates"
		value = if (arguments.logChannelUpdates) "Yes" else "No"
	}
	field {
		name = "Log Event updates"
		value = if (arguments.logEventUpdates) "Yes" else "No"
	}
	field {
		name = "Log Invite updates"
		value = if (arguments.logInviteUpdates) "Yes" else "No"
	}
	field {
		name = "Log Role updates"
		value = if (arguments.logRoleUpdates) "Yes" else "No"
	}

	footer {
		text = "Configured by ${user.asUserOrNull()?.username}"
		icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
	}
}

suspend fun EmbedBuilder.moderationEmbed(arguments: ModerationArgs, user: UserBehavior) {
	title = "Configuration: Moderation"
	field {
		name = "Moderators"
		value = arguments.moderatorRole?.mention ?: "Disabled"
	}
	field {
		name = "Action log"
		value = arguments.modActionLog?.mention ?: "Disabled"
	}
	field {
		name = "Log publicly"
		value = when (arguments.logPublicly) {
			true -> "Enabled"
			false -> "Disabled"
			null -> "Disabled"
		}
	}
	field {
		name = "Quick timeout length"
		value = arguments.quickTimeoutLength.interval() ?: "No quick timeout length set"
	}
	field {
		name = "Warning Auto-punishments"
		value = when (arguments.warnAutoPunishments) {
			true -> "Enabled"
			false -> "Disabled"
			null -> "Disabled"
		}
	}
	field {
		name = "Ban DM Message"
		value = arguments.banDmMessage ?: "No custom Ban DM message set"
	}
	field {
		name = "Auto-invite Moderator Role"
		value = when (arguments.autoInviteModeratorRole) {
			true -> "Enabled"
			false -> "Disabled"
			null -> "Disabled"
		}
	}
	footer {
		text = "Configured by ${user.asUserOrNull()?.username}"
	}
}

suspend fun EmbedBuilder.loggingEmbed(arguments: LoggingArgs, guild: GuildBehavior?, user: UserBehavior) {
	title = "Configuration: Logging"
	field {
		name = "Message Delete Logs"
		value = if (arguments.enableMessageDeleteLogs && arguments.messageLogs != null) {
			arguments.messageLogs!!.mention
		} else {
			"Disabled"
		}
	}
	field {
		name = "Message Edit Logs"
		value = if (arguments.enableMessageEditLogs && arguments.messageLogs != null) {
			arguments.messageLogs!!.mention
		} else {
			"Disabled"
		}
	}
	field {
		name = "Member Logs"
		value = if (arguments.enableMemberLogging && arguments.memberLog != null) {
			arguments.memberLog!!.mention
		} else {
			"Disabled"
		}
	}

	field {
		name = "Public Member logs"
		value = if (arguments.enablePublicMemberLogging && arguments.publicMemberLog != null) {
			arguments.publicMemberLog!!.mention
		} else {
			"Disabled"
		}
	}
	if (arguments.enableMemberLogging && arguments.publicMemberLog != null) {
		val config = LoggingConfigCollection().getConfig(guild!!.id)
		if (config != null) {
			field {
				name = "Join Message"
				value = config.publicMemberLogData?.joinMessage.trimmedContents(256)!!
			}
			field {
				name = "Leave Message"
				value = config.publicMemberLogData?.leaveMessage.trimmedContents(256)!!
			}
			field {
				name = "Ping on join"
				value = config.publicMemberLogData?.pingNewUsers.toString()
			}
		}
	}

	footer {
		text = "Configured by ${user.asUserOrNull()?.username}"
		icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
	}
}
