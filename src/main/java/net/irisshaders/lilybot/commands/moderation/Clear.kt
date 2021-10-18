package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import kotlinx.coroutines.flow.*
import java.lang.Integer.min

class Clear: Extension() {
    override val name = "clear"
    val GUILD_ID = Snowflake(  // Store this as a Discord snowflake, aka an ID
            env("GUILD_ID").toLong()  // An exception will be thrown if it can't be found
    )


    override suspend fun setup() {
        ephemeralSlashCommand(::ClearArgs) {  // Public slash commands have public responses
            name = "clear"
            description = "Clears messages."
            allowByDefault = false
            allowedRoles.add(Snowflake(env("MODERATOR_ROLE")))


            // Use guild commands for testing, global ones take up to an hour to update
            guild(GUILD_ID)

            action {
                val kord = this@Clear.kord
                val messageAmount = arguments.messages
                val messageHolder = arrayListOf<Snowflake>()
                val textChannel = channel as GuildMessageChannelBehavior

                channel.getMessagesBefore(channel.messages.last().id, min(messageAmount, 100)).filterNotNull().onEach { messageHolder.add(it.id)
                        }.catch { it.printStackTrace()
                            println("error")
                        }.collect()
                textChannel.bulkDelete(messageHolder)
                respond {
                    content = "Cleared $messageAmount messages!"
                }

            }
        }
    }

    inner class ClearArgs : Arguments() {
        // A single user argument, required for the command to be able to run
        val messages by int(
                "messages",
                description = "Messages"
        )
    }
}