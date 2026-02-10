package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for Welcome channels.
 *
 * @property channelId The ID of the welcome channel
 * @property url The URL to search to populate the welcome channel with
 * @since 4.3.0
 */
@Serializable
data class WelcomeChannelData(
    val channelId: Snowflake,
    val url: String
)
