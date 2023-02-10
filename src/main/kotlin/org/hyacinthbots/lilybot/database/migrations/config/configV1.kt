package org.hyacinthbots.lilybot.database.migrations.config

import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.exists
import org.litote.kmongo.setValue

@Suppress("UnusedPrivateMember", "UNUSED_PARAMETER")
suspend fun configV1(configDb: CoroutineDatabase, mainDb: CoroutineDatabase) {
	with(configDb.getCollection<LoggingConfigData>("loggingConfigData")) {
		updateMany(
			LoggingConfigData::enableMessageEditLogs exists false,
			setValue(LoggingConfigData::enableMessageEditLogs, false)
		)
	}

	configDb.getCollection<LoggingConfigData>("loggingConfigData").updateMany(
		"{}",
		"{\$rename: {enableMessageLogs: \"enableMessageDeleteLogs\"}}"
	)
}
