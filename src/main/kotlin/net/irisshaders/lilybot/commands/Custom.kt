package net.irisshaders.lilybot.commands

import com.github.jezza.TomlArray
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.config

class Custom : Extension() {
    override var name = "custom"


    override suspend fun setup() {
        val commands: TomlArray = config.get("command") as TomlArray
        for (cmds in commands) {
            var cmd = cmds as TomlTable
            if (cmd.get("subcommand") is TomlArray) {
                var subCmds = cmd.get("subcommand") as TomlArray

            }
            addCommand(cmd.get("name") as String, cmd.getOrDefault("help", "A Lily bot command.") as String, cmd.getOrDefault("title", "No title") as String, cmd.getOrDefault("description", "") as String, cmd.get("subcommand") as TomlArray?)
        }
    }

    suspend fun addCommand(names: String, desc: String, cmdTitle: String, cmdValue: String, subCmds: TomlArray?) {
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
                    var sub = subs as TomlTable
                    publicSubCommand {
                        name = sub.get("name") as String
                        description = sub.getOrDefault("help", "A Lily bot command.") as String
                        action {
                            respond {
                                embed {
                                    color = DISCORD_RED
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