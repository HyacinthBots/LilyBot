package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.kotlin.client.coroutine.MongoDatabase

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER", "RedundantSuspendModifier")
suspend fun configV4(db: MongoDatabase, tempKeDb: MongoDatabase) {
	// Support config has been removed.
// 	if (db.getCollection<SupportConfigData>().find().toList().isEmpty()) {
// 		db.dropCollection("supportConfigData")
// 	} else {
// 		utilsLogger.warn { "Support database was not empty!" }
// 	}
}
