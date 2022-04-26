package net.irisshaders.lilybot.utils

import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

object DatabaseHelper {

	/**
	 * Using the provided [inputGuildId] the config for that guild  will be returned from the database.
	 *
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @return The config for [inputGuildId]
	 * @author NoComment1105
	 * @author tempest15
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
	 * Using the provided [inputComponentId] the [ComponentData] will be returned from the database.
	 *
	 * @param inputComponentId The ID of the component the event was triggered with
	 * @return The component from the database
	 * @author tempest15
	 */
	suspend fun getComponent(inputComponentId: String): ComponentData? {
		// this returns any because it can return either a string or a snowflake
		val collection = database.getCollection<ComponentData>()
		return collection.findOne(ComponentData::componentId eq inputComponentId)
	}

	/**
	 * Add the given [newComponent] to the database.
	 *
	 * @param newComponent The data for the component to be added.
	 * @author tempest15
	 */
	suspend fun setComponent(newComponent: ComponentData) {
		val collection = database.getCollection<ComponentData>()
		collection.deleteOne(ComponentData::componentId eq newComponent.componentId)
		collection.insertOne(newComponent)
	}

	/**
	 * Gets Lily's status from the database.
	 *
	 * @return null or the set status in the database.
	 * @author NoComment1105
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
	 */
	suspend fun getAllTags(inputGuildId: Snowflake): List<TagsData> {
		val collection = database.getCollection<TagsData>()
		return collection.find(TagsData::guildId eq inputGuildId).toList()
	}

	/**
	 * Using the provided [inputThreadId] the owner's ID or null is returned from the database.
	 *
	 * @param [inputThreadId] The ID of the thread you wish to find the owner for
	 *
	 * @return null or the thread owner's ID
	 * @author tempest15
	 */
	suspend fun getThreadOwner(inputThreadId: Snowflake): Snowflake? {
		val collection = database.getCollection<ThreadData>()
		val selectedThread = collection.findOne(ThreadData::threadId eq inputThreadId) ?: return null
		return selectedThread.ownerId
	}

	/**
	 * Using the provided [inputOwnerId] the list of threads that person owns is returned from the database.
	 *
	 * @param [inputOwnerId] The ID of the member whose threads you wish to find
	 *
	 * @return null or a list of threads the member owns
	 * @author tempest15
	 */
	suspend fun getOwnerThreads(inputOwnerId: Snowflake): List<ThreadData> {
		val collection = database.getCollection<ThreadData>()
		return collection.find(ThreadData::ownerId eq inputOwnerId).toList()
	}

	/**
	 * Add or update the ownership of the given [inputThreadId] to the given [newOwnerId].
	 *
	 * @param [inputThreadId] The ID of the thread you wish to update or set the owner for
	 * @param [newOwnerId] The new owner of the thread
	 *
	 * @return null or the thread owner's ID
	 * @author tempest15
	 */
	suspend fun setThreadOwner(inputThreadId: Snowflake, newOwnerId: Snowflake) {
		val collection = database.getCollection<ThreadData>()
		collection.deleteOne(ThreadData::threadId eq inputThreadId)
		collection.insertOne(ThreadData(inputThreadId, newOwnerId))
	}

	/**
	 * Adds a tag to the database, using the provided parameters.
	 *
	 * @param inputGuildId The ID of the guild the command was run in. Used to keep things guild specific.
	 * @param name The named identifier of the tag being created.
	 * @param tagTitle The title of the tag being created.
	 * @param tagValue The contents of the tag being created.
	 * @author NoComment1105
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
	 */
	suspend fun deleteTag(inputGuildId: Snowflake, name: String) {
		val collection = database.getCollection<TagsData>()
		collection.deleteOne(TagsData::guildId eq inputGuildId, TagsData::name eq name)
	}
}

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

@Serializable
data class WarnData(
	val userId: Snowflake,
	val guildId: Snowflake,
	val strikes: Int
)

@Serializable
data class ComponentData(
	val componentId: String,
	val roleId: Snowflake,
	val addOrRemove: String
)

@Serializable
data class StatusData(
	val key: String, // this is just so we can find the status and should always be set to "LilyStatus"
	val status: String
)

@Serializable
data class TagsData(
	val guildId: Snowflake,
	val name: String,
	val tagTitle: String,
	val tagValue: String
)

@Serializable
data class ThreadData(
	val threadId: Snowflake,
	val ownerId: Snowflake,
)
