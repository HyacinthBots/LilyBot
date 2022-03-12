@file:OptIn(PrivilegedIntent::class)

package net.irisshaders.lilybot

import com.github.jezza.Toml
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import mu.KotlinLogging
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
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.GITHUB_OAUTH
import net.irisshaders.lilybot.utils.MONGO_URI
import net.irisshaders.lilybot.utils.SENTRY_DSN
import org.bson.UuidRepresentation
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.nio.file.Files
import java.nio.file.Path

val config: TomlTable = Toml.from(Files.newInputStream(Path.of(CUSTOM_COMMANDS_PATH)))
var github: GitHub? = null

// Connect to the database
private var uri = MONGO_URI ?: "mongodb://localhost:27017" // this is the default for localhost
private val settings = MongoClientSettings
	.builder()
	.uuidRepresentation(UuidRepresentation.STANDARD)
	.applyConnectionString(ConnectionString(uri)) //todo make sure this actually works
	.build()

private val client = KMongo.createClient(settings).coroutine
val database = client.getDatabase("LilyBot")
private val gitHubLogger = KotlinLogging.logger { }

suspend fun main() {
	val bot = ExtensibleBot(BOT_TOKEN) {

		applicationCommands {
			enabled = true
		}

		members {
			lockMemberRequests = true
			all()
		}

		intents {
			+Intent.GuildMembers
		}

		extensions {
			add(::Config)
			add(::CustomCommands)
			add(::Github)
			add(::JoinLeaveEvent)
			add(::MessageEvents)
			add(::Moderation)
			add(::Report)
			add(::RoleMenu)
			add(::ThreadControl)
			add(::ThreadInviter)
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

		presence { playing(DatabaseHelper.selectInStatus()) }

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