package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.StatusData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object contains the functions for interacting with the [Status Database][StatusData]. This object contains the
 * function for getting and setting the status.
 *
 * @since 4.0.0
 * @see getStatus
 * @see setStatus
 */
class StatusCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<StatusData>()

	/**
	 * Gets Lily's status from the database.
	 *
	 * @return null or the set status in the database.
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun getStatus(): String =
		collection.findOne(StatusData::key eq "LilyStatus")?.status ?: "default"

	/**
	 * Add the given [newStatus] to the database.
	 *
	 * @param newStatus The new status you wish to set
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun setStatus(newStatus: String) {
		collection.deleteOne(StatusData::key eq "LilyStatus")
		collection.insertOne(StatusData("LilyStatus", newStatus))
	}
}
