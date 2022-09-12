package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.CleanupsData
import org.koin.core.component.inject
import org.litote.kmongo.eq

class CleanupsCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<CleanupsData>()

	/**
	 * Sets the time to run the cleanup code. Stored so the time persists across restarts.
	 *
	 * @param cleanUpData The data for when to run the cleanups
	 */
	suspend inline fun setCleanupTime(cleanUpData: CleanupsData) {
		collection.deleteMany(CleanupsData::id eq "cleanups")
		collection.insertOne(cleanUpData)
	}

	suspend inline fun getCleanupTime() = collection.findOne(CleanupsData::id eq "cleanups")
}
