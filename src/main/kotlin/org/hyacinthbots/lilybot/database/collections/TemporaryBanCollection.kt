package org.hyacinthbots.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.TemporaryBanData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains the functions for interacting with [the temporary ban database][TemporaryBanData]. This class
 * contains functions for settings temporary bans, getting temporary bans based off of various parameters and removing
 * them.
 *
 * @since 5.0.0
 * @see getAllTempBans
 * @see getTempBansForGuild
 * @see getUserTempBan
 * @see setTempBan
 * @see removeTempBan
 */
class TemporaryBanCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<TemporaryBanData>()

	/**
	 * Gets all the temporary bans currently in the database.
	 *
	 * @return A list of temporary bans in the database
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun getAllTempBans(): List<TemporaryBanData> = collection.find().toList()

	/**
	 * Gets all the temporary bans for a given guild.
	 *
	 * @param guildId The ID of the guild to get the bans in
	 * @return A list of Temporary bans for the given [guildId]
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun getTempBansForGuild(guildId: Snowflake): List<TemporaryBanData> =
		collection.find(TemporaryBanData::guildId eq guildId).toList()

	/**
	 * Gets a temporary ban for a given user.
	 *
	 * @param guildId The ID of the guild the temporary ban occurred in
	 * @param bannedUserId The ID of the user that was temporarily banned
	 * @return The [TemporaryBanData] for the [bannedUserId]
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun getUserTempBan(guildId: Snowflake, bannedUserId: Snowflake): TemporaryBanData? =
		collection.findOne(TemporaryBanData::guildId eq guildId, TemporaryBanData::bannedUserId eq bannedUserId)

	/**
	 * Sets a temporary ban.
	 *
	 * @param tempBanData The data for the temporary ban
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun setTempBan(tempBanData: TemporaryBanData) = collection.insertOne(tempBanData)

	/**
	 * Removes the temporary ban for a user in a given guild. This is called once a temporary ban is completed.
	 *
	 * @param guildId The guild the temporary ban is being removed from
	 * @param bannedUserId The ID of the user to remove the temporary ban from
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun removeTempBan(guildId: Snowflake, bannedUserId: Snowflake) =
		collection.deleteOne(TemporaryBanData::guildId eq guildId, TemporaryBanData::bannedUserId eq bannedUserId)
}
