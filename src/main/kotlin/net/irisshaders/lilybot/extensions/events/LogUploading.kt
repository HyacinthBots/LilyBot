package net.irisshaders.lilybot.extensions.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.sentry.tag
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.download
import com.kotlindiscord.kord.extensions.utils.isNullOrBot
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.readBytes
import io.ktor.http.Parameters
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import net.irisshaders.lilybot.database.collections.SupportConfigCollection
import net.irisshaders.lilybot.database.collections.ThreadsCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.botHasChannelPerms
import net.irisshaders.lilybot.utils.configPresent
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

/**
 * The class for the uploading of logs to mclo.gs.
 *
 * @since 2.0
 */
class LogUploading : Extension() {
	override val name = "log-uploading"

	/** The file extensions that will be read and decoded by this system. */
	private val logFileExtensions = setOf("log", "gz", "txt")

	override suspend fun setup() {
		/**
		 * Upload files that have the extensions specified in [logFileExtensions] to mclo.gs,
		 * giving a user confirmation.
		 *
		 * @author Caio_MGT, maximumpower55
		 * @since 2.0
		 */
		event<MessageCreateEvent> {
			check {
				anyGuild()
				failIf {
					event.message.author.isNullOrBot()
					event.message.getChannelOrNull() !is MessageChannel
				}
				botHasChannelPerms(Permissions(Permission.SendMessages, Permission.EmbedLinks))
				configPresent(ConfigType.SUPPORT)
			}
			action {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)!!
				var deferUploadUntilThread = false
				if (supportConfig.enabled && event.message.channel.id == supportConfig.channel) {
					deferUploadUntilThread = true
				}

				val eventMessage = event.message.asMessageOrNull() // Get the message
				var uploadChannel = eventMessage.channel.asChannel()

				if (deferUploadUntilThread) {
					delay(1500) // Delay to allow for thread creation
					ThreadsCollection().getOwnerThreads(event.member!!.id).forEach {
						if (event.getGuild()!!.getChannelOf<TextChannelThread>(it.threadId).parentId ==
							supportConfig.channel
						) {
							uploadChannel = event.getGuild()!!.getChannelOf<GuildMessageChannel>(it.threadId)
						}
					}
				}

				eventMessage.attachments.forEach { attachment ->
					val attachmentFileName = attachment.filename
					val attachmentFileExtension = attachmentFileName.substring(
						attachmentFileName.lastIndexOf(".") + 1
					)

					if (attachmentFileExtension in logFileExtensions) {
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
							uploadChannel.createEmbed {
								title = "Not Enough Crashes detected in logs"
								description = "Not Enough Crashes (NEC) is well known to cause issues and often " +
										"makes the debugging process more difficult. " +
										"Please remove NEC, recreate the issue, and resend the relevant files " +
										"(i.e. log or crash report) if the issue persists."
								footer {
									text = eventMessage.author?.tag ?: ""
									icon = eventMessage.author?.avatar?.url
								}
								color = DISCORD_PINK
							}
						} else {
							// Ask the user if they're ok with uploading their log to a paste site
							var confirmationMessage: Message? = null

							confirmationMessage = uploadChannel.createMessage {
								embed {
									title = "Do you want to upload this file to mclo.gs?"
									description =
										"mclo.gs is a website that allows users to share minecraft logs " +
												"through public posts.\nIt's easier for the support team to view " +
												"the file on mclo.gs, do you want it to be uploaded?"
									footer {
										text = eventMessage.author?.tag ?: ""
										icon = eventMessage.author?.avatar?.url
									}
									color = DISCORD_PINK
								}

								components {
									ephemeralButton(row = 0) {
										label = "Yes"
										style = ButtonStyle.Success

										action {
											// Make sure only the log uploader can confirm this
											if (event.interaction.user.id == eventMessage.author?.id) {
												// Delete the confirmation and proceed to upload
												confirmationMessage!!.delete()

												val uploadMessage = uploadChannel.createEmbed {
													title = "Uploading `$attachmentFileName` to mclo.gs..."
													footer {
														text = "Uploaded by ${eventMessage.author?.tag}"
														icon = eventMessage.author?.avatar?.url
													}
													timestamp = Clock.System.now()
													color = DISCORD_PINK
												}

												try {
													val response = postToMCLogs(builder.toString())

													uploadMessage.edit {
														embed {
															title = "`$attachmentFileName` uploaded to mclo.gs"
															footer {
																text = "Uploaded by ${eventMessage.author?.tag}"
																icon = eventMessage.author?.avatar?.url
															}
															timestamp = Clock.System.now()
															color = DISCORD_PINK
														}

														actionRow {
															linkButton(response) {
																label = "Click here to view"
															}
														}
													}
												} catch (e: IOException) {
													// If the upload fails, we'll just show the error
													uploadMessage.edit {
														embed {
															title =
																"Failed to upload `$attachmentFileName` to mclo.gs"
															description = "Error: $e"
															footer {
																text = "Uploaded by ${eventMessage.author?.tag}"
																icon = eventMessage.author?.avatar?.url
															}
															timestamp = Clock.System.now()
															color = DISCORD_RED
														}
													}
													// Capture Exception to Sentry
													this.sentry.captureException(e) {
														tag("log_file_name", attachmentFileName)
														tag("extension", extension.name)
														tag("id", eventMessage.id.toString())
													}
													e.printStackTrace()
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

	/**
	 * This data class will store the data of the log being uploaded by [postToMCLogs].
	 *
	 * **NOTE:** Setting these to null is necessary in case a value is missing, which would cause an error.
	 *
	 * @param success Whether the log upload was a success or not
	 * @param id The ID of the log uploaded
	 * @param error Any errors that were returned by the upload
	 *
	 * @author CaioMGT
	 * @since 3.1.0
	 */
	@Serializable
	data class LogData(val success: Boolean, val id: String? = null, val error: String? = null)

	/**
	 * This function coordinates the uploading of a user provided log to mclo.gs, allowing support to view logs easily.
	 *
	 * @param text The content of the log
	 * @return The link to the log upload
	 * @author Maximumpower55, CaioMGT
	 * @since 3.1.0
	 */
	private suspend fun postToMCLogs(text: String): String {
		val client = HttpClient()
		val response = client.post("https://api.mclo.gs/1/log") {
			setBody(
				FormDataContent(
					Parameters.build {
						append("content", text)
					}
				)
			)
		}.readBytes().decodeToString()
		client.close()
		val json = Json { ignoreUnknownKeys = true } // to avoid causing any errors due to missing values in the JSON
		val log = json.decodeFromString<LogData>(response)
		if (log.success) {
			return "https://mclo.gs/" + log.id
		} else {
			throw IOException("Failed to upload log: " + log.error)
		}
	}
}
