package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.commands.application.message.EphemeralMessageCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.kordLogger
import dev.kord.rest.builder.message.EmbedBuilder
import kotlinx.coroutines.flow.toList

/**
 * This is a simple function to get a value from the configuration database in an [EphemeralSlashCommandContext],
 * null check it, and respond accordingly. It takes a string input of the column that is requested and gets it, if it's
 * null a private error message (i.e. for commands staff run), and null is returned. If null this should be handled with
 * an elvis operator to return the action.
 *
 * @param inputColumn The column you wish to get from the database
 * @return The column value from the database or null
 * @author NoComment1105
 */
suspend fun EphemeralSlashCommandContext<*>.getConfigPrivateResponse(inputColumn: String) =
	DatabaseHelper.getConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Is your configuration set?"
		}
		null
}


/**
 * This is a simple function to get a value from the configuration database in an [EphemeralSlashCommandContext],
 * null check it, and respond accordingly. It takes a string input of the column that is requested and gets it, if it's
 * null a public error message (i.e. for commands the people run), and null is returned. If null this should be handled
 * with an elvis operator to return the action.
 *
 * @param inputColumn The column you wish to get from the database
 * @return The column value from the database or a public error and null
 * @author NoComment1105
 */
suspend fun EphemeralSlashCommandContext<*>.getConfigPublicResponse(inputColumn: String) =
	DatabaseHelper.getConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Please inform a member of staff!"
		}
		null
}

/**
 * This is a simple function to get a value from the configuration database in an [EphemeralMessageCommandContext],
 * null check it, and respond accordingly. It takes a string input of the column that is requested and gets it, if it's
 * null a private error message (i.e. for commands staff run), and null is returned. If null this should be handled with
 * an elvis operator to return the action.
 *
 * @param inputColumn The column you wish to get from the database
 * @return The column value from the database or a private error and null
 * @author NoComment1105
 */
suspend fun EphemeralMessageCommandContext.getConfigPublicResponse(inputColumn: String) =
	DatabaseHelper.getConfig(guild!!.id, inputColumn) ?: run {
		respond {
			content = "**Error:** Unable to access config for this guild! Please inform a member of staff!"
		}
		null
}

/**
 * This function runs a check to see if the target user is a bot or moderator in an [EphemeralSlashCommandContext],
 * before responding accordingly. It takes the target user as an input, allowing said user to pass through the checks.
 * It also takes in the command name to make the response more detailed to the command it is called from. If at any
 * point the check fails, null is returned. This should be handled with an elvis operator to return the action in the
 * code.
 *
 * @param user The target user in the command
 * @param commandName The name of the command. Used for the responses and error message
 * @return *null*, if user is a bot/moderator. *success* if it isn't
 * @author NoComment1105
 */
suspend fun EphemeralSlashCommandContext<*>.isBotOrModerator(user: User, commandName: String): String? {
	val moderatorRoleId = getConfigPrivateResponse("moderatorsPing")

	try {
		// Get the users roles into a List of Snowflakes
		val roles = user.asMember(guild!!.id).roles.toList().map { it.id }
		// If the user is a bot, return
		if (guild?.getMember(user.id)?.isBot == true) {
			respond {
				content = "You cannot $commandName bot users!"
			}
			return null
		// If the moderator ping role is in roles, return
		} else if (moderatorRoleId in roles) {
			respond {
				content = "You cannot $commandName moderators!"
			}
			return null
		}
	// Just to catch any errors in the checks
	} catch (exception: EntityNotFoundException) {
		kordLogger.warn { "isBot and isModerator checks failed on $commandName." }
	}

	return "success"
}

/**
 * This function uses a DM variable provided by the place it is run it, and checks to see it succeeded in sending the
 * user a DM.
 *
 * @param dm The direct message that is sent to the user
 * @author NoComment1105
 */
fun EmbedBuilder.dmNotificationStatusEmbedField(dm: Message?) {
	field {
		name = "User Notification:"
		value =
			if (dm != null) {
				"User notified with a direct message"
			} else {
				"Failed to notify user with a direct message"
			}
		inline = false
	}
}

/**
 * This function will take in a [reason] for a sanction and the [user] who ran the command before generating the fields to
 * an embed, reason and footer. Despite this calling the footer builder, you can provide field elements after this
 * function in code, and it will still place the footer where it should be, without error
 *
 * @param reason The reason for the sanction
 * @param user The user that ran the command
 * @author NoComment1105
 */
suspend fun EmbedBuilder.reasonAndFooterEmbedField(reason: String, user: UserBehavior) {
	field {
		name = "Reason:"
		value = reason
		inline = false
	}
	footer {
		text = "Requested by ${user.asUser().tag}"
		icon = user.asUser().avatar?.url
	}
}
