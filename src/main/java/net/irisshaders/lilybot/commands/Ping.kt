package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake

class Ping: Extension() {
    // Used throughout KordEx to refer to your extension
    override val name = "ping"
    val GUILD_ID = Snowflake(  // Store this as a Discord snowflake, aka an ID
            env("GUILD_ID").toLong()  // An exception will be thrown if it can't be found
    )


    override suspend fun setup() {
        // ...

        publicSlashCommand() {  // Public slash commands have public responses
            name = "ping"
            description = "Am I alive?"

            // Use guild commands for testing, global ones take up to an hour to update
            guild(GUILD_ID)

            action {
                val kord = this@Ping.kord

                respond {
                    content = "I am alive!"
                }
            }
        }
    }

// ...
}