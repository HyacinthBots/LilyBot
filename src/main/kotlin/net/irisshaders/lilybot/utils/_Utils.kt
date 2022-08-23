package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import mu.KotlinLogging

@PublishedApi
internal val utilsLogger = KotlinLogging.logger("Checks Logger")

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
	guildFor(event) ?: fail("Must be in a server")

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

	val eventChannel = channelFor(event)?.asChannel() ?: return

	val permissionsSet: MutableSet<String> = mutableSetOf()
	var count = 0
	permissions.values.forEach { _ ->
		permissionsSet.add(
			permissions.values.toString()
				.split(",")[count]
				.split(".")[4]
				.split("$")[1]
				.split("@")[0]
				.replace("[", "`")
				.replace("]", "`")
		)
		count++
	}

	/* Use `TextChannel` when the channel is a Text channel */
	if (eventChannel is TextChannel) {
		if (eventChannel.asChannelOf<TextChannel>().getEffectivePermissions(event.kord.selfId)
				.contains(Permissions(permissions))
		) {
			pass()
		} else {
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${eventChannel.mention}"
			)
		}
	} else if (eventChannel is NewsChannel) {
		if (eventChannel.asChannelOf<NewsChannel>().getEffectivePermissions(event.kord.selfId)
				.contains(Permissions(permissions))
		) {
			pass()
		} else {
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${eventChannel.mention}"
			)
		}
	} else if (eventChannel is ThreadChannel) {
		if (eventChannel.asChannelOf<ThreadChannel>().getParent().getEffectivePermissions(event.kord.selfId)
				.contains(Permissions(permissions))
		) {
			pass()
		} else {
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${eventChannel.mention}"
			)
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
	val moderatorRoleId = DatabaseHelper.getConfig(guild!!.id)?.moderatorsPing
	moderatorRoleId ?: run {
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

/**
 * Update the presence to reflect the new number of guilds, if the presence is set to "default".
 * @author NoComment1105
 * @since 3.4.5
 */
suspend inline fun Extension.updateDefaultPresence() {
	if (DatabaseHelper.getStatus() != "default") {
		return
	}

	kord.editPresence {
		status = PresenceStatus.Online
		watching("${getGuildCount()} servers")
	}
}

/**
 * Get the number of guilds the bot is in.
 *
 * @return The number of guilds the bot is in.
 * @author NoComment1105
 * @since 3.4.5
 */
suspend inline fun Extension.getGuildCount() = kord.with(EntitySupplyStrategy.cacheWithRestFallback).guilds.count()

/**
 * Get the first text channel the bot can send a message in.
 *
 * @param inputGuild The guild in which to get the channel.
 * @return The first text channel the bot can send a message in or null if there isn't one.
 * @author tempest15
 * @since 3.5.4
 */
suspend inline fun getFirstUsableChannel(inputGuild: Guild): TextChannel? {
	var channel: TextChannel? = null

	inputGuild.channels.collect {
		if (it.botHasPermissions(Permission.ViewChannel, Permission.SendMessages)) {
			channel = it.fetchChannelOrNull()?.asChannelOfOrNull()
			if (channel != null) {
				return@collect
			}
		}
	}
	return channel
}
