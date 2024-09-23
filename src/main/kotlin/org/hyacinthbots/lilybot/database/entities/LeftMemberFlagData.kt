package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for a user leaving a guild, to show it should stop certain events.
 *
 * @property guildId The id of the guild they left
 * @property userId The id of the user that left
 * @since 5.0.0
 */
@Serializable
data class LeftMemberFlagData(
	val guildId: Snowflake,
	val userId: Snowflake
)
