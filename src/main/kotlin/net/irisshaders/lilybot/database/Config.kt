package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.configDatabase
import net.irisshaders.lilybot.database.LoggingConfig.clearConfig
import net.irisshaders.lilybot.database.LoggingConfig.getConfig
import net.irisshaders.lilybot.database.LoggingConfig.setConfig
import org.litote.kmongo.eq

/**
 * The data for moderation configuration.
 *
 * @param guildId The ID of the guild the config is for
 * @param enableMessageLogs If edited and deleted messages should be logged
 * @param messageChannel The channel to send message logs to
 * @param enableJoinLogs If joining and leaving users should be logged
 * @param joinChannel The channel to send join logs to
 * @since 4.0.0
 */
@Serializable
data class LoggingConfigData(
	val guildId: Snowflake,
	val enableMessageLogs: Boolean,
	val messageChannel: Snowflake,
	val enableJoinLogs: Boolean,
	val joinChannel: Snowflake,
)

/**
 * The data for moderation configuration.
 *
 * @param guildId The ID of the guild the config is for
 * @param enabled If the support module is enabled or not
 * @param channel The ID of the action log for the guild
 * @param team The ID of the moderation role for the guild
 * @since 4.0.0
 */
@Serializable
data class ModerationConfigData(
	val guildId: Snowflake,
	val enabled: Boolean,
	val channel: Snowflake,
	val team: Snowflake,
)

/**
 * The data for support configuration.
 *
 * @param guildId The ID of the guild the config is for
 * @param enabled If the support module is enabled or not
 * @param channel The ID of the support channel for the guild
 * @param team The ID of the support team for the guild
 * @param message The support message as a string, nullable
 * @since 4.0.0
 */
@Serializable
data class SupportConfigData(
	val guildId: Snowflake,
	val enabled: Boolean,
	val channel: Snowflake,
	val team: Snowflake,
	val message: String?
)

/**
 * This object contains the functions or interacting with the [Logging Config Database][LoggingConfigData]. This object
 * contains functions for getting, setting and removing logging config.
 *
 * @since 4.0.0
 * @see getConfig
 * @see setConfig
 * @see clearConfig
 */
object LoggingConfig {
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
object ModerationConfig {
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
object SupportConfig {
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
