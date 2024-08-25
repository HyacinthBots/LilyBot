package org.hyacinthbots.lilybot.extensions.utils.config

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.utils.botHasPermissions
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData

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

		suspend fun EmbedBuilder.utilityEmbed() {
			title = "Configuration: Utility"
			field {
				name = "Utility Log"
				value = if (arguments.utilityLogChannel != null) {
					"${arguments.utilityLogChannel!!.mention} ${arguments.utilityLogChannel!!.data.name.value}"
				} else {
					"Disabled"
				}
			}

			footer {
				text = "Configured by ${user.asUserOrNull()?.username}"
				icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
			}
		}

		respond {
			embed {
				utilityEmbed()
			}
		}

		UtilityConfigCollection().setConfig(
			UtilityConfigData(
				guild!!.id,
				arguments.utilityLogChannel?.id
			)
		)

		utilityLog?.createMessage {
			embed {
				utilityEmbed()
			}
		}
	}
}
