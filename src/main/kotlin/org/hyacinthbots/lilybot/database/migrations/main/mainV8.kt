package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV8(db: CoroutineDatabase) {
	db.createCollection("roleSubscriptionData")
}
