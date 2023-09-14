package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.ThreadData

suspend fun mainV2(db: MongoDatabase) {
	with(db.getCollection<ThreadData>(ThreadData.name)) {
		updateMany(Filters.exists(ThreadData::guildId.name, false), Updates.set(ThreadData::guildId.name, null))
	}
}
