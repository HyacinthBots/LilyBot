package org.hyacinthbots.lilybot.api.pluralkit

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#member-model).
 *
 * @param id The ID of the member.
 * @param uuid The UUID of the member.
 * @param name The name of the member.
 * @param displayName The display name of the member.
 * @param color The color of the member. String because the PK docs are funny. Not prefixed with a #
 * @param birthday The birthday of the member. YYYY-MM-DD format. 0004 hides the year.
 * @param pronouns The member's pronouns.
 * @param avatarUrl The URL of the member's avatar.
 * @param banner The URL of the member's banner.
 * @param description The description of the member
 * @param created The instant the member was created
 * @param proxyTags A list of tags the member uses for proxying.
 * @param keepProxy Whether messages should retain the proxy tag after being proxied.
 * @param privacy The privacy settings of the member.
 * @since 3.4.5
 */
@Serializable
data class PluralKitMember(
	val id: String,
	val uuid: String,
	val name: String,

	@SerialName("display_name")
	val displayName: String?,

	val color: String?, // Ask the PK Docs
	val birthday: String?,
	val pronouns: String?,

	@SerialName("avatar_url")
	val avatarUrl: String?,

	val banner: String?,
	val description: String?,
	val created: Instant?,

	@SerialName("proxy_tags")
	val proxyTags: List<PluralKitProxyTag>,

	@SerialName("keep_proxy")
	val keepProxy: Boolean,
	val privacy: PluralKitMemberPrivacy?,
)
