/*
 * This code has been utilised from [QuiltMCs Cozy](https://github.com/QuiltMC/cozy-discord)
 * and as such this Source Code Form is subject to the terms of the Mozilla
 * Public License, v. 2.0. If a copy of the MPL was not distributed with
 * this file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.WelcomeChannelData
import org.koin.core.component.inject
import org.litote.kmongo.eq

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
	internal val collection = db.mainDatabase.getCollection<WelcomeChannelData>()

	override suspend fun getChannelURLs(): Map<Snowflake, String> =
		collection.find()
			.toList().associate { it.channelId to it.url }

	override suspend fun getUrlForChannel(channelId: Snowflake): String? =
		collection.findOne(WelcomeChannelData::channelId eq channelId)
			?.url

	override suspend fun setUrlForChannel(guildId: Snowflake, channelId: Snowflake, url: String) {
		collection.save(WelcomeChannelData(guildId, channelId, url))
	}

	override suspend fun removeChannel(channelId: Snowflake): String? {
		val url = getUrlForChannel(channelId)
			?: return null

		collection.deleteOne(WelcomeChannelData::channelId eq channelId)

		return url
	}

	suspend fun removeWelcomeChannelForGuild(guildId: Snowflake) {
		collection.deleteOne(WelcomeChannelData::guildId eq guildId)
	}
}

interface CozyWelcomeChannelData {
	suspend fun getChannelURLs(): Map<Snowflake, String>
	suspend fun getUrlForChannel(channelId: Snowflake): String?

	suspend fun setUrlForChannel(guildId: Snowflake, channelId: Snowflake, url: String)
	suspend fun removeChannel(channelId: Snowflake): String?
}
