@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
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
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.DatabaseHelper.getWarn
import net.irisshaders.lilybot.utils.baseModerationEmbed
import net.irisshaders.lilybot.utils.dmNotificationStatusEmbedField
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

				isBotOrModerator(userArg, "warn") ?: return@action

				DatabaseHelper.setWarn(userArg.id, guild!!.id, false)
				val newStrikes = getWarn(userArg.id, guild!!.id)

				respond {
					content = "Warned user."
				}

				var dm: Message? = null
				// Check the amount of points before running sanctions and dming the user
				if (newStrikes == 1) {
					dm = userDMEmbed(
						userArg,
						"First warning in ${guild?.fetchGuild()?.name}",
						"**Reason:** ${arguments.reason}\n\n" +
						"No moderation action has been taken. Please consider your actions carefully.\n\n" +
						"For more information about the warn system, please see [this document]" +
						"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)",
						DISCORD_BLACK
					)
				} else if (newStrikes == 2) {
					dm = userDMEmbed(
						userArg,
						"Second warning and timeout in ${guild?.fetchGuild()?.name}",
						"**Reason:** ${arguments.reason}\n\n" +
								"You have been timed out for 3 hours. Please consider your actions carefully.\n\n" +
								"For more information about the warn system, please see [this document]" +
								"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)",
						DISCORD_RED
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT3H"))
					}

					responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 3 hours due to $newStrikes warn " +
								"strikes\n${userArg.id} (${userArg.tag})" +
								"Reason: ${arguments.reason}\n\n",
						DISCORD_BLACK,
						user.asUser()
					)
				} else if (newStrikes == 3) {
					dm = userDMEmbed(
						userArg,
						"Third warning and timeout in ${guild!!.fetchGuild().name}",
						"**Reason:** ${arguments.reason}\n\n" +
								"You have been timed out for 12 hours. Please consider your actions carefully.\n\n" +
								"For more information about the warn system, please see [this document]" +
								"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)",
						DISCORD_RED
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT12H"))
					}

					responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 12 hours due to $newStrikes warn " +
								"strikes\n${userArg.id} (${userArg.tag})" +
								"Reason: ${arguments.reason}\n\n",
						DISCORD_BLACK,
						user.asUser()
					)
				} else if (newStrikes > 3) {
					dm = userDMEmbed(
						userArg,
						"Warning number $newStrikes and timeout in ${guild!!.fetchGuild().name}",
						"**Reason:** ${arguments.reason}\n\n" +
								"You have been timed out for 3 days. Please consider your actions carefully.\n\n" +
								"For more information about the warn system, please see [this document]" +
								"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md#L89)",
						DISCORD_RED
					)

					guild?.getMember(userArg.id)?.edit {
						timeoutUntil = Clock.System.now().plus(Duration.parse("PT72H"))
					}

					responseEmbedInChannel(
						actionLog,
						"Timeout",
						"${userArg.mention} has been timed-out for 3 days due to $newStrikes warn " +
								"strike\n${userArg.id} (${userArg.tag})\nIt might be time to consider other action." +
								"Reason: ${arguments.reason}\n\n",
						DISCORD_BLACK,
						user.asUser()
					)
				}

				actionLog.createEmbed {
					title = "Warning"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()

					baseModerationEmbed(arguments.reason, userArg, user)
					field {
						name = "Total Strikes:"
						value = newStrikes.toString()
						inline = false
					}
					dmNotificationStatusEmbedField(dm)
				}
			}
		}

		/**
		 * Remove warn command
		 *
		 * @author NoComment1105
		 */
		ephemeralSlashCommand(::RemoveWarnArgs) {
			name = "remove-warn"
			description = "Remove a warning strike from a user"

			check { hasPermission(Permission.ModerateMembers) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument

				val targetUser = guild?.getMember(userArg.id)
				if (getWarn(targetUser!!.id, guild!!.id) == 0) {
					respond {
						content = "This user does not have any warning strikes!"
					}
					return@action
				}

				DatabaseHelper.setWarn(userArg.id, guild!!.id, true)
				val newStrikes = getWarn(userArg.id, guild!!.id)

				respond {
					content = "Removed strike from user"
				}

				val dm = userDMEmbed(
					userArg,
					"Warn strike removal in ${guild?.fetchGuild()?.name}",
					"You have had a warn strike removed. You have $newStrikes strikes",
					DISCORD_GREEN
				)

				actionLog.createEmbed {
					title = "Warning Removal"
					color = DISCORD_BLACK
					timestamp = Clock.System.now()

					baseModerationEmbed(null, userArg, user)
					field {
						name = "Total Strikes:"
						value = newStrikes.toString()
						inline = false
					}
					dmNotificationStatusEmbedField(dm)
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

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val userArg = arguments.userArgument
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.UTC)

				isBotOrModerator(userArg, "timeout") ?: return@action

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

					baseModerationEmbed(arguments.reason, userArg, user)
					field {
						name = "Duration:"
						value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
							.replace("PT", "") + ")"
						inline = false
					}
					dmNotificationStatusEmbedField(dm)
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
		val reason by defaultingString {
			name = "reason"
			description = "Reason for Warn"
			defaultValue = "No Reason Provided"
		}
	}

	inner class RemoveWarnArgs : Arguments() {
		val userArgument by user {
			name = "warnUser"
			description = "Person to remove warn from"
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
