package net.irisshaders.lilybot.api.pluralkit

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#message-model).
 *
 * **NOTE:** All values are encoded as a string by the api for precision reasons.
 *
 * @param timestamp The time the message was sent
 * @param id The ID of the message sent by the webhook
 * @param original The ID of the (now-deleted) message that triggered the proxy
 * @param sender The user ID of the account that triggered the proxy.
 * @param channel The ID of the channel the message was sent in
 * @param guild The ID of the server the message was sent in
 * @param system The system that sent the message.
 * @param member The member that sent the message.
 * @since 3.3.0
 */
@Serializable
data class PluralKitMessage(
	val timestamp: Instant,
	val id: Snowflake,
	val original: Snowflake,
	val sender: Snowflake,
	val channel: Snowflake,
	val guild: Snowflake,

	val system: PluralKitSystem,
	val member: PluralKitMember
)
