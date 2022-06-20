package net.irisshaders.lilybot.database.tables

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

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
