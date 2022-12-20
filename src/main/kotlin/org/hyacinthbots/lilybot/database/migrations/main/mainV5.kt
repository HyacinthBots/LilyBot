package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV5(db: CoroutineDatabase) {
	db.dropCollection("reminderData")
	db.createCollection("reminderData")
}
