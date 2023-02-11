package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

@Serializable
data class RoleSubscriptionData(
	val guildId: Snowflake,
	val subscribableRoles: MutableList<Snowflake>
)
