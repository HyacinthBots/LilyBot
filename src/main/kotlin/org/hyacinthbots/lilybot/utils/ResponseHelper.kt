package org.hyacinthbots.lilybot.utils

import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PKMessage
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
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
suspend inline fun EmbedBuilder.baseModerationEmbed(reason: String?, targetUser: User?, commandUser: UserBehavior) {
	field {
		name = "User:"
		value = "${targetUser?.tag ?: "Unable to get user"}\n${targetUser?.id ?: ""}"
		inline = false
	}
	field {
		name = "Reason:"
		value = reason ?: "No reason provided"
		inline = false
	}
	footer {
		text = "Requested by ${commandUser.asUserOrNull()?.tag}"
		icon = commandUser.asUserOrNull()?.avatar?.url
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

/**
 * This function removed duplicated code from MessageDelete and MessageEdit.
 * It holds attachment and PluralKit info fields for the logging embeds.
 * @author tempest15
 * @since 4.1.0
 */
suspend inline fun EmbedBuilder.attachmentsAndProxiedMessageInfo(
	guild: Guild,
	message: Message,
	proxiedMessage: PKMessage?
) {
	if (message.attachments.isNotEmpty()) {
		field {
			name = "Attachments"
			value = message.attachments.joinToString(separator = "\n") { it.url }
			inline = false
		}
	}
	if (proxiedMessage != null) {
		field {
			name = "Message Author:"
			value = "System Member: ${proxiedMessage.member?.name}\n" +
					"Account: ${guild.getMemberOrNull(proxiedMessage.sender)?.tag ?: "Unable to get account"} " +
					guild.getMemberOrNull(proxiedMessage.sender)?.mention
			inline = true
		}

		field {
			name = "Author ID:"
			value = proxiedMessage.sender.toString()
		}
	} else {
		field {
			name = "Message Author:"
			value = "${message.author?.tag ?: "Failed to get author of message"} ${message.author?.mention ?: ""}"
			inline = true
		}

		field {
			name = "Author ID:"
			value = message.author?.id.toString()
		}
	}
}
