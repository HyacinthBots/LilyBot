package net.irisshaders.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database.entities.LoggingConfigData
import net.irisshaders.lilybot.database.entities.ModerationConfigData
import net.irisshaders.lilybot.database.entities.SupportConfigData
import org.litote.kmongo.eq

/**
 * This object contains the functions or interacting with the [Logging Config Database][LoggingConfigData]. This object
 * contains functions for getting, setting and removing logging config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
class LoggingConfigCollection {
	suspend inline fun getConfig(inputGuildId: Snowflake): LoggingConfigData? {
		val collection = configDatabase.getCollection<LoggingConfigData>()
		return collection.findOne(LoggingConfigData::guildId eq inputGuildId)
	}

	suspend inline fun setConfig(loggingConfig: LoggingConfigData) {
		val collection = configDatabase.getCollection<LoggingConfigData>()
		collection.deleteOne(LoggingConfigData::guildId eq loggingConfig.guildId)
		collection.insertOne(loggingConfig)
	}

	suspend inline fun clearConfig(inputGuildId: Snowflake) {
		val collection = configDatabase.getCollection<LoggingConfigData>()
		collection.deleteOne(LoggingConfigData::guildId eq inputGuildId)
	}
}

/**
 * This object contains the functions or interacting with the [Moderation Config Database][ModerationConfigData]. This
 * object contains functions for getting, setting and removing logging config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
class ModerationConfigCollection {
	suspend inline fun getConfig(inputGuildId: Snowflake): ModerationConfigData? {
		val collection = configDatabase.getCollection<ModerationConfigData>()
		return collection.findOne(ModerationConfigData::guildId eq inputGuildId)
	}

	suspend inline fun setConfig(moderationConfig: ModerationConfigData) {
		val collection = configDatabase.getCollection<ModerationConfigData>()
		collection.deleteOne(ModerationConfigData::guildId eq moderationConfig.guildId)
		collection.insertOne(moderationConfig)
	}

	suspend inline fun clearConfig(inputGuildId: Snowflake) {
		val collection = configDatabase.getCollection<ModerationConfigData>()
		collection.deleteOne(ModerationConfigData::guildId eq inputGuildId)
	}
}

/**
 * This object contains the functions or interacting with the [Support Config Database][SupportConfigData]. This object
 * contains functions for getting, setting and removing support config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
class SupportConfigCollection {
	suspend inline fun getConfig(inputGuildId: Snowflake): SupportConfigData? {
		val collection = configDatabase.getCollection<SupportConfigData>()
		return collection.findOne(SupportConfigData::guildId eq inputGuildId)
	}

	/**
	 * Adds the given [supportConfig] to the database.
	 * @param supportConfig The new config values for the support module you want to set.
	 * @author Miss Corruption
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(supportConfig: SupportConfigData) {
		val collection = configDatabase.getCollection<SupportConfigData>()
		collection.deleteOne(SupportConfigData::guildId eq supportConfig.guildId)
		collection.insertOne(supportConfig)
	}

	suspend inline fun clearConfig(inputGuildId: Snowflake) {
		val collection = configDatabase.getCollection<SupportConfigData>()
		collection.deleteOne(SupportConfigData::guildId eq inputGuildId)
	}
}
