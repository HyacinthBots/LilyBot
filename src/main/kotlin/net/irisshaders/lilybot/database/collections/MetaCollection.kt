package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.ConfigMetaData
import net.irisshaders.lilybot.database.entities.MainMetaData
import org.koin.core.component.inject
import org.litote.kmongo.eq

// TODO Kdoc

class MainMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<MainMetaData>()

	suspend fun get(): MainMetaData? =
		collection.findOne()

	suspend fun set(meta: MainMetaData) =
		collection.insertOne(meta)

	suspend fun update(meta: MainMetaData) =
		collection.findOneAndReplace(
			MainMetaData::id eq "mainMeta",
			meta
		)
}

class ConfigMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.configDatabase.getCollection<ConfigMetaData>()

	suspend fun get(): ConfigMetaData? =
		collection.findOne()

	suspend fun set(meta: ConfigMetaData) =
		collection.insertOne(meta)

	suspend fun update(meta: ConfigMetaData) =
		collection.findOneAndReplace(
			ConfigMetaData::id eq "configMeta",
			meta
		)
}
