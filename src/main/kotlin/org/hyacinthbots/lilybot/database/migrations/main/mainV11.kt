package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

suspend fun mainV11(db: CoroutineDatabase) {
	db.createCollection("leftMemberFlagData")
}
