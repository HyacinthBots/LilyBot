package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Collection
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject

/**
 * This class contains the functions for interacting with the [Logging Config Database][LoggingConfigData]. This class
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
	internal val collection = configDb.configDatabase.getCollection<LoggingConfigData>(name)

	/**
	 * Gets the logging config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The logging config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): LoggingConfigData? =
		collection.findOne(eq(LoggingConfigData::guildId.name, inputGuildId))

	/**
	 * Adds the given [loggingConfig] to the database.
	 *
	 * @param loggingConfig The new config values for the logging config you want to set.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(loggingConfig: LoggingConfigData) {
		collection.deleteOne(eq(LoggingConfigData::guildId.name, loggingConfig.guildId))
		collection.insertOne(loggingConfig)
	}

	/**
	 * Clears the logging config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to clear the config for.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(eq(LoggingConfigData::guildId.name, inputGuildId))

	companion object : Collection("loggingConfigData")
}

/**
 * This class contains the functions for interacting with the [Moderation Config Database][ModerationConfigData]. This
 * class contains functions for getting, setting and removing logging config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
class ModerationConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.configDatabase.getCollection<ModerationConfigData>(name)

	/**
	 * Gets the Moderation config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The moderation config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): ModerationConfigData? =
		collection.findOne(eq(ModerationConfigData::guildId.name, inputGuildId))

	/**
	 * Adds the given [moderationConfig] to the database.
	 *
	 * @param moderationConfig The new config values for the moderation config you want to set.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(moderationConfig: ModerationConfigData) {
		collection.deleteOne(eq(ModerationConfigData::guildId.name, moderationConfig.guildId))
		collection.insertOne(moderationConfig)
	}

	/**
	 * Clears the moderation config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to clear the config for.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(eq(ModerationConfigData::guildId.name, inputGuildId))

	companion object : Collection("moderationConfigData")
}

/**
 * This class contains the functions for interacting with the [Utility Config Database][UtilityConfigData].
 * This class contains functions for getting, setting and removing Utility config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
class UtilityConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.configDatabase.getCollection<UtilityConfigData>(name)

	/**
	 * Gets the Utility config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The Utility config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): UtilityConfigData? =
		collection.findOne(eq(UtilityConfigData::guildId.name, inputGuildId))

	/**
	 * Adds the given [utilityConfig] to the database.
	 *
	 * @param utilityConfig The new config values for the Utility config you want to set.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(utilityConfig: UtilityConfigData) {
		collection.deleteOne(eq(UtilityConfigData::guildId.name, utilityConfig.guildId))
		collection.insertOne(utilityConfig)
	}

	/**
	 * Clears the Utility config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to clear the config for.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(eq(UtilityConfigData::guildId.name, inputGuildId))

	companion object : Collection("utilityConfigData")
}
