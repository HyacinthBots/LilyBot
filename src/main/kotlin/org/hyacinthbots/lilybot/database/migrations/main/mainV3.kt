package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun mainV3(db: CoroutineDatabase, configDb: CoroutineDatabase) {
	db.dropCollection("remindMeData")
	db.createCollection("reminderData")
}
