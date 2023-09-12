package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import kotlinx.coroutines.flow.firstOrNull
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.deleteOne
import org.hyacinthbots.lilybot.database.entities.StatusData
import org.koin.core.component.inject

/**
 * This class contains the functions for interacting with the [Status Database][StatusData]. This class contains the
 * function for getting and setting the status.
 *
 * @since 4.0.0
 * @see getStatus
 * @see setStatus
 */
class StatusCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<StatusData>(StatusData.name)

	/**
	 * Gets Lily's status from the database.
	 *
	 * @return null or the set status in the database.
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun getStatus(): String? =
		collection.find().firstOrNull()?.status

	/**
	 * Add the given [newStatus] to the database.
	 *
	 * @param newStatus The new status you wish to set
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun setStatus(newStatus: String?) {
		collection.deleteOne()
		collection.insertOne(StatusData(newStatus))
	}
}
