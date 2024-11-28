package org.hyacinthbots.lilybot.extensions.logging.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.boolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import lilybot.i18n.Translations

class LoggingArgs : Arguments() {
	val enableMessageDeleteLogs by boolean {
		name = Translations.Config.Arguments.Logging.EnableDelete.name
		description = Translations.Config.Arguments.Logging.EnableDelete.description
	}

	val enableMessageEditLogs by boolean {
		name = Translations.Config.Arguments.Logging.EnableEdit.name
		description = Translations.Config.Arguments.Logging.EnableEdit.description
	}

	val enableMemberLogging by boolean {
		name = Translations.Config.Arguments.Logging.EnableMember.name
		description = Translations.Config.Arguments.Logging.EnableMember.description
	}

	val enablePublicMemberLogging by boolean {
		name = Translations.Config.Arguments.Logging.EnablePublicMember.name
		description = Translations.Config.Arguments.Logging.EnablePublicMember.description
	}

	val messageLogs by optionalChannel {
		name = Translations.Config.Arguments.Logging.MessageLog.name
		description = Translations.Config.Arguments.Logging.MessageLog.description
	}

	val memberLog by optionalChannel {
		name = Translations.Config.Arguments.Logging.MemberLog.name
		description = Translations.Config.Arguments.Logging.MemberLog.description
	}

	val publicMemberLog by optionalChannel {
		name = Translations.Config.Arguments.Logging.PublicMemberLog.name
		description = Translations.Config.Arguments.Logging.PublicMemberLog.description
	}
}
