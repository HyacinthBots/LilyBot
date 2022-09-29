package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for moderation configuration. The logging config stores where logs are sent to, and whether to enable or
 * disable certain configurations.
 *
 * @property guildId The ID of the guild the config is for
 * @property enableMessageDeleteLogs If deleted messages should be logged
 * @property enableMessageEditLogs If edited messages should be logged
 * @property messageChannel The channel to send message logs to
 * @property enableMemberLogs If users joining or leaving the guild should be logged
 * @property memberLog The channel to send member logs to
 * @since 4.0.0
 */
@Serializable
data class LoggingConfigData(
	val guildId: Snowflake,
	val enableMessageDeleteLogs: Boolean,
	val enableMessageEditLogs: Boolean,
	val messageChannel: Snowflake?,
	val enableMemberLogs: Boolean,
	val memberLog: Snowflake?,
)

/**
 * The data for moderation configuration. The moderation config is what stores the data for moderation actions. The
 * channel for logging and the team for pinging.
 *
 * @property guildId The ID of the guild the config is for
 * @property enabled If the support module is enabled or not
 * @property channel The ID of the action log for the guild
 * @property role The ID of the moderation role for the guild
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
 * @property guildId The ID of the guild the config is for
 * @property enabled If the support module is enabled or not
 * @property channel The ID of the support channel for the guild
 * @property role The ID of the support team for the guild
 * @property message The support message as a string, nullable
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
 * @property guildId The ID of the guild the config is for
 * @property disableLogUploading If log uploading is enabled or not
 * @property utilityLogChannel The channel to log various utility actions too
 * @since 4.0.0
 */
@Serializable
data class UtilityConfigData(
	val guildId: Snowflake,
	val disableLogUploading: Boolean,
	val utilityLogChannel: Snowflake?
)
