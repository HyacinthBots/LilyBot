package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.WarnData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object stores all the functions for interacting with the [Warn Database][WarnData]. The object contains the
 * functions for querying, adding and removal of warnings for a user.
 *
 * @see getWarn
 * @see setWarn
 * @see removeWarn
 * @since 4.0.0
 */
class WarnCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<WarnData>()

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
			WarnData::userId eq inputUserId,
			WarnData::guildId eq inputGuildId
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
		collection.deleteOne(WarnData::userId eq inputUserId, WarnData::guildId eq inputGuildId)
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
	suspend inline fun removeWarn(inputGuildId: Snowflake) =
		collection.deleteMany(WarnData::guildId eq inputGuildId)
}
