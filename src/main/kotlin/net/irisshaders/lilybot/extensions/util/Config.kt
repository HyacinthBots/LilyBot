@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import net.irisshaders.lilybot.database.DatabaseManager
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
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

					newSuspendedTransaction {

						DatabaseManager.Config.insertIgnore {
							it[guildId] = arguments.guildId.parsed
							it[moderatorsPing] = arguments.moderatorPing.parsed
							it[supportTeam] = arguments.supportTeam.parsed
							it[modActionLog] = arguments.modActionLog.parsed
							it[messageLogs] = arguments.messageLogs.parsed
							it[supportChanel] = arguments.supportChannel.parsed
							it[joinChannel] = arguments.joinChannel.parsed
						}
					}

					respond { content = "Config Set!" }
				}
			}
			ephemeralSubCommand(::Clear) {
				name = "clear"
				description = "Clear the config!"
				action {
					var guildConfig: String?
					newSuspendedTransaction {
						guildConfig = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq arguments.guidldId.parsed
						}.single()[DatabaseManager.Config.guildId]
						println(guildConfig)
					}
				}
			}
		}
	}
	inner class Config : Arguments() {
		val guildId = string {
			name = "guildId"
			description = "The ID of your guild. Used for getting action log channels"
		}
		val moderatorPing = string {
			name = "moderatorRoleId"
			description = "The ID of your Moderator role"
		}
		val modActionLog = string {
			name = "modActionLogId"
			description = "The ID of your Mod Action Log channel"
		}
		val messageLogs = string {
			name = "messageLogsId"
			description = "The ID of your Messsage Logs Channel"
		}
		val joinChannel = string {
			name = "joinChannelId"
			description = "The ID of your Join Logs Channel"
		}
		val supportTeam = optionalString {
			name = "supportTeamRoleId"
			description = "The ID of your Support Team role"
		}
		val supportChannel = optionalString {
			name = "supportChannelId"
			description = "The ID of your Support Channel "
		}
	}
	inner class Clear : Arguments() {
		val guidldId = string {
			name = "guildId"
			description = "The ID of the guild to clear the config for"
		}
	}
}
