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
 */
suspend fun CheckContext<*>.configPresent() {
	if (!passed) {
		return
	}

	if (guildFor(event) == null) fail("Must be in a server")

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

	return "success"
}
