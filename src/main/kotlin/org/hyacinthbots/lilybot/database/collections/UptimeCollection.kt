package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import kotlinx.datetime.Instant
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.UptimeData
import org.koin.core.component.inject
import org.litote.kmongo.ne
import org.litote.kmongo.setValue

/**
 * Stores the functions for interacting with [the uptime database][UptimeData]. This class contains the functions for
 * getting, setting and updating the data
 *
 * @see get
 * @see set
 * @see update
 * @since 4.2.0
 */
class UptimeCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<UptimeData>()

	/**
	 * Gets the uptime data from the database.
	 *
	 * @return the main metadata
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun get(): UptimeData? =
		collection.findOne()

	/**
	 * Sets the on time.
	 *
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun set(onTime: Instant) =
		collection.insertOne(UptimeData(onTime))

	/**
	 * Updates the uptime data in the database with the new [data][UptimeData].
	 *
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun update(onTime: Instant) =
		collection.updateOne(UptimeData::onTime ne null, setValue(UptimeData::onTime, onTime))
}
