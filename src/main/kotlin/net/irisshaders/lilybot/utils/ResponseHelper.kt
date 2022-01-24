package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.Color
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

object ResponseHelper {
	/**
	 * This function generates a Failure [embed] for ease of use in other places
	 * @param channel The channel that the embed will be created in
	 * @param embedTitle The title of the embed, or null if no title
	 * @param embedDescription The description of the embed, or null if no description needed
	 * @return the built-in embed
	 * @author NoComment1105
	 */
	suspend fun failureEmbed(channel: MessageChannelBehavior, embedTitle: String?, embedDescription: String?): Message {
		return channel.createEmbed {
			if (embedTitle != null) title = embedTitle
			if (embedDescription != null) description = embedDescription
			color = DISCORD_RED
			timestamp = Clock.System.now()
		}
	}

    /**
     * Using the provided [channel] an [embed] will be returned, populated by the fields specified by the user
     *
     * @param channel The channel that the embed will be created in.
     * @param embedTitle The title to set the embed to, or null to set no title.
     * @param embedDescription The description of the embed. If null, the description will be empty.
     * @param embedColor The color to set the embed to. If null, the embed will be coloured black.
     * @param requestedBy The user that requested the embed. If null, the footer will be empty.
     * @return An embed in the channel
     * @author Maximumpower55
     * @author NoComment1105
     */
    suspend fun responseEmbedInChannel(channel: MessageChannelBehavior, embedTitle: String?, embedDescription: String?, embedColor: Color?, requestedBy: User?): Message {
        return channel.createEmbed {
            if (embedTitle != null) title = embedTitle
            if (embedDescription != null) description = embedDescription
            color = embedColor ?: DISCORD_BLACK
            timestamp = Clock.System.now()
            if (requestedBy != null) {
                footer {
                    text = requestedBy.tag
                    icon = requestedBy.avatar?.url
                }
            }
        }
    }

	/**
	 * Using the provided [User], a DM [embed] is sent to the said [User]
	 *
	 * @param user The user you wish to DM
	 * @param embedTitle The title of the embed, or null to set no title
	 * @param embedDescription The description of the embed, or null to set no description
	 * @param embedColor The colour of the embed, or [DISCORD_BLACK] if null
	 * @return An embed that is DM'd to the user
	 * @author NoComment1105
	 */
	suspend fun userDMEmbed(user: User, embedTitle: String?, embedDescription: String?, embedColor: Color?): Message? {
		return user.dm {
			embed {
				if (embedTitle != null) title = embedTitle
				if (embedDescription != null) description = embedDescription
				color = embedColor ?: DISCORD_BLACK
				timestamp = Clock.System.now()
			}
		}
	}
}
