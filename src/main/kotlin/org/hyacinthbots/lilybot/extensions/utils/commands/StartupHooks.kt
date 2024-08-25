package org.hyacinthbots.lilybot.extensions.utils.commands

import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.event.gateway.ReadyEvent
import dev.kordex.core.DISCORD_GREEN
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.event
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.Cleanups
import org.hyacinthbots.lilybot.database.collections.UptimeCollection
import org.hyacinthbots.lilybot.utils.ONLINE_STATUS_CHANNEL
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import kotlin.time.Duration.Companion.days

/**
 * This class serves as a place for all functions that get run on bot start and bot start alone. This *hypothetically*
 * fixes a peculiar bug with [PublicUtilities], where if these functions we're present within, all other feature from
 * the class don't get added to a server when the bot joins the server, and instead only present themselves after a
 * bot instance restart.
 *
 * @since 3.2.2
 */
class StartupHooks : Extension() {
	override val name = "startup-hooks"

	private val cleanupScheduler = Scheduler()

	private lateinit var cleanupTask: Task

	override suspend fun setup() {
		cleanupTask = cleanupScheduler.schedule(1.days, repeat = true, callback = ::cleanup)

		event<ReadyEvent> {
			action {
				val now = Clock.System.now()

				UptimeCollection().set(now)

				/**
				 * Online notification, that is printed to the official [TEST_GUILD_ID]'s [ONLINE_STATUS_CHANNEL].
				 * @author IMS212
				 * @since v2.0
				 */
				// The channel specifically for sending online notifications to
				val homeGuild = kord.getGuildOrNull(TEST_GUILD_ID) ?: return@action
				val onlineLog = homeGuild.getChannelOfOrNull<NewsChannel>(ONLINE_STATUS_CHANNEL) ?: return@action
				onlineLog.createEmbed {
					title = "Lily is now online!"
					description =
						"${now.toDiscord(TimestampType.LongDateTime)} (${now.toDiscord(TimestampType.RelativeTime)})"
					color = DISCORD_GREEN
				}
			}
		}
	}

	/**
	 * This function is called to remove any threads in the database that haven't had a message sent in the last
	 * week.
	 * @author NoComment1105
	 * @since 4.1.0
	 */
	private suspend fun cleanup() {
		Cleanups.cleanupThreadData(kord)
		Cleanups.cleanupGuildData(kord)
	}
}
