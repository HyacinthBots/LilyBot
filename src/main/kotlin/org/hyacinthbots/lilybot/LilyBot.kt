@file:OptIn(PrivilegedIntent::class)

package org.hyacinthbots.lilybot

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.extPluralKit
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.storage.MongoDBDataAdapter
import org.hyacinthbots.lilybot.extensions.config.Config
import org.hyacinthbots.lilybot.extensions.config.GuildLogging
import org.hyacinthbots.lilybot.extensions.events.LogUploading
import org.hyacinthbots.lilybot.extensions.events.MemberLogging
import org.hyacinthbots.lilybot.extensions.events.MessageDelete
import org.hyacinthbots.lilybot.extensions.events.MessageEdit
import org.hyacinthbots.lilybot.extensions.events.ThreadInviter
import org.hyacinthbots.lilybot.extensions.moderation.Report
import org.hyacinthbots.lilybot.extensions.moderation.TemporaryModeration
import org.hyacinthbots.lilybot.extensions.moderation.TerminalModeration
import org.hyacinthbots.lilybot.extensions.util.GalleryChannel
import org.hyacinthbots.lilybot.extensions.util.Github
import org.hyacinthbots.lilybot.extensions.util.GuildAnnouncements
import org.hyacinthbots.lilybot.extensions.util.InfoCommands
import org.hyacinthbots.lilybot.extensions.util.ModUtilities
import org.hyacinthbots.lilybot.extensions.util.PublicUtilities
import org.hyacinthbots.lilybot.extensions.util.Reminders
import org.hyacinthbots.lilybot.extensions.util.RoleMenu
import org.hyacinthbots.lilybot.extensions.util.StartupHooks
import org.hyacinthbots.lilybot.extensions.util.Tags
import org.hyacinthbots.lilybot.extensions.util.ThreadControl
import org.hyacinthbots.lilybot.utils.BOT_TOKEN
import org.hyacinthbots.lilybot.utils.ENVIRONMENT
import org.hyacinthbots.lilybot.utils.SENTRY_DSN
import org.hyacinthbots.lilybot.utils.database
import org.hyacinthbots.lilybot.utils.docs.CommandDocs
import org.hyacinthbots.lilybot.utils.docs.DocsGenerator
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.IOException
import kotlin.io.path.Path

var github: GitHub? = null
private val gitHubLogger = KotlinLogging.logger("GitHub Logger")

var commandDocs: CommandDocs? = null

val docFile = Path("./docs/commands.md")

suspend fun main() {
	val mapper = tomlMapper { }
	val stream = LilyBot::class.java.getResourceAsStream("/commanddocs.toml")!!

	commandDocs = mapper.decode<CommandDocs>(stream)

	val bot = ExtensibleBot(BOT_TOKEN) {
		database(true)
		dataAdapter(::MongoDBDataAdapter)

		members {
			lockMemberRequests = true // Collect members one at a time to avoid hitting rate limits
			all()
		}

		// Enable the members intent to allow us to get accurate member counts for join logging
		intents {
			+Intent.GuildMembers
		}

		// Add the extensions to the bot
		extensions {
			add(::Config)
			add(::Github)
			add(::GalleryChannel)
			add(::InfoCommands)
			add(::GuildAnnouncements)
			add(::GuildLogging)
			add(::LogUploading)
			add(::MemberLogging)
			add(::MessageDelete)
			add(::MessageEdit)
			add(::ModUtilities)
			add(::PublicUtilities)
			add(::Reminders)
			add(::Report)
			add(::RoleMenu)
			add(::StartupHooks)
			add(::Tags)
			add(::TemporaryModeration)
			add(::TerminalModeration)
			add(::ThreadControl)
			add(::ThreadInviter)

			/*
			The anti-phishing extension automatically deletes and logs scam links. It also allows users to check links
			with a command. It kicks users who send scam links, rather than ban, to allow them to rejoin if they regain
			control of their account
			 */
			extPhishing {
				appName = "Lily Bot"
				detectionAction = DetectionAction.Kick
				logChannelName = "anti-phishing-logs"
				requiredCommandPermission = null
			}

			extMappings { } // Enable the mappings extension

			extPluralKit()

			sentry {
				enableIfDSN(SENTRY_DSN) // Use the nullable sentry function to allow the bot to be used without a DSN
			}
		}

		// Connect to GitHub to allow the GitHub commands to function
		try {
			github = GitHubBuilder().build()
			gitHubLogger.info("Connected to GitHub!")
		} catch (exception: IOException) {
			exception.printStackTrace()
			gitHubLogger.error("Failed to connect to GitHub!")
		}
	}

	DocsGenerator.clearDocs(ENVIRONMENT)
	DocsGenerator.writeNewDocs(ENVIRONMENT)

	bot.start()
}

object LilyBot
