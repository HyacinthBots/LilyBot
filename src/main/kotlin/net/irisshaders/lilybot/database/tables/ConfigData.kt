package net.irisshaders.lilybot.database.tables

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

// TODO Kdoc
data class ConfigData(
	val loggingConfig: LoggingConfigData,
	val moderationConfig: ModerationConfigData,
	val supportConfig: SupportConfigData
) {
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
}
