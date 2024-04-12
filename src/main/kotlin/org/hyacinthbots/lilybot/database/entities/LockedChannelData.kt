package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for locked channels.
 *
 * @property guildId The ID of the guild the locked channel is in
 * @property channelId The ID of the channel that is locked
 * @property allowed The Discord Bit Set code for the allowed permissions, formatted as a string
 * @property denied The Discord Bit Set code for the denied permissions, formatted as a string
 * @since 5.0.0
 */
@Serializable
data class LockedChannelData(
	val guildId: Snowflake,
	val channelId: Snowflake,
	val allowed: String,
	val denied: String,
)
