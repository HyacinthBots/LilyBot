package org.hyacinthbots.lilybot.database.collections

import dev.kord.common.entity.Snowflake
import dev.kordex.core.koin.KordExKoinComponent
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.GithubData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This class contains the functions for interacting with the [GitHub database][GithubData]. This class contains
 * functions for getting, setting and removing default repos
 *
 * @since 4.3.0
 * @see getDefaultRepo
 * @see setDefaultRepo
 * @see removeDefaultRepo
 */
class GithubCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<GithubData>()

	/**
	 * Gets the default repo for GitHub commands.
	 *
	 * @param inputGuildId The ID of the guild to get the default for
	 * @return The default repo URL
	 * @author NoComment1105
	 * @since 4.3.0
	 */
	suspend inline fun getDefaultRepo(inputGuildId: Snowflake): String? =
		collection.findOne(GithubData::guildId eq inputGuildId)?.defaultRepo

	/**
	 * Sets the default repo for GitHub commands.
	 *
	 * @param inputGuildId The ID of the guild to set the default for
	 * @param url The URL to set as default
	 * @author NoComment1105
	 * @since 4.3.0
	 */
	suspend inline fun setDefaultRepo(inputGuildId: Snowflake, url: String) {
		collection.deleteOne(GithubData::guildId eq inputGuildId)
		collection.insertOne(GithubData(inputGuildId, url))
	}

	/**
	 * Removes the default repo for GitHub commands.
	 *
	 * @param inputGuildId The ID of the guild to remove the default for
	 * @author NoComment1105
	 * @since 4.3.0
	 */
	suspend inline fun removeDefaultRepo(inputGuildId: Snowflake) =
		collection.deleteOne(GithubData::guildId eq inputGuildId)
}
