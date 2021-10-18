package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.*
import java.lang.Integer.min
import kotlin.system.exitProcess

class ShutdownBot: Extension() {
    override val name = "shutdown"
    val GUILD_ID = Snowflake(  // Store this as a Discord snowflake, aka an ID
            env("GUILD_ID").toLong()  // An exception will be thrown if it can't be found
    )


    override suspend fun setup() {
        ephemeralSlashCommand() {  // Public slash commands have public responses
            name = "shutdown"
            description = "Shuts down the bot."
            allowByDefault = false
            allowedRoles.add(Snowflake(env("MODERATOR_ROLE")))


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
}