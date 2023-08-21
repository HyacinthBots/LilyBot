package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun configV1(configDb: MongoDatabase, tempKeDb: MongoDatabase) {
	with(configDb.getCollection<LoggingConfigData>("loggingConfigData")) {
		updateMany(
			Filters.exists(LoggingConfigData::enableMessageEditLogs.name, false),
			Updates.set(LoggingConfigData::enableMessageEditLogs.name, false)
		)
	}

	configDb.getCollection<LoggingConfigData>("loggingConfigData").updateMany(
		Filters.empty(),
		Updates.rename("enableMessageLogs", "enableMessageDeleteLogs")
	)
}
