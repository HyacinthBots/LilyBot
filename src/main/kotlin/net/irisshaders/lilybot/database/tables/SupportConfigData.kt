package net.irisshaders.lilybot.database.tables

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

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
