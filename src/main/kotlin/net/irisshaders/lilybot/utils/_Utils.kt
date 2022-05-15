package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.core.entity.User
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.kordLogger
import kotlinx.coroutines.flow.toList

/**
 * This is a check to verify that no element of the guild config is null, since these are all non-nullable values, if
 * any one of them is null, we fail with the unable to access config error message.
 *
 * @author NoComment1105
 * @since 3.2.0
 */
suspend fun CheckContext<*>.configPresent() {
	if (!passed) {
		return
	}

	// Prevent commands being run in DMs, although [anyGuild] should still be used as backup
	if (guildFor(event) == null) fail("Must be in a server")

	// Check all not-null values in the database are not null
	if (DatabaseHelper.getConfig(guildFor(event)!!.id)?.modActionLog == null ||
		DatabaseHelper.getConfig(guildFor(event)!!.id)?.moderatorsPing == null ||
		DatabaseHelper.getConfig(guildFor(event)!!.id)?.messageLogs == null ||
		DatabaseHelper.getConfig(guildFor(event)!!.id)?.joinChannel == null
	) {
		fail("Unable to access config for this guild! Please inform a member of staff")
	} else pass()
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
 * @since 2.1.0
 */
suspend fun EphemeralSlashCommandContext<*>.isBotOrModerator(user: User, commandName: String): String? {
	val moderatorRoleId = DatabaseHelper.getConfig(guild!!.id)?.moderatorsPing
	if (moderatorRoleId == null) {
		respond {
			content = "**Error:** Unable to access configuration for this guild! Is your configuration set?"
		}
		return null
	}
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

	return "success" // Nothing should be done with the success, checks should be based on if this function returns null
}

/**
 * This function will check image inputs from commands and verify they contain all the required [String]s to form
 * a good URL, and avoid any KtorRequestExceptions. It uses [String]s from [URL_REQUIREMENTS] and checks them against
 * a provided [imageURL]. The [imageURL] must contain everything in [URL_REQUIREMENTS] or else the function will
 * return null, to allow the command it has been called in to be returned using an elvis.
 *
 * @param imageURL The URL of the image provided
 * @return **null** if the [imageURL] is bad, "success" if the [imageURL] is good
 * @author NoComment1105
 * @since 3.3.0
 */
suspend fun EphemeralSlashCommandContext<*>.checkImages(imageURL: String?): String? {
	var success = 0

	for (i in URL_REQUIREMENTS) {
		if (imageURL != null && imageURL.contains(i, true)) {
			success++
		}
	}

	return if (success != URL_REQUIREMENTS.size) {
		respond { content = "Invalid Image! Please try again." }
		null
	} else {
		"success"
	}
}
