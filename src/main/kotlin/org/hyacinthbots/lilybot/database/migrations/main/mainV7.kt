package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase

suspend fun mainV7(db: MongoDatabase) {
	db.createCollection("newsChannelPublishingData")
}
