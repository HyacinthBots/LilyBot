package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

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

object RoleMenuDatabase {
	/**
	 * Using the provided [inputMessageId] the associated [RoleMenuData] will be returned from the database.
	 *
	 * @param inputMessageId The ID of the message the event was triggered via.
	 * @return The role menu data from the database
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun getRoleData(inputMessageId: Snowflake): RoleMenuData? {
		val collection = database.getCollection<RoleMenuData>()
		return collection.findOne(RoleMenuData::messageId eq inputMessageId)
	}

	/**
	 * Add the given [inputRoles] to the database entry for the role menu for the provided [inputMessageId],
	 * [inputChannelId], and [inputGuildId].
	 *
	 * @param inputMessageId The ID of the message the role menu is in.
	 * @param inputChannelId The ID of the channel the role menu is in.
	 * @param inputGuildId The ID of the guild the role menu is in.
	 * @param inputRoles The [MutableList] of [Snowflake]s representing the role IDs for the role menu.
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun setRoleMenu(
		inputMessageId: Snowflake,
		inputChannelId: Snowflake,
		inputGuildId: Snowflake,
		inputRoles: MutableList<Snowflake>
	) {
		val newRoleMenu = RoleMenuData(inputMessageId, inputChannelId, inputGuildId, inputRoles)
		val collection = database.getCollection<RoleMenuData>()
		collection.deleteOne(RoleMenuData::messageId eq inputMessageId)
		collection.insertOne(newRoleMenu)
	}

	/**
	 * Remove the given [inputRoleId] from the database entry associated with the given [inputMessageId].
	 *
	 * @param inputMessageId The ID of the message the role menu is in.
	 * @param inputRoleId The ID of the role to remove from the menu.
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun removeRoleFromMenu(inputMessageId: Snowflake, inputRoleId: Snowflake) {
		val collection = database.getCollection<RoleMenuData>()
		val roleMenu = collection.findOne(RoleMenuData::messageId eq inputMessageId) ?: return

		roleMenu.roles.remove(inputRoleId)

		collection.deleteOne(RoleMenuData::messageId eq inputMessageId)
		collection.insertOne(roleMenu)
	}
}
