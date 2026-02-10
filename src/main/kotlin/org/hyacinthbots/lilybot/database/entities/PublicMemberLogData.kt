package org.hyacinthbots.lilybot.database.entities

import kotlinx.serialization.Serializable

/**
 * The data for public member logging.
 *
 * @property pingNewUsers Whether to ping new users when they join or not
 * @property joinMessage The message to send when new users join. Can be null
 * @property leaveMessage The message to send when users leave. Can be null
 */
@Serializable
data class PublicMemberLogData(
    val pingNewUsers: Boolean,
    val joinMessage: String?,
    val leaveMessage: String?
)
