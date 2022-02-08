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
							it[guildId] = arguments.guildId.id.toString()
							it[moderatorsPing] = arguments.moderatorPing.id.toString()
							it[supportTeam] = arguments.supportTeam?.id.toString()
							it[modActionLog] = arguments.modActionLog.id.toString()
							it[messageLogs] = arguments.messageLogs.id.toString()
							it[supportChanel] = arguments.supportChannel?.id.toString()
							it[joinChannel] = arguments.joinChannel.id.toString()
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
							DatabaseManager.Config.guildId eq arguments.guidldId.id.toString()
						}.single()[DatabaseManager.Config.guildId]
						println(guildConfig)
					}
				}
			}
		}
	}
	inner class Config : Arguments() {
		val guildId by guild {
			name = "guildId"
			description = "The ID of your guild. Used for getting action log channels"
		}
		val moderatorPing by role {
			name = "moderatorRoleId"
			description = "The ID of your Moderator role"
		}
		val modActionLog by channel {
			name = "modActionLogId"
			description = "The ID of your Mod Action Log channel"
		}
		val messageLogs by channel {
			name = "messageLogsId"
			description = "The ID of your Messsage Logs Channel"
		}
		val joinChannel by channel {
			name = "joinChannelId"
			description = "The ID of your Join Logs Channel"
		}
		val supportTeam by optionalRole {
			name = "supportTeamRoleId"
			description = "The ID of your Support Team role"
		}
		val supportChannel by optionalChannel {
			name = "supportChannelId"
			description = "The ID of your Support Channel "
		}
	}
	inner class Clear : Arguments() {
		val guidldId by guild {
			name = "guildId"
			description = "The ID of the guild to clear the config for"
		}
	}
}
