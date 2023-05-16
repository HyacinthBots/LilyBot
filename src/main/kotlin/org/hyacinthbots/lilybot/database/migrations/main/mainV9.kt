package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV9(db: CoroutineDatabase) {
	db.dropCollection("logUploadingBlacklistData")
}
