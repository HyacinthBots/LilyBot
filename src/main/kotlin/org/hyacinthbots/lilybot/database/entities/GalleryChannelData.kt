package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for image channels in a guild.
 *
 * @property guildId The ID of the guild the image channel is for
 * @property channelId The ID of the image channel being set
 * @since 3.3.0
 */
@Serializable
data class GalleryChannelData(
	val guildId: Snowflake,
	val channelId: Snowflake
)
