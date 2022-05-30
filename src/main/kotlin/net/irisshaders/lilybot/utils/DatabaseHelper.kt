package net.irisshaders.lilybot.utils

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.thread.TextChannelThread
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

/**
 * The object containing functions for interacting with the database.
 *
 * @since 3.0.0
 */
object DatabaseHelper {

	private val databaseLogger = KotlinLogging.logger("Database Logger")

	/**
	 * Using the provided [inputGuildId] the config for that guild  will be returned from the database.
	 *
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @return The config for [inputGuildId]
	 * @author NoComment1105, tempest15
	 * @since 3.0.0
	 */
	suspend fun getConfig(inputGuildId: Snowflake): ConfigData? {
		val collection = database.getCollection<ConfigData>()
		return collection.findOne(ConfigData::guildId eq inputGuildId)
	}

	/**
	 * Adds the given [newConfig] to the database.
	 *
	 * @param newConfig The new config values you want to set.
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend fun setConfig(newConfig: ConfigData) {
		val collection = database.getCollection<ConfigData>()
		collection.deleteOne(ConfigData::guildId eq newConfig.guildId)
		collection.insertOne(newConfig)
	}

	/**
	 * Clears the config for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend fun clearConfig(inputGuildId: Snowflake) {
		val collection = database.getCollection<ConfigData>()
		collection.deleteOne(ConfigData::guildId eq inputGuildId)
	}

	/**
	 * Gets the number of points the provided [inputUserId] has in the provided [inputGuildId] from the database.
	 *
	 * @param inputUserId The ID of the user to get the point value for
	 * @param inputGuildId The ID of the guild the command was run in
	 * @return null or the result from the database
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend fun getWarn(inputUserId: Snowflake, inputGuildId: Snowflake): WarnData? {
		val collection = database.getCollection<WarnData>()
		return collection.findOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
	}

	/**
	 * Updates the number of points the provided [inputUserId] has in the provided [inputGuildId] in the database.
	 *
	 * @param inputUserId The ID of the user to get the point value for.
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @param remove Remove a warn strike, or add a warn strike.
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend fun setWarn(inputUserId: Snowflake, inputGuildId: Snowflake, remove: Boolean) {
		val currentStrikes = getWarn(inputUserId, inputGuildId)?.strikes ?: 0
		val collection = database.getCollection<WarnData>()
		collection.deleteOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
		collection.insertOne(
			WarnData(
				inputUserId,
				inputGuildId,
				if (!remove) currentStrikes.plus(1) else currentStrikes.minus(1)
			)
		)
	}

	/**
	 * Clears all warn strikes for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.0.0
	 */
	private suspend fun clearWarn(inputGuildId: Snowflake) {
		val collection = database.getCollection<WarnData>()
		collection.deleteMany(WarnData::guildId eq inputGuildId)
	}

	/**
	 * Using the provided [inputComponentId] the [ComponentData] will be returned from the database.
	 *
	 * @param inputGuildId The ID of the guild the component event was triggered in
	 * @param inputComponentId The ID of the component the event was triggered with
	 * @return The component from the database
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend fun getComponent(inputGuildId: Snowflake, inputComponentId: String): ComponentData? {
		val collection = database.getCollection<ComponentData>()
		return collection.findOne(
			ComponentData::guildId eq inputGuildId,
			ComponentData::componentId eq inputComponentId
		)
	}

	/**
	 * Add the given [newComponent] to the database.
	 *
	 * @param newComponent The data for the component to be added.
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend fun setComponent(newComponent: ComponentData) {
		val collection = database.getCollection<ComponentData>()
		collection.deleteOne(
			ComponentData::guildId eq newComponent.guildId,
			ComponentData::componentId eq newComponent.componentId
		)
		collection.insertOne(newComponent)
	}

	/**
	 * Gets Lily's status from the database.
	 *
	 * @return null or the set status in the database.
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	fun getStatus(): String {
		var selectedStatus: StatusData?
		runBlocking {
			val collection = database.getCollection<StatusData>()
			selectedStatus = collection.findOne(StatusData::key eq "LilyStatus")
		}
		return selectedStatus?.status ?: "Iris"
	}

	/**
	 * Add the given [newStatus] to the database.
	 *
	 * @param newStatus The new status you wish to set
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend fun setStatus(newStatus: String) {
		val collection = database.getCollection<StatusData>()
		collection.deleteOne(StatusData::key eq "LilyStatus")
		collection.insertOne(StatusData("LilyStatus", newStatus))
	}

	/**
	 * Gets the given tag using it's [name] and returns its [TagsData]. If the tag does not exist.
	 * it will return null
	 *
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @param name The named identifier of the tag.
	 * @return null or the result from the database.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend fun getTag(inputGuildId: Snowflake, name: String): TagsData? {
		val collection = database.getCollection<TagsData>()
		return collection.findOne(TagsData::guildId eq inputGuildId, TagsData::name eq name)
	}

	/**
	 * Gets all tags in the given [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild.
	 * @return A [List] of tags for the specified [inputGuildId].
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend fun getAllTags(inputGuildId: Snowflake): List<TagsData> {
		val collection = database.getCollection<TagsData>()
		return collection.find(TagsData::guildId eq inputGuildId).toList()
	}

	/**
	 * Adds a tag to the database, using the provided parameters.
	 *
	 * @param inputGuildId The ID of the guild the command was run in. Used to keep things guild specific.
	 * @param name The named identifier of the tag being created.
	 * @param tagTitle The title of the tag being created.
	 * @param tagValue The contents of the tag being created.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend fun setTag(inputGuildId: Snowflake, name: String, tagTitle: String, tagValue: String) {
		val collection = database.getCollection<TagsData>()
		collection.insertOne(TagsData(inputGuildId, name, tagTitle, tagValue))
	}

	/**
	 * Deletes the tag [name] from the [database].
	 *
	 * @param inputGuildId The guild the tag was created in.
	 * @param name The named identifier of the tag being deleted.
	 * @author NoComment1105
	 * @since 3.1.0
	 */
	suspend fun deleteTag(inputGuildId: Snowflake, name: String) {
		val collection = database.getCollection<TagsData>()
		collection.deleteOne(TagsData::guildId eq inputGuildId, TagsData::name eq name)
	}

	/**
	 * Clears all tags for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.1.0
	 */
	private suspend fun clearTags(inputGuildId: Snowflake) {
		val collection = database.getCollection<TagsData>()
		collection.deleteMany(TagsData::guildId eq inputGuildId)
	}

	/**
	 * Using the provided [inputThreadId] the owner's ID or null is returned from the database.
	 *
	 * @param inputThreadId The ID of the thread you wish to find the owner for
	 *
	 * @return null or the thread owner's ID
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend fun getThreadOwner(inputThreadId: Snowflake): Snowflake? {
		val collection = database.getCollection<ThreadData>()
		val selectedThread = collection.findOne(ThreadData::threadId eq inputThreadId) ?: return null
		return selectedThread.ownerId
	}

	/**
	 * Using the provided [inputOwnerId] the list of threads that person owns is returned from the database.
	 *
	 * @param inputOwnerId The ID of the member whose threads you wish to find
	 *
	 * @return null or a list of threads the member owns
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend fun getOwnerThreads(inputOwnerId: Snowflake): List<ThreadData> {
		val collection = database.getCollection<ThreadData>()
		return collection.find(ThreadData::ownerId eq inputOwnerId).toList()
	}

	/**
	 * Add or update the ownership of the given [inputThreadId] to the given [newOwnerId].
	 *
	 * @param inputThreadId The ID of the thread you wish to update or set the owner for
	 * @param newOwnerId The new owner of the thread
	 *
	 * @return null or the thread owner's ID
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend fun setThreadOwner(inputThreadId: Snowflake, newOwnerId: Snowflake) {
		val collection = database.getCollection<ThreadData>()
		collection.deleteOne(ThreadData::threadId eq inputThreadId)
		collection.insertOne(ThreadData(inputThreadId, newOwnerId))
	}

	/**
	 * This function deletes the ownership data stored in the database for the given [inputThreadId].
	 *
	 * @param inputThreadId The ID of the thread to delete
	 *
	 * @author henkelmax
	 * @since 3.2.2
	 */
	suspend fun deleteThread(inputThreadId: Snowflake) {
		val collection = database.getCollection<ThreadData>()
		collection.deleteOne(ThreadData::threadId eq inputThreadId)
	}

	/**
	 * This function deletes the ownership data stored in the database for any thread older than a week.
	 *
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend fun cleanupThreadData(kordInstance: Kord) {
		val collection = database.getCollection<ThreadData>()
		val threads = collection.find().toList()
		var deletedThreads = 0
		threads.forEach {
			val thread = kordInstance.getChannel(it.threadId) as TextChannelThread? ?: return@forEach
			val latestMessage = thread.getLastMessage() ?: return@forEach
			val timeSinceLatestMessage = Clock.System.now() - latestMessage.id.timestamp
			if (timeSinceLatestMessage.inWholeDays > 7) {
				collection.deleteOne(ThreadData::threadId eq thread.id)
				deletedThreads = +1
			}
		}
		databaseLogger.info("Deleted $deletedThreads old threads from the database")
	}

	/**
	 * Adds the time Lily bot left a guild with a config.
	 *
	 * @param inputGuildId The guild the event took place in
	 * @param time The current time
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend fun setLeaveTime(inputGuildId: Snowflake, time: Instant) {
		val collection = database.getCollection<GuildLeaveTimeData>()
		collection.insertOne(GuildLeaveTimeData(inputGuildId, time))
	}

	/**
	 * This function deletes a [GuildLeaveTimeData] from the database.
	 *
	 * @param inputGuildId The guild to delete the [GuildLeaveTimeData] for
	 *
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend fun deleteLeaveTime(inputGuildId: Snowflake) {
		val collection = database.getCollection<GuildLeaveTimeData>()
		collection.deleteOne(GuildLeaveTimeData::guildId eq inputGuildId)
	}

	/**
	 * This function deletes the [ConfigData] stored in the database for guilds Lily left a month or more ago.
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend fun cleanupGuildData() {
		val collection = database.getCollection<GuildLeaveTimeData>()
		val leaveTimeData = collection.find().toList()
		var deletedGuildData = 0

		leaveTimeData.forEach {
			// Calculate the time since Lily left the guild.
			val leaveDuration = Clock.System.now() - it.guildLeaveTime

			if (leaveDuration.inWholeDays > 30) {
				// If the bot has been out of the guild for more than 30 days, delete any related data.
				clearConfig(it.guildId)
				clearTags(it.guildId)
				clearWarn(it.guildId)
				// Once role menu is rewritten, component data should also be cleared here.
				collection.deleteOne(GuildLeaveTimeData::guildId eq it.guildId)
				deletedGuildData += 1 // Increment the counter for logging
			}
		}

		databaseLogger.info("Deleted old data for $deletedGuildData guilds from the database")
	}
}

/**
 * The data for guild configuration.
 *
 * @param guildId The ID of the guild the config is for
 * @param moderatorsPing The ID of the moderator ping role
 * @param modActionLog The ID of the guild's action/audit log channel
 * @param messageLogs The ID of the guild's message logging channel
 * @param joinChannel The ID of the guild's member flow channel
 * @param supportChannel The ID of the support channel for the guild, nullable
 * @param supportTeam The ID of the support team for the guild, nullable
 * @since 3.0.0
 */
@Serializable
data class ConfigData(
	val guildId: Snowflake,
	val moderatorsPing: Snowflake,
	val modActionLog: Snowflake,
	val messageLogs: Snowflake,
	val joinChannel: Snowflake,
	val supportChannel: Snowflake?,
	val supportTeam: Snowflake?,
)

/**
 * The data for warnings in guilds.
 *.
 * @param userId The ID of the user with warnings
 * @param guildId The ID of the guild they received the warning in
 * @param strikes The amount of strikes they have received
 * @since 3.0.0
 */
@Serializable
data class WarnData(
	val userId: Snowflake,
	val guildId: Snowflake,
	val strikes: Int
)

/**
 * The data for role menu components.
 *
 * @param guildId The ID of the guild tied to the component
 * @param componentId The ID of the components
 * @param roleId The ID of the role the component will add
 * @param addOrRemove Whether to add or remove the role from the user, when the component is clicked
 * @since 3.0.0
 */
@Serializable
data class ComponentData(
	val guildId: Snowflake,
	val componentId: String,
	val roleId: Snowflake,
	val addOrRemove: String
)

/**
 * The data for the bot status.
 *
 * @param key This is just so we can find the status and should always be set to "LilyStatus"
 * @param status The string value that will be seen in the bots presence
 * @since 3.0.0
 */
@Serializable
data class StatusData(
	val key: String,
	val status: String
)

/**
 * The data of guild tags, which are stored in the database.
 *
 * @param guildId The ID of the guild the tag will be saved for
 * @param name The named identifier of the tag
 * @param tagTitle The title of the created tag
 * @param tagValue The value of the created tag
 * @since 3.1.0
 */
@Serializable
data class TagsData(
	val guildId: Snowflake,
	val name: String,
	val tagTitle: String,
	val tagValue: String
)

/**
 * The data for threads.
 *
 * @param threadId The ID of the thread
 * @param ownerId The ID of the thread's owner
 * @since 3.2.0
 */
@Serializable
data class ThreadData(
	val threadId: Snowflake,
	val ownerId: Snowflake,
)

/**
 * The data for when Lily leaves a guild.
 *
 * @param guildId The ID of the guild Lily left
 * @param guildLeaveTime The [Instant] that Lily left the guild
 * @since 3.2.0
 */
@Serializable
data class GuildLeaveTimeData(
	val guildId: Snowflake,
	val guildLeaveTime: Instant
)
