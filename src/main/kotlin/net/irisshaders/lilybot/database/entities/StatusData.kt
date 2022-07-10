package net.irisshaders.lilybot.database.entities

import kotlinx.serialization.Serializable

/**
 * The data for the bot status.
 *
 * @param key This is just so we can find the status and should always be set to "LilyStatus"
 * @param status The string value that will be seen in the bots presence
 * @since 3.0.0
 */
@Serializable
data class StatusData(
	val key: String,
	val status: String
)
