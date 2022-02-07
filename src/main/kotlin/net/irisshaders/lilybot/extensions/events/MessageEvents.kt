@file:OptIn(ExperimentalTime::class)
@file:Suppress("PrivatePropertyName", "BlockingMethodInNonBlockingContext")

package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.sentry.BreadcrumbType
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.MESSAGE_LOGS
import net.irisshaders.lilybot.utils.ResponseHelper
import net.irisshaders.lilybot.utils.SUPPORT_CHANNEL
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.collections.set
import kotlin.time.ExperimentalTime

class MessageEvents : Extension() {
	override val name = "messageevents"

	private val LOG_FILE_EXTENSIONS = setOf("log", "gz", "txt")

	override suspend fun setup() {
		/**
		 * Log the deletion of messages to the guilds [MESSAGE_LOGS] channel
		 * @author NoComment1105
		 */
		event<MessageDeleteEvent> {
			action {
				// Ignore messages from Lily itself
				if (event.message?.author?.id == kord.selfId) return@action
				if (event.message?.channel !is ThreadChannel && event.message?.channel?.id == SUPPORT_CHANNEL) return@action

				val actionLog = event.guild?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior
				val messageContent = event.message?.asMessageOrNull()?.content.toString()
				val eventMessage = event.message
				val messageLocation = event.channel.id.value

				actionLog.createEmbed {
					color = DISCORD_PINK
					title = "Message Deleted"
					description = "Location: <#$messageLocation>"
					timestamp = Clock.System.now()

					field {
						name = "Message Contents:"
						value =
							messageContent.ifEmpty {
								"Failed to get content of message\nMessage was likely from a Bot"
							}
						inline = false
					}
					field {
						name = "Message Author:"
						value = eventMessage?.author?.tag.toString()
						inline = true
					}
					field {
						name = "Author ID:"
						value = eventMessage?.author?.id.toString()
						inline = true
					}
				}

				sentry.breadcrumb(BreadcrumbType.Info) {
					category = "events.messageevents.MessageDeleted"
					message = "A message was deleted"
					data["content"] = messageContent.ifEmpty { "Failed to get content of message" }
				}
			}
		}

		/**
		 * Upload files that have the extensions specified in [LOG_FILE_EXTENSIONS] to hastebin, giving a user confirmation
		 *
		 * @author maximumpower55
		 */
		event<MessageCreateEvent> {
			action {
				val eventMessage = event.message.asMessageOrNull()

				eventMessage.attachments.forEach { attachment ->
					val attachmentFileName = attachment.filename
					val attachmentFileExtension = attachmentFileName.substring(attachmentFileName.lastIndexOf(".") + 1)

					if (attachmentFileExtension in LOG_FILE_EXTENSIONS) {
						val logBytes = attachment.download()

						val builder = StringBuilder()

						if (attachmentFileExtension != "gz") {
							builder.append(logBytes.decodeToString())
						} else {
							val bis = ByteArrayInputStream(logBytes)
							val gis = GZIPInputStream(bis)

							builder.append(String(gis.readAllBytes()))
						}

						val necText = "at Not Enough Crashes";
						val indexOfnecText = builder.indexOf(necText);
						if (indexOfnecText != -1) {
							ResponseHelper.responseEmbedInChannel(
								eventMessage.channel,
								"Not Enough Crashes detected in logs",
								"Not Enough Crashes (NEC) is well know to cause issues and often makes the debugging process more difficult. Please remove NEC, recreate the issue, and resend the relevant files (ie. log or crash report) if the issue persists.",
								DISCORD_PINK,
								eventMessage.author
							)
						} else {
							var confirmationMessage: Message? = null

							confirmationMessage = ResponseHelper.responseEmbedInChannel(
								eventMessage.channel,
								"Do you want to upload this file to Hastebin?",
								"Hastebin is a website that allows users to share plain text through public posts called “pastes.”\nIt's easier for the support team to view the file on Hastebin, do you want it to be uploaded?",
								DISCORD_PINK,
								eventMessage.author
							).edit {
								components {
									ephemeralButton(row = 0) {
										label = "Yes"
										style = ButtonStyle.Success

										action {
											sentry.breadcrumb(BreadcrumbType.Info) {
												category = "events.messageevents.loguploading.uploadAccept"
												message = "Upload accpeted"
											}
											if (event.interaction.user.id == eventMessage.author?.id) {
												confirmationMessage!!.delete()

												val uploadMessage = eventMessage.channel.createEmbed {
													color = DISCORD_PINK
													title = "Uploading `$attachmentFileName` to Hastebin..."
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
															title = "`$attachmentFileName` uploaded to Hastebin"
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
													sentry.breadcrumb(BreadcrumbType.Error) {
														category = "events.messageevnets.loguploading.UploadTask"
														message = "Failed to upload a file to hastebin"
													}
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
												sentry.breadcrumb(BreadcrumbType.Info) {
													category = "events.messagevents.loguploading.uploadDeny"
													message = "Upload of log denied"
												}
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

	private suspend fun postToHasteBin(text: String): String {
		val client = HttpClient()

		var response = client.post<HttpResponse>("https://www.toptal.com/developers/hastebin/documents") {
			body = text
		}.content.toByteArray().decodeToString()

		if (response.contains("\"key\"")) {
			response = "https://www.toptal.com/developers/hastebin/" + response.substring(
				response.indexOf(":") + 2,
				response.length - 2
			)
		}

		client.close()

		return response
	}
}
