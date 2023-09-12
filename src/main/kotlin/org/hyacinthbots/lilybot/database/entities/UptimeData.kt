package org.hyacinthbots.lilybot.database.entities

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.hyacinthbots.lilybot.database.Collection

/**
 * The data to help calculate bot uptime.
 *
 * @property onTime The instant the bot turned on
 * @since 4.2.0
 */
@Serializable
data class UptimeData(
	val onTime: Instant
) {
	companion object : Collection("uptimeData")
}
