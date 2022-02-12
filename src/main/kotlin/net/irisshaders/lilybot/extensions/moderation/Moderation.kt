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

			check { hasPermission(Permission.ManageMessages) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				if (actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val messageAmount = arguments.messages
				val messageHolder = arrayListOf<Snowflake>()
				val textChannel = channel as GuildMessageChannelBehavior

				channel.getMessagesBefore(channel.messages.last().id, Integer.min(messageAmount, 100))
					.filterNotNull()
					.onEach {
						messageHolder.add(it.fetchMessage().id)
					}.catch {
						it.printStackTrace()
						println("error")
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

			check { hasPermission(Permission.BanMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot ban bot users!"
						}
						return@action
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot ban moderators!"
						}
						return@action
					}
				} catch (exception: Exception) {
					logger.warn("IsBot and moderator checks skipped on `Ban` due to error")
				}

				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been banned from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}",
					null
				)

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

			check { hasPermission(Permission.BanMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				if (actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				guild?.unban(userArg.id)

				respond {
					content = "Unbanned User"
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
			name = "softban"
			description = "Softbans a user"

			check { hasPermission(Permission.BanMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot soft-ban bot users!"
						}
						return@action
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot soft-ban moderators!"
						}
						return@action
					}
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks skipped on `Soft-Ban` due to error")
				}

				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been soft-banned from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}\n\nYou are free to rejoin without the need to be unbanned",
					null
				)

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

			check { hasPermission(Permission.KickMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot kick bot users!"
						}
						return@action
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot kick moderators!"
						}
					}
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks skipped on `Kick` due to error")
				}

				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been kicked from ${guild?.fetchGuild()?.name}",
					"**Reason:**\n${arguments.reason}",
					null
				)

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

			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val userArg = arguments.userArgument
				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				var databasePoints: Int? = null
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot warn bot users!"
						}
						return@action
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot warn moderators!"
						}
						return@action
					}
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks skipped on `Warn` due to error")
				}

				newSuspendedTransaction {
					DatabaseManager.Warn.insertIgnore {
						it[id] = userArg.id.toString()
						it[points] = 0
					}

					databasePoints = DatabaseManager.Warn.select {
						DatabaseManager.Warn.id eq userArg.id.toString()
					}.single()[DatabaseManager.Warn.points]

					DatabaseManager.Warn.update({ DatabaseManager.Warn.id eq userArg.id.toString() }) {
						it.update(points, points.plus(arguments.warnPoints))
					}

					databasePoints = DatabaseManager.Warn.select {
						DatabaseManager.Warn.id eq userArg.id.toString()
					}.single()[DatabaseManager.Warn.points]
				}

				if (databasePoints!! in (50..99)) {
					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT3H"))
					}
				} else if (databasePoints!! in (100..149)) {
					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT12H"))
					}
				} else if (databasePoints!! >= 150) {
					guild?.ban(userArg.id, builder = {
						this.reason = "Banned due to point accumulation"
						this.deleteMessagesDays = 0
					})
				}

				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been warned in ${guild?.fetchGuild()?.name}",
					"You were given ${arguments.warnPoints} points\nYour total is now $databasePoints\n\n**Reason:**\n${arguments.reason}",
					null
				)

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

			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				val moderators = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.moderatorsPing)
				if (moderators.equals("NoSuchElementException") || actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.currentSystemDefault())
				val roles = userArg.asMember(guild!!.id).roles.toList().map { it.id }

				try {
					if (guild?.getMember(userArg.id)?.isBot == true) {
						respond {
							content = "You cannot timeout a bot!"
						}
						return@action
					} else if (Snowflake(moderators!!) in roles) {
						respond {
							content = "You cannot timeout a moderator!"
						}
						return@action
					}
				} catch (exception: Exception) {
					logger.warn("IsBot and Moderator checks failed on `Timeout` due to error")
				}

				val dm = ResponseHelper.userDMEmbed(
					userArg,
					"You have been timed out in ${guild?.fetchGuild()?.name}",
					"**Duration:**\n${
						duration.toDiscord(TimestampType.Default) + "(" + arguments.duration.toString()
							.replace("PT", "") + ")"
					}\n**Reason:**\n${arguments.reason}",
					null
				)

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

			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = DatabaseHelper.selectInConfig(guild!!.id, DatabaseManager.Config.modActionLog)
				if (actionLogId.equals("NoSuchElementException")) {
					respond {
						content =
							"**Error:** Unable to access config for this guild! Please inform a member of staff!"
					}
					return@action
				}

				val actionLog = guild?.getChannel(Snowflake(actionLogId!!)) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

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
