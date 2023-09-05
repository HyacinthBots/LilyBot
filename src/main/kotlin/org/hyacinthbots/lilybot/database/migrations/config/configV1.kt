package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData

suspend fun configV1(configDb: MongoDatabase) {
	with(configDb.getCollection<LoggingConfigData>(LoggingConfigData.name)) {
		updateMany(
			Filters.exists(LoggingConfigData::enableMessageEditLogs.name, false),
			Updates.set(LoggingConfigData::enableMessageEditLogs.name, false)
		)
	}

	configDb.getCollection<LoggingConfigData>(LoggingConfigData.name).updateMany(
		Filters.empty(),
		Updates.rename("enableMessageLogs", "enableMessageDeleteLogs")
	)
}
