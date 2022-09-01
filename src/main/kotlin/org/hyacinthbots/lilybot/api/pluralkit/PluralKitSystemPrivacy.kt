package org.hyacinthbots.lilybot.api.pluralkit

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#system-model).
 *
 * @param visibility Whether the system is publicly visible or not.
 * @param namePrivacy Whether the system's name is publicly visible or not.
 * @param descriptionPrivacy Whether the system description is publicly visible or not.
 * @param birthdayPrivacy Whether the system's birthday is publicly visible or not.
 * @param pronounPrivacy Whether the system's pronouns are publicly visible or not.
 * @param avatarPrivacy Whether the system's avatar is publicly visible or not.
 * @param metadataPrivacy Whether the system's metadata is publicly visible or not.
 * @since 3.4.5
 */
@Serializable
data class PluralKitSystemPrivacy(
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
