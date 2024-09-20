package org.hyacinthbots.lilybot.database.migrations.config

import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

suspend fun configV7(db: CoroutineDatabase) {
	with(db.getCollection<ModerationConfigData>("moderationConfigData")) {
		updateMany(
			ModerationConfigData::autoInviteModeratorRole exists false,
			setValue(ModerationConfigData::autoInviteModeratorRole, null)
		)
		updateMany(
			ModerationConfigData::dmDefault exists false,
			setValue(ModerationConfigData::dmDefault, true)
		)
	}
	with(db.getCollection<UtilityConfigData>("utilityConfigData")) {
		updateMany(
			UtilityConfigData::logChannelUpdates exists false,
			setValue(UtilityConfigData::logChannelUpdates, false)
		)
		updateMany(
			UtilityConfigData::logEventUpdates exists false,
			setValue(UtilityConfigData::logEventUpdates, false)
		)
		updateMany(
			UtilityConfigData::logInviteUpdates exists false,
			setValue(UtilityConfigData::logInviteUpdates, false)
		)
		updateMany(
			UtilityConfigData::logRoleUpdates exists false,
			setValue(UtilityConfigData::logRoleUpdates, false)
		)
	}
}
