package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.utils.DatabaseHelper

class RemindMe : Extension() {
	override val name = "remind-me"

	override suspend fun setup() {
		ephemeralSlashCommand(::RemindArgs) {
			name = "remind"
			description = "Remind me after a certain amount of time"

			check {
				anyGuild()
			}

			action {
				val setTime = Clock.System.now()
				val duration = Clock.System.now().plus(arguments.time, TimeZone.UTC)

				DatabaseHelper.setReminder(
					setTime,
					guild!!.id,
					channel.id,
					user.id,
					duration,
					arguments.customMessage
				)

				respond {
					content = "Reminder set!\nI will remind you in " + duration.toDiscord(TimestampType.RelativeTime)
				}
			}
		}
	}

	inner class RemindArgs : Arguments() {
		val time by coalescingDuration {
			name = "time"
			description = "How long until you need reminding?"
		}
		val customMessage by optionalString {
			name = "customMessage"
			description = "Add a custom message to your reminder"
		}
	}
}
