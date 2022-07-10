package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.event.guild.GuildCreateEvent
import dev.kord.core.event.guild.GuildDeleteEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.collections.GuildLeaveTimeCollection

/**
 * This class is used to detect when Lily leaves or joins a guild, allowing us to delete old guild data, if Lily has
 * not been in the guild for more than a month.
 *
 * @since 3.2.0
 */
class JoinLeaveDetection : Extension() {
	override val name = "join-leave-detection"

	override suspend fun setup() {
		/**
		 * Log the instant Lily leaves a guild in the database.
		 *
		 * @author NoComment1105, tempest15
		 * @since 3.2.0
		 */
		event<GuildDeleteEvent> {
			action {
				GuildLeaveTimeCollection().setLeaveTime(event.guildId, Clock.System.now())
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
				GuildLeaveTimeCollection().removeLeaveTime(event.guild.id)
			}
		}
	}
}
