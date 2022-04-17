@file:OptIn(ExperimentalTime::class)


package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.*
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import io.ktor.util.toByteArray
import kotlinx.datetime.Clock
import kotlinx.serialization.decodeFromString
import net.irisshaders.lilybot.utils.responseEmbedInChannel
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.time.ExperimentalTime
import kotlinx.serialization.json.Json

class LogUploading : Extension() {
	override val name = "log-uploading"

	@Suppress("PrivatePropertyName")
	private val LOG_FILE_EXTENSIONS = setOf("log", "gz", "txt")

	override suspend fun setup() {
		/**
		 * Upload files that have the extensions specified in [LOG_FILE_EXTENSIONS] to mclo.gs,
		 * giving a user confirmation.
		 *
		 * @author Caio_MGT
		 * @author maximumpower55
		 */
		event<MessageCreateEvent> {
			action {
				val eventMessage = event.message.asMessageOrNull() // Get the message

				eventMessage.attachments.forEach { attachment ->
					val attachmentFileName = attachment.filename
					val attachmentFileExtension = attachmentFileName.substring(
						attachmentFileName.lastIndexOf(".") + 1)

					if (attachmentFileExtension in LOG_FILE_EXTENSIONS) {
						val logBytes = attachment.download()

						val builder = StringBuilder()

						if (attachmentFileExtension != "gz") {
							// If the file is not a gz log, we just decode it
							builder.append(logBytes.decodeToString())
						} else {
							// If the file is a gz log, we convert it to a byte array,
							// and unzip it
							val bis = ByteArrayInputStream(logBytes)
							val gis = GZIPInputStream(bis)

							builder.append(String(gis.readAllBytes()))
						}

						// Ask the user to remove NEC to ease the debugging on the support team
						val necText = "at Not Enough Crashes"
						val indexOfNECText = builder.indexOf(necText)
						if (indexOfNECText != -1) {
							responseEmbedInChannel(
								eventMessage.channel,
								"Not Enough Crashes detected in logs",
								"Not Enough Crashes (NEC) is well known to cause issues and often " +
										"makes the debugging process more difficult. " +
										"Please remove NEC, recreate the issue, and resend the relevant files " +
										"(i.e. log or crash report) if the issue persists.",
								DISCORD_PINK,
								eventMessage.author
							)
						} else {
							// Ask the user if they're ok with uploading their log to a paste site
							var confirmationMessage: Message? = null

							confirmationMessage = responseEmbedInChannel(
								eventMessage.channel,
								"Do you want to upload this file to mclo.gs?",
								"mclo.gs is a website that allows users to share minecraft logs through " +
										"public posts.\nIt's easier for the support team to view " +
										"the file on mclo.gs, do you want it to be uploaded?",
								DISCORD_PINK,
								eventMessage.author
							).edit {
								components {
									ephemeralButton(row = 0) {
										label = "Yes"
										style = ButtonStyle.Success

										action {
											// Make sure only the log uploader can confirm this
											if (event.interaction.user.id == eventMessage.author?.id) {
												// Delete the confirmation and proceed to upload
												confirmationMessage!!.delete()

												val uploadMessage = eventMessage.channel.createEmbed {
													color = DISCORD_PINK
													title = "Uploading `$attachmentFileName` to mclo.gs..."
													timestamp = Clock.System.now()

													footer {
														text = "Uploaded by ${eventMessage.author?.tag}"
														icon = eventMessage.author?.avatar?.url
													}
												}

												try {
													val response = postToHasteBin(builder.toString())

													uploadMessage.edit {
														embed {
															color = DISCORD_PINK
															title = "`$attachmentFileName` uploaded to mclo.gs"
															timestamp = Clock.System.now()

															footer {
																text = "Uploaded by ${eventMessage.author?.tag}"
																icon = eventMessage.author?.avatar?.url
															}
														}

														actionRow {
															linkButton(response) {
																label = "Click here to view"
															}
														}
													}
												} catch (e: Exception) {
													// Just swallow this exception
													// If something has gone wrong here, something is wrong
													// somewhere else, so it's probably fine
												}
											} else {
												respond { content = "Only the uploader can use this menu." }
											}
										}
									}

									ephemeralButton(row = 0) {
										label = "No"
										style = ButtonStyle.Danger

										action {
											if (event.interaction.user.id == eventMessage.author?.id) {
												confirmationMessage!!.delete()
											} else {
												respond { content = "Only the uploader can use this menu." }
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	@kotlinx.serialization.Serializable
	data class logClass(val success: Boolean, val id: String, val url: String, val raw: String)

	private suspend fun postToHasteBin(text: String): String {
		val client = HttpClient()

		val response = client.post<HttpResponse>("https://api.mclo.gs/1/log") {
			body = FormDataContent(Parameters.build {
				append("content", text)
			})
		}.content.toByteArray().decodeToString()
		val log = Json.decodeFromString<logClass>(response)
		if (log.success == false){
			throw Exception("Failed to upload log")
			/* mclo.gs returns the error but I can't think
			of a good way to get it, as if I add an error value to logClass,
			Json.decodeFromString will hang if it does not find that value
			on the JSON String - CaioMGT
			 */
		}
		client.close()
		return "https://mclo.gs/" + log.id
	}
}
