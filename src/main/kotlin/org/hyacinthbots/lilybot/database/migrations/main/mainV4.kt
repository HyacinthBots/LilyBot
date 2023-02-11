package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV4(db: CoroutineDatabase) {
	db.createCollection("welcomeChannelData")
	db.createCollection("githubData")
}
