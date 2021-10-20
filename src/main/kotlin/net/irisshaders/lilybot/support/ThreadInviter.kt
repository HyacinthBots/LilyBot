package net.irisshaders.lilybot.support

import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.edit
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.last
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.MODERATORS
import net.irisshaders.lilybot.utils.OWNER_ID
import kotlin.system.exitProcess

class ThreadInviter : Extension() {
    override val name = "threads"

    override suspend fun setup() {
        publicSlashCommand() {  // Ephemeral slash commands have private responses
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
                                    val thread = textchannel.startPublicThread("Thread for " + user.asUser().username)
                                    val editMessage = thread.createMessage("a")
                                    editMessage.edit {
                                        this.content = guild?.getRole(MODERATORS)?.mention
                                    }
                                    if (channel.messages.last().author?.id == kord.selfId) {
                                        channel.deleteMessage(
                                            channel.messages.last().id,
                                            "Automatic deletion of thread creation message"
                                        )
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