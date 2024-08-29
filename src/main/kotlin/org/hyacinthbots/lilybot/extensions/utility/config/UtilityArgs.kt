package org.hyacinthbots.lilybot.extensions.utility.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.defaultingBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel

class UtilityArgs : Arguments() {
	val utilityLogChannel by optionalChannel {
		name = "utility-log"
		description = "The channel to log various utility actions too."
	}
	val logChannelUpdates by defaultingBoolean {
		name = "log-channel-updates"
		description = "Whether to log changes made to channels in this guild."
		defaultValue = false
	}
	val logEventUpdates by defaultingBoolean {
		name = "log-event-updates"
		description = "Whether to log changes made to scheduled events in this guild."
	}
	val logInviteUpdates by defaultingBoolean {
		name = "log-invite-updates"
		description = "Whether to log changes made to invites in this guild."
	}
	val logRoleUpdates by defaultingBoolean {
		name = "log-role-updates"
		description = "Whether to log changes made to roles in this guild."
	}
}
