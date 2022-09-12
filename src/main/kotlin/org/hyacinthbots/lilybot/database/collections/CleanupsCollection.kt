package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.CleanupsData
import org.koin.core.component.inject

/**
 * This class contains the functions for interacting with the [Cleanups Database][CleanupsData]. This
 * object contains functions for getting and setting cleanup time. Deleting old times in incorporated
 *
 * @since 4.1.0
 * @see setCleanupTime
 * @see getCleanupTime
 */
class CleanupsCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<CleanupsData>()

	/**
	 * Sets the time to run the cleanup code. Stored so the time persists across restarts.
	 *
	 * @param cleanUpData The data for when to run the cleanups
	 *
	 * @author NoComment1105
	 * @since 4.1.0
	 */
	suspend inline fun setCleanupTime(cleanUpData: CleanupsData) {
		collection.deleteOne()
		collection.insertOne(cleanUpData)
	}

	/**
	 * Gets the cleanup time, allowing the scheduler to check if it is time to run the cleanups.
	 *
	 * @return The cleanup times
	 *
	 * @author NoComment1105
	 * @since 4.1.0
	 */
	suspend inline fun getCleanupTime() = collection.findOne()
}
