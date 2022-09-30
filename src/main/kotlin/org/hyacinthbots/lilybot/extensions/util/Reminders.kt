package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingOptionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task

class Reminders : Extension() {
	override val name = "reminders"

	/** The scheduler that will track the time for reminder posting. */
	private val reminderScheduler = Scheduler()

	/** The task that will run the [reminderScheduler]. */
	private lateinit var reminderTask: Task

	override suspend fun setup() {
		reminderTask = reminderScheduler.schedule(30, repeat = true, callback = ::postReminders)

		/*
		Reminder Set
		 */
		ephemeralSlashCommand(::ReminderSetArgs) { }

		/*
		Reminder List
		 */
		ephemeralSlashCommand { }

		/*
		Reminder Remove
		 */
		ephemeralSlashCommand(::ReminderRemoveArgs) { }

		/*
		Reminder Removal all
		 */
		ephemeralSlashCommand(::ReminderRemoveAllArgs) { }

		/*
		Reminder Mod List
		 */
		ephemeralSlashCommand(::ReminderModListArgs) { }

		/*
		Reminder Mod Remove
		 */
		ephemeralSlashCommand(::ReminderModRemoveArgs) { }

		/*
		Reminder Mod Remove All
		 */
		ephemeralSlashCommand(::ReminderModRemoveAllArgs) { }
	}

	/**
	 * Checks the database to see if reminders need posting and posts them if necessary.
	 *
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	@Suppress("EmptyFunctionBlock")
	private suspend fun postReminders() {
	}

	inner class ReminderSetArgs : Arguments() {
		/** The time until the reminding should happen. */
		val time by coalescingDuration {
			name = "time"
			description = "How long until reminding? Format: 1d12h30m / 3d / 26m30s"
		}

		/** An optional message to attach to the reminder. */
		val customMessage by optionalString {
			name = "custom-message"
			description = "A message to attach to your reminder"
		}

		/** Whether to repeat the reminder or have it run once. */
		val repeating by optionalBoolean {
			name = "repeat"
			description = "Whether to repeat the reminder or not"
		}

		/** The interval for the repeating reminder to run at. */
		val repeatingInterval by coalescingOptionalDuration {
			name = "repeat-interval"
			description = "The interval to repeat the reminder at. Format: 1d / 1h / 5d"
		}
	}

	inner class ReminderRemoveArgs : Arguments() {
		/** The number of the reminder to remove. */
		val reminder by int {
			name = "reminder-number"
			description = "The number of the reminder to remove. Use '/reminder list' to get this"
		}
	}

	inner class ReminderRemoveAllArgs : Arguments() {
		/** The type of reminder to remove. */
		val type by stringChoice {
			name = "reminder-type"
			description = "The type of reminder to remove"
			choices = mutableMapOf(
				"repeating" to "repeating",
				"non-repeating" to "non-repeating",
				"all" to "all"
			)
		}
	}

	inner class ReminderModListArgs : Arguments() {
		/** The user whose reminders are being viewed. */
		val user by user {
			name = "user"
			description = "The user to view reminders for"
		}
	}

	inner class ReminderModRemoveArgs : Arguments() {
		/** The user whose reminders need removing. */
		val user by user {
			name = "user"
			description = "The user to remove the reminder for"
		}

		/** The number of the reminder to remove. */
		val reminder by int {
			name = "reminder-number"
			description = "The number of the reminder to remove. Use '/reminder mod-list' to get this"
		}
	}

	inner class ReminderModRemoveAllArgs : Arguments() {
		/** The user whose reminders need removing. */
		val user by user {
			name = "user"
			description = "The user to remove the reminders for"
		}

		/** The type of reminder to remove. */
		val type by stringChoice {
			name = "reminder-type"
			description = "The type of reminder to remove"
			choices = mutableMapOf(
				"repeating" to "repeating",
				"non-repeating" to "non-repeating",
				"all" to "all"
			)
		}
	}
}
