package org.hyacinthbots.lilybot.extensions.utility.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.defaultingBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import lilybot.i18n.Translations

class UtilityArgs : Arguments() {
	val utilityLogChannel by optionalChannel {
		name = Translations.Config.Arguments.Utility.UtilityLog.name
		description = Translations.Config.Arguments.Utility.UtilityLog.description
	}
	val logChannelUpdates by defaultingBoolean {
		name = Translations.Config.Arguments.Utility.ChannelUpdates.name
		description = Translations.Config.Arguments.Utility.ChannelUpdates.description
		defaultValue = false
	}
	val logEventUpdates by defaultingBoolean {
		name = Translations.Config.Arguments.Utility.EventUpdates.name
		description = Translations.Config.Arguments.Utility.EventUpdates.description
		defaultValue = false
	}
	val logInviteUpdates by defaultingBoolean {
		name = Translations.Config.Arguments.Utility.InviteUpdates.name
		description = Translations.Config.Arguments.Utility.InviteUpdates.description
		defaultValue = false
	}
	val logRoleUpdates by defaultingBoolean {
		name = Translations.Config.Arguments.Utility.RoleUpdates.name
		description = Translations.Config.Arguments.Utility.RoleUpdates.description
		defaultValue = false
	}
}
