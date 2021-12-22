@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.events

import com.kotlindiscord.kord.extensions.DISCORD_PINK
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.event.message.MessageDeleteEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.MESSAGE_LOGS
import kotlin.time.ExperimentalTime

/**
 * Log Message events to the Guilds channel action log.
 * @author NoComment1105
 */
class MessageEvents : Extension() {
    override val name = "messageevents"

    override suspend fun setup() {
        event<MessageDeleteEvent> {
            action {
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
    }
}