package net.irisshaders.lilybot.database.collections

import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.entities.MetaData

class MetaCollection {
	suspend fun get(): MetaData? {
		val collection = database.getCollection<MetaData>()
		return collection.findOne()
	}

	suspend fun set(meta: MetaData) {
		val collection = database.getCollection<MetaData>()
		collection.save(meta)
	}
}
