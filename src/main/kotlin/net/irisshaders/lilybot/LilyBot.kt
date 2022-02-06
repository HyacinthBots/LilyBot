@file:OptIn(PrivilegedIntent::class)

package net.irisshaders.lilybot

import com.github.jezza.Toml
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import dev.kord.common.entity.PresenceStatus
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import mu.KotlinLogging
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.events.JoinLeaveEvent
import net.irisshaders.lilybot.events.MessageEvents
import net.irisshaders.lilybot.extensions.moderation.Moderation
import net.irisshaders.lilybot.extensions.moderation.Report
import net.irisshaders.lilybot.extensions.moderation.ThreadModInviter
import net.irisshaders.lilybot.extensions.support.ThreadInviter
import net.irisshaders.lilybot.extensions.util.CustomCommands
import net.irisshaders.lilybot.extensions.util.Github
import net.irisshaders.lilybot.extensions.util.Ping
import net.irisshaders.lilybot.extensions.util.ThreadControl
import net.irisshaders.lilybot.utils.BOT_TOKEN
import net.irisshaders.lilybot.utils.CONFIG_PATH
import net.irisshaders.lilybot.utils.GITHUB_OAUTH
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.SENTRY_DSN
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

val configPath: Path = Paths.get(CONFIG_PATH)
val config: TomlTable = Toml.from(Files.newInputStream(configPath))
var github: GitHub? = null

private val gitHubLogger = KotlinLogging.logger { }

suspend fun main() {
	val bot = ExtensibleBot(BOT_TOKEN) {

		applicationCommands {
			defaultGuild(GUILD_ID)
		}

		members {
			fill(GUILD_ID)
		}

		intents {
			+Intent.GuildMembers
		}

		extensions {
			add(::Ping)
			add(::Moderation)
			add(::ThreadInviter)
			add(::Report)
			add(::JoinLeaveEvent)
			add(::MessageEvents)
			add(::Github)
			add(::CustomCommands)
			add(::ThreadModInviter)
			add(::ThreadControl)

			extPhishing {
				appName = "Lily Bot"
				detectionAction = DetectionAction.Kick
				logChannelName = "anti-phishing-logs"
				requiredCommandPermission = null
			}

			extMappings { }

			sentry {
				enableIfDSN(SENTRY_DSN)
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
