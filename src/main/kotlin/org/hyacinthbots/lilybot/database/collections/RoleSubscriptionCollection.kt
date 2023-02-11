package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.RoleMenuData
import org.hyacinthbots.lilybot.database.entities.RoleSubscriptionData
import org.koin.core.component.inject
import org.litote.kmongo.eq

class RoleSubscriptionCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<RoleSubscriptionData>()

	suspend inline fun getSubscribableRoles(inputGuildId: Snowflake): RoleSubscriptionData? =
		collection.findOne(RoleMenuData::guildId eq inputGuildId)

	suspend inline fun createSubscribableRoleRecord(inputGuildId: Snowflake) =
		collection.insertOne(RoleSubscriptionData(inputGuildId, mutableListOf()))

	suspend inline fun addSubscribableRole(inputGuildId: Snowflake, inputRoleId: Snowflake): Boolean? {
		val col = collection.findOne(RoleSubscriptionData::guildId eq inputGuildId) ?: return null
		val newRoleList = col.subscribableRoles
		if (newRoleList.contains(inputRoleId)) return false else newRoleList.add(inputRoleId)
		collection.updateOne(
			RoleSubscriptionData::guildId eq inputGuildId,
			RoleSubscriptionData(inputGuildId, newRoleList)
		)
		return true
	}

	suspend inline fun removeSubscribableRole(inputGuildId: Snowflake, inputRoleId: Snowflake): Boolean? {
		val col = collection.findOne(RoleSubscriptionData::guildId eq inputGuildId) ?: return null
		val newRoleList = col.subscribableRoles
		if (!newRoleList.contains(inputRoleId)) {
			return false
		} else {
			val removal = newRoleList.remove(inputRoleId)
			if (!removal) return false
		}
		collection.updateOne(
			RoleSubscriptionData::guildId eq inputGuildId,
			RoleSubscriptionData(inputGuildId, newRoleList)
		)
		return true
	}

	suspend inline fun removeAllSubscribableRoles(inputGuildId: Snowflake) =
		collection.deleteOne(RoleSubscriptionData::guildId eq inputGuildId)
}
