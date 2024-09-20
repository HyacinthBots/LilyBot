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
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.hyacinthbots.lilybot.extensions.config.utils.utilityEmbed

suspend fun SlashCommand<*, *, *>.utilityCommand() = ephemeralSubCommand(::UtilityArgs) {
	name = "utility"
	description = "Configure Lily's utility settings"

	requirePermission(Permission.ManageGuild)

	check {
		anyGuild()
		hasPermission(Permission.ManageGuild)
	}

	action {
		val utilityConfig = UtilityConfigCollection().getConfig(guild!!.id)

		if (utilityConfig != null) {
			respond {
				content = "You already have a utility configuration set. " +
					"Please clear it before attempting to set a new one."
			}
			return@action
		}

		var utilityLog: TextChannel? = null
		if (arguments.utilityLogChannel != null) {
			utilityLog = guild!!.getChannelOfOrNull(arguments.utilityLogChannel!!.id)
			if (utilityLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
				respond {
					content = "The utility log you've selected is invalid, or I can't view it. " +
						"Please attempt to resolve this and try again."
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
