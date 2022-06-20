package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.tables.GalleryChannelData
import org.litote.kmongo.coroutine.CoroutineCollection
import org.litote.kmongo.eq

object GalleryChannelDatabase {
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
		val collection = database.getCollection<GalleryChannelData>()
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
		val collection = database.getCollection<GalleryChannelData>()
		collection.deleteOne(
			GalleryChannelData::channelId eq inputChannelId,
			GalleryChannelData::guildId eq inputGuildId
		)
	}
}
