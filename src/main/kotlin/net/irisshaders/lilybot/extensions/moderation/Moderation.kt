@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import mu.KotlinLogging
import net.irisshaders.lilybot.database.DatabaseHelper
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.ResponseHelper
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.update
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@Suppress("DuplicatedCode")
class Moderation : Extension() {
	override val name = "moderation"

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
				// Try to get the action log from the config, if none is set return a NoSuchElementException
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				if (actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val messageAmount = arguments.messages
				val messageHolder = arrayListOf<Snowflake>()
				val textChannel = channel as GuildMessageChannelBehavior

				// Get the specified amount of messages into an
				// Array List of Snowflakes, and delete them
				channel.getMessagesAround(channel.messages.last().id, Integer.min(messageAmount, 100))

				channel.getMessagesBefore(channel.messages.last().id, Integer.min(messageAmount, 100))
					.filterNotNull()
					.onEach {
						messageHolder.add(it.fetchMessage().id)
					}.catch {
						it.printStackTrace()
						logger.error("Error in the clear command")
					}.collect()

				textChannel.bulkDelete(messageHolder)

				respond {
					content = "Messages cleared"
				}

				ResponseHelper.responseEmbedInChannel(
					actionLog,
					"$messageAmount messages have been cleared.",
					"Action occurred in ${textChannel.mention}",
					DISCORD_BLACK,
					user.asUser()
				)
			}
		}


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
				// Try to get the action log and moderators from config. NoSuchElementException if it fails
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				// Get all the members roles into a List of snowflakes
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					// Check if the user is a bot and fail
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot ban bot users!"
						}
						return@action
					// If the moderators role is found in the roles list, fail
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot ban moderators!"
						}
						return@action
					}
				// In the case of any exceptions that crop up
				} catch (exception: Exception) {
					logger.warn("IsBot and moderator checks skipped on `Ban` due to error")
				}

				// DM the user before the ban task is run, to avoid error, null if fails
				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been banned from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}",
					null
				)

				guild?.getMember(userArg.id)?.edit { timeoutUntil = null } // remove timeout incase they were timedout when banned

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
					field {
						name = "User Notification:"
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
				// Try to get the action log from config, in case of NoSuchElementException, fail
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				if (actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
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

				ResponseHelper.responseEmbedInChannel(
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
				// Try to get the action log and moderators from config. NoSuchElementException if failure
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				// Gather users roles into a List of Snowflakes
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					// Check if the user is a bot and return
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot soft-ban bot users!"
						}
						return@action
					// Check if the moderator role is in the roles list and return
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot soft-ban moderators!"
						}
						return@action
					}
				// In case of any extra errors
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks skipped on `Soft-Ban` due to error")
				}

				// DM the user before the ban task is run
				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been soft-banned from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}\n\nYou are free to rejoin without the need to be unbanned",
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
					field {
						name = "User Notification:"
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
				// Try to get the action log and moderators from config. NoSuchElementException if failure
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				// Get the users roles into a List of Snowflake
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					// If the user is a bot, fail
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot kick bot users!"
						}
						return@action
					// If the moderators role is in the roles list, return
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot kick moderators!"
						}
					}
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks skipped on `Kick` due to error")
				}

				// DM the user about it before the kick
				val dm = ResponseHelper.userDMEmbed(
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
					field {
						name = "User Notification:"
						value =
							if (dm != null) {
								"User notified with a direct message"
							} else {
								"Failed to notify user with a direct message"
							}
						inline = false
					}
					footer {
						text = "Requested By ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
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
				// Try to get the action log and moderators from config. NoSuchElementException if failure
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val userArg = arguments.userArgument
				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				var databasePoints: Int? = null
				// Get the users roles into a List of Snowflakes
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					// If the user is a bot, return
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot warn bot users!"
						}
						return@action
					// If the moderators role is in roles, return
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot warn moderators!"
						}
						return@action
					}
				// Just to catch any errors in the checks
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks skipped on `Warn` due to error")
				}

				newSuspendedTransaction {
					// Insert the user into the database with 0 points. If they are not already in the database
					DatabaseManager.Warn.insertIgnore {
						it[id] = userArg.id.toString()
						it[points] = 0
					}

					// Select the users point count and assign it to a variable
					databasePoints = DatabaseManager.Warn.select {
						DatabaseManager.Warn.id eq userArg.id.toString()
					}.single()[DatabaseManager.Warn.points]

					// Update the point of a user with the specified amount
					DatabaseManager.Warn.update({ DatabaseManager.Warn.id eq userArg.id.toString() }) {
						it.update(points, points.plus(arguments.warnPoints))
					}

					// We'll get the user's updated points
					databasePoints = DatabaseManager.Warn.select {
						DatabaseManager.Warn.id eq userArg.id.toString()
					}.single()[DatabaseManager.Warn.points]
				}

				// DM the user about the warning
				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been warned in ${guild?.fetchGuild()?.name}",
					"You were given ${arguments.warnPoints} points\nYour total is now $databasePoints\n\n**Reason:**\n${arguments.reason}",
					null
				)

				// Check the points amount, before running sanctions
				if (databasePoints!! in (75..124)) {
					ResponseHelper.userDMEmbed(
						userArg,
						"You have been timed-out in ${guild!!.fetchGuild().name}",
						"You have accumulated too many warn points, and have hence been given a 3 hour timeout",
						DISCORD_BLACK
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT3H"))
					}

					ResponseHelper.responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 3 hours due to point accumulation\n${userArg.id} (${userArg.tag})",
						DISCORD_BLACK,
						user.asUser()
					)
				} else if (databasePoints!! in (125..199)) {
					ResponseHelper.userDMEmbed(
						userArg,
						"You have been timed-out in ${guild!!.fetchGuild().name}",
						"You have accumulated too many warn points, and have hence been given a 12 hour timeout",
						DISCORD_BLACK
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT12H"))
					}

					ResponseHelper.responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 12 hours due to point accumulation\n${userArg.id} (${userArg.tag})",
						DISCORD_BLACK,
						user.asUser()
					)
				} else if (databasePoints!! >= 200) {
					guild?.getMember(userArg.id)?.edit { timeoutUntil = null } // Remove timeout incase they were timed out when banned
					ResponseHelper.userDMEmbed(
						userArg,
						"You have been banned from ${guild!!.fetchGuild().name}",
						"You have accumulated too many warn points, and have hence been banned",
						DISCORD_BLACK
					)

					guild?.ban(userArg.id, builder = {
						this.reason = "Banned due to point accumulation"
						this.deleteMessagesDays = 0
					})

					ResponseHelper.responseEmbedInChannel(
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
		 * @author NoComment/IMS
		 */
		ephemeralSlashCommand(::TimeoutArgs) {
			name = "timeout"
			description = "Timeout a user"

			// Requires Moderate Members permission
			check { hasPermission(Permission.ModerateMembers) }

			action {
				// Try to get the action log and moderators from config. NoSuchElementException if failure
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.currentSystemDefault())
				// Get the users roles into a List of Snowflakes
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					// Fail if the user is a bot
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot timeout a bot!"
						}
						return@action
					// If the moderators role is in roles list, fail
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot timeout a moderator!"
						}
						return@action
					}
				// To catch any errors in checking
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks failed on `Timeout` due to error")
				}

				// DM the user about it
				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been timed out in ${guild?.fetchGuild()?.name}",
					"**Duration:**\n${
						duration.toDiscord(TimestampType.Default) + "(" + arguments.duration.toString()
							.replace("PT", "") + ")"
					}\n**Reason:**\n${arguments.reason}",
					null
				)

				// Run the timeout task
				guild?.getMember(userArg.id)?.edit {
					timeoutUntil = duration
				}

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
		 * @author IMS
		 */
		ephemeralSlashCommand(::UnbanArgs) {
			name = "remove-timeout"
			description = "Remove timeout on a user"

			// Requires Moderate Members permission
			check { hasPermission(Permission.ModerateMembers) }

			action {
				// Try to get the action log from config. NoSuchElementException if failure
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				if (actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Is your configuration set?"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
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
