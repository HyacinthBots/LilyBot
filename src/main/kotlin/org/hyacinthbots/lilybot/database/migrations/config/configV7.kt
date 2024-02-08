package org.hyacinthbots.lilybot.database.migrations.config

import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

suspend fun configV7(db: CoroutineDatabase) {
	with(db.getCollection<ModerationConfigData>("moderationConfigData")) {
		updateMany(
			ModerationConfigData::autoInviteModeratorRole exists false,
			setValue(ModerationConfigData::autoInviteModeratorRole, false)
		)
	}
}
