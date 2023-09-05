package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.collections.RoleSubscriptionCollection

suspend fun mainV8(db: MongoDatabase) {
	db.createCollection(RoleSubscriptionCollection.name)
}
