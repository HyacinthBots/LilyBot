package org.hyacinthbots.lilybot.database.migrations.main

import org.litote.kmongo.coroutine.CoroutineDatabase

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun mainV8(db: CoroutineDatabase, configDb: CoroutineDatabase) {
	db.createCollection("roleSubscriptionData")
}
