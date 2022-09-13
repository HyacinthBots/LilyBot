package org.hyacinthbots.lilybot.database

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.Cleanups.cleanupGuildData
import org.hyacinthbots.lilybot.database.Cleanups.cleanupThreadData
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.entities.GuildLeaveTimeData
import org.hyacinthbots.lilybot.database.entities.ThreadData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object contains the Database clean up functions, for removing old data from the database that Lily no longer
 * requires.
 *
 * @since 4.0.0
 * @see cleanupGuildData
 * @see cleanupThreadData
 */
object Cleanups : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val guildLeaveTimeCollection = db.mainDatabase.getCollection<GuildLeaveTimeData>()

	@PublishedApi
	internal val threadDataCollection = db.mainDatabase.getCollection<ThreadData>()

	@PublishedApi
	internal val cleanupsLogger = KotlinLogging.logger("Database Cleanups")

	/**
	 * This function deletes the Configs stored in the database for guilds Lily left a month or more ago.
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend inline fun cleanupGuildData() {
		cleanupsLogger.info("Starting guild cleanup...")
		val leaveTimeData = guildLeaveTimeCollection.find().toList()
		var deletedGuildData = 0

		leaveTimeData.forEach {
			// Calculate the time since Lily left the guild.
			val leaveDuration = Clock.System.now() - it.guildLeaveTime

			if (leaveDuration.inWholeDays > 30) {
				// If the bot has been out of the guild for more than 30 days, delete any related data.
				ModerationConfigCollection().clearConfig(it.guildId)
				SupportConfigCollection().clearConfig(it.guildId)
				LoggingConfigCollection().clearConfig(it.guildId)
				UtilityConfigCollection().clearConfig(it.guildId)
				TagsCollection().clearTags(it.guildId)
				WarnCollection().clearWarns(it.guildId)
				RoleMenuCollection().removeAllRoleMenus(it.guildId)
				guildLeaveTimeCollection.deleteOne(GuildLeaveTimeData::guildId eq it.guildId)
				deletedGuildData += 1 // Increment the counter for logging
			}
		}

		cleanupsLogger.info("Deleted old data for $deletedGuildData guilds from the database")
	}

	/**
	 * This function deletes the ownership data stored in the database for any thread older than a week.
	 *
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun cleanupThreadData(kordInstance: Kord) {
		cleanupsLogger.info("Starting thread cleanup...")
		val threads = threadDataCollection.find().toList()
		var deletedThreads = 0
		for (it in threads) {
			try {
				val thread = kordInstance.getGuild(it.guildId!!)?.getChannelOf<ThreadChannel>(it.threadId) ?: continue
				val latestMessage = thread.getLastMessage() ?: continue
				val timeSinceLatestMessage = Clock.System.now() - latestMessage.id.timestamp
				if (timeSinceLatestMessage.inWholeDays > 7) {
					ThreadsCollection().removeThread(thread.id)
					deletedThreads++
				}
			} catch (e: KtorRequestException) {
				ThreadsCollection().removeThread(it.threadId)
				deletedThreads++
				continue
			}
		}
		cleanupsLogger.info("Deleted $deletedThreads old threads from the database")
	}
}
