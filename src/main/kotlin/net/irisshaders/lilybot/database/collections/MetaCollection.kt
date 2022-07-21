package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.ConfigMetaData
import net.irisshaders.lilybot.database.entities.MainMetaData
import org.koin.core.component.inject

class MainMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<MainMetaData>()

	suspend fun get(): MainMetaData? =
		collection.findOne()

	suspend fun set(meta: MainMetaData) =
		collection.save(meta)
}

class ConfigMetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.configDatabase.getCollection<ConfigMetaData>()

	suspend fun get(): ConfigMetaData? =
		collection.findOne()

	suspend fun set(meta: ConfigMetaData) =
		collection.save(meta)
}
