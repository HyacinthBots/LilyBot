package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData

suspend fun mainV6(db: MongoDatabase) {
	with(db.getCollection<AutoThreadingData>("autoThreadingData")) {
		updateMany(
			Filters.exists(AutoThreadingData::addModsAndRole.name, false),
			Updates.set(AutoThreadingData::addModsAndRole.name, false)
		)
	}
}
