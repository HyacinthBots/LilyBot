package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.hyacinthbots.lilybot.database.Collection

/**
 * The data for warnings in guilds.
 *.
 * @property userId The ID of the user with warnings
 * @property guildId The ID of the guild they received the warning in
 * @property strikes The amount of strikes they have received
 * @since 3.0.0
 */
@Serializable
data class WarnData(
	val userId: Snowflake,
	val guildId: Snowflake,
	val strikes: Int
) {
	companion object : Collection("warnData")
}
