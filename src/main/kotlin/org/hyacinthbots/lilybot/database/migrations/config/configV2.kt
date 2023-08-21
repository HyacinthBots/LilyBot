package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun configV2(db: MongoDatabase, tempKeDb: MongoDatabase) {
	with(db.getCollection<ModerationConfigData>("moderationConfigData")) {
		updateMany(
			Filters.exists(ModerationConfigData::quickTimeoutLength.name, false),
			Updates.set(ModerationConfigData::quickTimeoutLength.name, null)
		)

		updateMany(
			Filters.exists(ModerationConfigData::autoPunishOnWarn.name, false),
			Updates.set(ModerationConfigData::autoPunishOnWarn.name, null)
		)
	}
}
