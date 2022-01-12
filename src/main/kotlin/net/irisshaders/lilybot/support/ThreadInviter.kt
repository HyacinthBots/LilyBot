@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.support

import com.kotlindiscord.kord.extensions.checks.isInThread
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.utils.delete
import com.kotlindiscord.kord.extensions.utils.respond
import dev.kord.common.entity.ArchiveDuration
import dev.kord.common.entity.MessageType
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.reply
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.event.message.MessageCreateEvent
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.last
import net.irisshaders.lilybot.utils.SUPPORT_CHANNEL
import net.irisshaders.lilybot.utils.SUPPORT_TEAM
import kotlin.time.ExperimentalTime

class ThreadInviter : Extension() {
    override val name = "supportthreads"

    override suspend fun setup() {
        /**
         * Thread inviting system for Support Channels
         * @author IMS212
         */
        event<MessageCreateEvent> {
            check { failIf(event.message.channelId != SUPPORT_CHANNEL) }
            check { failIf(event.member!!.id == kord.selfId) }
            check { failIf(event.message.type == MessageType.ThreadCreated) } // Don't try and run this if the thread is manually created

            action {
                var userThreadExists = false
                var existingUserThread: TextChannelThread? = null
                val textChannel = event.message.getChannel() as TextChannel
                //TODO: this is incredibly stupid, there has to be a better way to do this.
                textChannel.activeThreads.collect {
                    if (it.name == "Support thread for " + event.member!!.asUser().username) {
                        userThreadExists = true
                        existingUserThread = it
                    }
                }

                if (userThreadExists) {
                    val response = event.message.respond {
                        content =
                            "You already have a thread, please talk about your issue in it. " + existingUserThread!!.mention
                    }
                    event.message.delete("User already has a thread")
                    response.delete(10000L, false)
                } else {
                    val thread =
                        textChannel.startPublicThreadWithMessage(
                            event.message.id,
                            "Support thread for " + event.member!!.asUser().username,
                            ArchiveDuration.Hour
                        )
                    val editMessage = thread.createMessage("edit message")

                    editMessage.edit {
                        this.content =
                            event.member!!.asUser().mention + ", the " + event.getGuild()
                                ?.getRole(SUPPORT_TEAM)?.mention + " will be with you shortly!"
                    }

                    if (textChannel.messages.last().author?.id == kord.selfId) {
                        textChannel.deleteMessage(
                            textChannel.messages.last().id,
                            "Automatic deletion of thread creation message"
                        )
                    }

                    val response = event.message.reply {
                        content = "A thread has been created for you: " + thread.mention
                    }
                    response.delete(10000L, false)
                }
            }
        }
    }

}
