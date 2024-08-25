package org.hyacinthbots.lilybot.extensions.config.commands

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

suspend fun SlashCommand<*, *, *>.configClearCommand() = ephemeralSubCommand(::ClearArgs) {
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

class ClearArgs : Arguments() {
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
