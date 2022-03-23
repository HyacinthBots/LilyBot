@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import mu.KotlinLogging
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.getConfigPrivateResponse
import net.irisshaders.lilybot.utils.isBotOrModerator
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import net.irisshaders.lilybot.utils.userDMEmbed
import java.lang.Integer.min
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

class TemporaryModeration : Extension() {
	override val name = "temporary-moderation"

	override suspend fun setup() {
		val logger = KotlinLogging.logger { }

		/**
		 * Clear Command
		 * @author IMS212
		 */
		ephemeralSlashCommand(::ClearArgs) {
			name = "clear"
			description = "Clears messages."

			// Require message managing permissions to run this command
			check { hasPermission(Permission.ManageMessages) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val messageAmount = arguments.messages
				val textChannel = channel as GuildMessageChannelBehavior

				// Get the specified amount of messages into an array list of Snowflakes and delete them

				val messages = channel.withStrategy(EntitySupplyStrategy.rest).getMessagesBefore(
					Snowflake.max, min(messageAmount, 100)).map { it.id }.toList()

				textChannel.bulkDelete(messages)

				respond {
					content = "Messages cleared"
				}

				responseEmbedInChannel(
					actionLog,
					"$messageAmount messages have been cleared.",
					"Action occurred in ${textChannel.mention}",
					DISCORD_BLACK,
					user.asUser()
				)
			}
		}

		/**
		 * Warn Command
		 * @author chalkyjeans
		 * @author Miss-Corruption
		 * @author NoComment1105
		 */
		ephemeralSlashCommand(::WarnArgs) {
			name = "warn"
			description = "Warn a member for any infractions."

			// Require the ModerateMembers permission
			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val userArg = arguments.userArgument
				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior

				isBotOrModerator() ?: return@action

				val oldPoints = DatabaseHelper.getWarn(userArg.id, guild!!.id)
				val newPoints = oldPoints.plus(arguments.warnPoints)
				DatabaseHelper.setWarn(userArg.id, guild!!.id, newPoints)
				val databasePoints = DatabaseHelper.getWarn(userArg.id, guild!!.id)

				// DM the user about the warning
				val dm = userDMEmbed(
					userArg,
					"You have been warned in ${guild?.fetchGuild()?.name}",
					"You were given ${arguments.warnPoints} points\n" +
							"Your total is now $databasePoints\n\n**Reason:**\n${arguments.reason}",
					null
				)

				// Check the points amount, before running sanctions
				if (databasePoints in (75..124)) {
					userDMEmbed(
						userArg,
						"You have been timed-out in ${guild!!.fetchGuild().name}",
						"You have accumulated too many warn points, and have hence been given a " +
								"3 hour timeout",
						DISCORD_BLACK
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT3H"))
					}

					responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 3 hours due to point " +
								"accumulation\n${userArg.id} (${userArg.tag})",
						DISCORD_BLACK,
						user.asUser()
					)
				} else if (databasePoints in (125..199)) {
					userDMEmbed(
						userArg,
						"You have been timed-out in ${guild!!.fetchGuild().name}",
						"You have accumulated too many warn points, and have hence been given " +
								"a 12 hour timeout",
						DISCORD_BLACK
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT12H"))
					}

					responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 12 hours due to point " +
								"accumulation\n${userArg.id} (${userArg.tag})",
						DISCORD_BLACK,
						user.asUser()
					)
				} else if (databasePoints >= 200) {
					guild?.getMember(userArg.id)
						?.edit { timeoutUntil = null } // Remove timeout in case they were timed out when banned
					userDMEmbed(
						userArg,
						"You have been banned from ${guild!!.fetchGuild().name}",
						"You have accumulated too many warn points, and have hence been banned",
						DISCORD_BLACK
					)

					guild?.ban(userArg.id, builder = {
						this.reason = "Banned due to point accumulation"
						this.deleteMessagesDays = 0
					})

					responseEmbedInChannel(
						actionLog,
						"User Banned!",
						"${userArg.mention} has been banned due to point accumulation\n${userArg.id} (${userArg.tag})",
						DISCORD_BLACK,
						user.asUser()
					)
				}

				respond {
					content = "Warned User"
				}

				actionLog.createEmbed {
					title = "Warning"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()

					field {
						name = "User:"
						value = "${userArg.tag} \n${userArg.id}"
						inline = false
					}
					field {
						name = "Total Points:"
						value = databasePoints.toString()
						inline = false
					}
					field {
						name = "Points added:"
						value = arguments.warnPoints.toString()
						inline = false
					}
					field {
						name = "Reason:"
						value = arguments.reason
						inline = false
					}
					field {
						name = "User notification"
						value =
							if (dm != null) {
								"User notified with a direct message"
							} else {
								"Failed to notify user with a direct message"
							}
						inline = false
					}
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}

		/**
		 * Timeout command
		 *
		 * @author NoComment1105
		 * @author IMS212
		 */
		ephemeralSlashCommand(::TimeoutArgs) {
			name = "timeout"
			description = "Timeout a user"

			// Requires Moderate Members permission
			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
				val moderatorRoleId = getConfigPrivateResponse("moderatorsPing") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.currentSystemDefault())

				try {
					// Get the users roles into a List of Snowflakes
					val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }
					// Fail if the user is a bot
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot timeout a bot!"
						}
						return@action
					// If the moderator ping role is in roles list, fail
					} else if (moderatorRoleId in roles) {
						respond {
							content = "You cannot timeout a moderator!"
						}
						return@action
					}
				// To catch any errors in checking
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks failed on `Timeout` due to error")
				}

				try {
					// Run the timeout task
					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = duration
					}
				} catch (e: KtorRequestException) {
					respond {
						content = "Sorry, I can't timeout this person! Try doing the timeout manually instead!"
					}
				}

				// Send the DM after the timeout task, in case Lily doesn't have required permissions
				// DM the user about it
				val dm = userDMEmbed(
					userArg,
					"You have been timed out in ${guild?.fetchGuild()?.name}",
					"**Duration:**\n${
						duration.toDiscord(TimestampType.Default) + "(" + arguments.duration.toString()
							.replace("PT", "") + ")"
					}\n**Reason:**\n${arguments.reason}",
					null
				)

				respond {
					content = "Timed out ${userArg.id}"
				}

				actionLog.createEmbed {
					title = "Timeout"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()

					field {
						name = "User:"
						value = "${userArg.tag} \n${userArg.id}"
						inline = false
					}
					field {
						name = "Duration:"
						value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
							.replace("PT", "") + ")"
						inline = false
					}
					field {
						name = "Reason:"
						value = arguments.reason
						inline = false
					}
					field {
						name = "User notification"
						value =
							if (dm != null) {
								"User notified with a direct message"
							} else {
								"Failed to notify user with a direct message "
							}
						inline = false
					}
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}

		/**
		 * Timeout removal command
		 *
		 * @author IMS212
		 */
		ephemeralSlashCommand(::RemoveArgs) {
			name = "remove-timeout"
			description = "Remove timeout on a user"

			// Requires Moderate Members permission
			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = DatabaseHelper.getConfig(guild!!.id, "modActionLog")
				if (actionLogId == null) {
					respond {
						content = "**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				// Set timeout to null, or no timeout
				guild?.getMember(userArg.id)?.edit {
					timeoutUntil = null
				}

				respond {
					content = "Removed timeout on ${userArg.id}"
				}

				actionLog.createEmbed {
					title = "Remove Timeout"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()

					field {
						name = "User:"
						value = "${userArg.tag} \n${userArg.id}"
						inline = false
					}
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}
	}

	inner class ClearArgs : Arguments() {
		val messages by int {
			name = "messages"
			description = "Number of messages to delete"
		}
	}

	inner class RemoveArgs : Arguments() {
		val userArgument by user {
			name = "unbanUserId"
			description = "Person Unbanned"
		}
	}

	inner class WarnArgs : Arguments() {
		val userArgument by user {
			name = "warnUser"
			description = "Person to Warn"
		}
		val warnPoints by defaultingInt {
			name = "points"
			description = "Amount of points to add"
			defaultValue = 10
		}
		val reason by defaultingString {
			name = "reason"
			description = "Reason for Warn"
			defaultValue = "No Reason Provided"
		}
	}

	inner class TimeoutArgs : Arguments() {
		val userArgument by user {
			name = "timeoutUser"
			description = "Person to timeout"
		}
		val duration by coalescingDefaultingDuration {
			name = "duration"
			description = "Duration of timeout"
			defaultValue = DateTimePeriod(0, 0, 0, 6, 0, 0, 0)
		}
		val reason by defaultingString {
			name = "reason"
			description = "Reason for timeout"
			defaultValue = "No reason provided"
		}
	}
}
