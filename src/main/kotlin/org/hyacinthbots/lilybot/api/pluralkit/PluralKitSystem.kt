package org.hyacinthbots.lilybot.api.pluralkit

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#system-model).
 *
 * @param id The ID of the system.
 * @param uuid The UUID of the system.
 * @param name The name of the system.
 * @param description The description for the system.
 * @param tag The tag for the system.
 * @param avatarUrl The URL for the system's avatar.
 * @param banner The URL for the system's banner.
 * @param color The color of the system. String because the PK docs are funny. Not prefixed with a #.
 * @param created The instant the system was created.
 * @param timezone The timezone of the system. Defaults to null
 * @param privacy The privacy settings of the system
 * @since 3.4.5
 */
@Serializable
data class PluralKitSystem(
	val id: String,
	val uuid: String,
	val name: String?,
	val description: String?,
	val tag: String?,

	@SerialName("avatar_url")
	val avatarUrl: String?,

	val banner: String?,
	val color: String?, // Ask the PK Docs
	val created: Instant,
	val timezone: String? = null,
	val privacy: PluralKitSystemPrivacy?,
)
