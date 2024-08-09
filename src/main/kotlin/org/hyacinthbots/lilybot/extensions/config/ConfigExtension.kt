package org.hyacinthbots.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.embed
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.logging.config.loggingCommand
import org.hyacinthbots.lilybot.extensions.moderation.config.moderationCommand
import org.hyacinthbots.lilybot.extensions.utils.config.utilityCommand
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.interval

class ConfigExtension : Extension() {
	override val name: String = "config"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "config"
			description = "Configure Lily's settings"

			loggingCommand()

			moderationCommand()

			utilityCommand()

			ephemeralSubCommand(::ClearArgs) {
				name = "clear"
				description = "Clear a config type"

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					suspend fun logClear() {
						val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

						if (utilityLog == null) {
							respond {
								content = "Consider setting a utility config to log changes to configurations."
							}
							return
						}

						utilityLog.createMessage {
							embed {
								title = "Configuration Cleared: ${arguments.config[0]}${
									arguments.config.substring(1, arguments.config.length).lowercase()
								}"
								footer {
									text = "Config cleared by ${user.asUserOrNull()?.username}"
									icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
								}
							}
						}
					}

					when (arguments.config) {
						ConfigType.MODERATION.name -> {
							ModerationConfigCollection().getConfig(guild!!.id) ?: run {
								respond {
									content = "No moderation configuration exists to clear!"
								}
								return@action
							}

							logClear()

							ModerationConfigCollection().clearConfig(guild!!.id)
							respond {
								embed {
									title = "Config cleared: Moderation"
									footer {
										text = "Config cleared by ${user.asUserOrNull()?.username}"
										icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
									}
								}
							}
						}

						ConfigType.LOGGING.name -> {
							LoggingConfigCollection().getConfig(guild!!.id) ?: run {
								respond {
									content = "No logging configuration exists to clear!"
								}
								return@action
							}

							logClear()

							LoggingConfigCollection().clearConfig(guild!!.id)
							respond {
								embed {
									title = "Config cleared: Logging"
									footer {
										text = "Config cleared by ${user.asUserOrNull()?.username}"
										icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
									}
								}
							}
						}

						ConfigType.UTILITY.name -> {
							UtilityConfigCollection().getConfig(guild!!.id) ?: run {
								respond {
									content = "No utility configuration exists to clear"
								}
								return@action
							}

							logClear()

							UtilityConfigCollection().clearConfig(guild!!.id)
							respond {
								embed {
									title = "Config cleared: Utility"
									footer {
										text = "Config cleared by ${user.asUserOrNull()?.username}"
										icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
									}
								}
							}
						}

						ConfigType.ALL.name -> {
							ModerationConfigCollection().clearConfig(guild!!.id)
							LoggingConfigCollection().clearConfig(guild!!.id)
							UtilityConfigCollection().clearConfig(guild!!.id)
							respond {
								embed {
									title = "All configs cleared"
									footer {
										text = "Configs cleared by ${user.asUserOrNull()?.username}"
										icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
									}
								}
							}
						}
					}
				}
			}

			ephemeralSubCommand(::ViewArgs) {
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
		}
	}

	inner class ClearArgs : Arguments() {
		val config by stringChoice {
			name = "config-type"
			description = "The type of config to clear"
			choices = mutableMapOf(
				"moderation" to ConfigType.MODERATION.name,
				"logging" to ConfigType.LOGGING.name,
				"utility" to ConfigType.UTILITY.name,
				"all" to ConfigType.ALL.name
			)
		}
	}

	inner class ViewArgs : Arguments() {
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
}
