package org.hyacinthbots.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.LeftMemberFlagData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains the function for interacting with the [User Leaving Database][LeftMemberFlagData]. This
 * class contains functions for getting, adding and removing actions
 *
 * @since 5.0.0
 * @see addMemberToLeft
 * @see getMemberFromTable
 * @see removeMemberFromLeft
 */
class LeftMemberFlagCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<LeftMemberFlagData>()

	/**
	 * Adds a member that left to the table.
	 *
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user that left
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun addMemberToLeft(guildId: Snowflake, targetUserId: Snowflake) =
		collection.insertOne(LeftMemberFlagData(guildId, targetUserId))

	/**
	 * Gets a member that left from the table.
	 *
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user that left
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun getMemberFromTable(guildId: Snowflake, targetUserId: Snowflake) =
		collection.findOne(LeftMemberFlagData::guildId eq guildId, LeftMemberFlagData::userId eq targetUserId)

	/**
	 * Removes a member that from the table.
	 *
	 * @param guildId The ID of the guild the action occurred in
	 * @param targetUserId The ID of the user that left
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun removeMemberFromLeft(guildId: Snowflake, targetUserId: Snowflake) =
		collection.deleteOne(LeftMemberFlagData::guildId eq guildId, LeftMemberFlagData::userId eq targetUserId)
}
