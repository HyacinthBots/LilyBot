package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDefaultingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalAttachment
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import kotlinx.datetime.DateTimePeriod

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
			}
		}

		ephemeralSlashCommand(::SoftBanArgs) {
			name = "soft-ban"
			description = "Soft-bans a user."

			check { }

			action { }
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

			action { }
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

			action { }
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
		val messages by defaultingInt {
			name = "messages"
			description = "Messages"
			defaultValue = 3
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
