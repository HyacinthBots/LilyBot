package org.hyacinthbots.lilybot.utils

import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import io.github.nocomment1105.discordmoderationactions.enums.DmResult

/**
 * This is the base moderation embed for all moderation actions. This should be posted to the action log of a guild.
 * It takes in the reason for the action, the user the action is being targeted to, and the user of the command.
 *
 * @param reason The reason for the action.
 * @param targetUser The targeted user in the action.
 * @param commandUser The user that ran the command.
 * @author NoComment1105
 * @since 3.0.0
 */
suspend inline fun EmbedBuilder.baseModerationEmbed(reason: String?, targetUser: User, commandUser: UserBehavior) {
	field {
		name = "User:"
		value = "${targetUser.tag}\n${targetUser.id}"
		inline = false
	}
	field {
		name = "Reason:"
		value = reason ?: "No reason provided"
		inline = false
	}
	footer {
		text = "Requested by ${commandUser.asUser().tag}"
		icon = commandUser.asUser().avatar?.url
	}
}

/**
 * This function uses a DM variable provided by the place it is run it, and checks to see it succeeded in sending the
 * user a DM.
 *
 * @param dm The direct message that is sent to the user.
 * @author NoComment1105
 * @since 3.0.0
 */
fun EmbedBuilder.dmNotificationStatusEmbedField(dm: DmResult?) {
	field {
		name = "User Notification:"
		value = dm?.message ?: "Fireworks have frazzled the lily"
		inline = false
	}
}
