package net.irisshaders.lilybot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import net.irisshaders.lilybot.commands.Moderation
import net.irisshaders.lilybot.commands.Ping
import net.irisshaders.lilybot.commands.Report
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.support.ThreadInviter
import net.irisshaders.lilybot.utils.BOT_TOKEN
import net.irisshaders.lilybot.utils.GUILD_ID

suspend fun main() {
    val bot = ExtensibleBot(BOT_TOKEN) {
        applicationCommands {
            defaultGuild(GUILD_ID)
        }

        chatCommands {
            // Enable chat command handling
            enabled = true
        }

        extensions {
            add(::Ping)
            add(::Moderation)
            add(::ThreadInviter)
            add(::Report)
        }

        hooks {
            afterKoinSetup {
                DatabaseManager.startDatabase()

            }
        }
    }

    bot.start()
}
