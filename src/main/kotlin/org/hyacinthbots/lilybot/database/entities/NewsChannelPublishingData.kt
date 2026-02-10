package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for the news channel publishing database.
 *
 * @property guildId The guild the channel is in.
 * @property channelId The channel to publish messages from
 *
 * @since 4.7.0
 */
@Serializable
data class NewsChannelPublishingData(
    val guildId: Snowflake,
    val channelId: Snowflake
)
