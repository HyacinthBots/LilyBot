package net.irisshaders.lilybot.database

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

/**
 * This object contains the functions for interacting with the [Status Database][StatusData]. This object contains the
 * function for getting and setting the status.
 *
 * @since 4.0.0
 * @see getStatus
 * @see setStatus
 */
object StatusDatabase {
	/**
	 * Gets Lily's status from the database.
	 *
	 * @return null or the set status in the database.
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun getStatus(): String {
		val collection = database.getCollection<StatusData>()
		return collection.findOne(StatusData::key eq "LilyStatus")?.status ?: "default"
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
