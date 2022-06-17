package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.exception.EntityNotFoundException
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging
import net.irisshaders.lilybot.database.DatabaseGetters
import net.irisshaders.lilybot.database.DatabaseHelper

val utilsLogger = KotlinLogging.logger("Checks Logger")

/**
 * This is a check to verify that no element of the guild config is null, since these are all non-nullable values, if
 * any one of them is null, we fail with the unable to access config error message.
 *
 * @author NoComment1105
 * @since 3.2.0
 */
suspend inline fun CheckContext<*>.configPresent() {
	if (!passed) {
		return
	}

	// Prevent commands being run in DMs, although [anyGuild] should still be used as backup
	if (guildFor(event) == null) fail("Must be in a server")

	// Check all not-null values in the database are not null
	if (DatabaseHelper.getConfig(guildFor(event)!!.id) == null) {
		fail("Unable to access config for this guild! Please inform a member of staff")
	} else pass()
}

/**
 * Gets the channel of the event and checks that the bot has the required [permissions].
 *
 * @param permissions The permissions to check the bot for
 *
 * @author NoComment1105
 * @since 3.4.0
 */
suspend inline fun CheckContext<*>.botHasChannelPerms(permissions: Permissions) {
	if (!passed) {
		return
	}

	/* Use `TextChannel` when the channel is a Text channel */
	if (channelFor(event)!!.asChannel().type == ChannelType.GuildText) {
		if (channelFor(event)!!.asChannelOf<TextChannel>().getEffectivePermissions(event.kord.selfId)
				.contains(Permissions(permissions))
		) {
			pass()
		} else {
			fail("Incorrect permissions!\nI do not have the ${permissions.values} permissions for ${channelFor(event)?.mention}")
		}
	} else if (channelFor(event)!!.asChannel().type == ChannelType.PublicGuildThread ||
		channelFor(event)!!.asChannel().type == ChannelType.PublicNewsThread ||
		channelFor(event)!!.asChannel().type == ChannelType.PrivateThread
	) {
		if (channelFor(event)!!.asChannelOf<ThreadChannel>().getParent().getEffectivePermissions(event.kord.selfId)
				.contains(Permissions(permissions))
		) {
			pass()
		} else {
			fail("Incorrect permissions!\nI do not have the ${permissions.values} permissions for ${channelFor(event)?.mention}")
		}
	} else {
		fail("Unable to get permissions for channel! Please report this to the developers!")
	}
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
suspend inline fun EphemeralSlashCommandContext<*>.isBotOrModerator(user: User, commandName: String): String? {
	val moderatorRoleId = DatabaseGetters.getConfig(guild!!.id)?.moderationConfigData!!.team
	if (DatabaseGetters.getConfig(guild!!.id)?.moderationConfigData!!.enabled) {
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
		utilsLogger.warn { "isBot and isModerator checks failed on $commandName." }
	}

	return "success" // Nothing should be done with the success, checks should be based on if this function returns null
}
