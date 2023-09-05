package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.ThreadData

suspend fun mainV5(db: MongoDatabase) {
	// db.createCollection("autoThreadingData")

	with(db.getCollection<ThreadData>(ThreadData.name)) {
		updateMany(
			Filters.exists(ThreadData::parentChannelId.name, false),
			Updates.set(ThreadData::parentChannelId.name, null)
		)
	}

// 	with(configDb.getCollection<SupportConfigData>()) {
// 		for (it in find().toList()) {
// 			if (it.channel == null) continue
	// THIS MIGRATION IS COMPLETE. AWAY WITH THIS CODE IS FINE
// 			db.getCollection<AutoThreadingData>().insertOne(
// 				AutoThreadingData(
// 					it.guildId,
// 					it.channel,
// 					it.role,
// 					preventDuplicates = true,
// 					archive = false,
// 					contentAwareNaming = false,
// 					mention = true,
// 					it.message
// 				)
// 			)
// 			configDb.getCollection<SupportConfigData>().deleteOne(SupportConfigData::guildId eq it.guildId)
}
