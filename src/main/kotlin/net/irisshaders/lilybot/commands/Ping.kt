@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import kotlin.time.ExperimentalTime

@Suppress("PrivatePropertyName")
class Ping : Extension() {
    override val name = "ping"

    override suspend fun setup() {
        publicSlashCommand {  // Public slash commands have public responses
            name = "ping"
            description = "Am I alive?"

            action {
                val averagePing = this@Ping.kord.gateway.averagePing

                respond {
                    embed {
                        color = DISCORD_YELLOW
                        title = "Pong!"

                        timestamp = Clock.System.now()

                        field {
                            name = "Your Ping with Lily is:"
                            value = "**$averagePing**"
                            inline = true
                        }
                    }
                }
            }
        }
    }
}
