package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database.tables.LoggingConfigData
import org.litote.kmongo.eq

// TODO KDoc
object LoggingConfigDatabase {
	suspend inline fun getLoggingConfig(inputGuildId: Snowflake): LoggingConfigData? {
		val collection = configDatabase.getCollection<LoggingConfigData>()
		return collection.findOne(LoggingConfigData::guildId eq inputGuildId)
	}

	suspend inline fun setLoggingConfig(loggingConfig: LoggingConfigData) {
		val collection = configDatabase.getCollection<LoggingConfigData>()
		collection.deleteOne(LoggingConfigData::guildId eq loggingConfig.guildId)
		collection.insertOne(loggingConfig)
	}

	// TODO Removal/Clear
}
