package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase

suspend fun mainV8(db: MongoDatabase) {
	db.createCollection("roleSubscriptionData")
}
