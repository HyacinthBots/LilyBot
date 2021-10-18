package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.env
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import kotlinx.coroutines.flow.*
import java.lang.Integer.min

class Kick: Extension() {
    override val name = "kick"
    val GUILD_ID = Snowflake(  // Store this as a Discord snowflake, aka an ID
            env("GUILD_ID").toLong()  // An exception will be thrown if it can't be found
    )


    override suspend fun setup() {
        ephemeralSlashCommand(::KickArgs) {  // Public slash commands have public responses
            name = "kick"
            allowByDefault = false
            allowedRoles.add(Snowflake(env("MODERATOR_ROLE")))
            description = "Kicks a user."


            // Use guild commands for testing, global ones take up to an hour to update
            guild(GUILD_ID)

            action {
                guild?.kick(arguments.userArgument.id, "Requested by " + user.asUser().username)
                respond {
                    content = "Kicked ${arguments.userArgument.mention}!"
                }

            }
        }
    }

    inner class KickArgs : Arguments() {
        val userArgument by user("kickedUser", description = "Person to kick")
    }
}