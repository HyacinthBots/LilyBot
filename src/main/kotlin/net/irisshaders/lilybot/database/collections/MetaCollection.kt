package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.ConfigMetaData
import net.irisshaders.lilybot.database.entities.MainMetaData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains all the functions for updating and retrieving data from the main meta database.
 *
 * @since 4.0.0
 */
class MainMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<MainMetaData>()

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
			MainMetaData::id eq "mainMeta",
			meta
		)
}

/**
 * This class contains all the functions for updating and retrieving data from the config meta database.
 *
 * @since 4.0.0
 */
class ConfigMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.configDatabase.getCollection<ConfigMetaData>()

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
			ConfigMetaData::id eq "configMeta",
			meta
		)
}
