package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData

suspend fun configV6(db: MongoDatabase) {
	with(db.getCollection<ModerationConfigData>(ModerationConfigCollection.name)) {
		updateMany(
			Filters.exists(ModerationConfigData::banDmMessage.name, false),
			Updates.set(ModerationConfigData::banDmMessage.name, null)
		)
	}
}
