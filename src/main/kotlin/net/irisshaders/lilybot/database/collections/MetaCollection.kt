package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.MetaData
import org.koin.core.component.inject

class MetaCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<MetaData>()

	suspend fun get(): MetaData? =
		collection.findOne()

	suspend fun set(meta: MetaData) =
		collection.save(meta)
}
