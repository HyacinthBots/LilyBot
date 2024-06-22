package org.hyacinthbots.lilybot.utils

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.checks.types.CheckContextWithCache
import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import com.kotlindiscord.kord.extensions.utils.getTopRole
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions

/**
 * This function checks if a single config exists and is valid. Returns true if it is or false otherwise.
 *
 * @param channelType The type of logging channel desired
 * @param guild The guild the desired channel is in
 * @param resetConfig If configured channels should be reset if invalid.
 * Should only be passed as false, and defaults to true.
 * @return The logging channel of [channelType] for the [guild] or null if it doesn't exist
 * @author tempest15
 * @since 4.1.0
 */
suspend inline fun getLoggingChannelWithPerms(
	channelType: ConfigOptions,
	guild: GuildBehavior,
	resetConfig: Boolean? = null
): GuildMessageChannel? {
	val guildId = guild.id

	if (!configIsUsable(guildId, channelType)) return null

	val channelId = when (channelType) {
		ConfigOptions.ACTION_LOG -> ModerationConfigCollection().getConfig(guildId)?.channel ?: return null
		ConfigOptions.UTILITY_LOG -> UtilityConfigCollection().getConfig(guildId)?.utilityLogChannel ?: return null
		ConfigOptions.MESSAGE_LOG -> LoggingConfigCollection().getConfig(guildId)?.messageChannel ?: return null
		ConfigOptions.MEMBER_LOG -> LoggingConfigCollection().getConfig(guildId)?.memberLog ?: return null
		else -> throw IllegalArgumentException("$channelType does not point to a channel.")
	}
	val channel = guild.getChannelOfOrNull<GuildMessageChannel>(channelId) ?: return null

	if (!channel.botHasPermissions(Permission.ViewChannel) || !channel.botHasPermissions(Permission.SendMessages)) {
		if (resetConfig == true) {
			when (channelType) {
				ConfigOptions.ACTION_LOG -> ModerationConfigCollection().clearConfig(guildId)
				ConfigOptions.UTILITY_LOG -> UtilityConfigCollection().clearConfig(guildId)
				ConfigOptions.MESSAGE_LOG -> LoggingConfigCollection().clearConfig(guildId)
				ConfigOptions.MEMBER_LOG -> LoggingConfigCollection().clearConfig(guildId)
				else -> throw IllegalArgumentException("$channelType does not point to a channel.")
			}
			val informChannel = getSystemChannelWithPerms(guild as Guild) ?: getFirstUsableChannel(guild)
			informChannel?.createMessage(
				"Lily is unable to send messages in the configured " +
						"${channelType.toString().lowercase()} for this guild. " +
						"As a result, the corresponding config has been reset. \n\n" +
						"*Note:* this channel has been used to send this message because it's the first channel " +
						"in the guild Lily could use. Please inform this guild's staff about this message."
			)
		}
		return null
	}

	return channel
}

/**
 * Get the first text channel the bot can send a message in.
 *
 * @param inputGuild The guild in which to get the channel.
 * @return The first text channel the bot can send a message in or null if there isn't one.
 * @author tempest15
 * @since 3.5.4
 */
suspend inline fun getFirstUsableChannel(inputGuild: GuildBehavior): GuildMessageChannel? {
	var firstUsable: GuildMessageChannel? = null
	inputGuild.channels.toList().toSortedSet().forEach {
		if (it.botHasPermissions(Permission.ViewChannel) && it.botHasPermissions(Permission.SendMessages)) {
			firstUsable = it.asChannelOfOrNull()
			return@forEach
		}
	}
	return firstUsable
}

/**
 * Gets a guild's system channel as designated by Discord, or null if said channel is invalid or doesn't exist.
 *
 * @param inputGuild The guild in which to get the channel.
 * @return The guild's system channel or null if it's invalid
 * @author tempest15
 * @since 4.1.0
 */
suspend inline fun getSystemChannelWithPerms(inputGuild: Guild): GuildMessageChannel? {
	val systemChannel = inputGuild.getSystemChannel() ?: return null
	if (!systemChannel.botHasPermissions(Permission.ViewChannel) ||
		!systemChannel.botHasPermissions(Permission.SendMessages)
	) {
		return null
	}
	return systemChannel
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

	val eventChannel = channelFor(event)?.asChannelOrNull() ?: return

	val permissionsSet: String = formatPermissionSet(permissions)

	/* Use `TextChannel` when the channel is a Text channel */
	if (eventChannel is TextChannel) {
		if (eventChannel.asChannelOfOrNull<TextChannel>()?.getEffectivePermissions(event.kord.selfId)
				?.contains(Permissions(permissions)) == true
		) {
			pass()
		} else {
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${eventChannel.mention}"
			)
		}
	} else if (eventChannel is NewsChannel) {
		if (eventChannel.asChannelOfOrNull<NewsChannel>()?.getEffectivePermissions(event.kord.selfId)
				?.contains(Permissions(permissions)) == true
		) {
			pass()
		} else {
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${eventChannel.mention}"
			)
		}
	} else if (eventChannel is ThreadChannel) {
		if (eventChannel.asChannelOfOrNull<ThreadChannel>()?.getParent()?.getEffectivePermissions(event.kord.selfId)
				?.contains(Permissions(permissions)) == true
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
 * This function runs a check to see if the target user is a bot or moderator in an [EphemeralInteractionContext],
 * before responding accordingly. It takes the target user as an input, allowing said user to pass through the checks.
 * It also takes in the command name to make the response more detailed to the command it is called from. If at any
 * point the check fails, null is returned. This should be handled with an elvis operator to return the action in the
 * code.
 *
 * @param kord The kord instance so the self of the bot can be gotten if needed
 * @param user The target user in the command
 * @param guild The guild the command was run in
 * @param commandName The name of the command. Used for the responses and error message
 * @return *null*, if user is a bot/moderator. *success* if it isn't
 * @author NoComment1105
 * @since 2.1.0
 */
suspend inline fun EphemeralInteractionContext.isBotOrModerator(
	kord: Kord,
	user: User,
	guild: GuildBehavior?,
	commandName: String
): String? {
	if (guild == null) {
		respond {
			content = "**Error:** Unable to access this guild. Please try again"
		}
		return null
	}

	val moderationConfig = ModerationConfigCollection().getConfig(guild.id)

	moderationConfig ?: run {
		respond {
			content = "**Error:** Unable to access configuration for this guild! Is your configuration set?"
		}
		return null
	}

	val member = user.asMemberOrNull(guild.id) ?: run {
		utilsLogger.debug { "isBotOrModerator skipped on $commandName due to this user not being a member" }
		return "skip"
	}
	val self = kord.getSelf().asMemberOrNull(guild.id) ?: run {
		respond {
			content = "There was an error getting Lily as a member of this server, please try again!"
		}
		return null
	}
	// Get the users roles into a List of Snowflakes
	val roles = member.roles.toList().map { it.id }
	// If the user is a bot, return
	if (member.isBot) {
		respond {
			content = "You cannot $commandName bot users!"
		}
		return null
		// If the moderator ping role is in roles, return
	} else if (moderationConfig.role in roles) {
		respond {
			content = "You cannot $commandName moderators!"
		}
		return null
	} else if (member.getTopRole()?.getPosition() != null && self.getTopRole()?.getPosition() == null) {
		respond {
			content = "This user has a role and Lily does not, therefore she cannot $commandName them."
		}
		return null
	} else if ((member.getTopRole()?.getPosition() ?: 0) > (self.getTopRole()?.getPosition() ?: 0)) {
		respond {
			content = "This users highest role is above Lily's, therefore she cannot $commandName them."
		}
		return null
	}

	return "success" // Nothing should be done with the success, checks should be based on if this function returns null
}

/**
 * Performs the common checks for a moderation command.
 *
 * @param actionPermission The permission to check the user has.
 * @author NoComment1105
 * @since 4.4.0
 */
suspend fun CheckContextWithCache<*>.modCommandChecks(actionPermission: Permission) {
	anyGuild()
	requiredConfigs(ConfigOptions.MODERATION_ENABLED)
	hasPermission(actionPermission)
}

fun formatPermissionSet(permissions: Permissions): String {
	val permissionsSet: MutableSet<String> = mutableSetOf()
	var count = 0
	permissions.values.forEach { _ ->
		permissionsSet.add(
			permissions.values.toString()
				.split(",")[count]
				.split(".")[1]
				.replace("]", "")
		)
		count++
	}
	return permissionsSet.toString().replace("[", "").replace("]", "").ifEmpty { "None" }
}
