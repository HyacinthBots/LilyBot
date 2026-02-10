package org.hyacinthbots.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.LockedChannelData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains the function for interacting with the [Locked Channel Database][LockedChannelData]. This class
 * contains functions for getting, setting and removing locked channels
 *
 * @since 5.0.0
 * @see addLockedChannel
 * @see removeLockedChannel
 * @see removeAllLockedChannels
 * @see getLockedChannel
 */
class LockedChannelCollection : KordExKoinComponent {
    private val db: Database by inject()

    @PublishedApi
    internal val collection = db.mainDatabase.getCollection<LockedChannelData>()

    /**
     * Adds the data about a newly locked channel to the database.
     *
     * @param data The [LockedChannelData] for the locked channel
     *
     * @author NoComment1105
     * @since 5.0.0
     */
    suspend inline fun addLockedChannel(data: LockedChannelData) = collection.insertOne(data)

    /**
     * Removes a locked channel from the database. This is usually called when a channel is unlocked.
     *
     * @param inputGuildId The ID of the guild the locked channel is in
     * @param inputChannelId The ID of the locked channel itself
     *
     * @author NoComment1105
     * @since 5.0.0
     */
    suspend inline fun removeLockedChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) =
        collection.deleteOne(LockedChannelData::guildId eq inputGuildId, LockedChannelData::channelId eq inputChannelId)

    /**
     * Removes all locked channels for a given guild from the database. Used in guild cleanups.
     *
     * @param inputGuildId The ID of the guild to remove the locked channels from
     *
     * @author NoComment1105
     * @since 5.0.0
     */
    suspend inline fun removeAllLockedChannels(inputGuildId: Snowflake) =
        collection.deleteMany(LockedChannelData::guildId eq inputGuildId)

    /**
     * Gets a locked channel based on the input parameters.
     *
     * @param inputGuildId The ID of the guild the locked channel is in
     * @param inputChannelId The ID of the channel to get the locked data for
     * @return A [LockedChannelData] object for the given channel
     *
     * @author NoComment1105
     * @since 5.0.0
     */
    suspend inline fun getLockedChannel(inputGuildId: Snowflake, inputChannelId: Snowflake): LockedChannelData? =
        collection.findOne(LockedChannelData::guildId eq inputGuildId, LockedChannelData::channelId eq inputChannelId)
}
