package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.utils.DatabaseHelper
import kotlin.time.Duration

/** The class that contains the reminding functions in the bot. */
class RemindMe : Extension() {
	override val name = "remind-me"

	/** The timer for checking reminders. */
	private val scheduler = Scheduler()

	/** The task to attach the [scheduler] to. */
	private lateinit var task: Task

	override suspend fun setup() {
		/** Set the task to run every 60 seconds. */
		task = scheduler.schedule(60, pollingSeconds = 60, repeat = true, callback = ::postReminders)

		/**
		 * The command for reminders
		 *
		 * @since 3.3.2
		 * @author NoComment1105
		 */
		publicSlashCommand(::RemindArgs) {
			name = "remind"
			description = "Remind me after a certain amount of time"

			check {
				anyGuild()
			}

			action {
				val setTime = Clock.System.now()
				val remindTime = Clock.System.now().plus(arguments.time, TimeZone.UTC)
				val duration = arguments.time

				val response = respond {
					content = "Reminder set!\nI will remind you ${remindTime.toDiscord(TimestampType.RelativeTime)} " +
							"at ${remindTime.toDiscord(TimestampType.ShortTime)}. That's `${
								Duration.parse(duration.toString())}` after this message was sent."
				}

				DatabaseHelper.setReminder(
					setTime,
					guild!!.id,
					user.id,
					channel.id,
					remindTime,
					response.message.getJumpUrl(),
					arguments.customMessage
				)
			}
		}
	}

	/**
	 * Sends reminders if the time for the reminder to be sent has passed.
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	private suspend fun postReminders() {
		val reminders = DatabaseHelper.getReminders()

		reminders.forEach {
			if (it.remindTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0) {
				val channel = kord.getGuild(it.guildId)!!.getChannel(it.channelId) as GuildMessageChannelBehavior
				if (it.customMessage.isNullOrEmpty()) {
					channel.createMessage {
						content =
							"Reminder for <@${it.userId}> set ${it.initialSetTime.toDiscord(TimestampType.RelativeTime)}" +
									" at ${it.initialSetTime.toDiscord(TimestampType.ShortDateTime)}"
					}.edit {
						components {
							linkButton {
								label = "Jump to message"
								url = it.originalMessageUrl
							}
						}
					}
				} else {
					channel.createMessage {
						content =
							"Reminder for <@${it.userId}> set ${it.initialSetTime.toDiscord(TimestampType.RelativeTime)} " +
									"at ${it.initialSetTime.toDiscord(TimestampType.ShortDateTime)}\n> ${it.customMessage}"
					}.edit {
						components {
							linkButton {
								label = "Jump to message"
								url = it.originalMessageUrl
							}
						}
					}
				}

				// Remove the old reminder from the database
				DatabaseHelper.removeReminder(it.initialSetTime, it.guildId, it.userId)
			}
		}
	}

	inner class RemindArgs : Arguments() {
		/** The time until the user should be reminded. */
		val time by coalescingDuration {
			name = "time"
			description = "How long until reminding? Format: years(y) days(d) hours(h) minutes(m) seconds(s) XyXdXhXmXs"
		}

		/** A custom message the user may want to provide. */
		val customMessage by optionalString {
			name = "customMessage"
			description = "Add a custom message to your reminder"
		}
	}
}
