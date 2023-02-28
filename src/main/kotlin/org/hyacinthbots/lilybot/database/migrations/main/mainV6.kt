package org.hyacinthbots.lilybot.database.migrations.main

import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

suspend fun mainV6(db: CoroutineDatabase) {
	with(db.getCollection<AutoThreadingData>()) {
		updateMany(AutoThreadingData::addModsAndRole exists false, setValue(AutoThreadingData::addModsAndRole, false))
	}
}
