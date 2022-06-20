package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database.tables.ModerationConfigData
import org.litote.kmongo.eq

// TODO KDocs
object ModerationConfigDatabase {
	suspend inline fun getModerationConfig(inputGuildId: Snowflake): ModerationConfigData? {
		val collection = configDatabase.getCollection<ModerationConfigData>()
		return collection.findOne(ModerationConfigData::guildId eq inputGuildId)
	}

	suspend inline fun setModerationConfig(moderationConfig: ModerationConfigData) {
		val collection = configDatabase.getCollection<ModerationConfigData>()
		collection.deleteOne(ModerationConfigData::guildId eq moderationConfig.guildId)
		collection.insertOne(moderationConfig)
	}

	// TODO Removal/Clear
}
