package net.irisshaders.lilybot.extensions.util

import com.github.jezza.TomlArray
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.config

/**
 * This class reads in the config TOML file and converts each array of info into a usable discord slash command.
 * @author IMS212
 */
class CustomCommands : Extension() {
    override var name = "custom-commands"

    override suspend fun setup() {
        // Reads all toml arrays titled "command"
        val commands: TomlArray = config.get("command") as TomlArray
        for (cmds in commands) {
            val cmd = cmds as TomlTable
            addCommand(
                cmd.get("name") as String, // Uses the name field from the array
                cmd.getOrDefault("help", "A Lily bot command.") as String,
                cmd.getOrDefault("title", "No title") as String,
                cmd.getOrDefault("description", "") as String,
                cmd.get("subcommand") as TomlArray? // Gets any sub commands in the parent array
            )
        }
    }

    /**
     * This function adds commands from the Toml config.
     *
     * @param names The name of the command. What you type to run the command
     * @param desc The information tooltip that appears when you type the command
     * @param cmdTitle The title of the command's embed
     * @param cmdValue The text below the title of the command's embed
     * @param subCmds Any sub commands that require adding. can be null
     * @return A public slash/sub command with the provided data
     *
     * @author IMS212
     */
    private suspend fun addCommand(
        names: String,
        desc: String,
        cmdTitle: String,
        cmdValue: String,
        subCmds: TomlArray?
    ) {
        publicSlashCommand {
            name = names
            description = desc
            if (subCmds == null) {
                action {
                    respond {
                        embed {
                            color = DISCORD_BLURPLE
                            title = cmdTitle
                            description = cmdValue
                            timestamp = Clock.System.now()
                        }
                    }
                }
            } else {
                for (subs in subCmds) {
                    val sub = subs as TomlTable
                    publicSubCommand {
                        name = sub.get("name") as String
                        description = sub.getOrDefault("help", "A Lily bot command.") as String
                        action {
                            respond {
                                embed {
                                    color = DISCORD_BLURPLE
                                    timestamp = Clock.System.now()
                                    title = sub.getOrDefault("title", null) as String
                                    description = sub.getOrDefault("description", "") as String
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
