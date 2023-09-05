package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.NewsChannelPublishingData

suspend fun mainV7(db: MongoDatabase) {
	db.createCollection(NewsChannelPublishingData.name)
}
