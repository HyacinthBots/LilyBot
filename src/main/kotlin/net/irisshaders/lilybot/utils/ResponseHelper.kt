package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_RED
import dev.kord.common.Color
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.User
import dev.kord.core.entity.Message
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
     * @param channel The channel that the embed will be created in.
     * @param embedTitle The title to set the embed to, or null to set no title.
     * @param embedDescription The description of the embed. If null, the description will be empty.
     * @param embedColor The color to set the embed to. If null, the embed will be coloured black.
     * @param requestedBy The user that requested the embed. If null, the footer will be empty.
     * @return the created message
     * @author Maximumpower55
     * @author NoComment1105
     */
    suspend fun responseEmbedInChannel(channel: MessageChannelBehavior, embedTitle: String?, embedDescription: String?, embedColor: Color?, requestedBy: User?): Message {
        return channel.createEmbed {
            title = embedTitle
            if (embedDescription != null) description = embedDescription
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