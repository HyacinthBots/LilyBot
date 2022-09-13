package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for moderation configuration. The logging config stores where logs are sent to, and whether to enable or
 * disable certain configurations.
 *
 * @param guildId The ID of the guild the config is for
 * @param enableMessageLogs If edited and deleted messages should be logged
 * @param messageChannel The channel to send message logs to
 * @param enableMemberLogs If users joining or leaving the guild should be logged
 * @param memberLog The channel to send member logs to
 * @since 4.0.0
 */
@Serializable
data class LoggingConfigData(
	val guildId: Snowflake,
	val enableMessageLogs: Boolean,
	val messageChannel: Snowflake?,
	val enableMemberLogs: Boolean,
	val memberLog: Snowflake?,
)

/**
 * The data for moderation configuration. The moderation config is what stores the data for moderation actions. The
 * channel for logging and the team for pinging.
 *
 * @param guildId The ID of the guild the config is for
 * @param enabled If the support module is enabled or not
 * @param channel The ID of the action log for the guild
 * @param role The ID of the moderation role for the guild
 * @since 4.0.0
 */
@Serializable
data class ModerationConfigData(
	val guildId: Snowflake,
	val enabled: Boolean,
	val channel: Snowflake?,
	val role: Snowflake?,
	val publicLogging: Boolean?,
)

/**
 * The data for support configuration. The support config stores the data for support functionality. Channel for the
 * place to create threads to and team for pinging into support threads.
 *
 * @param guildId The ID of the guild the config is for
 * @param enabled If the support module is enabled or not
 * @param channel The ID of the support channel for the guild
 * @param role The ID of the support team for the guild
 * @param message The support message as a string, nullable
 * @since 4.0.0
 */
@Serializable
data class SupportConfigData(
	val guildId: Snowflake,
	val enabled: Boolean,
	val channel: Snowflake?,
	val role: Snowflake?,
	val message: String?
)

/**
 * The data for miscellaneous configuration. The miscellaneous config stores the data for enabling or disabling log
 * uploading.
 *
 * @param guildId The ID of the guild the config is for
 * @param disableLogUploading If log uploading is enabled or not
 * @param utilityLogChannel The channel to log various utility actions too
 * @since 4.0.0
 */
@Serializable
data class UtilityConfigData(
	val guildId: Snowflake,
	val disableLogUploading: Boolean,
	val utilityLogChannel: Snowflake?
)
