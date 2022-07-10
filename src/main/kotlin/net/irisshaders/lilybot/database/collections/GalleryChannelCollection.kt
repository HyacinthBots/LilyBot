package net.irisshaders.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.entities.GalleryChannelData
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

/**
 * This object contains the functions or interacting with the [Gallery Channel Database][GalleryChannelData]. This
 * object contains functions for getting, setting and removing gallery channels.
 *
 * @since 4.0.0
 * @see getChannels
 * @see setChannel
 * @see removeChannel
 */
class GalleryChannelCollection {
	/**
	 * Collects every gallery channel in the database into a [List].
	 *
	 * @return The [CoroutineCollection] of [GalleryChannelData] for all the gallery channels in the database
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	fun getChannels(): CoroutineCollection<GalleryChannelData> = database.getCollection()

	/**
	 * Stores a channel ID as input by the user, in the database, with it's corresponding guild, allowing us to find
	 * the channel later.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel that is being set as a gallery channel
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun setChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) {
		val collection = getChannels()
		collection.insertOne(GalleryChannelData(inputGuildId, inputChannelId))
	}

	/**
	 * Removes a channel ID from the gallery channel database.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel being removed
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun removeChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) {
		val collection = getChannels()
		collection.deleteOne(
			GalleryChannelData::channelId eq inputChannelId,
			GalleryChannelData::guildId eq inputGuildId
		)
	}
}
