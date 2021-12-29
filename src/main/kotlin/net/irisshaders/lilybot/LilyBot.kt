package net.irisshaders.lilybot

import com.github.jezza.Toml
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import dev.kord.common.entity.PresenceStatus
import mu.KotlinLogging
import net.irisshaders.lilybot.commands.Custom
import net.irisshaders.lilybot.commands.Github
import net.irisshaders.lilybot.commands.Moderation
import net.irisshaders.lilybot.commands.Ping
import net.irisshaders.lilybot.commands.Report
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.events.JoinLeaveEvent
import net.irisshaders.lilybot.events.MessageEvents
import net.irisshaders.lilybot.support.ThreadInviter
import net.irisshaders.lilybot.utils.BOT_TOKEN
import net.irisshaders.lilybot.utils.CONFIG_PATH
import net.irisshaders.lilybot.utils.GITHUB_OAUTH
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.ENABLE_SENTRY
import net.irisshaders.lilybot.utils.SENTRY_DSN
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.Exception

val configPath: Path = Paths.get(CONFIG_PATH)
val config: TomlTable = Toml.from(Files.newInputStream(configPath))
var github: GitHub? = null

private val gitHubLogger = KotlinLogging.logger {}

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
            add(::JoinLeaveEvent)
            add(::MessageEvents)
            add(::Github)
            add(::Custom)

            extPhishing {
                appName = "Lily Bot"
                detectionAction = DetectionAction.Kick
                logChannelName = "anti-phishing-logs"
                requiredCommandPermission = null
            }


            sentry {
                if (ENABLE_SENTRY.toBoolean() && SENTRY_DSN != null) {

                    enable = true
                    dsn = SENTRY_DSN
                } else {
                    enable = false
                }
            }
        }

        hooks {
            afterKoinSetup {
                DatabaseManager.startDatabase()
            }
        }
        presence {
            status = PresenceStatus.Online
            playing(config.get("activity") as String)
        }

        try {
            github = GitHubBuilder().withOAuthToken(GITHUB_OAUTH).build()
            gitHubLogger.info("Logged into GitHub!")
        } catch (exception: Exception) {
            exception.printStackTrace()
            gitHubLogger.error("Failed to log into GitHub!")
            throw Exception(exception)
        }
    }
    bot.start()
}
