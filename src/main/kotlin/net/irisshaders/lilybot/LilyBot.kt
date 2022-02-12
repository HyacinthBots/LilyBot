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
import net.irisshaders.lilybot.extensions.config.Config
import net.irisshaders.lilybot.extensions.events.JoinLeaveEvent
import net.irisshaders.lilybot.extensions.events.MessageEvents
import net.irisshaders.lilybot.extensions.moderation.Moderation
import net.irisshaders.lilybot.extensions.moderation.Report
import net.irisshaders.lilybot.extensions.util.CustomCommands
import net.irisshaders.lilybot.extensions.util.Github
import net.irisshaders.lilybot.extensions.util.RoleMenu
import net.irisshaders.lilybot.extensions.util.ThreadControl
import net.irisshaders.lilybot.extensions.util.ThreadInviter
import net.irisshaders.lilybot.extensions.util.Utilities
import net.irisshaders.lilybot.utils.BOT_TOKEN
import net.irisshaders.lilybot.utils.CUSTOM_COMMANDS_PATH
import net.irisshaders.lilybot.utils.GITHUB_OAUTH
import net.irisshaders.lilybot.utils.SENTRY_DSN
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.nio.file.Files
import java.nio.file.Path

val config: TomlTable = Toml.from(Files.newInputStream(Path.of(CUSTOM_COMMANDS_PATH)))
var github: GitHub? = null

private val gitHubLogger = KotlinLogging.logger { }

suspend fun main() {
	val bot = ExtensibleBot(BOT_TOKEN) {

		hooks {
			DatabaseManager.startDatabase()
		}

		applicationCommands {
			enabled = true
		}

		members {
			var guildIds: Collection<String>? = null
			try {
			    newSuspendedTransaction {
					guildIds = DatabaseManager.Config.selectAll().map { it[DatabaseManager.Config.guildId] }
				}
			} catch (e: NumberFormatException) {
				// oh well, just wait for some configs to be added
				return@members
			}
			guildIds?.let { fill(it) }
		}

		intents {
			+Intent.GuildMembers
		}

		extensions {
			add(::Config)
			add(::Moderation)
			add(::ThreadInviter)
			add(::Report)
			add(::JoinLeaveEvent)
			add(::MessageEvents)
			add(::Github)
			add(::CustomCommands)
			add(::ThreadControl)
			add(::RoleMenu)
			add(::Utilities)

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
