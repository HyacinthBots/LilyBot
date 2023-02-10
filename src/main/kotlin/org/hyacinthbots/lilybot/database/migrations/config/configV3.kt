package org.hyacinthbots.lilybot.database.migrations.config

import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun configV3(db: CoroutineDatabase, mainDb: CoroutineDatabase) {
	with(db.getCollection<LoggingConfigData>("loggingConfigData")) {
		updateMany(
			LoggingConfigData::enablePublicMemberLogs exists false,
			setValue(LoggingConfigData::enablePublicMemberLogs, false)
		)
		updateMany(
			LoggingConfigData::publicMemberLog exists false,
			setValue(LoggingConfigData::publicMemberLog, null)
		)
		updateMany(
			LoggingConfigData::publicMemberLogData exists false,
			setValue(LoggingConfigData::publicMemberLogData, null)
		)
	}
}
