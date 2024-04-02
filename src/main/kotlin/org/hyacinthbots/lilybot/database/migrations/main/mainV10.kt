package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV10(db: CoroutineDatabase) {
	db.createCollection("temporaryBanData")
}
