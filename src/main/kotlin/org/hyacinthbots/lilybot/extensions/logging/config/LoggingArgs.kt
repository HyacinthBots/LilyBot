package org.hyacinthbots.lilybot.extensions.logging.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.boolean
import dev.kordex.core.commands.converters.impl.optionalChannel

class LoggingArgs : Arguments() {
	val enableMessageDeleteLogs by boolean {
		name = "enable-delete-logs"
		description = "Enable logging of message deletions"
	}

	val enableMessageEditLogs by boolean {
		name = "enable-edit-logs"
		description = "Enable logging of message edits"
	}

	val enableMemberLogging by boolean {
		name = "enable-member-logging"
		description = "Enable logging of members joining and leaving the guild"
	}

	val enablePublicMemberLogging by boolean {
		name = "enable-public-member-logging"
		description =
			"Enable logging of members joining and leaving the guild with a public message and ping if enabled"
	}

	val messageLogs by optionalChannel {
		name = "message-logs"
		description = "The channel for logging message deletions"
	}

	val memberLog by optionalChannel {
		name = "member-log"
		description = "The channel for logging members joining and leaving the guild"
	}

	val publicMemberLog by optionalChannel {
		name = "public-member-log"
		description = "The channel for the public logging of members joining and leaving the guild"
	}
}
