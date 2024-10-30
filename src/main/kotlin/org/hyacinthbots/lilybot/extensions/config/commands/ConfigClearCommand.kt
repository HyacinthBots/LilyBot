package org.hyacinthbots.lilybot.extensions.config.commands

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.EphemeralSlashCommandContext
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms

suspend fun SlashCommand<*, *, *>.configClearCommand() = ephemeralSubCommand(::ClearArgs) {
	name = Translations.Config.Clear.name
	description = Translations.Config.Clear.description

	requirePermission(Permission.ManageGuild)

	check {
		anyGuild()
		hasPermission(Permission.ManageGuild)
	}

	action {
		when (arguments.config) {
			ConfigType.MODERATION.name -> clearConfig(ConfigType.MODERATION, arguments)

			ConfigType.LOGGING.name -> clearConfig(ConfigType.LOGGING, arguments)

			ConfigType.UTILITY.name -> clearConfig(ConfigType.UTILITY, arguments)

			ConfigType.ALL.name -> clearConfig(ConfigType.ALL, arguments)
		}

		respond {
			embed {
				title = if (arguments.config == ConfigType.ALL.name) {
					Translations.Config.Clear.all.translate()
				} else {
					Translations.Config.Clear.Embed.title.translate(arguments.config)
				}
				footer {
					text = Translations.Config.configuredBy.translate(user.asUserOrNull()?.username)
					icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
				}
			}
		}
	}
}

/**
 * Handles the clearing of a configuration(s) based on the [args] and [type].
 *
 * @param type The type of config to clear
 * @param args The [ClearArgs] for the clearing of the config
 * @author NoComment1105
 * @since 5.0.0
 */
private suspend fun EphemeralSlashCommandContext<*, *>.clearConfig(type: ConfigType, args: ClearArgs) {
	val obj = Translations.Config.Clear
	when (type) {
		ConfigType.MODERATION -> {
			ModerationConfigCollection().getConfig(guild!!.id) ?: run {
				respond {
					content = obj.noConfigMod.translate()
				}
				return
			}
			logClear(args)
			ModerationConfigCollection().clearConfig(guild!!.id)
		}

		ConfigType.LOGGING -> {
			LoggingConfigCollection().getConfig(guild!!.id) ?: run {
				respond {
					content = obj.noConfigLogging.translate()
				}
			}
			logClear(args)
			LoggingConfigCollection().clearConfig(guild!!.id)
		}

		ConfigType.UTILITY -> {
			UtilityConfigCollection().getConfig(guild!!.id) ?: run {
				respond {
					content = obj.noConfigUtility.translate()
				}
			}
			logClear(args)
			UtilityConfigCollection().clearConfig(guild!!.id)
		}

		ConfigType.ALL -> {
			ModerationConfigCollection().clearConfig(guild!!.id)
			LoggingConfigCollection().clearConfig(guild!!.id)
			UtilityConfigCollection().clearConfig(guild!!.id)
		}
	}
}

/**
 * Log the clearing of a configuration to the utility log, should there still be one.
 *
 * @param arguments The [ClearArgs] for the clearing of the config
 * @author NoComment1105
 * @since 5.0.0
 */
suspend fun EphemeralSlashCommandContext<*, *>.logClear(arguments: ClearArgs) {
	// Skip this if the utility config is cleared or all configs are cleared
	if (arguments.config == ConfigType.UTILITY.name || arguments.config == ConfigType.ALL.name) return
	val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

	if (utilityLog == null) {
		respond {
			content = Translations.Config.considerUtility.translate()
		}
		return
	}

	utilityLog.createMessage {
		embed {
			title = Translations.Config.Clear.Embed.title.translate(
			    arguments.config[0] +
				arguments.config.substring(1, arguments.config.length).lowercase()
			)
			footer {
				text = Translations.Config.Clear.footer.translate(user.asUserOrNull()?.username)
				icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
			}
		}
	}
}

class ClearArgs : Arguments() {
	private val choiceObj = Translations.Config.Arguments.Clear.Choice
	val config by stringChoice {
		name = Translations.Config.Arguments.Clear.name
		description = Translations.Config.Arguments.Clear.description
		choices = mutableMapOf(
			choiceObj.moderation to ConfigType.MODERATION.name,
			choiceObj.logging to ConfigType.LOGGING.name,
			choiceObj.utility to ConfigType.UTILITY.name,
			choiceObj.all to ConfigType.ALL.name
		)
	}
}
