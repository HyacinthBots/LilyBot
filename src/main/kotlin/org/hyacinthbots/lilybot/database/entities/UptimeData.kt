package org.hyacinthbots.lilybot.database.entities

import kotlinx.serialization.Serializable
import kotlin.time.Instant

/**
 * The data to help calculate bot uptime.
 *
 * @property onTime The instant the bot turned on
 * @since 4.2.0
 */
@Serializable
data class UptimeData(
    val onTime: Instant
)
