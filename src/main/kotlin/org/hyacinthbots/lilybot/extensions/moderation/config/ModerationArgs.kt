package org.hyacinthbots.lilybot.extensions.moderation.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.boolean
import dev.kordex.core.commands.converters.impl.coalescingOptionalDuration
import dev.kordex.core.commands.converters.impl.optionalBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.commands.converters.impl.optionalRole
import dev.kordex.core.commands.converters.impl.optionalString

class ModerationArgs : Arguments() {
	val enabled by boolean {
		name = "enable-moderation"
		description = "Whether to enable the moderation system"
	}

	val moderatorRole by optionalRole {
		name = "moderator-role"
		description = "The role of your moderators, used for pinging in message logs."
	}

	val modActionLog by optionalChannel {
		name = "action-log"
		description = "The channel used to store moderator actions."
	}

	val quickTimeoutLength by coalescingOptionalDuration {
		name = "quick-timeout-length"
		description = "The length of timeouts to use for quick timeouts"
	}

	val warnAutoPunishments by optionalBoolean {
		name = "warn-auto-punishments"
		description = "Whether to automatically punish users for reach a certain threshold on warns"
	}

	val logPublicly by optionalBoolean {
		name = "log-publicly"
		description = "Whether to log moderation publicly or not."
	}

	val banDmMessage by optionalString {
		name = "ban-dm-message"
		description = "A custom message to send to users when they are banned."
	}

	val autoInviteModeratorRole by optionalBoolean {
		name = "auto-invite-moderator-role"
		description = "Silently ping moderators to invite them to new threads."
	}
}
