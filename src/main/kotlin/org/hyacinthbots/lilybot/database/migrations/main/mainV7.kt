package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.collections.NewsChannelPublishingCollection

suspend fun mainV7(db: MongoDatabase) {
	db.createCollection(NewsChannelPublishingCollection.name)
}
