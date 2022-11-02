package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for auto-threaded channels.
 *
 * @property guildId The ID of the guild the auto-threaded channel is in
 * @property channelId The ID of the auto-threaded channel being set
 * @property roleId The ID of the role to be mentioned in new threads
 * @property allowDuplicates If users should be allowed to have multiple threads in the channel
 * @property archive If threads should be archived upon creation
 * @property smartNaming If the threads should use a content-aware naming scheme
 * @property mention If the user who created the thread should be welcomed in the first message
 * @property creationMessage The message to send in the thread when it is created
 *
 * @since 4.4.0
 */
@Serializable
data class AutoThreadingData(
	val guildId: Snowflake,
	val channelId: Snowflake,
	val roleId: Snowflake?,
	val allowDuplicates: Boolean,
	val archive: Boolean,
	val smartNaming: Boolean,
	val mention: Boolean,
	val creationMessage: String?
)
