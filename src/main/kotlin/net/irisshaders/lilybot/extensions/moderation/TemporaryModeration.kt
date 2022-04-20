package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
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
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.editRolePermission
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
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

			check { anyGuild() }
			check { hasPermission(Permission.ManageMessages) }

			action {
				val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action

				val actionLog = guild?.getChannel(actionLogId) as GuildMessageChannelBehavior
				val messageAmount = arguments.messages
				val textChannel = channel as GuildMessageChannelBehavior

				// Get the specified amount of messages into an array list of Snowflakes and delete them

				val messages = channel.withStrategy(EntitySupplyStrategy.rest).getMessagesBefore(
					Snowflake.max, min(messageAmount, 100)
				).map { it.id }.toList()

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

			check { anyGuild() }
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

			check { anyGuild() }
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

			check { anyGuild() }
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
		ephemeralSlashCommand(::RemoveTimeoutArgs) {
			name = "remove-timeout"
			description = "Remove timeout on a user"

			check { anyGuild() }
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

		/**
		 * Server and channel locking commands
		 *
		 * @author tempest15
		 */
		ephemeralSlashCommand {
			name = "lock"
			description = "The parent command for all locking commands"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			ephemeralSubCommand(::LockChannelArgs) {
				name = "channel"
				description = "Lock a channel so only mods can send messages"

				@Suppress("DuplicatedCode")
				action {
					val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
					val actionLogChannel = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior

					val channelArg = arguments.channel ?: event.interaction.getChannel()
					var channelParent: TextChannel? = null
					if (channelArg is TextChannelThread) {
						channelParent = channelArg.getParent()
					}
					val targetChannel = channelParent ?: channelArg.asChannelOf()

					val channelPerms = targetChannel.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms != null) {
						if (channelPerms.denied.contains(Permission.SendMessages)) {
							respond { content = "This channel is already locked!" }
							return@action
						}
					}

					targetChannel.createEmbed {
						title = "Channel Locked"
						description = "This channel has been locked by a moderator."
						color = DISCORD_RED
					}

					targetChannel.editRolePermission(guild!!.id) {
						denied += Permission.SendMessages
						denied += Permission.SendMessagesInThreads
						denied += Permission.AddReactions
						denied += Permission.UseApplicationCommands
					}

					actionLogChannel.createEmbed {
						title = "Channel Locked"
						description = "${targetChannel.mention} has been locked.\n\n**Reason:** ${arguments.reason}"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}

					respond { content = "${targetChannel.mention} has been locked." }
				}
			}

			ephemeralSubCommand(::LockServerArgs) {
				name = "server"
				description = "Lock the server so only mods can send messages"

				action {
					val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
					val actionLogChannel = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior
					val everyoneRole = guild!!.getRole(guild!!.id)

					if (!everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "The server is already locked!" }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.minus(Permission.SendMessages)
							.minus(Permission.SendMessagesInThreads)
							.minus(Permission.AddReactions)
							.minus(Permission.UseApplicationCommands)
					}

					actionLogChannel.createEmbed {
						title = "Server locked"
						description = "**Reason:** ${arguments.reason}"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_RED
					}

					respond { content = "Server locked." }
				}
			}
		}

		/**
		 * Server and channel unlocking commands
		 *
		 * @author tempest15
		 */
		ephemeralSlashCommand {
			name = "unlock"
			description = "The parent command for all unlocking commands"

			check { anyGuild() }
			check { hasPermission(Permission.ModerateMembers) }

			ephemeralSubCommand(::UnlockChannelArgs) {
				name = "channel"
				description = "Unlock a channel so everyone can send messages"

				@Suppress("DuplicatedCode")
				action{
					val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
					val actionLogChannel = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior

					val channelArg = arguments.channel ?: event.interaction.getChannel()
					var channelParent : TextChannel? = null
					if (channelArg is TextChannelThread) {
						channelParent = channelArg.getParent()
					}
					val targetChannel = channelParent ?: channelArg.asChannelOf()

					val channelPerms = targetChannel.getPermissionOverwritesForRole(guild!!.id)
					if (channelPerms == null) {
						respond { content = "This channel is not locked!" }
						return@action
					}
					if (!channelPerms.denied.contains(Permission.SendMessages)) {
						respond { content = "This channel is not locked!" }
						return@action
					}

					targetChannel.editRolePermission(guild!!.id) {
						allowed += Permission.SendMessages
						allowed += Permission.SendMessagesInThreads
						allowed += Permission.AddReactions
						allowed += Permission.UseApplicationCommands
					}

					targetChannel.createEmbed {
						title = "Channel Unlocked"
						description = "This channel has been unlocked by a moderator.\n" +
								"Please be aware of the rules when continuing discussion."
						color = DISCORD_GREEN
					}

					actionLogChannel.createEmbed {
						title = "Channel Unlocked"
						description = "${targetChannel.mention} has been unlocked."
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}

					respond { content = "${targetChannel.mention} has been unlocked." }
				}
			}

			ephemeralSubCommand {
				name = "server"
				description = "Unlock the server so everyone can send messages"

				action {
					val actionLogId = getConfigPrivateResponse("modActionLog") ?: return@action
					val actionLogChannel = guild!!.getChannel(actionLogId) as GuildMessageChannelBehavior
					val everyoneRole = guild!!.getRole(guild!!.id)

					if (everyoneRole.permissions.contains(Permission.SendMessages)) {
						respond { content = "The server isn't locked!" }
						return@action
					}

					everyoneRole.edit {
						permissions = everyoneRole.permissions
							.plus(Permission.SendMessages)
							.plus(Permission.SendMessagesInThreads)
							.plus(Permission.AddReactions)
							.plus(Permission.UseApplicationCommands)
					}

					actionLogChannel.createEmbed {
						title = "Server unlocked"
						footer {
							text = user.asUser().tag
							icon = user.asUser().avatar?.url
						}
						timestamp = Clock.System.now()
						color = DISCORD_GREEN
					}

					respond { content = "Server unlocked." }
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

	inner class TimeoutArgs : Arguments() {
		val userArgument by user {
			name = "user"
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

	inner class RemoveTimeoutArgs : Arguments() {
		val userArgument by user {
			name = "user"
			description = "Person to remove timeout from"
		}
	}

	inner class WarnArgs : Arguments() {
		val userArgument by user {
			name = "user"
			description = "Person to warn"
		}
		val reason by defaultingString {
			name = "reason"
			description = "Reason for warn"
			defaultValue = "No reason provided"
		}
	}

	inner class RemoveWarnArgs : Arguments() {
		val userArgument by user {
			name = "user"
			description = "Person to remove warn from"
		}
	}

	inner class LockChannelArgs : Arguments() {
		val channel by optionalChannel {
			name = "channel"
			description = "Channel to lock. Defaults to current channel"
		}

		val reason by defaultingString {
			name = "reason"
			description = "Reason for locking the channel"
			defaultValue = "No reason provided"
		}
	}

	inner class LockServerArgs : Arguments() {
		val reason by defaultingString {
			name = "reason"
			description = "Reason for locking the server"
			defaultValue = "No reason provided"
		}
	}

	inner class UnlockChannelArgs : Arguments() {
		val channel by optionalChannel {
			name = "channel"
			description = "Channel to unlock. Defaults to current channel"
		}
	}
}
