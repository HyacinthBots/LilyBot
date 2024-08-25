@file:OptIn(PrivilegedIntent::class)

package org.hyacinthbots.lilybot

import dev.kord.common.entity.Permission
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import dev.kordex.core.ExtensibleBot
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import dev.kordex.data.api.DataCollection
import dev.kordex.modules.func.phishing.DetectionAction
import dev.kordex.modules.func.phishing.extPhishing
import dev.kordex.modules.func.welcome.welcomeChannel
import dev.kordex.modules.pluralkit.extPluralKit
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.hyacinthbots.docgenerator.docsGenerator
import org.hyacinthbots.docgenerator.enums.CommandTypes
import org.hyacinthbots.docgenerator.enums.SupportedFileFormat
import org.hyacinthbots.lilybot.database.collections.UptimeCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.database.storage.MongoDBDataAdapter
import org.hyacinthbots.lilybot.extensions.config.ConfigExtension
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.GuildLogging
import org.hyacinthbots.lilybot.extensions.events.AutoThreading
import org.hyacinthbots.lilybot.extensions.events.MemberLogging
import org.hyacinthbots.lilybot.extensions.events.MessageDelete
import org.hyacinthbots.lilybot.extensions.events.MessageEdit
import org.hyacinthbots.lilybot.extensions.events.ModThreadInviting
import org.hyacinthbots.lilybot.extensions.moderation.ClearCommands
import org.hyacinthbots.lilybot.extensions.moderation.LockingCommands
import org.hyacinthbots.lilybot.extensions.moderation.ModerationCommands
import org.hyacinthbots.lilybot.extensions.moderation.Report
import org.hyacinthbots.lilybot.extensions.util.GalleryChannel
import org.hyacinthbots.lilybot.extensions.util.Github
import org.hyacinthbots.lilybot.extensions.util.GuildAnnouncements
import org.hyacinthbots.lilybot.extensions.util.InfoCommands
import org.hyacinthbots.lilybot.extensions.util.ModUtilities
import org.hyacinthbots.lilybot.extensions.util.NewsChannelPublishing
import org.hyacinthbots.lilybot.extensions.util.PublicUtilities
import org.hyacinthbots.lilybot.extensions.util.Reminders
import org.hyacinthbots.lilybot.extensions.util.RoleMenu
import org.hyacinthbots.lilybot.extensions.util.StartupHooks
import org.hyacinthbots.lilybot.extensions.util.StatusPing
import org.hyacinthbots.lilybot.extensions.util.Tags
import org.hyacinthbots.lilybot.extensions.util.ThreadControl
import org.hyacinthbots.lilybot.internal.BuildInfo
import org.hyacinthbots.lilybot.utils.BOT_TOKEN
import org.hyacinthbots.lilybot.utils.ENVIRONMENT
import org.hyacinthbots.lilybot.utils.HYACINTH_GITHUB
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
		dataCollectionMode = DataCollection.None

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

		about {
			ephemeral = false
			general {
				message { locale ->
					embed {
						title = "Info about LilyBot"

						thumbnail {
							url =
								"https://github.com/HyacinthBots/LilyBot/blob/main/docs/lily-logo-transparent.png?raw=true"
						}

						description =
							"Lily is a FOSS multi-purpose bot for Discord created by the HyacinthBots organization. " +
								"Use `/help` for support or `/invite` to get an invite link."
						field {
							name = "How can I support the continued development of Lily?"

							value = "Lily is developed primarily by NoComment#6411 in their free time. Hyacinth " +
								"doesn't have the resources to invest in hosting, so financial donations via " +
								"[Buy Me a Coffee](https://buymeacoffee.com/Hyacinthbots) help keep Lily afloat. " +
								"Currently, we run lily on a Hetzner cloud server, which we can afford in our " +
								"current situation. We also have domain costs for our website.\n\nContributions of " +
								"code & documentation are also incredibly appreciated, and you can read our " +
								"[contributing guide]($HYACINTH_GITHUB/LilyBot/blob/main/CONTRIBUTING.md) or " +
								"[development guide]($HYACINTH_GITHUB/LilyBot/blob/main/docs/development-guide.md) " +
								"to get started."
						}

						field {
							name = "Version"
							// To avoid IntelliJ shouting about build errors, use https://plugins.jetbrains.com/plugin/9407-pebble
							value = "${BuildInfo.LILY_VERSION} (${BuildInfo.BUILD_ID})"
							inline = true
						}

						field {
							name = "Up Since"
							value = "${
								UptimeCollection().get()?.onTime?.toLocalDateTime(TimeZone.UTC)
									?.time.toString().split(".")[0]
							} ${UptimeCollection().get()?.onTime?.toLocalDateTime(TimeZone.UTC)?.date} UTC\n " +
								"(${UptimeCollection().get()?.onTime?.toDiscord(TimestampType.RelativeTime) ?: "??"})"
							inline = true
						}

						field {
							name = "Useful links"
							value =
								"Website: Coming Soon™️\n" +
									"GitHub: ${HYACINTH_GITHUB}\n" +
									"Buy Me a Coffee: https://buymeacoffee.com/HyacinthBots\n" +
									"Twitter: https://twitter.com/HyacinthBots\n" +
									"Email: `hyacinthbots@outlook.com`\n" +
									"Discord: https://discord.gg/hy2329fcTZ"
						}
					}

					actionRow {
						linkButton(
						    "https://discord.com/api/oauth2/authorize?client_id=876278900836139008&" +
							"permissions=1151990787078&scope=bot%20applications.commands"
						) {
							label = "extensions.about.buttons.invite"
						}

						linkButton("$HYACINTH_GITHUB/LilyBot/blob/main/docs/privacy-policy.md") {
							label = "Privacy Policy"
						}

						linkButton("$HYACINTH_GITHUB/.github/blob/main/terms-of-service.md") {
							label = "Terms of Service"
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
