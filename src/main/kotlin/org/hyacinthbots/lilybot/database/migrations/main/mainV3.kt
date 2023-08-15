package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase

suspend fun mainV3(db: MongoDatabase) {
	// db.getCollection<RemindMeData>("remindMeData")
	db.createCollection("reminderData")
}
