@file:Suppress("DEPRECATION_ERROR")

package net.irisshaders.lilybot.database.migrations.config

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.entities.ConfigData
import net.irisshaders.lilybot.database.entities.ConfigMetaData
import net.irisshaders.lilybot.database.entities.LoggingConfigData
import net.irisshaders.lilybot.database.entities.ModerationConfigData
import net.irisshaders.lilybot.database.entities.SupportConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

// Due to the needing of ConfigData having to hang around deprecated until after the migration this function will
// have to be deleted and as such at the end of the function we reset the configMeta version, so that the migrations are
// reset until we need to do a new migration.
suspend fun configV1(mainDb: CoroutineDatabase, configDb: CoroutineDatabase) {
	configDb.createCollection("loggingConfigData")
	configDb.createCollection("moderationConfigData")
	configDb.createCollection("supportConfigData")

	val oldLoggingData = mutableListOf<Snowflake>()
	val oldModerationData = mutableListOf<Snowflake>()
	val oldSupportData = mutableListOf<Snowflake?>()
	var guildId = Snowflake(0)

	val oldConfig = mainDb.getCollection<ConfigData>("configData")
	val loggingConfig = configDb.getCollection<LoggingConfigData>("loggingConfigData")
	val moderationConfig = configDb.getCollection<ModerationConfigData>("moderationConfigData")
	val supportConfig = configDb.getCollection<SupportConfigData>("supportConfigData")

	oldConfig.find().consumeEach {
		guildId = it.guildId
		oldLoggingData.add(0, it.messageLogs)
		oldLoggingData.add(1, it.joinChannel)
		oldModerationData.add(0, it.modActionLog)
		oldModerationData.add(1, it.moderatorsPing)
		oldSupportData.add(0, it.supportChannel)
		oldSupportData.add(1, it.supportTeam)
	}

	if (oldLoggingData.isNotEmpty()) {
		loggingConfig.insertOne(LoggingConfigData(guildId, true, oldLoggingData[0], true, oldLoggingData[1]))
	}
	if (oldModerationData.isNotEmpty()) {
		moderationConfig.insertOne(ModerationConfigData(guildId, true, oldModerationData[0], oldModerationData[1]))
	}
	if (oldSupportData[0] != null) {
		supportConfig.insertOne(SupportConfigData(guildId, true, oldSupportData[0]!!, oldSupportData[1]!!, null))
	}

	mainDb.dropCollection("configData")

	configDb.getCollection<ConfigMetaData>().updateOne(ConfigMetaData::id eq "configMeta", ConfigMetaData(0))
}
