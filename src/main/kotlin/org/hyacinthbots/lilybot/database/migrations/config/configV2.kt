package org.hyacinthbots.lilybot.database.migrations.config

import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

suspend fun configV2(db: CoroutineDatabase) {
	with(db.getCollection<ModerationConfigData>("moderationConfigData")) {
		updateMany(
			ModerationConfigData::quickTimeoutLength exists false,
			setValue(ModerationConfigData::quickTimeoutLength, null)
		)
	}
}
