package net.irisshaders.lilybot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.utils.loadModule
import net.irisshaders.lilybot.commands.Moderation
import net.irisshaders.lilybot.commands.Ping
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.support.ThreadInviter
import net.irisshaders.lilybot.tags.TagRepo
import net.irisshaders.lilybot.tags.TagsExtension
import net.irisshaders.lilybot.utils.BOT_TOKEN
import java.nio.file.Paths

suspend fun main() {

    val bot = ExtensibleBot(BOT_TOKEN) {
        chatCommands {
            // Enable chat command handling
            enabled = true
        }

        extensions {
            add(::Ping)
            add(::Moderation)
            add(::ThreadInviter)
            add(::TagsExtension)
        }
    }
    val tagRepo = TagRepo(Paths.get("tags-repo"))
    tagRepo.init()
    DatabaseManager.startDatabase()

    loadModule {
        single { tagRepo }
    }

    bot.start()
}