@file:OptIn(PrivilegedIntent::class)
@file:Suppress("DEPRECATION")

package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.modules.extra.mappings.extMappings
import com.kotlindiscord.kord.extensions.modules.extra.phishing.DetectionAction
import com.kotlindiscord.kord.extensions.modules.extra.phishing.extPhishing
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import mu.KotlinLogging
import net.irisshaders.lilybot.extensions.config.Config
import net.irisshaders.lilybot.extensions.events.LogUploading
import net.irisshaders.lilybot.extensions.events.MemberJoinLeave
import net.irisshaders.lilybot.extensions.events.MessageDelete
import net.irisshaders.lilybot.extensions.events.ThreadInviter
import net.irisshaders.lilybot.extensions.moderation.Report
import net.irisshaders.lilybot.extensions.moderation.TemporaryModeration
import net.irisshaders.lilybot.extensions.moderation.TerminalModeration
import net.irisshaders.lilybot.extensions.util.CustomCommands
import net.irisshaders.lilybot.extensions.util.Github
import net.irisshaders.lilybot.extensions.util.ModUtilities
import net.irisshaders.lilybot.extensions.util.PublicUtilities
import net.irisshaders.lilybot.extensions.util.RoleMenu
import net.irisshaders.lilybot.extensions.util.Tags
import net.irisshaders.lilybot.extensions.util.ThreadControl
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.IOException

var github: GitHub? = null
private val gitHubLogger = KotlinLogging.logger { }

/**
 * The common features for the bot that should be present across testing and production.
 *
 * @author NoComment1105
 * @since 3.2.0
 */
suspend fun ExtensibleBotBuilder.common() {
	members {
		lockMemberRequests = true
		all()
	}

	intents {
		+Intent.GuildMembers
	}

	try {
		github = GitHubBuilder().withOAuthToken(GITHUB_OAUTH).build()
		gitHubLogger.info("Logged into GitHub!")
	} catch (exception: IOException) {
		gitHubLogger.error("Failed to log into GitHub!")
	}
}

/**
 * The extensions used by the bot.
 *
 * @param mode The [Mode] the bot is using. Defines certain extensions
 * @author NoComment1105
 * @since 3.2.0
 */
suspend fun ExtensibleBotBuilder.addExtensions(mode: Mode) {
	extensions {
		add(::Config)
		add(::CustomCommands)
		add(::Github)
		add(::LogUploading)
		add(::MemberJoinLeave)
		add(::MessageDelete)
		add(::ModUtilities)
		add(::PublicUtilities)
		add(::Report)
		add(::RoleMenu)
		add(::Tags)
		add(::TemporaryModeration)
		add(::TerminalModeration)
		add(::ThreadControl)
		add(::ThreadInviter)

		if (mode == Mode.PRODUCTION) {
			extPhishing {
				appName = "Lily Bot"
				detectionAction = DetectionAction.Kick
				logChannelName = "anti-phishing-logs"
				requiredCommandPermission = null
			}

			sentry {
				enableIfDSN(SENTRY_DSN)
			}
		}

		extMappings { }
	}
}

/**
 * The enum to store the modes for the bot.
 *
 * @property PRODUCTION Used for Production Mode
 * @property DEVELOPMENT Used for Development mode
 *
 * @author NoComment1105
 * @since 3.2.0
 */
enum class Mode {
	PRODUCTION,
	DEVELOPMENT
}
