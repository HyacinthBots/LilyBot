package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import net.irisshaders.lilybot.utils.ConfigData
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.responseEmbedInChannel

class Config : Extension() {

	override val name = "config"

	override suspend fun setup() {
		/**
		 * Commands to handle configuration
		 * @author NoComment1105
		 * @author tempest15
		 */
		ephemeralSlashCommand {
			name = "config"
			description = "Configuration set up commands!"

			ephemeralSubCommand(::Config) {
				name = "set"
				description = "Set the config"

				check { hasPermission(Permission.Administrator) }

				action {
					// If an action log ID doesn't exist, set the config
					// Otherwise, inform the user their config is already set

					if (DatabaseHelper.selectInConfig(guild!!.id, "modActionLog") == null) {
						val newConfig = ConfigData(
							guild!!.id,
							arguments.moderatorPing.id,
							arguments.modActionLog.id,
							arguments.messageLogs.id,
							arguments.joinChannel.id,
							arguments.supportChannel?.id,
							arguments.supportTeam?.id,
						)

						DatabaseHelper.putInConfig(newConfig)

						respond { content = "Config Set for Guild ID: ${guild!!.id}!" }

					} else {
						respond { content = "**Error:** There is already a configuration set for this guild!" }
						return@action
					}

					// Log the config being set in the action log
					val actionLogChannel = guild?.getChannel(arguments.modActionLog.id) as GuildMessageChannelBehavior
					responseEmbedInChannel(
						actionLogChannel,
						"Configuration set!",
						"An administrator has set a config for this guild!",
						null,
						user.asUser()
					)
				}
			}

			ephemeralSubCommand {
				name = "clear"
				description = "Clear the config!"

				check { hasPermission(Permission.Administrator) }

				action {
					// If an action log ID resists, inform the user their config isn't set.
					// Otherwise, clear the config.
					if (DatabaseHelper.selectInConfig(guild!!.id, "modActionLog") == null) {
						respond { content = "**Error:** There is no configuration set for this guild!" }
						return@action // Return to avoid the database trying to delete things that don't exist
					} else {
						respond { content = "Cleared config for Guild ID: ${guild!!.id}" }
						// Log the config being cleared to the action log
						val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, "modActionLog")
						val actionLogChannel = guild?.getChannel(actionLogId!!) as GuildMessageChannelBehavior
						responseEmbedInChannel(
							actionLogChannel,
							"Configuration cleared!",
							"An administrator has cleared the configuration for this guild!",
							null,
							user.asUser()
						)

						// clear the config (do this last)
						DatabaseHelper.clearConfig(guild!!.id)
					}
				}
			}
		}
	}

	inner class Config : Arguments() {
		val moderatorPing by role {
			name = "moderatorRole"
			description = "Your Moderator role"
		}
		val modActionLog by channel {
			name = "modActionLog"
			description = "Your Mod Action Log channel"
		}
		val messageLogs by channel {
			name = "messageLogs"
			description = "Your Message Logs Channel"
		}
		val joinChannel by channel {
			name = "joinChannel"
			description = "Your Join Logs Channel"
		}
		val supportTeam by optionalRole {
			name = "supportTeamRole"
			description = "Your Support Team role"
		}
		val supportChannel by optionalChannel {
			name = "supportChannel"
			description = "Your Support Channel"
		}
	}
}
