package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

// todo kdoc

@Serializable
data class WelcomeChannel(
	val channelId: Snowflake,
	val url: String
)
