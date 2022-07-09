package net.irisshaders.lilybot.database

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import net.irisshaders.lilybot.database
import org.litote.kmongo.eq

/**
 * The data for when Lily leaves a guild.
 *
 * @param guildId The ID of the guild Lily left
 * @param guildLeaveTime The [Instant] that Lily left the guild
 * @since 3.2.0
 */
@Serializable
data class GuildLeaveTimeData(
	val guildId: Snowflake,
	val guildLeaveTime: Instant
)

/**
 * This object contains the functions or interacting with the [Guild Leave Time Database][GuildLeaveTimeData]. This
 * object contains functions for setting and removing leave time.
 *
 * @since 4.0.0
 * @see setLeaveTime
 * @see removeLeaveTime
 */
object GuildLeaveTimeDatabase {
	/**
	 * Adds the time Lily bot left a guild with a config.
	 *
	 * @param inputGuildId The guild the event took place in
	 * @param time The current time
	 *
	 * @author NoComment1105
	 * @since 3.2.0
	 */
	suspend inline fun setLeaveTime(inputGuildId: Snowflake, time: Instant) {
		val collection = database.getCollection<GuildLeaveTimeData>()
		collection.insertOne(GuildLeaveTimeData(inputGuildId, time))
	}

	/**
	 * This function deletes a [GuildLeaveTimeData] from the database.
	 *
	 * @param inputGuildId The guild to delete the [GuildLeaveTimeData] for
	 *
	 * @author tempest15
	 * @since 3.2.0
	 */
	suspend inline fun removeLeaveTime(inputGuildId: Snowflake) {
		val collection = database.getCollection<GuildLeaveTimeData>()
		collection.deleteOne(GuildLeaveTimeData::guildId eq inputGuildId)
	}
}
