package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData

suspend fun configV3(db: MongoDatabase) {
	with(db.getCollection<LoggingConfigData>("loggingConfigData")) {
		updateMany(
			Filters.exists(LoggingConfigData::enablePublicMemberLogs.name, false),
			Updates.set(LoggingConfigData::enablePublicMemberLogs.name, false)
		)
		updateMany(
			Filters.exists(LoggingConfigData::publicMemberLog.name, false),
			Updates.set(LoggingConfigData::publicMemberLog.name, null)
		)
		updateMany(
			Filters.exists(LoggingConfigData::publicMemberLogData.name, false),
			Updates.set(LoggingConfigData::publicMemberLogData.name, null)
		)
	}
}
