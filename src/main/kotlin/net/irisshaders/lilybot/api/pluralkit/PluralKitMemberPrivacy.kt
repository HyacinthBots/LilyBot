package net.irisshaders.lilybot.api.pluralkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#member-model).
 *
 * @param visibility Whether the member is publicly visible or not.
 * @param namePrivacy Whether the member's name is publicly visible or not.
 * @param descriptionPrivacy Whether the member's description is publicly visible or not.
 * @param birthdayPrivacy Whether the member's birthday is publicly visible or not.
 * @param pronounPrivacy Whether the member's pronoun is publicly visible or not.
 * @param avatarPrivacy Whether the member's avatar is publicly visible or not.
 * @param metadataPrivacy Whether the member's metadata is publicly visible or not.
 * @since 3.4.5
 */
@Serializable
data class PluralKitMemberPrivacy(
	val visibility: Boolean,

	@SerialName("name_privacy")
	val namePrivacy: Boolean,

	@SerialName("description_privacy")
	val descriptionPrivacy: Boolean,

	@SerialName("birthday_privacy")
	val birthdayPrivacy: Boolean,

	@SerialName("pronoun_privacy")
	val pronounPrivacy: Boolean,

	@SerialName("avatar_privacy")
	val avatarPrivacy: Boolean,

	@SerialName("metadata_privacy")
	val metadataPrivacy: Boolean,
)
