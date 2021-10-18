package net.irisshaders.lilybot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.commands.Ping

    private val TOKEN = env("TOKEN")
    val GUILD_ID = Snowflake(  // Store this as a Discord snowflake, aka an ID
            env("GUILD_ID").toLong()  // An exception will be thrown if it can't be found
    )

    suspend fun main() {
        val bot = ExtensibleBot(TOKEN) {
            chatCommands {
                // Enable chat command handling
                enabled = true
            }

            extensions {
                add(::Ping)
            }
        }

        bot.start()
    }