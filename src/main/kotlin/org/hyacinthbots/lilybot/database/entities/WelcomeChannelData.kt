package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for Welcome channels.
 *
 * @property guildId The ID of the guild this welcome channel is for
 * @property channelId The ID of the welcome channel
 * @property url The URL to search to populate the welcome channel with
 * @since 4.3.0
 */
@Serializable
data class WelcomeChannelData(
	val guildId: Snowflake,
	val channelId: Snowflake,
	val url: String
)
