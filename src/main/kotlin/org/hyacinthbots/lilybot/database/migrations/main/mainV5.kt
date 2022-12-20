package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

// TODO delete post restart
suspend fun mainV5(db: CoroutineDatabase) {
	db.dropCollection("reminderData")
	db.createCollection("reminderData")
}
