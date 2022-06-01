package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.PublicResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.Locale
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.commandDocs

/**
 * This class contains the info commands that allow users to get a better idea of how to use the bot.
 *
 * @since 3.3.0
 */
class InfoCommands : Extension() {
	override val name = "info-commands"

	override suspend fun setup() {
		val pagesObj = Pages()

		/**
		 * The command that generate the help paginator, after getting it from `./docs/commanddocs.toml`
		 *
		 * @author NoComment1105
		 * @since 3.3.0
		 */
		publicSlashCommand {
			name = "help"
			description = "Help for Lily's commands!"

			action {
				commandDocs!!.command.forEach {
					pagesObj.addPage(
						Page {
							title = it.category
							description = "**Name:** `${it.name}`"
							field {
								name = "Arguments:"
								value = it.args.replace("*", "•")
							}
							field {
								name = "Permissions:"
								value = it.permissions ?: "none"
							}
							field {
								name = "Result"
								value =
									if (it.name != "warn") {
										it.result
									} else {
										// Embeds don't support Markdown tables, so we have to get creative and make
										// it looks nice ourselves
										val result = it.result.split(".\n")
										"${result[0]}\n```markdown\n${result[1].replace(":", "-")}\n```"
									}
							}
							timestamp = Clock.System.now()
						}
					)
				}

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

		publicSlashCommand {
			name = "about"
			description = "Find out about LilyBot!"

			action {
				val responseEmbed = EmbedBuilder()
				responseEmbed.title = "LilyBot"
				responseEmbed.description =
					"LilyBot is a bot designed initially for [The Iris Project](https://irisshaders.net) " +
							"Discord server, but has since expanded out to be used across servers!\nLily " +
							"provides various moderation and utility commands to a server, as well as built in " +
							"anti-phishing to keep users safe from those pesky phishermen."
				responseEmbed.color = DISCORD_BLURPLE
				responseEmbed.field {
					name = "How do I configure LilyBot?"
					value = "To configure LilyBot, all you need to do is run `/config set`, and provided the " +
							"requested values, and Lily will be all set and ready to go in your server. For more " +
							"information, see the relevant page on the `/help` command."
				}
				responseEmbed.field {
					name = "What commands are there?"
					value = "Lots! Too many to list here. You can read about the commands using the `/help` " +
							"command, or visiting the [Commands List]" +
							"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md) on the GitHub."
				}
				responseEmbed.field {
					name = "More information"
					value = "For a full list of commands, see the [Commands List]" +
							"(https://github.com/IrisShaders/LilyBot/blob/main/docs/commands.md).\nFor information " +
							"on how to start developing with LilyBot, see the [Development guide]" +
							"(https://github.com/IrisShaders/LilyBot/blob/main/docs/development-guide.md)."
				}
				responseEmbed.field {
					name = "Development and Support"
					value =
						"LilyBot is primarily developed by NoComment#6411 and tempest#4510, who work together " +
								"nearly everyday on improving and upgrading the LilyBot you see today!\nThe " +
								"source code for LilyBot, which will remain free and available for as long " +
								"as LilyBot lives, can be found on the GitHub.\nThanks to the wonderful gdude#2002, " +
								"Lily is hosted with incredible uptimes and support, making use of Docker Compose!\n" +
								"If you need support with LilyBot, or would like to provide suggestions, keep up " +
								"with development and announcements, etc, you can join the Discord server at: " +
								"https://discord.gg/hy2329fcTZ"
				}
				responseEmbed.footer {
					text = "Brought to you by NoComment#6411, tempest#4510 and all the awesome contributors!"
					icon = this@publicSlashCommand.kord.getSelf().avatar!!.url
				}

				respond {
					embeds.add(responseEmbed)
				}.edit {
					components {
						linkButton(0) {
							label = "GitHub"
							url = "https://github.com/IrisShaders/LilyBot"
						}
						linkButton(0) {
							label = "Invite"
							url = "https://discord.com/api/oauth2/authorize?client_id=876278900836139008&" +
									"permissions=1428479371270&scope=bot%20applications.commands"
						}
					}
				}
			}
		}
	}
}
