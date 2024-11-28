package org.hyacinthbots.lilybot.utils

import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kordex.modules.pluralkit.api.PKMessage
import lilybot.i18n.Translations

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
suspend inline fun EmbedBuilder.baseModerationEmbed(reason: String?, targetUser: User?, commandUser: UserBehavior?) {
	field {
		name = Translations.Basic.userField.translate()
		value = "${targetUser?.username ?: Translations.Basic.UnableTo.findUser}\n${targetUser?.id ?: ""}"
		inline = false
	}
	field {
		name = Translations.Moderation.ModCommands.Arguments.Reason.name.translate()
		value = reason ?: Translations.Basic.noReason.translate()
		inline = false
	}
	if (commandUser != null) {
		footer {
			text = Translations.Basic.requestedBy.translate(commandUser.asUserOrNull()?.username)
			icon = commandUser.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
		}
	}
}

/**
 * This function uses a DM variable provided by the place it is run it, and checks to see it succeeded in sending the
 * user a DM.
 *
 * @param dm The direct message that is sent to the user.
 * @param override Whether the DM was forcefully disabled by the command runner.
 * @author NoComment1105
 * @since 3.0.0
 */
fun EmbedBuilder.dmNotificationStatusEmbedField(dm: Message?, override: Boolean) {
	field {
		name = Translations.Utils.DmField.userNotif.translate()
		value = if (dm != null) {
			Translations.Utils.DmField.success.translate()
		} else if (!override) {
			Translations.Utils.DmField.disabled.translate()
		} else {
			Translations.Utils.DmField.failure.translate()
		}
		inline = false
	}
}

/**
 * This function uses a success variable and checks to see if it succeeded in sending the user a DM.
 *
 * @param success Whether the DM was a success or not
 * @param override Whether the DM was forcefully disabled by the command runner.
 * @author NoComment1105
 * @since 5.0.0
 */
fun EmbedBuilder.dmNotificationStatusEmbedField(success: Boolean?, override: Boolean?) {
	field {
		name = Translations.Utils.DmField.userNotif.translate()
		value = if (success != null && success) {
			Translations.Utils.DmField.success.translate()
		} else if (override != null && !override) {
			Translations.Utils.DmField.disabled.translate()
		} else {
			Translations.Utils.DmField.failure.translate()
		}
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
			name = Translations.Utils.Attachments.attachments.translate()
			value = message.attachments.joinToString(separator = "\n") { it.url }
			inline = false
		}
	}
	if (proxiedMessage != null) {
		field {
			name = Translations.Moderation.Report.Confirmation.embedAuthorField.translate()
			value = "${Translations.Utils.Attachments.systemMember.translate()}: ${proxiedMessage.member?.name}\n" +
				"${Translations.Utils.Attachments.account.translate()}: ${
					guild.getMemberOrNull(proxiedMessage.sender)?.username
					    ?: Translations.Utils.Attachments.unableToAccount.translate()
				} " +
				guild.getMemberOrNull(proxiedMessage.sender)?.mention
			inline = true
		}

		field {
			name = Translations.Utils.Attachments.authorId.translate()
			value = proxiedMessage.sender.toString()
		}
	} else {
		field {
			name = Translations.Moderation.Report.Confirmation.embedAuthorField.translate()
			value =
				"${message.author?.username ?: Translations.Basic.UnableTo.findUser.translate()} ${message.author?.mention ?: ""}"
			inline = true
		}

		field {
			name = Translations.Utils.Attachments.authorId.translate()
			value = message.author?.id.toString()
		}
	}
}
