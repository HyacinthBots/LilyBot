package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalAttachment
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import io.github.nocomment1105.discordmoderationactions.builder.ban
import io.github.nocomment1105.discordmoderationactions.builder.kick
import io.github.nocomment1105.discordmoderationactions.builder.softban
import io.github.nocomment1105.discordmoderationactions.builder.timeout
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.baseModerationEmbed
import org.hyacinthbots.lilybot.utils.dmNotificationStatusEmbedField
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.isBotOrModerator

class ModerationCommands : Extension() {
	override val name = "moderation"

	override suspend fun setup() {
		ephemeralMessageCommand {
			name = "moderate"
			locking = true

			action {
				respond {
					content = "How would you like to moderate this message?"
					components {
						ephemeralSelectMenu {
							placeholder = "Select action..."
							maximumChoices = 1 // Prevent selecting multiple options at once

							option(label = "Ban user", ModerationActions.BAN.name) {
								description = "Ban the user that sent this message"
							}

							option("Soft-ban", ModerationActions.SOFT_BAN.name) {
								description = "Soft-ban the user that sent this message"
							}

							option("Kick", ModerationActions.KICK.name) {
								description = "Kick the user that sent this message"
							}

							option("Timeout", ModerationActions.TIMEOUT.name) {
								description = "Timeout the user that sent this message"
							}

							option("Warn", ModerationActions.WARN.name) {
								description = "Warn the user that sent this message"
							}

							action SelectMenu@{
								val option = event.interaction.values.firstOrNull() // Get the first because there can only be one
								if (option == null) { respond { content = "You did not select an option!" } }

								when (option) {
									ModerationActions.BAN.name -> {
										// TODO ban function and functions in said ban function that do the other stuff
									}

									ModerationActions.SOFT_BAN.name -> {
									}

									ModerationActions.KICK.name -> {
									}

									ModerationActions.TIMEOUT.name -> {
									}

									ModerationActions.WARN.name -> {
									}
								}
							}
						}
					}
				}
			}
		}

		ephemeralSlashCommand(::BanArgs) {
			name = "ban"
			description = "Bans a user."

			check {
				// TODO make a function for all the general checks?
			}

			action {
				// TODO ban function and functions in said ban function that do the other stuff

				isBotOrModerator(arguments.userArgument, "ban") ?: return@action

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages > 7 || arguments.messages < 0) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				ban(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					sendActionLog = true
					sendDm = arguments.dm
					removeTimeout = true
					deleteMessageDuration = DateTimePeriod(days = arguments.messages)
					this.loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Banned a user"
						description = "${arguments.userArgument.mention} has been banned!"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Days of messages deleted:"
							value = arguments.messages.toString()
							inline = false
						}
					}

					publicActionEmbed {
						title = "Banned a user"
						description = "${arguments.userArgument.mention} has been banned!"
						color = DISCORD_BLACK
					}

					dmEmbed {
						title = "You have been banned from ${guild?.asGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}"
					}
				}

				respond {
					content = "Banned a user"
				}
			}
		}

		ephemeralSlashCommand(::SoftBanArgs) {
			name = "soft-ban"
			description = "Soft-bans a user."

			check { }

			action {
				isBotOrModerator(arguments.userArgument, "ban") ?: return@action

				// The discord limit for deleting days of messages in a ban is 7, so we should catch invalid inputs.
				if (arguments.messages != null && (arguments.messages!! > 7 || arguments.messages!! < 0)) {
					respond { content = "Invalid `messages` parameter! This number must be between 0 and 7!" }
					return@action
				}

				softban(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					sendActionLog = true
					sendDm = arguments.dm
					removeTimeout = true
					if (arguments.messages != null) deleteMessageDuration = DateTimePeriod(days = arguments.messages!!)
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Soft-Banned a user"
						description = "${arguments.userArgument.mention} has been soft-banned!"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Days of messages deleted"
							value = arguments.messages.toString()
							inline = false
						}
					}

					publicActionEmbed {
						title = "Soft-Banned a user"
						description = "${arguments.userArgument.mention} has been soft-banned!"
					}

					dmEmbed {
						title = "You have been soft-banned from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}\n\n" +
								"You are free to rejoin without the need to be unbanned"
					}
				}

				respond {
					content = "Soft-banned user"
				}
			}
		}

		ephemeralSlashCommand(::UnbanArgs) {
			name = "unban"
			description = "Unbans a user."

			check { }

			action { }
		}

		ephemeralSlashCommand(::KickArgs) {
			name = "kick"
			description = "Kicks a user."

			check { }

			action {
				isBotOrModerator(arguments.userArgument, "kick") ?: return@action

				kick(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					sendActionLog = true
					sendDm = arguments.dm
					removeTimeout = true
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)

					actionEmbed {
						title = "Kicked a user"
						description = "${arguments.userArgument.mention} has been kicked!"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
					}

					publicActionEmbed {
						title = "Kicked a user"
						description = "${arguments.userArgument.mention} has been kicked!"
					}

					dmEmbed {
						title = "You have been kicked from ${guild?.fetchGuild()?.name}"
						description = "**Reason:**\n${arguments.reason}"
					}
				}
			}
		}

		ephemeralSlashCommand(::ClearArgs) {
			name = "clear"
			description = "Clears messages from a channel."

			check { }

			action { }
		}

		ephemeralSlashCommand(::TimeoutArgs) {
			name = "timeout"
			description = "Times out a user."

			check { }

			action {
				val duration = Clock.System.now().plus(arguments.duration, TimeZone.UTC)

				isBotOrModerator(arguments.userArgument, "timeout") ?: return@action

				timeout(arguments.userArgument) {
					reason = arguments.reason
					logPublicly = ModerationConfigCollection().getConfig(guild!!.id)?.publicLogging
					timeoutDuration = duration
					sendDm = arguments.dm
					loggingChannel = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, guild!!)
					actionEmbed {
						title = "Timeout"
						image = arguments.image?.url
						baseModerationEmbed(arguments.reason, arguments.userArgument, user)
						dmNotificationStatusEmbedField(dmResult)
						timestamp = Clock.System.now()
						field {
							name = "Duration:"
							value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
								.replace("PT", "") + ")"
							inline = false
						}
					}
					publicActionEmbed {
						title = "Timeout"
						description = "${arguments.userArgument.mention} was timed out by a moderator"
						color = DISCORD_BLACK
						field {
							name = "Duration:"
							value = duration.toDiscord(TimestampType.Default) + " (" + arguments.duration.toString()
								.replace("PT", "") + ")"
							inline = false
						}
					}
					dmEmbed {
						title = "You have been timed out in ${guild?.fetchGuild()?.name}"
						description = "**Duration:**\n${
							duration.toDiscord(TimestampType.Default) + "(" + arguments.duration.toString()
								.replace("PT", "") + ")"
						}\n**Reason:**\n${arguments.reason}"
					}
				}
			}
		}

		ephemeralSlashCommand(::RemoveTimeoutArgs) {
			name = "remove-timeout"
			description = "Removes a timeout from a user"

			check { }

			action { }
		}

		ephemeralSlashCommand(::WarnArgs) {
			name = "warn"
			description = "Warns a user."

			check { }

			action { }
		}

		ephemeralSlashCommand(::RemoveWarnArgs) {
			name = "remove-warn"
			description = "Removes a user's warnings"

			check { }

			action { }
		}
	}

	inner class BanArgs : Arguments() {
		/** The user to ban. */
		val userArgument by user {
			name = "user"
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
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the ban. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class SoftBanArgs : Arguments() {
		/** The user to soft-ban. */
		val userArgument by user {
			name = "user"
			description = "Person to Soft ban"
		}

		/** The number of days worth of messages to delete, defaults to 3 days. */
		val messages by optionalInt {
			name = "messages"
			description = "Messages"
		}

		/** The reason for the soft-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the ban"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the soft-ban. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class UnbanArgs : Arguments() {
		/** The ID of the user to unban. */
		val userArgument by user {
			name = "user"
			description = "Person to un-ban"
		}

		/** The reason for the un-ban. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the un-ban"
			defaultValue = "No reason provided"
		}
	}

	inner class KickArgs : Arguments() {
		/** The user to kick. */
		val userArgument by user {
			name = "user"
			description = "Person to kick"
		}

		/** The reason for the kick. */
		val reason by defaultingString {
			name = "reason"
			description = "The reason for the Kick"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class ClearArgs : Arguments() {
		/** The number of messages the user wants to remove. */
		val messages by int {
			name = "messages"
			description = "Number of messages to delete"
		}
	}

	inner class TimeoutArgs : Arguments() {
		/** The requested user to timeout. */
		val userArgument by user {
			name = "user"
			description = "Person to timeout"
		}

		/** The time the timeout should last for. */
		val duration by coalescingDefaultingDuration {
			name = "duration"
			description = "Duration of timeout"
			defaultValue = DateTimePeriod(0, 0, 0, 6, 0, 0, 0)
		}

		/** The reason for the timeout. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for timeout"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class RemoveTimeoutArgs : Arguments() {
		/** The requested user to remove the timeout from. */
		val userArgument by user {
			name = "user"
			description = "Person to remove timeout from"
		}
	}

	inner class WarnArgs : Arguments() {
		/** The requested user to warn. */
		val userArgument by user {
			name = "user"
			description = "Person to warn"
		}

		/** The reason for the warning. */
		val reason by defaultingString {
			name = "reason"
			description = "Reason for warn"
			defaultValue = "No reason provided"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}

		/** An image that the user wishes to provide for context to the kick. */
		val image by optionalAttachment {
			name = "image"
			description = "An image you'd like to provide as extra context for the action"
		}
	}

	inner class RemoveWarnArgs : Arguments() {
		/** The requested user to remove the warning from. */
		val userArgument by user {
			name = "user"
			description = "Person to remove warn from"
		}

		/** Whether to DM the user or not. */
		val dm by defaultingBoolean {
			name = "dm"
			description = "Whether to send a direct message to the user about the warn"
			defaultValue = true
		}
	}
}
