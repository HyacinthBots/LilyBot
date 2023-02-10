package org.hyacinthbots.lilybot.database.migrations.main

import org.hyacinthbots.lilybot.database.entities.ThreadData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun mainV2(db: CoroutineDatabase, configDb: CoroutineDatabase) {
	with(db.getCollection<ThreadData>()) {
		updateMany(ThreadData::guildId exists false, setValue(ThreadData::guildId, null))
	}
}
