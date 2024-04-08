package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class LockedChannelData(
	val guildId: Snowflake,
	val channelId: Snowflake,
	@Contextual val allowed: Permissions,
	@Contextual val denied: Permissions,
)
