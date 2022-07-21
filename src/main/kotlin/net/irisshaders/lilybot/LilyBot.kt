@file:OptIn(PrivilegedIntent::class)

package net.irisshaders.lilybot

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import mu.KotlinLogging
import net.irisshaders.lilybot.extensions.config.Config
import net.irisshaders.lilybot.extensions.config.JoinLeaveDetection
import net.irisshaders.lilybot.extensions.events.LogUploading
import net.irisshaders.lilybot.extensions.events.MemberJoinLeave
import net.irisshaders.lilybot.extensions.events.MessageDelete
import net.irisshaders.lilybot.extensions.events.ThreadInviter
import net.irisshaders.lilybot.extensions.moderation.Report
import net.irisshaders.lilybot.extensions.moderation.TemporaryModeration
import net.irisshaders.lilybot.extensions.moderation.TerminalModeration
import net.irisshaders.lilybot.extensions.util.GalleryChannel
import net.irisshaders.lilybot.extensions.util.Github
import net.irisshaders.lilybot.extensions.util.InfoCommands
import net.irisshaders.lilybot.extensions.util.ModUtilities
import net.irisshaders.lilybot.extensions.util.PublicUtilities
import net.irisshaders.lilybot.extensions.util.Reminders
import net.irisshaders.lilybot.extensions.util.RoleMenu
import net.irisshaders.lilybot.extensions.util.StartupHooks
import net.irisshaders.lilybot.extensions.util.Tags
import net.irisshaders.lilybot.extensions.util.ThreadControl
import net.irisshaders.lilybot.utils.BOT_TOKEN
import net.irisshaders.lilybot.utils.ENVIRONMENT
import net.irisshaders.lilybot.utils.SENTRY_DSN
import net.irisshaders.lilybot.utils.database
import net.irisshaders.lilybot.utils.docs.CommandDocs
import net.irisshaders.lilybot.utils.docs.DocsGenerator
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
			add(::JoinLeaveDetection)
			add(::LogUploading)
			add(::MemberJoinLeave)
			add(::MessageDelete)
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
