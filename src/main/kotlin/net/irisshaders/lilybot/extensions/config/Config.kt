@file:OptIn(ExperimentalTime::class)

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

			ephemeralSubCommand(::Config) {
				name = "set"
				description = "Set the config"

				// We only want admins doing this, not your average moderator
				check { hasPermission(Permission.Administrator) }

				action {
					val actionLogId: String?
					// Try to select the guild from the database, with either return NoSuchElementException,
					//  or the guild id
					val alreadySet = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.guildId)

					// If the database is empty, there is nothing set for this guild, so we move ahead with
					// adding config else we inform the config is already set
					if (alreadySet.equals("NoSuchElementException")) {
						newSuspendedTransaction {
							DatabaseManager.Config.insertIgnore {
								it[guildId] = guild!!.id.toString()
								it[moderatorsPing] = arguments.moderatorPing.id.toString()
								it[supportTeam] = arguments.supportTeam?.id.toString()
								it[modActionLog] = arguments.modActionLog.id.toString()
								it[messageLogs] = arguments.messageLogs.id.toString()
								it[supportChannel] = arguments.supportChannel?.id.toString()
								it[joinChannel] = arguments.joinChannel.id.toString()
							}
						}

						// Once inserted to the database we get the mod action log, so we can print a message to it
						actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)

						respond { content = "Config Set for Guild ID: ${guild!!.id}!" }

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
			ephemeralSubCommand {
				name = "clear"
				description = "Clear the config!"

				// We only want admins doing this, not your average moderator
				check { hasPermission(Permission.Administrator) }

				action {
					var error = false

					// Try to get the guild ID and action log ID. NoSuchElement means no config set
					val guildConfig: String? = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.guildId)
					val actionLogId: String? = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)

					if (guildConfig.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
						respond {
							content = "**Error:** There is no configuration set for this guild!"
						}
						// Return to avoid the database trying to delete things that don't exist
						return@action
					}

					newSuspendedTransaction {
						try {
							DatabaseManager.Config.deleteWhere {
								DatabaseManager.Config.guildId eq guild!!.id.toString()
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

					// Use error to decide whether to return. More of a sanity check,
					// the previous one should have caught the NoSuchElementException
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
