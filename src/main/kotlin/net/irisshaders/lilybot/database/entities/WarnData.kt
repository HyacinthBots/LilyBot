package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for warnings in guilds.
 *.
 * @param userId The ID of the user with warnings
 * @param guildId The ID of the guild they received the warning in
 * @param strikes The amount of strikes they have received
 * @since 3.0.0
 */
@Serializable
data class WarnData(
	val userId: Snowflake,
	val guildId: Snowflake,
	val strikes: Int
)
