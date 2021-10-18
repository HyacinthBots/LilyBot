package net.irisshaders.lilybot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake
import net.irisshaders.lilybot.commands.*

    suspend fun main() {
        val bot = ExtensibleBot(TOKEN) {
            chatCommands {
                // Enable chat command handling
                enabled = true
            }

            extensions {
                add(::Ping)
                add(::Moderation)
            }
        }

        bot.start()
    }