@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

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
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.database.DatabaseManager.getConnection
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
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

					newSuspendedTransaction {

						DatabaseManager.Config.insertIgnore {
							it[guildId] = arguments.guildId.toString()
							it[moderatorsPing] = arguments.moderatorPing.id.toString()
							it[supportTeam] = arguments.supportTeam?.id.toString()
							it[modActionLog] = arguments.modActionLog.id.toString()
							it[messageLogs] = arguments.messageLogs.id.toString()
							it[supportChanel] = arguments.supportChannel?.id.toString()
							it[joinChannel] = arguments.joinChannel.id.toString()
						}
					}

					respond { content = "Config Set for Guild ID: ${arguments.guildId}!" }
				}
			}
			ephemeralSubCommand(::Clear) {
				name = "clear"
				description = "Clear the config!"
				action {
					var guildConfig: String? = null

					newSuspendedTransaction {
						guildConfig = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq arguments.guidldId.toString()
						}.single()[DatabaseManager.Config.guildId]
					}

					try {
						val connection: Connection = getConnection()
						val ps: PreparedStatement = connection.prepareStatement("DELETE FROM config WHERE guildId = ?")
						ps.setString(1, guildConfig)
						ps.executeUpdate()
					} catch (e: SQLException) {
						respond { content = "An error occured in updating the Database. Please report this!" }
						e.printStackTrace()
						return@action
					}

					respond { content = "Cleared config for Guild ID: $guildConfig" }
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
			description = "Your Messsage Logs Channel"
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
			description = "Your Support Channel "
		}
	}
	inner class Clear : Arguments() {
		val guidldId by guild {
			name = "guildId"
			description = "The ID of your guild"
		}
	}
}
