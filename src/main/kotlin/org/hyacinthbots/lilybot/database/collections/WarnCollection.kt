package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.WarnData
import org.hyacinthbots.lilybot.database.findOne
import org.koin.core.component.inject

/**
 * This class stores all the functions for interacting with the [Warn Database][WarnData]. The class contains the
 * functions for querying, adding and removal of warnings for a user.
 *
 * @since 4.0.0
 * @see getWarn
 * @see setWarn
 * @see clearWarns
 */
class WarnCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<WarnData>("warnData")

	/**
	 * Gets the number of points the provided [inputUserId] has in the provided [inputGuildId] from the database.
	 *
	 * @param inputUserId The ID of the user to get the point value for
	 * @param inputGuildId The ID of the guild the command was run in
	 * @return null or the result from the database
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun getWarn(inputUserId: Snowflake, inputGuildId: Snowflake): WarnData? =
		collection.findOne(
			and(
				eq(WarnData::userId.name, inputUserId),
				eq(WarnData::guildId.name, inputGuildId)
			)
		)

	/**
	 * Updates the number of points the provided [inputUserId] has in the provided [inputGuildId] in the database.
	 *
	 * @param inputUserId The ID of the user to get the point value for.
	 * @param inputGuildId The ID of the guild the command was run in.
	 * @param remove Remove a warn strike, or add a warn strike.
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun setWarn(inputUserId: Snowflake, inputGuildId: Snowflake, remove: Boolean) {
		val currentStrikes = getWarn(inputUserId, inputGuildId)?.strikes ?: 0
		collection.deleteOne(and(eq(WarnData::userId.name, inputUserId), eq(WarnData::guildId.name, inputGuildId)))
		collection.insertOne(
			WarnData(
				inputUserId,
				inputGuildId,
				if (!remove) currentStrikes.plus(1) else currentStrikes.minus(1)
			)
		)
	}

	/**
	 * Clears all warn strikes for the provided [inputGuildId].
	 *
	 * @param inputGuildId The ID of the guild the command was run in
	 * @author tempest15
	 * @since 3.0.0
	 */
	suspend inline fun clearWarns(inputGuildId: Snowflake) =
		collection.deleteMany(eq(WarnData::guildId.name, inputGuildId))
}
