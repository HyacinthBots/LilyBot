package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData

suspend fun configV2(db: MongoDatabase) {
	with(db.getCollection<ModerationConfigData>(ModerationConfigData.name)) {
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
