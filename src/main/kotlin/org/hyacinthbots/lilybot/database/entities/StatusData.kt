package org.hyacinthbots.lilybot.database.entities

import kotlinx.serialization.Serializable

/**
 * The data for the bot status.
 *
 * @property status The string value that will be seen in the bots presence
 * @since 3.0.0
 */
@Serializable
data class StatusData(
	val statusType: String?,
	val status: String?
)
