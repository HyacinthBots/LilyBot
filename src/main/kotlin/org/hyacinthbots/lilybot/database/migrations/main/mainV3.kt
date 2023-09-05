package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.ReminderData

suspend fun mainV3(db: MongoDatabase) {
	// db.getCollection<RemindMeData>("remindMeData")
	db.createCollection(ReminderData.name)
}
