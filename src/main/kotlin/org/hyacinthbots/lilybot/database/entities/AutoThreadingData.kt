package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.serialization.Serializable

/**
 * The data for auto-threaded channels.
 *
 * @property guildId The ID of the guild the auto-threaded channel is in
 * @property channelId The ID of the auto-threaded channel being set
 * @property roleId The ID of the role to be mentioned in new threads
 * @property preventDuplicates If users should be allowed to have multiple threads in the channel
 * @property archive If threads should be archived upon creation
 * @property contentAwareNaming If the threads should use a content-aware naming scheme
 * @property mention If the user who created the thread should be welcomed in the first message
 * @property creationMessage The message to send in the thread when it is created
 * @property addModsAndRole Whether to add the moderators to the thread alongside the [roleId]
 *
 * @since 4.6.0
 */
@Serializable
data class AutoThreadingData(
	val guildId: Snowflake,
	val channelId: Snowflake,
	val roleId: Snowflake?,
	val preventDuplicates: Boolean,
	val archive: Boolean,
	val contentAwareNaming: Boolean,
	val mention: Boolean,
	val creationMessage: String?,
	val addModsAndRole: Boolean,
	val extraRoleIds: MutableList<Snowflake>
)
