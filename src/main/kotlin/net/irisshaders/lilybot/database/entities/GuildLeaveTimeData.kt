package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The data for when Lily leaves a guild.
 *
 * @param guildId The ID of the guild Lily left
 * @param guildLeaveTime The [Instant] that Lily left the guild
 * @since 3.2.0
 */
@Serializable
data class GuildLeaveTimeData(
	val guildId: Snowflake,
	val guildLeaveTime: Instant
)
