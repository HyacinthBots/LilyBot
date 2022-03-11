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
			else -> null // todo fix error processing (Nocomment switched to null to fix a todo elsewhere :> )
		}
	}

	/**
	 * A new config based on the given [newConfig] will be set for the provided [inputGuildId]
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @param newConfig The new config values you want to set
	 * @author tempest15
	 */
	suspend fun setConfig(inputGuildId: String, newConfig: ConfigData) {
		runBlocking {
			val collection = database.getCollection<ConfigData>()
			collection.deleteOne(ConfigData:: guildId eq inputGuildId)
			collection.insertOne(newConfig) // todo this might not actually override old ones
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
		var selectedUserInGuild: WarnData?

		runBlocking {
			val collection = database.getCollection<WarnData>()
			selectedUserInGuild = collection.findOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
			collection.deleteOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
			collection.insertOne(WarnData(inputUserId, inputGuildId, inputPointValue))
		}
	}
}

//todo switch literally every data type here from string to something that makes more sense

data class ConfigData (
	val guildId: String,
	val moderatorsPing: String,
	val modActionLog: String,
	val messageLogs: String,
	val joinChannel: String,
	val supportChannel: String,
	val supportTeam: String,
)

data class WarnData (val userId: String, val guildId: String, val points: Int)

data class ComponentsData (val componentId: String, val roleId: String, val addOrRemove: String)

data class StatusData (val status: String)
