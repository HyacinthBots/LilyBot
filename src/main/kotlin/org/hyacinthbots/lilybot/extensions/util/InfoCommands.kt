package org.hyacinthbots.lilybot.extensions.util

import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.components.components
import dev.kordex.core.components.linkButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.publicSlashCommand
import org.hyacinthbots.lilybot.utils.HYACINTH_GITHUB

/**
 * This class contains the info commands that allow users to get a better idea of how to use the bot.
 *
 * @since 3.3.0
 */
class InfoCommands : Extension() {
	override val name = "info-commands"

	override suspend fun setup() {
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
							url = event.kord.getSelf().avatar?.cdnUrl!!.toUrl()
						}
						title = "What is LilyBot?"
						description = "Lily is a FOSS multi-purpose bot for Discord created by " +
								"the HyacinthBots organization. " +
								"Use `/about` to learn more, or `/invite` to get an invite link."

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
									"($HYACINTH_GITHUB/LilyBot/blob/main/docs/commands.md)."
						}

						field {
							name = "How do I get more help or learn more?"
							value = "To get additional support, discuss Lily, suggest features, " +
									"or even lend a hand with development join our Discord at " +
									"https://discord.gg/hy2329fcTZ"
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
						color = DISCORD_BLURPLE
					}

					buttons()
				}
			}
		}

		/**
		 * A command that responds to the user with a link to invite Lily to their server.
		 *
		 * @author tempest15
		 * @since 4.4.0
		 */
		publicSlashCommand {
			name = "invite"
			description = "Get an invitation link for Lily!"

			action {
				respond {
					content = "Use this link to add Lily to your server:" +
							"https://discord.com/api/oauth2/authorize?client_id=876278900836139008" +
							"&permissions=1151990787078&scope=bot%20applications.commands"
				}
			}
		}
	}
}

/**
 * Applies info and utility buttons to a given message.
 *
 * @author NoComment1105
 * @since 4.4.0
 */
suspend fun MessageCreateBuilder.buttons() {
	components {
		linkButton {
			label = "Invite Link"
			url =
				"https://discord.com/api/oauth2/authorize?client_id=876278900836139008" +
						"&permissions=1151990787078&scope=bot%20applications.commands"
		}
		linkButton {
			label = "Privacy Policy"
			url = "$HYACINTH_GITHUB/LilyBot/blob/main/docs/privacy-policy.md"
		}
		linkButton {
			label = "Terms of Service"
			url = "$HYACINTH_GITHUB/.github/blob/main/terms-of-service.md"
		}
	}
}
