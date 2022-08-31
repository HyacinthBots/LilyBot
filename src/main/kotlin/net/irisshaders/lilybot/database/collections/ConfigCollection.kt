@file:Suppress("DEPRECATION_ERROR")

package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.LoggingConfigData
import net.irisshaders.lilybot.database.entities.MiscConfigData
import net.irisshaders.lilybot.database.entities.ModerationConfigData
import net.irisshaders.lilybot.database.entities.SupportConfigData
import org.koin.core.component.inject
import org.litote.kmongo.eq

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
	internal val collection = configDb.configDatabase.getCollection<LoggingConfigData>()

	/**
	 * Gets the logging config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The logging config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): LoggingConfigData? =
		collection.findOne(LoggingConfigData::guildId eq inputGuildId)

	/**
	 * Adds the given [loggingConfig] to the database.
	 *
	 * @param loggingConfig The new config values for the logging config you want to set.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(loggingConfig: LoggingConfigData) {
		collection.deleteOne(LoggingConfigData::guildId eq loggingConfig.guildId)
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
		collection.deleteOne(LoggingConfigData::guildId eq inputGuildId)
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
	internal val collection = configDb.configDatabase.getCollection<ModerationConfigData>()

	/**
	 * Gets the Moderation config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The moderation config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): ModerationConfigData? =
		collection.findOne(ModerationConfigData::guildId eq inputGuildId)

	/**
	 * Adds the given [moderationConfig] to the database.
	 *
	 * @param moderationConfig The new config values for the moderation config you want to set.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(moderationConfig: ModerationConfigData) {
		collection.deleteOne(ModerationConfigData::guildId eq moderationConfig.guildId)
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
		collection.deleteOne(ModerationConfigData::guildId eq inputGuildId)
}

/**
 * This class contains the functions for interacting with the [Support Config Database][SupportConfigData]. This class
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

	/**
	 * Gets the support config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The support config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): SupportConfigData? =
		collection.findOne(SupportConfigData::guildId eq inputGuildId)

	/**
	 * Adds the given [supportConfig] to the database.
	 *
	 * @param supportConfig The new config values for the support config you want to set.
	 * @author Miss Corruption
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(supportConfig: SupportConfigData) {
		collection.deleteOne(SupportConfigData::guildId eq supportConfig.guildId)
		collection.insertOne(supportConfig)
	}

	/**
	 * Clears the support config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to clear the config for.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(SupportConfigData::guildId eq inputGuildId)
}

/**
 * This class contains the functions for interacting with the [Miscellaneous Config Database][MiscConfigData].
 * This class contains functions for getting, setting and removing miscellaneous config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
class MiscConfigCollection : KordExKoinComponent {
	private val configDb: Database by inject()

	@PublishedApi
	internal val collection = configDb.configDatabase.getCollection<MiscConfigData>()

	/**
	 * Gets the Miscellaneous config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to get the config for.
	 * @return The Miscellaneous config for the given guild.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun getConfig(inputGuildId: Snowflake): MiscConfigData? =
		collection.findOne(MiscConfigData::guildId eq inputGuildId)

	/**
	 * Adds the given [miscellaneousConfig] to the database.
	 *
	 * @param miscellaneousConfig The new config values for the miscellaneous config you want to set.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun setConfig(miscellaneousConfig: MiscConfigData) {
		collection.deleteOne(MiscConfigData::guildId eq miscellaneousConfig.guildId)
		collection.insertOne(miscellaneousConfig)
	}

	/**
	 * Clears the miscellaneous config for the given guild using the [guildId][inputGuildId].
	 *
	 * @param inputGuildId The guild id to clear the config for.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun clearConfig(inputGuildId: Snowflake) =
		collection.deleteOne(MiscConfigData::guildId eq inputGuildId)
}
