package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_GREEN
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.gateway.ReadyEvent
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.Cleanups
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.collections.StatusCollection
import org.hyacinthbots.lilybot.database.entities.ThreadData
import org.hyacinthbots.lilybot.utils.ONLINE_STATUS_CHANNEL
import org.hyacinthbots.lilybot.utils.TEST_GUILD_ID
import org.hyacinthbots.lilybot.utils.updateDefaultPresence
import org.litote.kmongo.coroutine.toList
import org.litote.kmongo.eq
import org.litote.kmongo.setValue
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
		event<ReadyEvent> {
			action {
				// TODO Remove this once the migration is done, because of the fact we cannot access kord in the
				//  migration we need to do this to apply the guild IDs
				with(Database().mainDatabase.getCollection<ThreadData>("threadData")) {
					collection.find(ThreadData::guildId eq null).toList().forEach {
						updateOne(
							ThreadData::threadId eq it.threadId,
							setValue(ThreadData::guildId, kord.getChannelOf<ThreadChannel>(it.threadId)!!.guildId)
						)
					}
				}

				val now = Clock.System.now()

				/**
				 * Online notification, that is printed to the official [TEST_GUILD_ID]'s [ONLINE_STATUS_CHANNEL].
				 * @author IMS212
				 * @since v2.0
				 */
				// The channel specifically for sending online notifications to
				val onlineLog = kord.getGuild(TEST_GUILD_ID)?.getChannelOf<NewsChannel>(ONLINE_STATUS_CHANNEL)
				onlineLog?.createEmbed {
					title = "Lily is now online!"
					description =
						"${now.toDiscord(TimestampType.LongDateTime)} (${now.toDiscord(TimestampType.RelativeTime)})"
					color = DISCORD_GREEN
				}?.publish()

				/**
				 * Check the status value in the database. If it is "default", set the status to watching over X guilds,
				 * else the database value.
				 */
				if (StatusCollection().getStatus() == null) {
					updateDefaultPresence()
				} else {
					this@event.kord.editPresence {
						status = PresenceStatus.Online
						playing(StatusCollection().getStatus()!!)
					}
				}
			}
		}

		cleanupTask = cleanupScheduler.schedule(1.days, callback = ::cleanup)
	}

	/**
	 * This function is called to remove any threads in the database that haven't had a message sent in the last
	 * week.
	 * @author NoComment1105
	 * @since 4.1.0
	 */
	private suspend fun cleanup() {
		Cleanups.cleanupThreadData(kord)
		Cleanups.cleanupGuildData()
	}
}
