package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database.tables.ConfigData
import org.litote.kmongo.eq

// TODO KDoc
object ConfigDatabase {
	object LoggingConfig {
		suspend inline fun getLoggingConfig(inputGuildId: Snowflake): ConfigData.LoggingConfigData? {
			val collection = configDatabase.getCollection<ConfigData.LoggingConfigData>()
			return collection.findOne(ConfigData.LoggingConfigData::guildId eq inputGuildId)
		}

		suspend inline fun setLoggingConfig(loggingConfig: ConfigData.LoggingConfigData) {
			val collection = configDatabase.getCollection<ConfigData.LoggingConfigData>()
			collection.deleteOne(ConfigData.LoggingConfigData::guildId eq loggingConfig.guildId)
			collection.insertOne(loggingConfig)
		}

		// TODO Removal/Clear
	}

	object ModerationConfig {
		suspend inline fun getModerationConfig(inputGuildId: Snowflake): ConfigData.ModerationConfigData? {
			val collection = configDatabase.getCollection<ConfigData.ModerationConfigData>()
			return collection.findOne(ConfigData.ModerationConfigData::guildId eq inputGuildId)
		}

		suspend inline fun setModerationConfig(moderationConfig: ConfigData.ModerationConfigData) {
			val collection = configDatabase.getCollection<ConfigData.ModerationConfigData>()
			collection.deleteOne(ConfigData.ModerationConfigData::guildId eq moderationConfig.guildId)
			collection.insertOne(moderationConfig)
		}

		// TODO Removal/Clear
	}

	object SupportConfig {
		suspend inline fun getSupportConfig(inputGuildId: Snowflake): ConfigData.SupportConfigData? {
			val collection = configDatabase.getCollection<ConfigData.SupportConfigData>()
			return collection.findOne(ConfigData.SupportConfigData::guildId eq inputGuildId)
		}

		/**
		 * Adds the given [supportConfig] to the database.
		 * @param supportConfig The new config values for the support module you want to set.
		 * @author Miss Corruption
		 * @since 4.0.0
		 */
		suspend inline fun setSupportConfig(supportConfig: ConfigData.SupportConfigData) {
			val collection = configDatabase.getCollection<ConfigData.SupportConfigData>()
			collection.deleteOne(ConfigData.SupportConfigData::guildId eq supportConfig.guildId)
			collection.insertOne(supportConfig)
		}

		// TODO Removal/Clear
	}
}
