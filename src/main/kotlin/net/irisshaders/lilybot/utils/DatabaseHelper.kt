package net.irisshaders.lilybot.utils

import kotlinx.coroutines.runBlocking
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

object DatabaseHelper {

	/**
	 * Using the provided [guildId] and [column] a value or error will be returned from the
	 * config database
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @param inputColumn The config value associated with that guild that you want
	 * @return a [NoSuchElementException] or the result from the Database
	 * @author NoComment1105
	 * @author tempest15
	 */

	// todo write javadocs for all of these

	suspend fun selectInConfig(inputGuildId: String, inputColumn: String): String {
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
			else -> "Major error" // todo fix error processing
		}
	}

	suspend fun setConfig(inputGuildId: String, newConfig: ConfigData) {
		runBlocking {
			val collection = database.getCollection<ConfigData>()
			collection.deleteOne(ConfigData:: guildId eq inputGuildId)
			collection.insertOne(newConfig) // todo this might not actually override old ones
		}
	}

	suspend fun clearConfig(inputGuildId: String) {
		runBlocking {
			val collection = database.getCollection<ConfigData>()
			collection.deleteOne(ConfigData:: guildId eq inputGuildId)
		}

	}

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
