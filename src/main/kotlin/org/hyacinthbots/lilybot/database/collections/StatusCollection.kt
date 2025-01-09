package org.hyacinthbots.lilybot.database.collections

import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.StatusData
import org.hyacinthbots.lilybot.extensions.moderation.commands.ModUtilities
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
	internal val collection = db.mainDatabase.getCollection<StatusData>()

	/**
	 * Gets Lily's status from the database.
	 *
	 * @return null or the set status in the database.
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun getStatus(): StatusData? = collection.findOne()

	/**
	 * Add the given [newStatus] to the database.
	 *
	 * @param statusType The [ModUtilities.PresenceType] for the new presence
	 * @param newStatus The new status you wish to set
	 * @author NoComment1105
	 * @since 3.0.0
	 */
	suspend inline fun setStatus(statusType: ModUtilities.PresenceType?, newStatus: String?) {
		collection.deleteOne()
		collection.insertOne(StatusData(statusType?.readableName?.key, newStatus))
	}
}
