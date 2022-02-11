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
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.database.DatabaseManager.getConnection
import net.irisshaders.lilybot.utils.ResponseHelper
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
					var actionLogId: String? = null
					var alreadySet = false

				    newSuspendedTransaction {
						alreadySet = try {
							DatabaseManager.Config.select {
								DatabaseManager.Config.guildId eq arguments.guildId.id.toString()
							}.single()[DatabaseManager.Config.guildId]
							true
						} catch (e: NoSuchElementException) {
							false
							// Swallow the error and return because we want to know this
						}
					}

					if (!alreadySet) {
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

							actionLogId = DatabaseManager.Config.select {
								DatabaseManager.Config.guildId eq arguments.guildId.id.toString()
							}.single()[DatabaseManager.Config.modActionLog]
						}

						respond { content = "Config Set for Guild ID: ${arguments.guildId.id}!" }

						val actionLogChannel = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
						ResponseHelper.responseEmbedInChannel(
							actionLogChannel,
							"Config Set!",
							"An admin has set a config for this guild!",
							null,
							user.asUser()
						)
					} else {
						respond { content = "Your config is already set, clear it first before updating!" }
					}
				}
			}
			ephemeralSubCommand(::Clear) {
				name = "clear"
				description = "Clear the config!"
				action {
					var guildConfig: String? = null
					var actionLogId: String? = null

					newSuspendedTransaction {
						guildConfig = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq arguments.guildId.id.toString()
						}.single()[DatabaseManager.Config.guildId]

						actionLogId = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq arguments.guildId.id.toString()
						}.single()[DatabaseManager.Config.modActionLog]
					}

					// Yes I did write raw SQL, even though we're using exposed.
					// No I won't change it, exposed is clapped for removing stuff from the db
					try {
						val connection: Connection = getConnection()
						val ps: PreparedStatement = connection.prepareStatement("DELETE FROM config WHERE guildId = ?")
						ps.setString(1, guildConfig)
						ps.executeUpdate()
					} catch (e: SQLException) {
						respond { content = "An error occurred in updating the Database. Please report this!" }
						e.printStackTrace()
						return@action
					}

					respond { content = "Cleared config for Guild ID: $guildConfig" }

					val actionLogChannel = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
					ResponseHelper.responseEmbedInChannel(
						actionLogChannel,
						"Config Cleared!",
						"An admin has cleared the config for this guild!",
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
