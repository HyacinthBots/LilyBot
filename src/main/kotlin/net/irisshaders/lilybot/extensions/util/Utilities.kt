@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.ResponseHelper
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import kotlin.time.ExperimentalTime

@Suppress("DuplicatedCode")
class Utilities : Extension() {
	override val name = "utilities"

	override suspend fun setup() {
		/**
		 * Say Command
		 * @author NoComment1105
		 */
		ephemeralSlashCommand(::SayArgs) {
			name = "say"
			description = "Say something through Lily."

			check { hasPermission(Permission.ModerateMembers) } // Idk wasn't sure

			action {
				var actionLogId: String? = null
				var error = false
				newSuspendedTransaction {
					try {
						actionLogId = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq guild?.id.toString()
						}.single()[DatabaseManager.Config.modActionLog]
					} catch (e: NoSuchElementException) {
						error = true
					}
				}

				if (!error) {
					val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior

					if (arguments.embedMessage) {
						channel.createEmbed {
							color = DISCORD_BLURPLE
							description = arguments.messageArgument
							timestamp = Clock.System.now()
						}
					} else {
						channel.createMessage {
							content = arguments.messageArgument
						}
					}

					respond { content = "Command used" }

					ResponseHelper.responseEmbedInChannel(
						actionLog,
						"Message Sent",
						"/say has been used to say ${arguments.messageArgument}.",
						DISCORD_BLACK,
						user.asUser()
					)
				} else {
					respond { content = "**Error:** Unable to access a config for this guild! Have you set it?" }
				}
			}
		}

		/**
		 * Presence Command
		 * @author IMS
		 */
		ephemeralSlashCommand(::PresenceArgs) {
			name = "set-status"
			description = "Set Lily's current presence/status."

			check { hasPermission(Permission.ModerateMembers) } // Idk wasn't sure

			action {
				var actionLogId: String? = null
				var error = false
				newSuspendedTransaction {
					try {
						actionLogId = DatabaseManager.Config.select {
							DatabaseManager.Config.guildId eq guild?.id.toString()
						}.single()[DatabaseManager.Config.modActionLog]
					} catch (e: NoSuchElementException) {
						error = true
					}
				}

				if (!error) {
					val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior

					this@ephemeralSlashCommand.kord.editPresence {
						status = PresenceStatus.Online
						playing(arguments.presenceArgument)
					}

					respond { content = "Presence set to `${arguments.presenceArgument}`" }

					ResponseHelper.responseEmbedInChannel(
						actionLog,
						"Presence Changed",
						"Lily's presence has been set to `${arguments.presenceArgument}`",
						DISCORD_BLACK,
						user.asUser()
					)
				} else {
					respond { content = "**Error:** Unable to access a config for this guild! Have you set it?" }
				}
			}
		}
	}

	inner class SayArgs : Arguments() {
		val messageArgument by string {
			name = "message"
			description = "Message contents"
		}
		val embedMessage by boolean {
			name = "embed"
			description = "Would you like to send as embed"
		}
	}

	inner class PresenceArgs : Arguments() {
		val presenceArgument by string {
			name = "presence"
			description = "Lily's presence"
		}
	}
}
