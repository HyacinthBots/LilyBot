@file:OptIn(ExperimentalTime::class)
@file:Suppress("PrivatePropertyName", "BlockingMethodInNonBlockingContext")

package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.edit
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.DatabaseHelper
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.ResponseHelper
import java.io.ByteArrayInputStream
import java.util.zip.GZIPInputStream
import kotlin.time.ExperimentalTime

class MessageEvents : Extension() {
	override val name = "messageevents"

	private val LOG_FILE_EXTENSIONS = setOf("log", "gz", "txt")

	override suspend fun setup() {
		/**
		 * Log the deletion of messages to the guilds [DatabaseManager.Config.messageLogs] channel
		 * @author NoComment1105
		 */
		event<MessageDeleteEvent> {
			action {
				// Try to get the  message logs channel
				val messageLogId: String? = DatabaseHelper.selectInConfig(event.guildId!!, DatabaseManager.Config.messageLogs)

				// This uses return@action because events are triggered on bot startup
				if (messageLogId == "NoSuchElementException" || messageLogId == null) { return@action }

				check { failIf(event.message?.author?.isBot == true || event.message?.author?.id == kord.selfId) }

				val guild = kord.getGuild(event.guildId!!)
				val messageLogChannel = guild?.getChannel(Snowflake(messageLogId)) as GuildMessageChannelBehavior
				val messageContent = event.message?.asMessageOrNull()?.content.toString()
				val eventMessage = event.message
				val attachments = eventMessage?.attachments
				val messageLocation = event.channel.id.value

				messageLogChannel.createEmbed {
					color = DISCORD_PINK
					title = "Message Deleted"
					description = "Location: <#$messageLocation>"
					timestamp = Clock.System.now()

					field {
						name = "Message Contents:"
						value = messageContent.ifEmpty { "Failed to get content of message" }
						inline = false
					}
					if (attachments != null && attachments.isNotEmpty()) {
						val attachmentUrls = StringBuilder()
						for (attachment in attachments) {
							attachmentUrls.append(attachment.data.url + "\n")
						}
						field {
							name = "Attachments:"
							value = attachmentUrls.trim().toString()
							inline = false
						}
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
			}
		}

		/**
		 * Upload files that have the extensions specified in [LOG_FILE_EXTENSIONS] to hastebin, giving a user confirmation
		 *
		 * @author maximumpower55
		 */
		event<MessageCreateEvent> {
			action {
				val eventMessage = event.message.asMessageOrNull() // Get the message

				eventMessage.attachments.forEach { attachment ->
					val attachmentFileName = attachment.filename
					val attachmentFileExtension = attachmentFileName.substring(attachmentFileName.lastIndexOf(".") + 1)

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
						val indexOfnecText = builder.indexOf(necText)
						if (indexOfnecText != -1) {
							ResponseHelper.responseEmbedInChannel(
								eventMessage.channel,
								"Not Enough Crashes detected in logs",
								"Not Enough Crashes (NEC) is well know to cause issues and often makes the debugging process more difficult. Please remove NEC, recreate the issue, and resend the relevant files (ie. log or crash report) if the issue persists.",
								DISCORD_PINK,
								eventMessage.author
							)
						} else {
							// Ask the user if they're ok with uploading their log to a paste site
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
											// Make sure only the log uploader can confirm this
											if (event.interaction.user.id == eventMessage.author?.id) {
												// Delete the confirmation and proceed to upload
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
