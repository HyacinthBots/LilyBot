package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.LockedChannelData
import org.koin.core.component.inject
import org.litote.kmongo.eq

class LockedChannelCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<LockedChannelData>()

	suspend inline fun addLockedChannel(data: LockedChannelData) =
		collection.insertOne(data)

	suspend inline fun removeLockedChannel(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.deleteOne(LockedChannelData::guildId eq inputGuildId, LockedChannelData::channelId eq inputChannelId)

	suspend inline fun getLockedChannel(inputGuildId: Snowflake, inputChannelId: Snowflake): LockedChannelData? =
		collection.findOne(LockedChannelData::guildId eq inputGuildId, LockedChannelData::channelId eq inputChannelId)
}
