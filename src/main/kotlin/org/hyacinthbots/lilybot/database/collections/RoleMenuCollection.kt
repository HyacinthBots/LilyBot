package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Collection
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.RoleMenuData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject

/**
 * This class contains the functions for interacting with the [Role Menu Database][RoleMenuData]. This class contains
 * the functions for getting a menu, setting a menu and removing a role from a menu.
 *
 * @since 4.0.0
 * @see getRoleData
 * @see setRoleMenu
 * @see removeRoleFromMenu
 * @see removeAllRoleMenus
 */
class RoleMenuCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<RoleMenuData>(name)

	/**
	 * Using the provided [inputMessageId] the associated [RoleMenuData] will be returned from the database.
	 *
	 * @param inputMessageId The ID of the message the event was triggered via.
	 * @return The role menu data from the database
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun getRoleData(inputMessageId: Snowflake): RoleMenuData? =
		collection.findOne(eq(RoleMenuData::messageId.name, inputMessageId))

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
		collection.deleteOne(eq(RoleMenuData::messageId.name, inputMessageId))
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
		val roleMenu = collection.findOne(eq(RoleMenuData::messageId.name, inputMessageId)) ?: return

		roleMenu.roles.remove(inputRoleId)

		collection.deleteOne(eq(RoleMenuData::messageId.name, inputMessageId))
		collection.insertOne(roleMenu)
	}

	/**
	 * Deletes all role menus from the database.
	 *
	 * @param inputGuildId The ID of the guild to delete all role menus from.
	 * @author NoComment1105
	 * @since 4.0.0
	 */
	suspend inline fun removeAllRoleMenus(inputGuildId: Snowflake) =
		collection.deleteMany(eq(RoleMenuData::guildId.name, inputGuildId))

	companion object : Collection("roleMenuData")
}
