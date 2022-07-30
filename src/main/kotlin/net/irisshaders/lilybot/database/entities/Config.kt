package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for moderation configuration. The logging config stores where logs are sent to, and whether to enable or
 * disable certain configurations.
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
 * The data for moderation configuration. The moderation config is what stores the data for moderation actions. The
 * channel for logging and the team for pinging.
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
 * The data for support configuration. The support config stores the data for support functionality. Channel for the
 * place to create threads to and team for pinging into support threads.
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
 * The data for guild configuration.
 *
 * @param guildId The ID of the guild the config is for
 * @param moderatorsPing The ID of the moderator ping role
 * @param modActionLog The ID of the guild's action/audit log channel
 * @param messageLogs The ID of the guild's message logging channel
 * @param joinChannel The ID of the guild's member flow channel
 * @param supportChannel The ID of the support channel for the guild, nullable
 * @param supportTeam The ID of the support team for the guild, nullable
 * @since 3.0.0
 */
@Deprecated("Use the new config system", level = DeprecationLevel.ERROR)
@Serializable
data class ConfigData(
	val guildId: Snowflake,
	val moderatorsPing: Snowflake,
	val modActionLog: Snowflake,
	val messageLogs: Snowflake,
	val joinChannel: Snowflake,
	val supportChannel: Snowflake?,
	val supportTeam: Snowflake?,
)
