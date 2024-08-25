package org.hyacinthbots.lilybot.extensions.config

import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import org.hyacinthbots.lilybot.extensions.config.commands.configClearCommand
import org.hyacinthbots.lilybot.extensions.config.commands.configViewCommand
import org.hyacinthbots.lilybot.extensions.logging.config.loggingCommand
import org.hyacinthbots.lilybot.extensions.moderation.config.moderationCommand
import org.hyacinthbots.lilybot.extensions.utils.config.utilityCommand

class ConfigExtension : Extension() {
	override val name: String = "config"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "config"
			description = "Configure Lily's settings"

			loggingCommand()

			moderationCommand()

			utilityCommand()

			configClearCommand()

			configViewCommand()
		}
	}
}
