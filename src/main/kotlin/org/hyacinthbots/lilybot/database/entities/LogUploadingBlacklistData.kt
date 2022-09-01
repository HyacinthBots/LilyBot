package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The Data for blacklisting channels from uploading logs.
 *
 * @property guildId The guild the channel is in
 * @property channelId The channel to block uploads in
 *
 * @since 3.5.4
 */
@Serializable
data class LogUploadingBlacklistData(
	val guildId: Snowflake,
	val channelId: Snowflake
)
