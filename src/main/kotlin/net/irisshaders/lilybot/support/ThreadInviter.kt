package net.irisshaders.lilybot.support

import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.types.respondEphemeral
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.last
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.MODERATORS
import net.irisshaders.lilybot.utils.OWNER_ID
import net.irisshaders.lilybot.utils.SUPPORT_TEAM

class ThreadInviter : Extension() {
    override val name = "threads"

    override suspend fun setup() {
        publicSlashCommand {  // Ephemeral slash commands have private responses
            name = "supportthread"
            description = "Creates the button to launch a support thread."
            allowUser(OWNER_ID)


            // Use guild commands for testing, global ones take up to an hour to update
            guild(GUILD_ID)

            @Suppress("DSL_SCOPE_VIOLATION")
            action {
                respond {
                    embed {
                        title = "Create support thread"
                        description = "Please click the button below if you need support with Iris."
                        components {
                            ephemeralButton {
                                label = "Create"
                                style = ButtonStyle.Primary
                                val textchannel = channel.asChannel() as TextChannel

                                action {
                                    var userThreadExists = false
                                    var userThreadArchived = false
                                    var existingUserThread : TextChannelThread? = null
                                    //TODO: this is incredibly stupid, there has to be a better way to do this.
                                    textchannel.activeThreads.collect {
                                        if (it.name == "Support thread for " + user.asUser().username) {
                                            userThreadExists = true
                                            existingUserThread = it
                                        }
                                    }
                                    textchannel.getPublicArchivedThreads().collect {
                                        if (it.name == "Support thread for " + user.asUser().username) {
                                            userThreadArchived = true
                                            existingUserThread = it
                                        }
                                    }
                                    if (userThreadExists) {
                                        respondEphemeral {
                                            content = "You already have a thread! " + existingUserThread!!.mention
                                        }
                                    } else if (userThreadArchived) {
                                        existingUserThread!!.createMessage("Unarchived thread.")
                                        respondEphemeral {
                                            content = "Your previous thread has been unarchived.  " + existingUserThread!!.mention
                                        }
                                    } else {
                                        val thread = textchannel.startPublicThread("Support thread for " + user.asUser().username)
                                        val editMessage = thread.createMessage("edit message")
                                        editMessage.edit {
                                            this.content = user.mention + ", the " + guild?.getRole(SUPPORT_TEAM)?.mention + " will be with you shortly!"
                                        }
                                        if (channel.messages.last().author?.id == kord.selfId) {
                                            channel.deleteMessage(
                                                    channel.messages.last().id,
                                                    "Automatic deletion of thread creation message"
                                            )
                                        }
                                        respondEphemeral {
                                            content = "A thread has been created for you: " + thread.mention
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