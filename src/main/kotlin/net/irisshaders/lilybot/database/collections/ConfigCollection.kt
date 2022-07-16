@file:Suppress("DEPRECATION_ERROR")

package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.ConfigData
import net.irisshaders.lilybot.database.entities.LoggingConfigData
import net.irisshaders.lilybot.database.entities.ModerationConfigData
import net.irisshaders.lilybot.database.entities.SupportConfigData
import org.koin.core.component.inject
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
class LoggingConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.configDatabase.getCollection<LoggingConfigData>()

	suspend inline fun getConfig(inputGuildId: Snowflake): LoggingConfigData? =
		collection.findOne(LoggingConfigData::guildId eq inputGuildId)

	suspend inline fun setConfig(loggingConfig: LoggingConfigData) {
		collection.deleteOne(LoggingConfigData::guildId eq loggingConfig.guildId)
		collection.insertOne(loggingConfig)
	}

	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(LoggingConfigData::guildId eq inputGuildId)
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
class ModerationConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.configDatabase.getCollection<ModerationConfigData>()

	suspend inline fun getConfig(inputGuildId: Snowflake): ModerationConfigData? =
		collection.findOne(ModerationConfigData::guildId eq inputGuildId)

	suspend inline fun setConfig(moderationConfig: ModerationConfigData) {
		collection.deleteOne(ModerationConfigData::guildId eq moderationConfig.guildId)
		collection.insertOne(moderationConfig)
	}

	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(ModerationConfigData::guildId eq inputGuildId)
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
class SupportConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.configDatabase.getCollection<SupportConfigData>()

	suspend inline fun getConfig(inputGuildId: Snowflake): SupportConfigData? =
		collection.findOne(SupportConfigData::guildId eq inputGuildId)

	/**
	 * Adds the given [supportConfig] to the database.
	 * @param supportConfig The new config values for the support module you want to set.
	 * @author Miss Corruption
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(supportConfig: SupportConfigData) {
		collection.deleteOne(SupportConfigData::guildId eq supportConfig.guildId)
		collection.insertOne(supportConfig)
	}

	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(SupportConfigData::guildId eq inputGuildId)
}

class OldConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.mainDatabase.getCollection<ConfigData>()

	/**
	 * Using the provided [inputGuildId] the config for that guild  will be returned from the database.
	 *
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @return The config for [inputGuildId]
	 * @author NoComment1105, tempest15
	 * @since 3.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake) =
		collection.findOne(ConfigData::guildId eq inputGuildId)

	/**
	 * Adds the given [newConfig] to the database.
	 *
	 * @param newConfig The new config values you want to set.
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun setConfig(newConfig: ConfigData) {
		collection.deleteOne(ConfigData::guildId eq newConfig.guildId)
		collection.insertOne(newConfig)
	}

	/**
	 * Clears the config for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(ConfigData::guildId eq inputGuildId)
}
