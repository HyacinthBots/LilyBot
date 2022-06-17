package net.irisshaders.lilybot.database

import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.ThreadChannel
import kotlinx.datetime.Clock
import mu.KotlinLogging
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

object DatabaseCleanups {

	@PublishedApi
	internal val cleanupsLogger = KotlinLogging.logger("Database Cleanups")

	/**
	 * This function deletes the [ConfigData] stored in the database for guilds Lily left a month or more ago.
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend inline fun cleanupGuildData() {
		cleanupsLogger.info("Starting guild cleanup...")
		val collection = database.getCollection<DatabaseTables.GuildLeaveTimeData>()
		val leaveTimeData = collection.find().toList()
		var deletedGuildData = 0

		leaveTimeData.forEach {
			// Calculate the time since Lily left the guild.
			val leaveDuration = Clock.System.now() - it.guildLeaveTime

			if (leaveDuration.inWholeDays > 30) {
				// If the bot has been out of the guild for more than 30 days, delete any related data.
				// DatabaseRemovers.clearConfig(it.guildId)
				DatabaseRemovers.clearTags(it.guildId)
				DatabaseRemovers.clearWarn(it.guildId)
				// Once role menu is rewritten, component data should also be cleared here.
				collection.deleteOne(DatabaseTables.GuildLeaveTimeData::guildId eq it.guildId)
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
		val collection = database.getCollection<DatabaseTables.ThreadData>()
		val threads = collection.find().toList()
		var deletedThreads = 0
		for (it in threads) {
			val thread = kordInstance.getChannelOf<ThreadChannel>(it.threadId) ?: continue
			val latestMessage = thread.getLastMessage() ?: continue
			val timeSinceLatestMessage = Clock.System.now() - latestMessage.id.timestamp
			if (timeSinceLatestMessage.inWholeDays > 7) {
				collection.deleteOne(DatabaseTables.ThreadData::threadId eq thread.id)
				deletedThreads++
			}
		}
		cleanupsLogger.info("Deleted $deletedThreads old threads from the database")
	}
}
