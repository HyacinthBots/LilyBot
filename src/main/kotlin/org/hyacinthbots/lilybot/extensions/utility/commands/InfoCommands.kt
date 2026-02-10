package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_BLURPLE
import dev.kordex.core.components.components
import dev.kordex.core.components.linkButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.publicSlashCommand
import lilybot.i18n.Translations
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
            name = Translations.Utility.InfoCommands.Help.name
            description = Translations.Utility.InfoCommands.Help.description

            action {
                val translations = Translations.Utility.InfoCommands.Help
                respond {
                    embed {
                        thumbnail {
                            url = event.kord.getSelf().avatar?.cdnUrl!!.toUrl()
                        }
                        title = translations.embedTitle.translate()
                        description = translations.embedDesc.translate()

                        field {
                            name = translations.configFieldName.translate()
                            value = translations.configFieldValue.translate()
                        }

                        field {
                            name = translations.whatFieldName.translate()
                            value = translations.whatFieldValue.translate() +
                                "($HYACINTH_GITHUB/LilyBot/blob/main/docs/commands.md)."
                        }

                        field {
                            name = translations.supportFieldName.translate()
                            value = translations.supportFieldValue.translate()
                        }

                        field {
                            name = translations.usefulFieldName.translate()
                            value = translations.usefulFieldValue.translate(HYACINTH_GITHUB)
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
            name = Translations.Utility.InfoCommands.Invite.name
            description = Translations.Utility.InfoCommands.Invite.description

            action {
                respond {
                    content = Translations.Utility.InfoCommands.Invite.value.translate()
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
    val translations = Translations.Utility.InfoCommands.Help.Button
    components {
        linkButton {
            label = translations.invite
            url =
                "https://discord.com/api/oauth2/authorize?client_id=876278900836139008" +
                    "&permissions=1151990787078&scope=bot%20applications.commands"
        }
        linkButton {
            label = translations.privacy
            url = "$HYACINTH_GITHUB/LilyBot/blob/main/docs/privacy-policy.md"
        }
        linkButton {
            label = translations.tos
            url = "$HYACINTH_GITHUB/.github/blob/main/terms-of-service.md"
        }
    }
}
