package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.NewsChannelPublishingData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject

/**
 * This class contains functions for interacting with the [news channel publishing database][NewsChannelPublishingData].
 * This class contains functions for adding, removing, getting and getting all auto-publishing channels.
 *
 * @since 4.7.0
 * @see addAutoPublishingChannel
 * @see removeAutoPublishingChannel
 * @see getAutoPublishingChannel
 * @see getAutoPublishingChannels
 * @see clearAutoPublishingForGuild
 */
class NewsChannelPublishingCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<NewsChannelPublishingData>(NewsChannelPublishingData.name)

	/**
	 * Adds a channel for auto-publishing.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel to publish messages in
	 *
	 * @author NoComment1105
	 * @since 4.7.0
	 */
	suspend inline fun addAutoPublishingChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.insertOne(NewsChannelPublishingData(inputGuildId, inputChannelId))

	/**
	 * Removes a channel for auto-publishing.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel to stop publishing from.
	 *
	 * @author NoComment1105
	 * @since 4.7.0
	 */
	suspend inline fun removeAutoPublishingChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.deleteOne(
			and(
				eq(NewsChannelPublishingData::guildId.name, inputGuildId),
				eq(NewsChannelPublishingData::channelId.name, inputChannelId)
			)
		)

	/**
	 * Gets an auto-publishing channel.
	 *
	 * @param inputGuildId The guild the channel is in
	 * @param inputChannelId The channel to get.
	 * @return The data for the channel or null
	 *
	 * @author NoComment1105
	 * @since 4.7.0
	 */
	suspend inline fun getAutoPublishingChannel(
		inputGuildId: Snowflake,
		inputChannelId: Snowflake
	): NewsChannelPublishingData? =
		collection.findOne(
			eq(NewsChannelPublishingData::guildId.name, inputGuildId),
			eq(NewsChannelPublishingData::channelId.name, inputChannelId)
		)

	/**
	 * Gets all channels that are set for auto-publishing.
	 *
	 * @param inputGuildId The guild to get the channels for.
	 * @return A [List] of [NewsChannelPublishingData] for the guild
	 *
	 * @author NoComment1105
	 * @since 4.7.0
	 */
	suspend inline fun getAutoPublishingChannels(inputGuildId: Snowflake): List<NewsChannelPublishingData> =
		collection.find(eq(NewsChannelPublishingData::guildId.name, inputGuildId)).toList()

	/**
	 * Clears all the auto-publishing channels from a guild.
	 *
	 * @param inputGuildId The guild to clear channels for
	 *
	 * @author NoComment1105
	 * @since 4.7.0
	 */
	suspend inline fun clearAutoPublishingForGuild(inputGuildId: Snowflake) {
		collection.deleteMany(eq(NewsChannelPublishingData::guildId.name, inputGuildId))
	}
}
