@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.utils.download
import com.kotlindiscord.kord.extensions.DISCORD_RED
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.Message
import dev.kord.core.event.message.MessageDeleteEvent
import dev.kord.core.event.message.MessageCreateEvent
import dev.kord.core.behavior.edit
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.create.actionRow
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.MessageModifyBuilder
import dev.kord.rest.builder.message.modify.actionRow
import dev.kord.rest.builder.message.modify.embed
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import dev.kord.common.entity.ButtonStyle
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ResponseHelper
import net.irisshaders.lilybot.utils.MESSAGE_LOGS
import net.irisshaders.lilybot.utils.SUPPORT_CHANNEL
import kotlin.time.ExperimentalTime
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.content.*
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.*
import io.ktor.client.engine.*
import io.ktor.util.toByteArray
import io.ktor.util.GZip
import java.util.zip.GZIPInputStream
import java.io.ByteArrayInputStream
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Log Message events to the Guilds channel action log.
 * @author NoComment1105
 */
class MessageEvents : Extension() {
    override val name = "messageevents"

    private val LOG_FILE_EXTENSIONS = setOf("log", "gz", "txt")

    override suspend fun setup() {
        event<MessageDeleteEvent> {
            action {
                // Ignore messages from Lily itself
                if (event.message?.author?.id == kord.selfId) return@action
                if (event.message?.channel !is ThreadChannel && event.message?.channel?.id == SUPPORT_CHANNEL) return@action

                val actionLog = event.guild?.getChannel(MESSAGE_LOGS) as GuildMessageChannelBehavior
                val message = event.message?.asMessageOrNull()?.content.toString()
                val messageAuthor = event.message!!.author!!.tag
                val messageAuthorId = event.message!!.author!!.id.value
                val messageLocation = event.channel.id.value

                actionLog.createEmbed {
                    color = DISCORD_PINK
                    title = "Message Deleted"
                    description = "Location: <#$messageLocation>"
                    timestamp = Clock.System.now()

                    field {
                        name = "Message Contents:"
                        value = message
                        inline = false
                    }
                    field {
                        name = "Message Author:"
                        value = messageAuthor
                        inline = true
                    }
                    field {
                        name = "Author ID:"
                        value = "$messageAuthorId"
                        inline = true
                    }
                }
            }
        }

        event<MessageCreateEvent> {
            action {
                val message = event.message.asMessage()
                val messageAuthor = message.author
                val messageAuthorTag = messageAuthor!!.tag
                val messageAuthorAvatar = messageAuthor.avatar!!.url
                val attachments = message.attachments

                attachments.forEach {attachment ->
                    val attachmentFileName = attachment.filename
                    val attachmentFileExtension = attachmentFileName.substring(attachmentFileName.lastIndexOf(".") + 1)

                    if (attachmentFileExtension in LOG_FILE_EXTENSIONS) {
                        var confirmationMessage: Message? = null

                        confirmationMessage = ResponseHelper.responseEmbedInChannel(
                            message.channel,
                            "Do you want to upload this file to Hastebin?", 
                            "Hastebin is a website that allows users to share plain text through public posts called “pastes.”\nIt's easier for the support team to view the file on Hastebin, do you want it to be uploaded?", 
                            DISCORD_BLURPLE,
                            null
                        ).edit {
                            components {
                                ephemeralButton(row = 0) {
                                    label = "Yes"
                                    style = ButtonStyle.Primary

                                    action {
                                        if (event.interaction.user.id == messageAuthor.id) {
                                            confirmationMessage!!.delete()

                                            val uploadMessage = message.channel.createEmbed {
                                                color = DISCORD_BLURPLE
                                                title = "Uploading `" + attachmentFileName + "` to Hastebin..."
                                                timestamp = Clock.System.now()

                                                footer {
                                                    text = "Uploaded by " + messageAuthorTag
                                                    icon = messageAuthorAvatar
                                                }
                                            }

                                            try {
                                                val logBytes = attachment.download()

                                                val builder = StringBuilder()

                                                if (attachmentFileExtension != "gz") {
                                                    builder.append(logBytes.decodeToString())
                                                } else {
                                                    val bis = ByteArrayInputStream(logBytes)
                                                    val gis = GZIPInputStream(bis)

                                                    builder.append(String(gis.readAllBytes()))
                                                }

                                                val response = postToHasteBin(builder.toString())!!

                                                uploadMessage.edit {
                                                    embed {
                                                        color = DISCORD_BLURPLE
                                                        title = "`" + attachmentFileName + "` uploaded to Hastebin"
                                                        timestamp = Clock.System.now()

                                                        footer {
                                                            text = "Uploaded by " + messageAuthorTag
                                                            icon = messageAuthorAvatar
                                                        }
                                                    }

                                                    actionRow {
                                                        linkButton(response) {
                                                            label = "Click here to view"
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                uploadMessage.edit {
                                                    ResponseHelper.failureEmbed("Failed to upload `" + attachmentFileName + "` to Hastebin", e.toString())
                                                }
                                            }
                                        }
                                    }
                                }

                                ephemeralButton(row = 0) {
                                    label = "No"
                                    style = ButtonStyle.Secondary

                                    action {
                                        if (event.interaction.user.id == messageAuthor.id) confirmationMessage!!.delete()
                                    }
                                }
                            }
                        }
                    }
                }
            } 
        }
    }

    private suspend fun postToHasteBin(text: String) : String? {
        val client = HttpClient()

        var response = client.post<HttpResponse>("https://www.toptal.com/developers/hastebin/documents") {
            body = text
        }.content.toByteArray().decodeToString()

        if (response.contains("\"key\"")) {
            response = "https://www.toptal.com/developers/hastebin/" + response.substring(response.indexOf(":") + 2, response.length - 2);
        }

        client.close();

        return response;
    }
}