package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.entity.channel.Channel
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.MODERATORS
import kotlin.system.exitProcess

class Moderation: Extension() {
    override val name = "moderation"

    override suspend fun setup() {
        // Clear command
        ephemeralSlashCommand(::ClearArgs) {  // Ephemeral slash commands have private responses
            name = "clear"
            description = "Clears messages."
            allowRole(MODERATORS)
            // Use guild commands for commands that have guild-specific actions
            guild(GUILD_ID)

            action {
                val messageAmount = arguments.messages
                val messageHolder = arrayListOf<Snowflake>()
                val textChannel = channel as GuildMessageChannelBehavior

                channel.getMessagesBefore(channel.messages.last().id, Integer.min(messageAmount, 100)).filterNotNull().onEach {
                    messageHolder.add(it.id)
                }.catch {
                    it.printStackTrace()
                    println("error")
                }.collect()
                textChannel.bulkDelete(messageHolder)
                respond {
                    embed {
                        color = DISCORD_BLACK
                        title = "$messageAmount messages have been cleared."
                        description = "Action occured in ${textChannel.mention}."
                        timestamp = Clock.System.now()
                    }
                }
            }
        }

        //Kick command
        ephemeralSlashCommand(::KickArgs) {  // Ephemeral slash commands have private responses
            name = "kick"
            allowRole(MODERATORS)
            description = "Kicks a user."


            // Use guild commands for commands that have guild-specific actions
            guild(GUILD_ID)

            action {
                guild?.kick(arguments.userArgument.id, "Requested by " + user.asUser().username)
                respond {
                    content = "Kicked ${arguments.userArgument.mention}!"
                }

            }
        }

        //Shutdown command

        ephemeralSlashCommand() {  // Ephemeral slash commands have private responses
            name = "shutdown"
            description = "Shuts down the bot."
            allowByDefault = false
            allowedRoles.add(MODERATORS)


            // Use guild commands for testing, global ones take up to an hour to update
            guild(GUILD_ID)

            @Suppress("DSL_SCOPE_VIOLATION")
            action {
                respond {
                    embed {
                        title = "Shutdown"
                        description = "Are you sure you would like to shut down?"
                        components {
                            ephemeralButton {
                                label = "Yes"
                                style = ButtonStyle.Success

                                action {
                                    respond { content = "Shutting down..." }
                                    kord.shutdown()
                                    exitProcess(0)
                                }
                            }
                            ephemeralButton {
                                label = "No"
                                style = ButtonStyle.Danger

                                action {
                                    respond { content = "Shutdown aborted." }
                                }
                            }
                        }
                    }
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

    inner class KickArgs : Arguments() {
        val userArgument by user("kickedUser", description = "Person to kick")
    }
}