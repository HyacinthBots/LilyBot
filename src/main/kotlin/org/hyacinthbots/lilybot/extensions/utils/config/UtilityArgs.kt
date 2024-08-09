package org.hyacinthbots.lilybot.extensions.utils.config

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel

class UtilityArgs : Arguments() {
	val utilityLogChannel by optionalChannel {
		name = "utility-log"
		description = "The channel to log various utility actions too."
	}
}
