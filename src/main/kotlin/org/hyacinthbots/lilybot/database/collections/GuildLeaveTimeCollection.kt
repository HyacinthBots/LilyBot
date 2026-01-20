package org.hyacinthbots.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.GuildLeaveTimeData
import org.koin.core.component.inject
import org.litote.kmongo.eq
import kotlin.time.Instant

/**
 * This class contains the functions for interacting with the [Guild Leave Time Database][GuildLeaveTimeData]. This
 * class contains functions for setting and removing leave time.
 *
 * @since 4.0.0
 * @see setLeaveTime
 * @see removeLeaveTime
 */
class GuildLeaveTimeCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<GuildLeaveTimeData>()

	/**
	 * Adds the time Lily bot left a guild with a config.
	 *
	 * @param inputGuildId The guild the event took place in
	 * @param time The current time
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend inline fun setLeaveTime(inputGuildId: Snowflake, time: Instant) =
		collection.insertOne(GuildLeaveTimeData(inputGuildId, time))

	/**
	 * This function deletes a [GuildLeaveTimeData] from the database.
	 *
	 * @param inputGuildId The guild to delete the [GuildLeaveTimeData] for
	 *
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun removeLeaveTime(inputGuildId: Snowflake) =
		collection.deleteOne(GuildLeaveTimeData::guildId eq inputGuildId)
}
