package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase

suspend fun mainV4(db: MongoDatabase) {
	db.createCollection("welcomeChannelData")
	db.createCollection("githubData")
}
