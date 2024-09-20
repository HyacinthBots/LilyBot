package org.hyacinthbots.lilybot.extensions.config.commands

import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.interval

suspend fun SlashCommand<*, *, *>.configViewCommand() = ephemeralSubCommand(::ViewArgs) {
	name = "view"
	description = "View the current config that you have set"

	requirePermission(Permission.ManageGuild)

	check {
		anyGuild()
		hasPermission(Permission.ManageGuild)
	}

	action {
		when (arguments.config) {
			ConfigType.MODERATION.name -> {
				val config = ModerationConfigCollection().getConfig(guild!!.id)
				if (config == null) {
					respond {
						content = "There is no moderation config for this guild"
					}
					return@action
				}

				respond {
					embed {
						title = "Current moderation config"
						description = "This is the current moderation config for this guild"
						field {
							name = "Enabled/Disabled"
							value = if (config.enabled) "Enabled" else "Disabled"
						}
						field {
							name = "Moderators"
							value = config.role?.let { guild!!.getRoleOrNull(it)?.mention } ?: "Disabled"
						}
						field {
							name = "Action log"
							value =
								config.channel?.let { guild!!.getChannelOrNull(it)?.mention } ?: "Disabled"
						}
						field {
							name = "Log publicly"
							value = when (config.publicLogging) {
								true -> "Enabled"
								false -> "Disabled"
								null -> "Disabled"
							}
						}
						field {
							name = "Quick timeout length"
							value = config.quickTimeoutLength.interval() ?: "No quick timeout length set"
						}
						field {
							name = "Warning Auto-punishments"
							value = when (config.autoPunishOnWarn) {
								true -> "Enabled"
								false -> "Disabled"
								null -> "Disabled"
							}
						}
						field {
							name = "Ban DM Message"
							value = config.banDmMessage ?: "No custom Ban DM message set"
						}
						field {
							name = "Auto-invite Moderator Role"
							value = when (config.autoInviteModeratorRole) {
								true -> "Enabled"
								false -> "Disabled"
								null -> "Disabled"
							}
						}
						timestamp = Clock.System.now()
					}
				}
			}

			ConfigType.LOGGING.name -> {
				val config = LoggingConfigCollection().getConfig(guild!!.id)
				if (config == null) {
					respond {
						content = "There is no logging config for this guild"
					}
					return@action
				}

				respond {
					embed {
						title = "Current logging config"
						description = "This is the current logging config for this guild"
						field {
							name = "Message delete logs"
							value = if (config.enableMessageDeleteLogs) {
								"Enabled\n" +
									"* ${guild!!.getChannelOrNull(config.messageChannel!!)?.mention ?: "Unable to get channel mention"} (" +
									"${guild!!.getChannelOrNull(config.messageChannel)?.name ?: "Unable to get channel name"})"
							} else {
								"Disabled"
							}
						}
						field {
							name = "Message edit logs"
							value = if (config.enableMessageEditLogs) {
								"Enabled\n" +
									"* ${guild!!.getChannelOrNull(config.messageChannel!!)?.mention ?: "Unable to get channel mention"} (" +
									"${guild!!.getChannelOrNull(config.messageChannel)?.name ?: "Unable to get channel mention"})"
							} else {
								"Disabled"
							}
						}
						field {
							name = "Member logs"
							value = if (config.enableMemberLogs) {
								"Enabled\n" +
									"* ${guild!!.getChannelOrNull(config.memberLog!!)?.mention ?: "Unable to get channel mention"} (" +
									"${guild!!.getChannelOrNull(config.memberLog)?.name ?: "Unable to get channel mention."})"
							} else {
								"Disabled"
							}
						}
						timestamp = Clock.System.now()
					}
				}
			}

			ConfigType.UTILITY.name -> {
				val config = UtilityConfigCollection().getConfig(guild!!.id)
				if (config == null) {
					respond {
						content = "There is no utility config for this guild"
					}
					return@action
				}

				respond {
					embed {
						title = "Current utility config"
						description = "This is the current utility config for this guild"
						field {
							name = "Channel"
							value =
								"${
									config.utilityLogChannel?.let { guild!!.getChannelOrNull(it)?.mention } ?: "None"
								} ${config.utilityLogChannel?.let { guild!!.getChannelOrNull(it)?.name } ?: ""}"
						}
						timestamp = Clock.System.now()
					}
				}
			}
		}
	}
}

class ViewArgs : Arguments() {
	val config by stringChoice {
		name = "config-type"
		description = "The type of config to clear"
		choices = mutableMapOf(
			"moderation" to ConfigType.MODERATION.name,
			"logging" to ConfigType.LOGGING.name,
			"utility" to ConfigType.UTILITY.name,
		)
	}
}
