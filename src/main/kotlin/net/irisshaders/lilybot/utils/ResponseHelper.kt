package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.utils.dm
import dev.kord.common.Color
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock

/**
 * Using the provided [channel] an [embed] will be returned, populated by the fields specified by the user
 *
 * @param channel The channel that the embed will be created in.
 * @param embedTitle The title to set the embed to, or null to set no title.
 * @param embedDescription The description of the embed. If null, the description will be empty.
 * @param embedColor The color to set the embed to. If null, the embed will be coloured black.
 * @param requestedBy The user that requested the embed. If null, the footer will be empty.
 * @return An embed in the channel
 * @author Maximumpower55, NoComment1105
 */
suspend fun responseEmbedInChannel(channel: MessageChannelBehavior, embedTitle: String?,
								   embedDescription: String?, embedColor: Color?, requestedBy: User?): Message {
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

/**
 * This is the base moderation embed for all moderation actions. This should be posted to the action log of a guild.
 * It takes in the reason for the action, the user the action is being targeted to, and the user of the command
 *
 * @param reason The reason for the action
 * @param targetUser The targeted user in the action
 * @param commandUser The user that ran the command
 * @author NoComment1105
 */
suspend fun EmbedBuilder.baseModerationEmbed(reason: String, targetUser: User, commandUser: UserBehavior) {
	field {
		name = "User:"
		value = "${targetUser.tag}\n${targetUser.id}"
		inline = false
	}
	field {
		name = "Reason:"
		value = reason
		inline = false
	}
	footer {
		text = "Requested by ${commandUser.asUser().tag}"
		icon = commandUser.asUser().avatar?.url
	}
}
