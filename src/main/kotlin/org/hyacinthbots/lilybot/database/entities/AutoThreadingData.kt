package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable
import org.hyacinthbots.lilybot.extensions.events.AutoThreading

/**
 * The data for auto-threaded channels.
 *
 * @param guildId The ID of the guild the auto-threaded channel is in
 * @param channelId The ID of the auto-threaded channel being set
 * @param roleId The ID of the role to be mentioned in new threads
 * @param allowDuplicates If users should be allowed to have multiple threads in the channel
 * @param archive If threads should be archived upon creation
 * @param namingScheme What naming scheme the threads should use
 * @param mention If the user who created the thread should be welcomed in the first message
 * @param creationMessage The message to send in the thread when it is created
 *
 * @since 4.1.0
 */
@Serializable
data class AutoThreadingData(
	val guildId: Snowflake,
	val channelId: Snowflake,
	val roleId: Snowflake?,
	val allowDuplicates: Boolean,
	val archive: Boolean,
	val namingScheme: AutoThreading.ThreadNamingSchemes,
	val mention: Boolean,
	val creationMessage: String?
)
