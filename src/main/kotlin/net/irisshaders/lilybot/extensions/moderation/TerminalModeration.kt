package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.checks.anyGuild
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
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.baseModerationEmbed
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.dmNotificationStatusEmbedField
import net.irisshaders.lilybot.utils.isBotOrModerator
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import net.irisshaders.lilybot.utils.userDMEmbed

/**
 * The class for permanent moderation actions, such as ban and kick.
 *
 * @since 3.0.0
 */
class TerminalModeration : Extension() {
	override val name = "terminal-moderation"

	override suspend fun setup() {
		val logger = KotlinLogging.logger { }

		/**
		 * Ban command
		 * @author IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::BanArgs) {
			name = "ban"
			description = "Bans a user."

			check { anyGuild() }
			check { hasPermission(Permission.BanMembers) }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				// Clarify the user is not a bot or moderator
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

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
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
					description = "${userArg.mention} has been banned!"

					baseModerationEmbed(arguments.reason, userArg, user)
					field {
						name = "Days of messages deleted:"
						value = arguments.messages.toString()
						inline = false
					}
					dmNotificationStatusEmbedField(dm)

					timestamp = Clock.System.now()
				}
			}
		}

		/**
		 *  Unban command
		 *  @author NoComment1105
		 *  @since 2.0
		 */
		ephemeralSlashCommand(::UnbanArgs) {
			name = "unban"
			description = "Unbans a user"

			check { anyGuild() }
			check { hasPermission(Permission.BanMembers) }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				// Get all the bans into a list
				val bans = guild!!.bans.toList().map { it.userId }

				// Search the list for the banned user
				if (userArg.id in bans) {
					// Unban the user if they're banned
					guild?.unban(userArg.id)
				} else {
					// Respond with an error if they aren't
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
		 * @since 2.0
		 */
		ephemeralSlashCommand(::SoftBanArgs) {
			name = "soft-ban"
			description = "Soft-bans a user"

			check { anyGuild() }
			check { hasPermission(Permission.BanMembers) }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior
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

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				try {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // Remove timeout if they had a timeout when banned
				} catch (e: EntityNotFoundException) {
					logger.info("Unable to find user! Skipping timeout removal")
				}

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
					description = "${userArg.mention} has been soft banned"

					baseModerationEmbed(arguments.reason, userArg, user)
					field {
						name = "Days of messages deleted"
						value = arguments.messages.toString()
						inline = false
					}
					dmNotificationStatusEmbedField(dm)

					timestamp = Clock.System.now()
				}

				// Unban the user, as you're supposed to in soft-ban
				guild?.unban(userArg.id)
			}
		}

		/**
		 * Kick command
		 * @author IMS212
		 * @since 2.0
		 */
		ephemeralSlashCommand(::KickArgs) {
			name = "kick"
			description = "Kicks a user."

			check { anyGuild() }
			check { hasPermission(Permission.KickMembers) }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild?.getChannel(config.modActionLog) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				// Clarify the user isn't a bot or a moderator
				isBotOrModerator(userArg, "kick") ?: return@action

				// DM the user about it before the kick
				val dm = userDMEmbed(
					userArg,
					"You have been kicked from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}",
					null
				)

				try {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // Remove timeout if they had a timeout when kicked
				} catch (e: EntityNotFoundException) {
					logger.info("Unable to find user! Skipping timeout removal")
				}

				// Run the kick task
				guild?.kick(userArg.id, arguments.reason)

				respond {
					content = "Kicked User"
				}

				actionLog.createEmbed {
					color = DISCORD_BLACK
					title = "Kicked User"
					description = "${userArg.mention} has been kicked"

					baseModerationEmbed(arguments.reason, userArg, user)
					dmNotificationStatusEmbedField(dm)
				}
			}
		}
	}

	inner class KickArgs : Arguments() {
		/** The user to kick. */
		val userArgument by user {
			name = "kickUser"
			description = "Person to kick"
		}

		/** The reason for the kick. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the Kick"
			defaultValue = "No Reason Provided"
		}
	}

	inner class BanArgs : Arguments() {
		/** The user to ban. */
		val userArgument by user {
			name = "banUser"
			description = "Person to ban"
		}

		/** The number of days worth of messages to delete. */
		val messages by int {
			name = "messages"
			description = "Messages"
		}

		/** The reason for the ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No Reason Provided"
		}
	}

	inner class UnbanArgs : Arguments() {
		/** The ID of the user to unban. */
		val userArgument by user {
			name = "unbanUserId"
			description = "Person Unbanned"
		}
	}

	inner class SoftBanArgs : Arguments() {
		/** The user to soft-ban. */
		val userArgument by user {
			name = "softBanUser"
			description = "Person to Soft ban"
		}

		/** The number of days worth of messages to delete, defaults to 3 days. */
		val messages by defaultingInt {
			name = "messages"
			description = "Messages"
			defaultValue = 3
		}

		/** The reason for the soft-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No Reason Provided"
		}
	}
}
