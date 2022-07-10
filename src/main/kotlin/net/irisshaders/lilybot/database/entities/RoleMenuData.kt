package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for role menus.
 *
 * @param messageId The ID of the message of the role menu
 * @param channelId The ID of the channel the role menu is in
 * @param guildId The ID of the guild the role menu is in
 * @param roles A [MutableList] of the role IDs associated with this role menu.
 * @since 3.4.0
 */
@Serializable
data class RoleMenuData(
	val messageId: Snowflake,
	val channelId: Snowflake,
	val guildId: Snowflake,
	val roles: MutableList<Snowflake>
)
