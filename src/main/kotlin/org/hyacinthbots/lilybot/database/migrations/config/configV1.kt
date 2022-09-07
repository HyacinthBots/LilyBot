@file:Suppress("DEPRECATION_ERROR")

// TODO :crab: after first migration

package org.hyacinthbots.lilybot.database.migrations.config

import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.entities.ConfigData
import org.hyacinthbots.lilybot.database.entities.LogUploadingData
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.database.entities.SupportConfigData
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.litote.kmongo.coroutine.CoroutineDatabase

// Due to the needing of ConfigData having to hang around deprecated until after the migration this function will
// have to be deleted and as such at the end of the function we reset the configMeta version, so that the migrations are
// reset until we need to do a new migration.
suspend fun configV1(mainDb: CoroutineDatabase, configDb: CoroutineDatabase) {
	configDb.createCollection("loggingConfigData")
	configDb.createCollection("moderationConfigData")
	configDb.createCollection("supportConfigData")
	configDb.createCollection("utilityConfigData")

	val oldLoggingData = mutableListOf<Snowflake>()
	val oldModerationData = mutableListOf<Snowflake>()
	val oldSupportData = mutableListOf<Snowflake?>()
	val oldLogUploadingData = mutableListOf<Any>()
	var guildId = Snowflake(0)

	val oldConfig = mainDb.getCollection<ConfigData>("configData")
	val oldLogUploadingConfig = mainDb.getCollection<LogUploadingData>("logUploadingData")

	val loggingConfig = configDb.getCollection<LoggingConfigData>("loggingConfigData")
	val moderationConfig = configDb.getCollection<ModerationConfigData>("moderationConfigData")
	val supportConfig = configDb.getCollection<SupportConfigData>("supportConfigData")
	val utilityConfig = configDb.getCollection<UtilityConfigData>("utilityConfigData")

	oldConfig.find().consumeEach {
		guildId = it.guildId
		oldLoggingData.add(0, it.messageLogs)
		oldLoggingData.add(1, it.joinChannel)
		oldModerationData.add(0, it.modActionLog)
		oldModerationData.add(1, it.moderatorsPing)
		oldSupportData.add(0, it.supportChannel)
		oldSupportData.add(1, it.supportTeam)
	}

	oldLogUploadingConfig.find().consumeEach {
		oldLogUploadingData.add(0, it.guildId)
		oldLogUploadingData.add(1, it.disable)
	}

	if (oldLoggingData.isNotEmpty()) {
		loggingConfig.insertOne(LoggingConfigData(guildId, true, oldLoggingData[0], true, oldLoggingData[1]))
	}
	if (oldModerationData.isNotEmpty()) {
		moderationConfig.insertOne(ModerationConfigData(guildId, true, oldModerationData[0], oldModerationData[1], false))
	}
	try {
		if (oldSupportData.first() != null) {
			supportConfig.insertOne(SupportConfigData(guildId, true, oldSupportData[0]!!, oldSupportData[1]!!, null))
		}
	} catch (e: NoSuchElementException) {
		supportConfig.insertOne(SupportConfigData(guildId, false, null, null, null))
	}
	if (oldLogUploadingData.isNotEmpty()) {
		utilityConfig.insertOne(
			UtilityConfigData(
				oldLogUploadingData[0] as Snowflake,
				oldLogUploadingData[1] as Boolean,
				null
			)
		)
	}

	mainDb.dropCollection("configData")
	mainDb.dropCollection("logUploadingData")
}