package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV12(db: CoroutineDatabase) {
	db.dropCollection("statusData")
	db.createCollection("statusData")
}
