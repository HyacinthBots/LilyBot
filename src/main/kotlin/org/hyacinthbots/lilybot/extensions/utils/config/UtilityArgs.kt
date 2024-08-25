package org.hyacinthbots.lilybot.extensions.utils.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.optionalChannel

class UtilityArgs : Arguments() {
	val utilityLogChannel by optionalChannel {
		name = "utility-log"
		description = "The channel to log various utility actions too."
	}
}
