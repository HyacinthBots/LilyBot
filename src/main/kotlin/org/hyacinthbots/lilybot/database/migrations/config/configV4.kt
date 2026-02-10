package org.hyacinthbots.lilybot.database.migrations.config

import org.litote.kmongo.coroutine.CoroutineDatabase

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER", "RedundantSuspendModifier")
suspend fun configV4(db: CoroutineDatabase) {
    // Support config has been removed.
// 	if (db.getCollection<SupportConfigData>().find().toList().isEmpty()) {
// 		db.dropCollection("supportConfigData")
// 	} else {
// 		utilsLogger.warn { "Support database was not empty!" }
// 	}
}
