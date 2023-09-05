package org.hyacinthbots.lilybot.database

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters
import dev.kord.core.Kord
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.request.KtorRequestException
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.Clock
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.Cleanups.cleanupGuildData
import org.hyacinthbots.lilybot.database.Cleanups.cleanupThreadData
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.database.collections.GuildLeaveTimeCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.NewsChannelPublishingCollection
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.RoleSubscriptionCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.database.entities.GuildLeaveTimeData
import org.hyacinthbots.lilybot.database.entities.ThreadData
import org.koin.core.component.inject

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
	internal val guildLeaveTimeCollection =
		db.mainDatabase.getCollection<GuildLeaveTimeData>(GuildLeaveTimeCollection.name)

	@PublishedApi
	internal val threadDataCollection = db.mainDatabase.getCollection<ThreadData>(ThreadsCollection.name)

	@PublishedApi
	internal val cleanupsLogger = KotlinLogging.logger("Database Cleanups")

	/**
	 * This function deletes the Configs stored in the database for guilds Lily left a month or more ago.
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend fun cleanupGuildData(kord: Kord) {
		cleanupsLogger.info("Starting guild cleanup...")
		val leaveTimeData = guildLeaveTimeCollection.find().toList()
		var deletedGuildData = 0

		leaveTimeData.forEach {
			// Calculate the time since Lily left the guild.
			val leaveDuration = Clock.System.now() - it.guildLeaveTime

			if (leaveDuration.inWholeDays > 30) {
				// If the bot has been out of the guild for more than 30 days, delete any related data.
				GithubCollection().removeDefaultRepo(it.guildId)
				LoggingConfigCollection().clearConfig(it.guildId)
				ModerationConfigCollection().clearConfig(it.guildId)
				NewsChannelPublishingCollection().clearAutoPublishingForGuild(it.guildId)
				ReminderCollection().removeGuildReminders(it.guildId)
				RoleMenuCollection().removeAllRoleMenus(it.guildId)
				RoleSubscriptionCollection().removeAllSubscribableRoles(it.guildId)
				TagsCollection().clearTags(it.guildId)
				ThreadsCollection().removeGuildThreads(it.guildId)
				UtilityConfigCollection().clearConfig(it.guildId)
				WarnCollection().clearWarns(it.guildId)
				WelcomeChannelCollection().removeWelcomeChannelsForGuild(it.guildId, kord)
				val leaveFilters = Filters.eq(GuildLeaveTimeData::guildId.name, it.guildId)
				guildLeaveTimeCollection.deleteOne(leaveFilters)
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
	suspend fun cleanupThreadData(kordInstance: Kord) {
		cleanupsLogger.info("Starting thread cleanup...")
		val threads = threadDataCollection.find().toList()
		var deletedThreads = 0
		for (it in threads) {
			try {
				if (it.guildId == null) {
					ThreadsCollection().removeThread(it.threadId)
					deletedThreads++
					return
				}
				val guild = kordInstance.getGuildOrNull(it.guildId) ?: return
				val thread = guild.getChannelOfOrNull<ThreadChannel>(it.threadId) ?: continue
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
