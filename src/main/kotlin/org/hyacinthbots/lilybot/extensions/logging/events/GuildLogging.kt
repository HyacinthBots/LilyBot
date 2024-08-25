package org.hyacinthbots.lilybot.extensions.logging.events

import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.GuildLeaveTimeCollection
import java.util.concurrent.CancellationException

/**
 * This class is used to detect when Lily leaves or joins a guild, allowing us to delete old guild data, if Lily has
 * not been in the guild for more than a month.
 *
 * @since 3.2.0
 */
class GuildLogging : Extension() {
	override val name = "guild-logging"

	override suspend fun setup() {
		/**
		 * Log the instant Lily leaves a guild in the database.
		 *
		 * @author NoComment1105, tempest15
		 * @since 3.2.0
		 */
		event<GuildDeleteEvent> {
			action {
				try {
					GuildLeaveTimeCollection().setLeaveTime(event.guildId, Clock.System.now())
				} catch (_: CancellationException) {
				} catch (_: KtorRequestException) {
				}
			}
		}

		/**
		 * Remove the logged time when Lily rejoins the guild from the database.
		 *
		 * @author NoComment1105, tempest15
		 * @since 3.2.0
		 */
		event<GuildCreateEvent> {
			action {
				try {
					GuildLeaveTimeCollection().removeLeaveTime(event.guild.id)
				} catch (_: CancellationException) {
				} catch (_: KtorRequestException) {
				}
			}
		}
	}
}
