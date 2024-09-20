package org.hyacinthbots.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

/**
 * This class contains the functions for interacting with the [AutoThreading Database][AutoThreadingData]. This
 * class contains functions for getting, setting and removing auto threads channels.
 *
 * @since 4.6.0
 * @see getAllAutoThreads
 * @see getSingleAutoThread
 * @see setAutoThread
 * @see deleteAutoThread
 * @see deleteGuildAutoThreads
 */
class AutoThreadingCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<AutoThreadingData>()

	/**
	 * Gets all auto threads for a given [inputGuildId].
	 *
	 * @param inputGuildId The guild to get auto threads for
	 * @return A list of all auto threads for the given guild
	 * @author tempest15
	 * @since 4.6.0
	 */
	suspend inline fun getAllAutoThreads(inputGuildId: Snowflake): List<AutoThreadingData> =
		collection.find(AutoThreadingData::guildId eq inputGuildId).toList()

	/**
	 * Gets a single auto thread based off the channel ID.
	 *
	 * @param inputChannelId The ID of the channel to get
	 * @return The [AutoThreadingData] for the channel
	 * @author tempest15
	 * @since 4.6.0
	 */
	suspend inline fun getSingleAutoThread(inputChannelId: Snowflake): AutoThreadingData? =
		collection.findOne(AutoThreadingData::channelId eq inputChannelId)

	/**
	 * Sets a new auto thread.
	 *
	 * @param inputAutoThreadData The Data for the new auto thread
	 * @author tempest15
	 * @since 4.6.0
	 */
	suspend inline fun setAutoThread(inputAutoThreadData: AutoThreadingData) {
		collection.deleteOne(AutoThreadingData::channelId eq inputAutoThreadData.channelId)
		collection.insertOne(inputAutoThreadData)
	}

	/**
	 * Updates the extra roles on an auto-threaded channel.
	 *
	 * @param inputChannelId The channel to update extra roles for
	 * @param newRoleIds The new list of role IDs to set
	 * @author NoComment1105
	 * @since 5.0.0
	 */
	suspend inline fun updateExtraRoles(inputChannelId: Snowflake, newRoleIds: List<Snowflake?>) {
		collection.updateOne(
			AutoThreadingData::channelId eq inputChannelId,
			setValue(AutoThreadingData::extraRoleIds, newRoleIds)
			)
	}

	/**
	 * Deletes an auto thread based off of the [inputChannelId].
	 *
	 * @param inputChannelId The channel to remove auto threading for
	 * @author tempest15
	 * @since 4.6.0
	 */
	suspend inline fun deleteAutoThread(inputChannelId: Snowflake) =
		collection.deleteOne(AutoThreadingData::channelId eq inputChannelId)

	/**
	 * Deletes auto threads for a given guild.
	 *
	 * @param inputGuildId The guild to remove auto threads for
	 * @author NoComment1105
	 * @since 4.6.0
	 */
	suspend inline fun deleteGuildAutoThreads(inputGuildId: Snowflake) =
		collection.deleteMany(AutoThreadingData::guildId eq inputGuildId)
}
