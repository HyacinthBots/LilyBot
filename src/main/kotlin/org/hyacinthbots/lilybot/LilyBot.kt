@file:OptIn(PrivilegedIntent::class)

package org.hyacinthbots.lilybot

import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.extPluralKit
import com.kotlindiscord.kord.extensions.modules.extra.welcome.welcomeChannel
import dev.kord.common.entity.Permission
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import io.github.oshai.kotlinlogging.KotlinLogging
import org.hyacinthbots.docgenerator.docsGenerator
import org.hyacinthbots.docgenerator.enums.CommandTypes
import org.hyacinthbots.docgenerator.enums.SupportedFileFormat
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.database.storage.MongoDBDataAdapter
import org.hyacinthbots.lilybot.extensions.config.ConfigExtension
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.logging.events.*
import org.hyacinthbots.lilybot.extensions.moderation.commands.*
import org.hyacinthbots.lilybot.extensions.threads.AutoThreading
import org.hyacinthbots.lilybot.extensions.threads.ModThreadInviting
import org.hyacinthbots.lilybot.extensions.threads.ThreadControl
import org.hyacinthbots.lilybot.extensions.utils.commands.*
import org.hyacinthbots.lilybot.utils.BOT_TOKEN
import org.hyacinthbots.lilybot.utils.ENVIRONMENT
import org.hyacinthbots.lilybot.utils.database
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.IOException
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

lateinit var github: GitHub
private val gitHubLogger = KotlinLogging.logger("GitHub Logger")

val docFile = Path("./docs/commands.md")

suspend fun main() {
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
			add(::AutoThreading)
			add(::ClearCommands)
			add(::ConfigExtension)
			add(::GalleryChannel)
			add(::Github)
			add(::GuildAnnouncements)
			add(::GuildLogging)
			add(::InfoCommands)
			add(::LockingCommands)
			add(::MemberLogging)
			add(::MessageDelete)
			add(::MessageEdit)
			add(::ModThreadInviting)
			add(::ModUtilities)
			add(::ModerationCommands)
			add(::NewsChannelPublishing)
			add(::PublicUtilities)
			add(::Reminders)
			add(::Report)
			add(::RoleMenu)
			add(::StartupHooks)
			add(::StatusPing)
			add(::Tags)
			add(::ThreadControl)

			/*
			The welcome channel extension allows users to designate a YAML file to create a channel with
			a variety of pre-built blocks.
			 */
			welcomeChannel(WelcomeChannelCollection()) {
				staffCommandCheck {
					hasPermission(Permission.BanMembers)
				}

				getLogChannel { _, guild ->
					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)
				}

				refreshDuration = 5.minutes
			}

			/*
			The anti-phishing extension automatically deletes and logs scam links. It also allows users to check links
			with a command. It kicks users who send scam links, rather than ban, to allow them to rejoin if they regain
			control of their account
			 */
			extPhishing {
				detectionAction = DetectionAction.Kick
				logChannelName = "anti-phishing-logs"
				requiredCommandPermission = null
			}

			extPluralKit {
				defaultLimit(4, 1.seconds)
				domainLimit("api.pluralkit.me", 2, 1.seconds)
			}

// 			sentry {
// 				enableIfDSN(SENTRY_DSN) // Use the nullable sentry function to allow the bot to be used without a DSN
// 			}
		}

		docsGenerator {
			enabled = true
			fileFormat = SupportedFileFormat.MARKDOWN
			commandTypes = CommandTypes.ALL
			filePath = docFile
			environment = ENVIRONMENT
			useBuiltinCommandList = true
			botName = "LilyBot"
		}

		// Connect to GitHub to allow the GitHub commands to function
		try {
			github = GitHubBuilder().build()
			gitHubLogger.info { "Connected to GitHub!" }
		} catch (exception: IOException) {
			exception.printStackTrace()
			gitHubLogger.error { "Failed to connect to GitHub!" }
		}
	}

	bot.start()
}
