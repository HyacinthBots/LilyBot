package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_RED
import dev.kord.common.Color
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.datetime.Clock

object ResponseHelper {

    /**
     * This function generates a Failure embed for ease of use in other places
     * @param embedTitle The title of the embed
     * @param embedDescription The description of the embed
     * @return the build in embed
     * @author NoComment1105
     */
    fun failureEmbed(embedTitle: String?, embedDescription: String?) {
        val embed = EmbedBuilder()
        embed.title = "Something went wrong!"
        embed.description = "Please try again!"
        embed.color = DISCORD_RED

        if (embedTitle != null) embed.title
        if (embedDescription != null) embed.description
        return
    }

    /**
     * Creates an [EmbedBuilder] and populates it with the current timestamp, the given as the
     * requester and the given [Color] as its color.
     *
     * @param embedTitle The title to set the embed to, or null to set no title.
     * @param embedDescripton The description of the embed. If null, the description will be empty.
     * @param embedColor The color to set the embed to. If null, the embed will be coloured black.
     * @author NoComment1105
     */
    suspend fun responseEmbedInActionLog(channel: GuildMessageChannelBehavior, embedTitle: String?, embedDescripton: String?, embedColor: Color?, requestedBy: User?) {
        channel.createEmbed {
            title = embedTitle
            if (embedDescripton != null) description = embedDescripton
            color = embedColor
            timestamp = Clock.System.now()
            if (requestedBy != null) {
                footer {
                    text = requestedBy.tag
                    icon = requestedBy.avatar?.url
                }
            }
        }
    }
}