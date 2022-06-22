package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.configDatabase
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

object LoggingConfig {
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

object ModerationConfig {
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

object SupportConfig {
	suspend inline fun getSupportConfig(inputGuildId: Snowflake): SupportConfigData? {
		val collection = configDatabase.getCollection<SupportConfigData>()
		return collection.findOne(SupportConfigData::guildId eq inputGuildId)
	}

	/**
	 * Adds the given [supportConfig] to the database.
	 * @param supportConfig The new config values for the support module you want to set.
	 * @author Miss Corruption
	 * @since 4.0.0
	 */
	suspend inline fun setSupportConfig(supportConfig: SupportConfigData) {
		val collection = configDatabase.getCollection<SupportConfigData>()
		collection.deleteOne(SupportConfigData::guildId eq supportConfig.guildId)
		collection.insertOne(supportConfig)
	}

	// TODO Removal/Clear
}
