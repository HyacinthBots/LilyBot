package net.irisshaders.lilybot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import net.irisshaders.lilybot.commands.Moderation
import net.irisshaders.lilybot.commands.Ping
import net.irisshaders.lilybot.utils.BOT_TOKEN

suspend fun main() {
        val bot = ExtensibleBot(BOT_TOKEN) {
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