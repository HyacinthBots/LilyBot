package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.GalleryChannelData
import org.koin.core.component.inject
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
class GalleryChannelCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<GalleryChannelData>()

	/**
	 * Collects every gallery channel in the database into a [List].
	 *
	 * @return The [CoroutineCollection] of [GalleryChannelData] for all the gallery channels in the database
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun getChannels(inputGuildId: Snowflake): List<GalleryChannelData> =
		collection.find(GalleryChannelData::guildId eq inputGuildId).toList()

	/**
	 * Stores a channel ID as input by the user, in the database, with it's corresponding guild, allowing us to find
	 * the channel later.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel that is being set as a gallery channel
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun setChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.insertOne(GalleryChannelData(inputGuildId, inputChannelId))

	/**
	 * Removes a channel ID from the gallery channel database.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel being removed
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend inline fun removeChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.deleteOne(
			GalleryChannelData::channelId eq inputChannelId,
			GalleryChannelData::guildId eq inputGuildId
		)
}
