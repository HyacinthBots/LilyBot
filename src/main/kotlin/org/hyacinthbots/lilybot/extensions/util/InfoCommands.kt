package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.PublicResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Locale
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.hyacinthbots.lilybot.commandDocs
import org.hyacinthbots.lilybot.database.collections.UptimeCollection
import java.util.Properties

/**
 * This class contains the info commands that allow users to get a better idea of how to use the bot.
 *
 * @since 3.3.0
 */
class InfoCommands : Extension() {
	override val name = "info-commands"

	private val versionProperties = Properties()

	init {
		versionProperties.load(this.javaClass.getResourceAsStream("/version.properties"))
	}

	override suspend fun setup() {
		val pagesObj = Pages()

		for (it in commandDocs!!.command) {
			if (it.name.isNullOrEmpty() && it.result.isNullOrEmpty() && it.permissions.isNullOrEmpty() &&
				it.args.isNullOrEmpty() && it.category.isNotEmpty() && it.description!!.isNotEmpty()
			) {
				continue
			}
			pagesObj.addPage(
				Page {
					title = it.category
					description = "**Name:** `${it.name}`"
					field {
						name = "Arguments:"
						value = it.args?.replace("*", "•") ?: "None"
					}
					field {
						name = "Permissions:"
						value = it.permissions ?: "None"
					}
					field {
						name = "Result"
						value =
							if (it.name != "warn") {
								it.result!!
							} else {
								// Embeds don't support Markdown tables, so we have to get creative and make
								// it looks nice ourselves
								val result = it.result!!.split(".\n")
								"${result[0]}\n```markdown\n${result[1].replace(":", "-")}\n```"
							}
					}
					timestamp = Clock.System.now()
				}
			)
		}

		/**
		 * The command that generates the command list paginator, after getting it from `./docs/commanddocs.toml`
		 *
		 * @author NoComment1105
		 * @since 3.3.0
		 */
		publicSlashCommand {
			name = "command-list"
			description = "Show a list of Lily's commands!"

			action {
				val paginator = PublicResponsePaginator(
					pages = pagesObj,
					keepEmbed = true,
					owner = event.interaction.user,
					timeoutSeconds = 500,
					locale = Locale.ENGLISH_GREAT_BRITAIN.asJavaLocale(),
					interaction = interactionResponse
				)

				paginator.send()
			}
		}

		/**
		 * A command that creates an embed providing help with using Lily
		 *
		 * @author NoComment1105
		 * @author tempest15
		 * @since 3.3.0
		 */
		publicSlashCommand {
			name = "help"
			description = "Get help with using Lily!"

			action {
				respond {
					embed {
						thumbnail {
							url = event.kord.getSelf().avatar!!.url
						}
						title = "What is LilyBot?"
						description = "Lily is a FOSS multi-purpose bot for Discord created by " +
								"the HyacinthBots organization. " +
								"Use `/info` to learn more, or `/links` to get a list of links relevant to Lily."

						field {
							name = "How do I configure Lily?"
							value = "Run the `/config set` command and provided the requested values. " +
									"You may need to run the command multiple times to set a  config for each " +
									"section of the bot you wish to use. For more information, use the " +
									"`/command-list` command and navigate to the relevant page."
						}

						field {
							name = "What commands are there?"
							value = "Lots! Too many to list here. You can read about the commands " +
									"using the `/command-list` command, or visiting the [commands list on GitHub]" +
									"(https://github.com/HyacinthBots/LilyBot/blob/main/docs/commands.md)."
						}

						field {
							name = "How do I get more help or learn more?"
							value = "To get additional support, discuss Lily, suggest features, " +
									"or even lend a hand with development join our Discord at " +
									"https://discord.gg/hy2329fcTZ"
						}

						field {
							name = "How can I support the continued development of Lily?"
							value = "Lily is developed primarily by NoComment#6411 and tempest#4510 " +
									"in our free time. Neither of us have resources to invest in hosting, " +
									"so financial donations via [Buy Me a Coffee]" +
									"(https://buymeacoffee.com/Hyacinthbots) help keep Lily afloat. At the moment, " +
									"Lily is very generously hosted free of charge by gdude#2002, " +
									"but we're looking to move to our own hosting. " +
									"We also have domain costs for our website.\n\n" +
									"Contributions of code & documentation are also incredibly appreciated, " +
									"and you can read our [contributing guide]" +
									"(https://github.com/HyacinthBots/LilyBot/blob/main/CONTRIBUTING.md) " +
									"or [development guide]" +
									"(https://github.com/HyacinthBots/LilyBot/blob/main/docs/development-guide.md) " +
									"to get started."
						}
						color = DISCORD_BLURPLE
					}
				}
			}
		}

		publicSlashCommand {
			name = "info"
			description = "Learn about Lily, and get uptime data!"

			action {
				respond {
					embed {
						thumbnail {
							url = event.kord.getSelf().avatar!!.url
						}
						title = "Info about LilyBot"
						description = "Lily is a FOSS multi-purpose bot for Discord created by " +
								"the HyacinthBots organization. " +
								"Use `/help` for support, or `/links` to get a list of links relevant to Lily."

						field {
							name = "Version"
							value =
								"${versionProperties.getProperty("version") ?: "??"} (${System.getenv("SHORT_SHA") ?: "unknown"})"
							inline = true
						}
						field {
							name = "Up Since"
							value = """
								${UptimeCollection().get()?.onTime?.toLocalDateTime(TimeZone.UTC)
									?.time.toString().split(".")[0]} 
								${UptimeCollection().get()?.onTime?.toLocalDateTime(TimeZone.UTC)?.date} UTC
								(${UptimeCollection().get()?.onTime?.toDiscord(TimestampType.RelativeTime) ?: "??"})
							""".replace("\n", " ").trimIndent()
							inline = true
						}
						color = DISCORD_BLURPLE
					}
				}
			}
		}

		/**
		 * A command that creates an embed providing a list of links relevant to Lily.
		 *
		 * @author tempest15
		 * @since 3.4.0
		 */
		publicSlashCommand {
			name = "links"
			description = "Get a list of links relevant to Lily!"

			action {
				respond {
					embed {
						title = "Useful links"
						description =
							"Use `/help` for support, or `/info` to learn more about Lily.\n\n" +
							"Website: Coming Soon™️\n" +
							"GitHub: https://github.com/HyacinthBots\n" +
							"Buy Me a Coffee: https://buymeacoffee.com/HyacinthBots\n" +
							"Twitter: https://twitter.com/HyacinthBots\n" +
							"Email: `hyacinthbots@outlook.com`\n" +
							"Discord: https://discord.gg/hy2329fcTZ"
						color = DISCORD_BLURPLE
					}
				}
			}
		}
	}
}
