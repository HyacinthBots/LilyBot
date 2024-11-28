package org.hyacinthbots.lilybot.extensions.utility.config

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.utils.botHasPermissions
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.hyacinthbots.lilybot.extensions.config.utils.utilityEmbed

suspend fun SlashCommand<*, *, *>.utilityCommand() = ephemeralSubCommand(::UtilityArgs) {
	name = Translations.Config.Utility.name
	description = Translations.Config.Utility.description

	requirePermission(Permission.ManageGuild)

	check {
		anyGuild()
		hasPermission(Permission.ManageGuild)
	}

	action {
		val utilityConfig = UtilityConfigCollection().getConfig(guild!!.id)

		if (utilityConfig != null) {
			respond {
				content = Translations.Config.configAlreadyExists.translate("utility")
			}
			return@action
		}

		var utilityLog: TextChannel? = null
		if (arguments.utilityLogChannel != null) {
			utilityLog = guild!!.getChannelOfOrNull(arguments.utilityLogChannel!!.id)
			if (utilityLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
				respond {
					content = Translations.Config.invalidChannel.translate("utility")
				}
				return@action
			}
		}

		respond {
			embed {
				utilityEmbed(arguments, user)
			}
		}

		UtilityConfigCollection().setConfig(
			UtilityConfigData(
				guild!!.id,
				arguments.utilityLogChannel?.id,
				arguments.logChannelUpdates,
				arguments.logEventUpdates,
				arguments.logInviteUpdates,
				arguments.logRoleUpdates
			)
		)

		utilityLog?.createMessage {
			embed {
				utilityEmbed(arguments, user)
			}
		}
	}
}
