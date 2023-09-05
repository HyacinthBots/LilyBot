package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.RoleSubscriptionData

suspend fun mainV8(db: MongoDatabase) {
	db.createCollection(RoleSubscriptionData.name)
}
