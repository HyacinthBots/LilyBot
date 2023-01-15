package org.hyacinthbots.lilybot.database.migrations.main

import org.hyacinthbots.lilybot.database.entities.SupportConfigData
import org.hyacinthbots.lilybot.database.entities.ThreadData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

@Suppress("UnusedPrivateMember")
suspend fun mainV5(db: CoroutineDatabase, configDb: CoroutineDatabase) {
	db.createCollection("autoThreadingData")

	with(db.getCollection<ThreadData>()) {
		updateMany(ThreadData::parentChannelId exists false, setValue(ThreadData::parentChannelId, null))
	}

	with(configDb.getCollection<SupportConfigData>()) {
		for (it in find().toList()) {
			if (it.channel == null) continue
			// THIS MIGRATION IS COMPLETE. AWAY WITH THIS CODE IS FINE
// 			db.getCollection<AutoThreadingData>().insertOne(
// 				AutoThreadingData(
// 					it.guildId,
// 					it.channel,
// 					it.role,
// 					preventDuplicates = true,
// 					archive = false,
// 					contentAwareNaming = false,
// 					mention = true,
// 					it.message
// 				)
// 			)
			configDb.getCollection<SupportConfigData>().deleteOne(SupportConfigData::guildId eq it.guildId)
		}
	}
}
