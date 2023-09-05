/*
 * This code has been utilised from [QuiltMCs Cozy](https://github.com/QuiltMC/cozy-discord)
 * and as such this Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.database.Collection
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.WelcomeChannelData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject
import org.quiltmc.community.cozy.modules.welcome.data.WelcomeChannelData as CozyWelcomeChannelData

/**
 * This class contains the functions for interacting with the [Welcome channel database][WelcomeChannelData]. This class
 * contains functions for getting channel urls, setting urls and removing channels
 *
 * @since 4.3.0
 * @see getChannelURLs
 * @see getUrlForChannel
 * @see setUrlForChannel
 * @see removeChannel
 */
class WelcomeChannelCollection : KordExKoinComponent, CozyWelcomeChannelData {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<WelcomeChannelData>(name)

	override suspend fun getChannelURLs(): Map<Snowflake, String> =
		collection.find()
			.toList().associate { it.channelId to it.url }

	override suspend fun getUrlForChannel(channelId: Snowflake): String? =
		collection.findOne(eq(WelcomeChannelData::channelId.name, channelId))
			?.url

	override suspend fun setUrlForChannel(channelId: Snowflake, url: String) {
		collection.replaceOne(Filters.empty(), WelcomeChannelData(channelId, url))
	}

	override suspend fun removeChannel(channelId: Snowflake): String? {
		val url = getUrlForChannel(channelId)
			?: return null

		collection.deleteOne(eq(WelcomeChannelData::channelId.name, channelId))

		return url
	}

	suspend fun removeWelcomeChannelsForGuild(guildId: Snowflake, kord: Kord) {
		val guild = kord.getGuildOrNull(guildId) ?: return
		guild.channels.collect {
			collection.deleteOne(eq(WelcomeChannelData::channelId.name, it.id))
		}
	}

	companion object : Collection("welcomeChannelData")
}
