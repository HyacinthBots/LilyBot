package net.irisshaders.lilybot.database.functions

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import net.irisshaders.lilybot.database
import net.irisshaders.lilybot.database.tables.GuildLeaveTimeData
import org.litote.kmongo.eq

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
