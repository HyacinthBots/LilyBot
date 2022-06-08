package net.irisshaders.lilybot.api.pluralkit

import dev.kord.common.entity.Snowflake
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The URL of the [PluralKit API](https://pluralkit.me/api), which can be used for accessing other parts of the api
 * without typing out duplicating the main URL.
 */
internal const val PK_API_URL = "https://api.pluralkit.me/v2"

/** The URL of messages from the [PluralKit API](https://pluralkit.me/api). */
internal const val MESSAGE_URL = "$PK_API_URL/messages/{id}"

object PluralKit {

	/** The client used for querying values from the API. */
	private val client = HttpClient {
		install(ContentNegotiation) {
			json(
				Json { ignoreUnknownKeys = true },
				ContentType.Any
			)
		}

		expectSuccess = true
	}

	/**
	 * Using a provided message [Snowflake], we call [checkIfProxied] to find out if the message was proxied or not.
	 *
	 * @param id The ID of the message being checked
	 * @see checkIfProxied
	 * @return True if proxied, false if not
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	suspend fun checkIfProxied(id: Snowflake) = checkIfProxied(id.toString())

	/**
	 * Using a provided message ID, we call [getProxiedMessageAuthorId] to find out the author of the message.
	 *
	 * @param id The ID of the message being checked
	 * @return The ID of the message author or null
	 * @see getProxiedMessageAuthorId
	 * @author NoComment1105
	 * @since 3.3.2
	 */
	suspend fun getProxiedMessageAuthorId(id: Snowflake) = getProxiedMessageAuthorId(id.toString())

	/**
	 * Using a provided message ID, we check against the [PluralKit API](https://pluralkit.me/api/) to find out if
	 * the message has been proxied. If it has been, we'll return true on the function, allowing this to be checked in
	 * for in other places in the bot. If getting the message returns an error response in the range of 400 to 600, we
	 * return false, as the message has not been proxied.
	 *
	 * @param id The ID of the message being checked as a string
	 * @return True if proxied, false if not
	 * @see checkIfProxied
	 * @author NoComment1105
	 * @since 3.3.0
	 */
	 private suspend fun checkIfProxied(id: String): Boolean {
		val url = MESSAGE_URL.replace("{id}", id)

		var proxied = false

		try {
			client.get(url).body<PluralKitMessage>()

			proxied = true
		} catch (e: ClientRequestException) {
			if (e.response.status.value !in 200 until 300) {
				proxied = false
			}
		}

		return proxied
	}

	/**
	 * Using a provided message ID, we check against the [PluralKit API](https://pluralkit.me/api/) to find out the
	 * author of the proxied message. If there is no found author, we return null, or the [Snowflake] ID of the author.
	 *
	 * @param id The ID of the message being checked as a string
	 * @return The ID of the message author or null
	 * @see getProxiedMessageAuthorId
	 * @author NoComment1105
	 * @since 3.3.2
	 */
	 private suspend fun getProxiedMessageAuthorId(id: String): Snowflake? {
		val url = MESSAGE_URL.replace("{id}", id)

		var authorId: Snowflake? = null

		try {
			val message: PluralKitMessage = client.get(url).body()

			authorId = message.sender
		} catch (e: ClientRequestException) {
			if (e.response.status.value !in 200 until 300) {
				authorId = null
			}
		}

		return authorId
	}
}

/**
 * This is the data class for a PluralKit message, as per the documentation on the
 * [PluralKit Docs site](https://pluralkit.me/api/models/#message-model). It is missing the System and Member objects
 * currently (31st May), since for the use case above, they're not fully required.
 *
 * **NOTE:** All values are encoded as a string by the api for precision reasons.
 *
 * @param timestamp The time the message was sent
 * @param id The ID of the message sent by the webhook
 * @param original The ID of the (now-deleted) message that triggered the proxy
 * @param sender The user ID of the account that triggered the proxy.
 * @param channel The ID of the channel the message was sent in
 * @param guild The ID of the server the message was sent in
 * @since 3.3.0
 */
@Serializable
data class PluralKitMessage(
	val timestamp: Instant,
	val id: Snowflake,
	val original: Snowflake,
	val sender: Snowflake,
	val channel: Snowflake,
	val guild: Snowflake
)
