@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import mu.KotlinLogging
import net.irisshaders.lilybot.utils.dmNotificationEmbed
import net.irisshaders.lilybot.utils.getConfigPrivateResponse
import net.irisshaders.lilybot.utils.isBotOrModerator
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import net.irisshaders.lilybot.utils.userDMEmbed
import kotlin.time.ExperimentalTime

@Suppress("DuplicatedCode")
class TerminalModeration : Extension() {
	override val name = "terminal-moderation"

	override suspend fun setup() {
		val logger = KotlinLogging.logger { }

		/**
		 * Ban command
		 * @author IMS212
		 */
		ephemeralSlashCommand(::BanArgs) {
			name = "ban"
			description = "Bans a user."

			// Require the Ban Member permission
			check { hasPermission(Permission.BanMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				isBotOrModerator(userArg, "ban") ?: return@action

				// DM the user before the ban task is run, to avoid error, null if fails
				val dm = userDMEmbed(
					userArg,
					"You have been banned from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}",
					null
				)

				try {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // remove timeout if they had a timeout when banned
				} catch (e: EntityNotFoundException) {
					logger.info("Unable to find user! Skipping timeout removal")
				}

				// Run the ban task
				guild?.ban(userArg.id, builder = {
					this.reason = arguments.reason
					this.deleteMessagesDays = arguments.messages
				})

				respond {
					content = "Banned a user"
				}

				actionLog.createEmbed {
					color = DISCORD_BLACK
					title = "Banned a user"
					description = "${userArg.mention} has been banned!\n${userArg.id} (${userArg.tag})"

					field {
						name = "Reason:"
						value = arguments.reason
						inline = false
					}
					field {
						name = "Days of messages deleted:"
						value = arguments.messages.toString()
						inline = false
					}
					dmNotificationEmbed(dm)
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}

					timestamp = Clock.System.now()
				}
			}
		}

		/**
		 *  Unban command
		 *  @author NoComment1105
		 */
		ephemeralSlashCommand(::UnbanArgs) {
			name = "unban"
			description = "Unbans a user"

			// Require Ban Members permission, only this check
			// to avoid your everyday user from unbanning people
			check { hasPermission(Permission.BanMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val bans = guild!!.bans.toList().map { it.userId }

				// Unban the user
				if (userArg.id in bans) {
					guild?.unban(userArg.id)
				} else {
					respond { content = "**Error:** User is not banned" }
					return@action
				}
				respond {
					content = "Unbanned user"
				}

				responseEmbedInChannel(
					actionLog,
					"Unbanned a user",
					"${userArg.mention} has been unbanned!\n${userArg.id} (${userArg.tag})",
					DISCORD_GREEN,
					user.asUser()
				)
			}
		}

		/**
		 * Soft ban command
		 * @author NoComment1105
		 */
		ephemeralSlashCommand(::SoftBanArgs) {
			name = "soft-ban"
			description = "Soft-bans a user"

			// Requires Ban Members Permission
			check { hasPermission(Permission.BanMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				isBotOrModerator(userArg, "soft-ban") ?: return@action

				// DM the user before the ban task is run
				val dm = userDMEmbed(
					userArg,
					"You have been soft-banned from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}\n\n" +
							"You are free to rejoin without the need to be unbanned",
					null
				)

				guild?.getMember(userArg.id)?.edit { timeoutUntil = null }

				// Ban the user, mark it as a soft-ban clearly
				guild?.ban(userArg.id, builder = {
					this.reason = "${arguments.reason} + **SOFT-BAN**"
					this.deleteMessagesDays = arguments.messages
				})

				respond {
					content = "Soft-Banned User"
				}

				actionLog.createEmbed {
					color = DISCORD_BLACK
					title = "Soft-banned a user"
					description = "${userArg.mention} has been soft banned\n${userArg.id} (${userArg.tag})"

					field {
						name = "Reason:"
						value = arguments.reason
						inline = false
					}
					field {
						name = "Days of messages deleted"
						value = arguments.messages.toString()
						inline = false
					}
					dmNotificationEmbed(dm)
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}

					timestamp = Clock.System.now()
				}

				// Unban the user, as you're supposed to in soft-ban
				guild?.unban(userArg.id)
			}
		}

		/**
		 * Kick command
		 * @author IMS212
		 */
		ephemeralSlashCommand(::KickArgs) {
			name = "kick"
			description = "Kicks a user."

			// Require Kick Members permission
			check { hasPermission(Permission.KickMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				isBotOrModerator(userArg, "kick") ?: return@action

				// DM the user about it before the kick
				val dm = userDMEmbed(
					userArg,
					"You have been kicked from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}",
					null
				)

				// Run the kick task
				guild?.kick(userArg.id, arguments.reason)

				respond {
					content = "Kicked User"
				}

				actionLog.createEmbed {
					color = DISCORD_BLACK
					title = "Kicked User"
					description = "Kicked ${userArg.mention} from the server\n${userArg.id} (${userArg.tag})"

					field {
						name = "Reason"
						value = arguments.reason
						inline = false
					}
					dmNotificationEmbed(dm)
					footer {
						text = "Requested By ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}
	}

	inner class KickArgs : Arguments() {
		val userArgument by user {
			name = "kickUser"
			description = "Person to kick"
		}
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the Kick"
			defaultValue = "No Reason Provided"
		}
	}

	inner class BanArgs : Arguments() {
		val userArgument by user {
			name = "banUser"
			description = "Person to ban"
		}
		val messages by int {
			name = "messages"
			description = "Messages"
		}
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No Reason Provided"
		}
	}

	inner class UnbanArgs : Arguments() {
		val userArgument by user {
			name = "unbanUserId"
			description = "Person Unbanned"
		}
	}

	inner class SoftBanArgs : Arguments() {
		val userArgument by user {
			name = "softBanUser"
			description = "Person to Soft ban"
		}
		val messages by defaultingInt {
			name = "messages"
			description = "Messages"
			defaultValue = 3
		}
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No Reason Provided"
		}
	}
}
