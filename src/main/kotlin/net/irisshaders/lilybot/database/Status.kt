package net.irisshaders.lilybot.database

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

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

object StatusDatabase {
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
	suspend inline fun setStatus(newStatus: String) {
		val collection = database.getCollection<StatusData>()
		collection.deleteOne(StatusData::key eq "LilyStatus")
		collection.insertOne(StatusData("LilyStatus", newStatus))
	}
}
