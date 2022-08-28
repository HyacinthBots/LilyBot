package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.LogUploadingBlacklistData
import org.koin.core.component.inject
import org.litote.kmongo.eq

class LogUploadingBlacklistCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<LogUploadingBlacklistData>()

	/**
	 * Sets a channel as blacklisted for uploading logs.
	 *
	 * @param inputGuildId The guild the command was run in
	 * @param inputChannelId The channel to disable uploading for
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun setLogUploadingBlacklist(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.insertOne(LogUploadingBlacklistData(inputGuildId, inputChannelId))

	/**
	 * Removes a channel from the blacklist for uploading logs.
	 *
	 * @param inputGuildId The guild the command was run in
	 * @param inputChannelId The channel to re-enable uploading for
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun removeLogUploadingBlacklist(inputGuildId: Snowflake, inputChannelId: Snowflake) =
		collection.deleteOne(
			LogUploadingBlacklistData::guildId eq inputGuildId,
			LogUploadingBlacklistData::channelId eq inputChannelId
		)

	/**
	 * Checks the log uploading blacklist for the given [inputChannelId].
	 *
	 * @param inputGuildId The guild to get the [inputChannelId] is in
	 * @param inputChannelId The channel to check is blacklisted or not
	 * @return The data for the channel or null
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun isChannelInUploadBlacklist(
		inputGuildId: Snowflake,
		inputChannelId: Snowflake
	): LogUploadingBlacklistData? = collection.findOne(
		LogUploadingBlacklistData::guildId eq inputGuildId,
		LogUploadingBlacklistData::channelId eq inputChannelId
	)

	/**
	 * Gets the log uploading blacklist for the given [inputGuildId].
	 *
	 * @param inputGuildId The guild to get the blacklist for
	 * @return The list of blacklisted channels for the given guild
	 *
	 * @author NoComment1105
	 * @since 3.5.4
	 */
	suspend inline fun getLogUploadingBlacklist(inputGuildId: Snowflake): List<LogUploadingBlacklistData> =
		collection.find(LogUploadingBlacklistData::guildId eq inputGuildId).toList()
}
