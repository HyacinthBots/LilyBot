@file:OptIn(PrivilegedIntent::class)

package org.hyacinthbots.lilybot

import dev.kord.common.entity.Permission
import dev.kord.gateway.DefaultGateway
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.gateway.ratelimit.IdentifyRateLimiter
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.i18n.SupportedLocales
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import dev.kordex.data.api.DataCollection
import dev.kordex.modules.func.welcome.welcomeChannel
import dev.kordex.modules.pluralkit.extPluralKit
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import lilybot.i18n.Translations
import org.hyacinthbots.docgenerator.docsGenerator
import org.hyacinthbots.docgenerator.enums.CommandTypes
import org.hyacinthbots.docgenerator.enums.SupportedFileFormat
import org.hyacinthbots.lilybot.database.collections.UptimeCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.database.storage.MongoDBDataAdapter
import org.hyacinthbots.lilybot.extensions.config.ConfigExtension
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.logging.events.GuildLogging
import org.hyacinthbots.lilybot.extensions.logging.events.MemberLogging
import org.hyacinthbots.lilybot.extensions.logging.events.MessageDelete
import org.hyacinthbots.lilybot.extensions.logging.events.MessageEdit
import org.hyacinthbots.lilybot.extensions.moderation.commands.*
import org.hyacinthbots.lilybot.extensions.moderation.events.ModerationEvents
import org.hyacinthbots.lilybot.extensions.threads.AutoThreading
import org.hyacinthbots.lilybot.extensions.threads.ModThreadInviting
import org.hyacinthbots.lilybot.extensions.threads.ThreadControl
import org.hyacinthbots.lilybot.extensions.utility.commands.*
import org.hyacinthbots.lilybot.extensions.utility.events.UtilityEvents
import org.hyacinthbots.lilybot.internal.BuildInfo
import org.hyacinthbots.lilybot.utils.*
import org.kohsuke.github.GitHub
import org.kohsuke.github.GitHubBuilder
import java.io.IOException
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

lateinit var github: GitHub
private val gitHubLogger = KotlinLogging.logger("GitHub Logger")

val docFile = Path("./docs/commands.md")

val javaClient = HttpClient(Java) {
	install(WebSockets)
}

suspend fun main() {
	val bot = ExtensibleBot(BOT_TOKEN) {
		dataCollectionMode = DataCollection.None

		database(true)
		dataAdapter(::MongoDBDataAdapter)

		kord {
			stackTraceRecovery = true

			gateways { resources, shards ->
				val rateLimiter = IdentifyRateLimiter(resources.maxConcurrency, defaultDispatcher)
				shards.map {
					DefaultGateway {
						identifyRateLimiter = rateLimiter
						client = javaClient
					}
				}
			}
		}

		members {
			lockMemberRequests = true // Collect members one at a time to avoid hitting rate limits
			all()
		}

		// Enable the members intent to allow us to get accurate member counts for join logging
		intents {
			+Intent.GuildMembers
			// Deny the following intents because Lily doesn't need to receive the events (yet:tm:)
			-Intent.GuildVoiceStates
			-Intent.GuildMessageTyping
			-Intent.DirectMessageTyping
			// End denied intents
		}

		about {
			ephemeral = false
			general {
				message {
					embed {
						title = Translations.About.embedTitle.translate()

						thumbnail {
							url =
								"https://github.com/HyacinthBots/LilyBot/blob/main/docs/lily-logo-transparent.png?raw=true"
						}

						description = Translations.About.embedDesc.translate()
						field {
							name = Translations.About.howSupportTitle.translate()
							value = Translations.About.howSupportValue.translate(HYACINTH_GITHUB)
						}

						field {
							name = Translations.About.version.translate()
							// To avoid IntelliJ shouting about build errors, use https://plugins.jetbrains.com/plugin/9407-pebble
							value = "${BuildInfo.LILY_VERSION} (${BuildInfo.BUILD_ID})"
							inline = true
						}

						field {
							name = Translations.About.upSince.translate()
							value = "${
								UptimeCollection().get()?.onTime?.toLocalDateTime(TimeZone.UTC)
									?.time.toString().split(".")[0]
							} ${UptimeCollection().get()?.onTime?.toLocalDateTime(TimeZone.UTC)?.date} UTC\n " +
								"(${UptimeCollection().get()?.onTime?.toDiscord(TimestampType.RelativeTime) ?: "??"})"
							inline = true
						}

						field {
							name = Translations.Utility.InfoCommands.Help.usefulFieldName.translate()
							value = Translations.Utility.InfoCommands.Help.usefulFieldValue.translate(HYACINTH_GITHUB)
						}
					}

					actionRow {
						linkButton(
						    "https://discord.com/api/oauth2/authorize?client_id=876278900836139008&" +
							"permissions=1151990787078&scope=bot%20applications.commands"
						) {
							label = Translations.Utility.InfoCommands.Help.Button.invite.translate()
						}

						linkButton("$HYACINTH_GITHUB/LilyBot/blob/main/docs/privacy-policy.md") {
							label = Translations.Utility.InfoCommands.Help.Button.privacy.translate()
						}

						linkButton("$HYACINTH_GITHUB/.github/blob/main/terms-of-service.md") {
							label = Translations.Utility.InfoCommands.Help.Button.tos.translate()
						}
					}
				}
			}
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
			add(::ModerationEvents)
			add(::ModThreadInviting)
			add(::ModUtilities)
			add(::ModerationCommands)
			add(::NewsChannelPublishing)
			add(::PublicUtilities)
			add(::Reminders)
			add(::Report)
			add(::RoleMenu)
			add(::StartupHooks)
			add(::Tags)
			add(::ThreadControl)
			add(::UtilityEvents)

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
			// TODO evaluate if the copious amounts of errors this throws is worth it or not
// 			extPhishing {
// 				detectionAction = DetectionAction.Kick
// 				logChannelName = "anti-phishing-logs"
// 				requiredCommandPermission = null
// 			}

			extPluralKit {
				defaultLimit(4, 1.seconds)
				domainLimit("api.pluralkit.me", 2, 1.seconds)
			}

// 			sentry {
// 				enableIfDSN(SENTRY_DSN) // Use the nullable sentry function to allow the bot to be used without a DSN
// 			}
		}

		i18n {
			interactionUserLocaleResolver()
			interactionGuildLocaleResolver()

			applicationCommandLocale(SupportedLocales.ENGLISH)
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
