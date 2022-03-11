package net.irisshaders.lilybot.utils

import kotlinx.coroutines.runBlocking
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

object DatabaseHelper {

	/**
	 * Using the provided [inputGuildId] and [inputColumn] a value or null will be returned from the database
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @param inputColumn The config value associated with that guild that you want
	 * @return null or the result from the database
	 * @author NoComment1105
	 * @author tempest15
	 */
	suspend fun selectInConfig(inputGuildId: String, inputColumn: String): String? {
		var selectedConfig: ConfigData?

		runBlocking {
			val collection = database.getCollection<ConfigData>()
			selectedConfig = collection.findOne(ConfigData::guildId eq inputGuildId)!!
		}

		return when (inputColumn) {
			"guildId" -> selectedConfig!!.guildId
			"moderatorsPing" -> selectedConfig!!.moderatorsPing
			"modActionLog" -> selectedConfig!!.modActionLog
			"messageLogs" -> selectedConfig!!.messageLogs
			"joinChannel" -> selectedConfig!!.joinChannel
			"supportChannel" -> selectedConfig!!.supportChannel
			"supportTeam" -> selectedConfig!!.supportTeam
			else -> null // todo check that returning null on an error works
		}
	}

	/**
	 * Adds the given [newConfig] to the database
	 *
	 * @param newConfig The new config values you want to set
	 * @author tempest15
	 */
	suspend fun putInConfig(newConfig: ConfigData) {
		runBlocking {
			val collection = database.getCollection<ConfigData>()
			collection.deleteOne(ConfigData:: guildId eq newConfig.guildId)
			collection.insertOne(newConfig)
		}
	}

	/**
	 * Clears the config for the provided [inputGuildId]
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 */
	suspend fun clearConfig(inputGuildId: String) {
		runBlocking {
			val collection = database.getCollection<ConfigData>()
			collection.deleteOne(ConfigData:: guildId eq inputGuildId)
		}

	}

	/**
	 * Gets the number of points the provided [inputUserId] has in the provided [inputGuildId] from the database
	 *
	 * @return null or the result from the database
	 * @param inputUserId The ID of the user to get the point value for
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 */
	suspend fun selectInWarn(inputUserId: String, inputGuildId: String): Int? {
		var selectedUserInGuild: WarnData?

		runBlocking {
			val collection = database.getCollection<WarnData>()
			selectedUserInGuild = collection.findOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
		}

		return if (selectedUserInGuild != null) {
			selectedUserInGuild!!.points
		} else {
			null
		}
	}

	/**
	 * Updates the number of points the provided [inputUserId] has in the provided [inputGuildId] in the database
	 *
	 * @param inputUserId The ID of the user to get the point value for
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 */
	suspend fun putInWarn(inputUserId: String, inputGuildId: String, inputPointValue: Int) {
		runBlocking {
			val collection = database.getCollection<WarnData>()
			collection.deleteOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
			collection.insertOne(WarnData(inputUserId, inputGuildId, inputPointValue))
		}
	}

	/**
	 * Using the provided [inputComponentId] and [inputColumn] a value or null will be returned from the database
	 *
	 * @param inputComponentId The ID of the component the event was triggered with
	 * @param inputColumn The config value associated with that component that you want
	 * @return null or the result from the database
	 * @author tempest15
	 */
	suspend fun selectInComponents(inputComponentId: String, inputColumn: String): String? {
		var selectedComponent: ComponentData?
		runBlocking {
			val collection = database.getCollection<ComponentData>()
			selectedComponent = collection.findOne(ComponentData:: componentId eq inputComponentId)
		}

		return when (inputColumn) {
			"componentId" -> selectedComponent!!.componentId
			"roleId" -> selectedComponent!!.roleId
			"addOrRemove" -> selectedComponent!!.addOrRemove
			else -> null  // todo check that returning null on an error works
		}
	}

	/**
	 * Add the given [newComponent] to the database
	 *
	 * @param [newComponent] The data for the component to be added
	 * @author tempest15
	 */
	suspend fun putInComponents(newComponent: ComponentData) {
		val collection = database.getCollection<ComponentData>()
		collection.deleteOne(ComponentData:: componentId eq newComponent.componentId)
		collection.insertOne(newComponent)
	}

	/**
	 * Add the given [newStatus] to the database
	 *
	 * @param [newStatus] The new status you wish to set
	 * @author NoComment1105
	 */
	suspend fun putInStatus(newStatus: String) {
		runBlocking {
			val collection = database.getCollection<StatusData>()
			collection.deleteOne(StatusData::status eq newStatus)
			collection.insertOne(StatusData(newStatus))
		}
	}

	fun getStatus(): String? {
		var currentStatus: StatusData?
		runBlocking {
			val collection = database.getCollection<StatusData>()
			currentStatus = collection.findOne("status")
		}
		return if (currentStatus != null) {
			currentStatus!!.status
		} else null
	}
}

//todo switch literally every data type here from string to something that makes more sense

data class ConfigData (
	val guildId: String,
	val moderatorsPing: String,
	val modActionLog: String,
	val messageLogs: String,
	val joinChannel: String,
	val supportChannel: String?,
	val supportTeam: String?,
)

data class WarnData (
	val userId: String,
	val guildId: String,
	val points: Int
)

data class ComponentData (
	val componentId: String,
	val roleId: String,
	val addOrRemove: String
)

data class StatusData (
	val status: String
)
