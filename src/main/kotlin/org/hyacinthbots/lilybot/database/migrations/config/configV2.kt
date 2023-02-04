package org.hyacinthbots.lilybot.database.migrations.config

import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun configV2(db: CoroutineDatabase, mainDb: CoroutineDatabase) {
	with(db.getCollection<ModerationConfigData>("moderationConfigData")) {
		updateMany(
			ModerationConfigData::quickTimeoutLength exists false,
			setValue(ModerationConfigData::quickTimeoutLength, null)
		)

		updateMany(
			ModerationConfigData::autoPunishOnWarn exists false,
			setValue(ModerationConfigData::autoPunishOnWarn, null)
		)
	}
}
