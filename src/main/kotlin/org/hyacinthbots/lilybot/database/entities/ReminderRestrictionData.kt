package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class ReminderRestrictionData(
	val guildId: Snowflake,
	val restrict: Boolean,
	val whitelistedChannels: MutableList<Snowflake>?
)
