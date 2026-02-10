package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The data for when Lily leaves a guild.
 *
 * @property guildId The ID of the guild Lily left
 * @property guildLeaveTime The [Instant] that Lily left the guild
 * @since 3.2.0
 */
@Serializable
data class GuildLeaveTimeData(
    val guildId: Snowflake,
    val guildLeaveTime: Instant
)
