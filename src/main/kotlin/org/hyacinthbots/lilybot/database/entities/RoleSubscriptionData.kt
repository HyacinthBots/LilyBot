package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for role subscriptions.
 *
 * @property guildId The ID of the guild the subscription roles are for
 * @property subscribableRoles The roles that can be subscribed too.
 * @since 4.9.0
 */
@Serializable
data class RoleSubscriptionData(
    val guildId: Snowflake,
    val subscribableRoles: MutableList<Snowflake>
)
