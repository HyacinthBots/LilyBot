package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.RoleMenuData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object contains the functions for interacting with the [Role Menu Database][RoleMenuData]. This object contains
 * the functions for getting a menu, setting a menu and removing a role from a menu.
 *
 * @since 4.0.0
 * @see getRoleData
 * @see setRoleMenu
 * @see removeRoleFromMenu
 */
class RoleMenuCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<RoleMenuData>()

	/**
	 * Using the provided [inputMessageId] the associated [RoleMenuData] will be returned from the database.
	 *
	 * @param inputMessageId The ID of the message the event was triggered via.
	 * @return The role menu data from the database
	 * @author tempest15
	 * @since 3.4.0
	 */
	suspend inline fun getRoleData(inputMessageId: Snowflake): RoleMenuData? =
		collection.findOne(RoleMenuData::messageId eq inputMessageId)

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
		val roleMenu = collection.findOne(RoleMenuData::messageId eq inputMessageId) ?: return

		roleMenu.roles.remove(inputRoleId)

		collection.deleteOne(RoleMenuData::messageId eq inputMessageId)
		collection.insertOne(roleMenu)
	}
}
