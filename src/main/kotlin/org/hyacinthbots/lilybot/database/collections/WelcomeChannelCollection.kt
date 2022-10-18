package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.WelcomeChannel
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.quiltmc.community.cozy.modules.welcome.data.WelcomeChannelData

// todo kdoc

class WelcomeChannelCollection : KordExKoinComponent, WelcomeChannelData {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<WelcomeChannel>()

	override suspend fun getChannelURLs(): Map<Snowflake, String> =
		collection.find()
			.toList().associate { it.channelId to it.url }

	override suspend fun getUrlForChannel(channelId: Snowflake): String? =
		collection.findOne(WelcomeChannel::channelId eq channelId)
			?.url

	override suspend fun setUrlForChannel(channelId: Snowflake, url: String) {
		collection.save(WelcomeChannel(channelId, url))
	}

	override suspend fun removeChannel(channelId: Snowflake): String? {
		val url = getUrlForChannel(channelId)
			?: return null

		collection.deleteOne(WelcomeChannel::channelId eq channelId)

		return url
	}
}
