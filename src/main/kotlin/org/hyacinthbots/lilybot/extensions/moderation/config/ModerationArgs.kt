package org.hyacinthbots.lilybot.extensions.moderation.config

import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.boolean
import dev.kordex.core.commands.converters.impl.coalescingOptionalDuration
import dev.kordex.core.commands.converters.impl.optionalBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.commands.converters.impl.optionalRole
import dev.kordex.core.commands.converters.impl.optionalString
import lilybot.i18n.Translations

class ModerationArgs : Arguments() {
	val enabled by boolean {
		name = Translations.Config.Arguments.Moderation.Enabled.name
		description = Translations.Config.Arguments.Moderation.Enabled.description
	}

	val moderatorRole by optionalRole {
		name = Translations.Config.Arguments.Moderation.ModeratorRole.name
		description = Translations.Config.Arguments.Moderation.ModeratorRole.description
	}

	val modActionLog by optionalChannel {
		name = Translations.Config.Arguments.Moderation.ModActionLog.name
		description = Translations.Config.Arguments.Moderation.ModActionLog.description
	}

	val quickTimeoutLength by coalescingOptionalDuration {
		name = Translations.Config.Arguments.Moderation.QuickTimeout.name
		description = Translations.Config.Arguments.Moderation.QuickTimeout.description
	}

	val warnAutoPunishments by optionalBoolean {
		name = Translations.Config.Arguments.Moderation.AutoPunish.name
		description = Translations.Config.Arguments.Moderation.AutoPunish.description
	}

	val logPublicly by optionalBoolean {
		name = Translations.Config.Arguments.Moderation.LogPublicly.name
		description = Translations.Config.Arguments.Moderation.LogPublicly.description
	}

	val dmDefault by optionalBoolean {
		name = Translations.Config.Arguments.Moderation.DmDefault.name
		description = Translations.Config.Arguments.Moderation.DmDefault.description
	}

	val banDmMessage by optionalString {
		name = Translations.Config.Arguments.Moderation.BanDm.name
		description = Translations.Config.Arguments.Moderation.BanDm.description
	}

	val autoInviteModeratorRole by optionalBoolean {
		name = Translations.Config.Arguments.Moderation.InviteMods.name
		description = Translations.Config.Arguments.Moderation.InviteMods.description
	}

	val logMemberRoleChanges by optionalBoolean {
		name = Translations.Config.Arguments.Moderation.RoleChanges.name
		description = Translations.Config.Arguments.Moderation.RoleChanges.description
	}
}
