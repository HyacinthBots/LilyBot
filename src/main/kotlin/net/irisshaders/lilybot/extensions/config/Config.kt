@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.guild
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import net.irisshaders.lilybot.database.DatabaseHelper
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.ResponseHelper
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.ExperimentalTime

class Config : Extension() {

	override val name = "config"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "config"
			description = "Configuration set up commands!"

			check { hasPermission(Permission.Administrator) }

			ephemeralSubCommand(::Config) {
				name = "set"
				description = "Set the config"
				action {
					var actionLogId: String? = null
					val alreadySet: String? =
						DatabaseHelper.selectInConfig(arguments.guildId.id, DatabaseManager.Config.guildId)

					if (alreadySet.equals("NoSuchElementException")) {
						newSuspendedTransaction {

							DatabaseManager.Config.insertIgnore {
								it[guildId] = arguments.guildId.id.toString()
								it[moderatorsPing] = arguments.moderatorPing.id.toString()
								it[supportTeam] = arguments.supportTeam?.id.toString()
								it[modActionLog] = arguments.modActionLog.id.toString()
								it[messageLogs] = arguments.messageLogs.id.toString()
								it[supportChanel] = arguments.supportChannel?.id.toString()
								it[joinChannel] = arguments.joinChannel.id.toString()
							}

							actionLogId = DatabaseHelper.selectInConfig(arguments.guildId.id, DatabaseManager.Config.modActionLog)
						}

						respond { content = "Config Set for Guild ID: ${arguments.guildId.id}!" }

						val actionLogChannel = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
						ResponseHelper.responseEmbedInChannel(
							actionLogChannel,
							"Configuration set!",
							"An administrator has set a config for this guild!",
							null,
							user.asUser()
						)
					} else {
						respond { content = "Your configuration is already set, clear it first before updating!" }
					}
				}
			}
			ephemeralSubCommand(::Clear) {
				name = "clear"
				description = "Clear the config!"
				action {
					var error = false

					val guildConfig: String? = DatabaseHelper.selectInConfig(arguments.guildId.id, DatabaseManager.Config.guildId)
					val actionLogId: String? = DatabaseHelper.selectInConfig(arguments.guildId.id, DatabaseManager.Config.modActionLog)

					if (guildConfig.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
						respond {
							content = "**Error:** There is no configuration set for this guild!"
						}
						return@action
					}

					newSuspendedTransaction {
						try {
							DatabaseManager.Config.deleteWhere {
								DatabaseManager.Config.guildId eq arguments.guildId.id.toString()
							}
						} catch (e: NoSuchElementException) {
							// More of a sanity check, the action should've returned by
							// this point if there was no configuration for the guild
							respond {
								content = "**Error:** There is no configuration set for this guild!"
							}
							error = true
						}
					}

					if (error) return@action

					respond { content = "Cleared config for Guild ID: $guildConfig" }

					val actionLogChannel = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
					ResponseHelper.responseEmbedInChannel(
						actionLogChannel,
						"Configuration cleared!",
						"An administrator has cleared the configuration for this guild!",
						null,
						user.asUser()
					)
				}
			}
		}
	}
	inner class Config : Arguments() {
		val guildId by guild {
			name = "guildId"
			description = "The ID of your guild"
		}
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
	inner class Clear : Arguments() {
		val guildId by guild {
			name = "guildId"
			description = "The ID of your guild"
		}
	}
}
