package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The data for temporary bans in a guild.
 *
 * @property guildId The ID of the guild the ban occurred in
 * @property bannedUserId The ID of the user that was banned
 * @property moderatorUserId The ID of the moderator that applied the ban
 * @property startTime The time the ban was applied
 * @property endTime The time the ban will complete
 *
 * @since 5.0.0
 */
@Serializable
data class TemporaryBanData(
	val guildId: Snowflake,
	val bannedUserId: Snowflake,
	val moderatorUserId: Snowflake,
	val startTime: Instant,
	val endTime: Instant
)
