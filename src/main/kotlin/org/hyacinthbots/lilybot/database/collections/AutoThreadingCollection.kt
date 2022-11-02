package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.koin.core.component.inject
import org.litote.kmongo.eq

// todo kdoc
class AutoThreadingCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<AutoThreadingData>()

	suspend inline fun getAllAutoThreads(inputGuildId: Snowflake): List<AutoThreadingData> =
		collection.find(AutoThreadingData::guildId eq inputGuildId).toList()

	suspend inline fun getSingleAutoThread(inputChannelId: Snowflake): AutoThreadingData? =
		collection.findOne(AutoThreadingData::channelId eq inputChannelId)

	suspend inline fun setAutoThread(inputAutoThreadData: AutoThreadingData) = run {
		collection.deleteOne(AutoThreadingData::channelId eq inputAutoThreadData.channelId)
		collection.insertOne(inputAutoThreadData)
	}

	suspend inline fun deleteAutoThread(inputChannelId: Snowflake) =
		collection.deleteOne(AutoThreadingData::channelId eq inputChannelId)

	suspend inline fun deleteGuildAutoThreads(inputGuildId: Snowflake) =
		collection.deleteMany(AutoThreadingData::guildId eq inputGuildId)
}
