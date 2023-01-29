package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

@Suppress("UnusedPrivateMember")
suspend fun mainV7(db: CoroutineDatabase, configDb: CoroutineDatabase) {
	db.createCollection("newsChannelPublishingData")
}
