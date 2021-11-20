@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.commands

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ACTION_LOG
import net.irisshaders.lilybot.utils.GUILD_ID
import net.irisshaders.lilybot.utils.MODERATORS
import kotlin.time.ExperimentalTime

class Report : Extension() {
    override val name = "report"

    override suspend fun setup() {
        ephemeralMessageCommand {
            name = "Report"
            locking = true // To prevent the command from being run more than once concurrently

            action {
                val actionLog = guild?.getChannel(ACTION_LOG) as GuildMessageChannelBehavior

                respond {
                    content = "Message reported to staff"
                }
                actionLog.createMessage { content = "<@&${MODERATORS.value}> get summoned" }
                actionLog.createEmbed {
                    color = DISCORD_RED
                    title = "Message reported!"

                    field {
                        value = "**Messaged Content:** ${event.interaction.getTarget().content}"
                        inline = true
                    }
                    field {
                        value = "**Message Link:** https://discord.com/channels/${GUILD_ID.value}/${event.interaction.channelId.value}/${event.interaction.targetId.value}"
                        inline = false
                    }
                    field {
                        value = "**Reported by:** ${user.asUser().tag}"
                        inline = false
                    }
                    timestamp = Clock.System.now()
                }

            }

        }
    }
}