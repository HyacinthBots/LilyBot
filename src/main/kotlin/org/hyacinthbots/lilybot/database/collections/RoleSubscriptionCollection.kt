package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.RoleSubscriptionData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject

/**
 * This class contains the functions for interacting with the [Role Subscription database][RoleSubscriptionData]. This
 * class contains the functions for getting, adding, removing and clearing subscribable roles.
 *
 * @since 4.9.0
 * @see getSubscribableRoles
 * @see createSubscribableRoleRecord
 * @see addSubscribableRole
 * @see removeSubscribableRole
 * @see removeAllSubscribableRoles
 */
class RoleSubscriptionCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<RoleSubscriptionData>(RoleSubscriptionData.name)

	/**
	 * Gets the roles that are subscribable for a given guild.
	 *
	 * @param inputGuildId The guild to get the roles for
	 * @return The [RoleSubscriptionData] for the guild
	 *
	 * @author NoComment1105
	 * @since 4.9.0
	 */
	suspend inline fun getSubscribableRoles(inputGuildId: Snowflake): RoleSubscriptionData? =
		collection.findOne(eq(RoleSubscriptionData::guildId.name, inputGuildId))

	/**
	 * Creates a subscribable role record in the database. This should only be used if a record does not already exist.
	 *
	 * @param inputGuildId The ID of the guild to create the record for
	 *
	 * @author NoComment1105
	 * @since 4.9.0
	 */
	suspend inline fun createSubscribableRoleRecord(inputGuildId: Snowflake) =
		collection.insertOne(RoleSubscriptionData(inputGuildId, mutableListOf()))

	/**
	 * Adds a role to the subscribable role list.
	 *
	 * @param inputGuildId The ID of the guild to add to the list
	 * @param inputRoleId The ID of the role to add
	 * @return True if the transaction was a success, false if it was not, null if the collection does not exist
	 *
	 * @author NoComment1105
	 * @since 4.9.0
	 */
	suspend inline fun addSubscribableRole(inputGuildId: Snowflake, inputRoleId: Snowflake): Boolean? {
		val col = collection.findOne(eq(RoleSubscriptionData::guildId.name, inputGuildId)) ?: return null
		val newRoleList = col.subscribableRoles
		if (newRoleList.contains(inputRoleId)) return false else newRoleList.add(inputRoleId)
		collection.updateOne(
			eq(RoleSubscriptionData::guildId.name, inputGuildId),
			Updates.set(RoleSubscriptionData::guildId.name, newRoleList)
		)
		return true
	}

	/**
	 * Removes a role to the subscribable role list.
	 *
	 * @param inputGuildId The ID of the guild to alter the list of
	 * @param inputRoleId The ID of the role to rem,ove
	 * @return True if the transaction was a success, false if it was not, null if the collection does not exist
	 *
	 * @author NoComment1105
	 * @since 4.9.0
	 */
	suspend inline fun removeSubscribableRole(inputGuildId: Snowflake, inputRoleId: Snowflake): Boolean? {
		val col = collection.findOne(eq(RoleSubscriptionData::guildId.name, inputGuildId)) ?: return null
		val newRoleList = col.subscribableRoles
		if (!newRoleList.contains(inputRoleId)) {
			return false
		} else {
			val removal = newRoleList.remove(inputRoleId)
			if (!removal) return false
		}
		collection.updateOne(
			eq(RoleSubscriptionData::guildId.name, inputGuildId),
			Updates.set(RoleSubscriptionData::guildId.name, newRoleList)
		)
		return true
	}

	/**
	 * Removes all subscribable roles for a guild.
	 *
	 * @param inputGuildId The ID of the guild to remove subscribable roles for
	 *
	 * @author NoComment1105
	 * @since 4.9.0
	 */
	suspend inline fun removeAllSubscribableRoles(inputGuildId: Snowflake) =
		collection.deleteOne(eq(RoleSubscriptionData::guildId.name, inputGuildId))
}
