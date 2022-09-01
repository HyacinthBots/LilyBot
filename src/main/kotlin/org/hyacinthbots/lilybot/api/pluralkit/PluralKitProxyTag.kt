package org.hyacinthbots.lilybot.api.pluralkit

import kotlinx.serialization.Serializable

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#proxytag-object).
 *
 * **NOTE** `prefix + "text" + suffix` must be shorter than 100 characters in total
 *
 * @param prefix The prefix to proxy a message.
 * @param suffix The suffix to proxy a message.
 */
@Serializable
data class PluralKitProxyTag(
	val prefix: String?,
	val suffix: String?,
)
