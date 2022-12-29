package org.hyacinthbots.lilybot.database.migrations.main

import org.hyacinthbots.lilybot.database.entities.ThreadData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

suspend fun mainV5(db: CoroutineDatabase) {
	with(db.getCollection<ThreadData>()) {
		updateMany(ThreadData::parentChannelId exists false, setValue(ThreadData::parentChannelId, null))
	// todo rewrite to replace successfully
	}
}
