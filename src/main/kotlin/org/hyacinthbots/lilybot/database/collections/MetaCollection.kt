package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.eq
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ConfigMetaData
import org.hyacinthbots.lilybot.database.entities.MainMetaData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject

/**
 * This class contains the functions for interacting with the [main meta database][MainMetaData]. This class
 * contains functions for getting, setting and updating meta.
 *
 * @since 4.0.0
 * @see get
 * @see set
 * @see update
 */
class MainMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<MainMetaData>("mainMetaData")

	/**
	 * Gets the main metadata from the database.
	 *
	 * @return the main metadata
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend fun get(): MainMetaData? =
		collection.findOne()

	/**
	 * Sets the metadata when the table is first created.
	 *
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend fun set(meta: MainMetaData) =
		collection.insertOne(meta)

	/**
	 * Updates the config metadata in the database with the new [meta][MainMetaData].
	 *
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend fun update(meta: MainMetaData) =
		collection.findOneAndReplace(
			eq(MainMetaData::id.name, "mainMeta"),
			meta
		)
}

/**
 * This class contains the functions for interacting with the [config meta database][ConfigMetaData]. This class
 * contains functions for getting, setting and updating meta.
 *
 * @since 4.0.0
 * @see get
 * @see set
 * @see update
 */
class ConfigMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.configDatabase.getCollection<ConfigMetaData>("configMetaData")

	/**
	 * Gets the config metadata from the database.
	 *
	 * @return the config metadata
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend fun get(): ConfigMetaData? =
		collection.findOne()

	/**
	 * Sets the metadata when the table is first created.
	 *
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend fun set(meta: ConfigMetaData) =
		collection.insertOne(meta)

	/**
	 * Updates the config metadata in the database with the new [meta][ConfigMetaData].
	 *
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend fun update(meta: ConfigMetaData) =
		collection.findOneAndReplace(
			eq(ConfigMetaData::id.name, "configMeta"),
			meta
		)
}
