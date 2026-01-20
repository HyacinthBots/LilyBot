package org.hyacinthbots.lilybot.utils

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
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.channelFor
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.checks.types.CheckContext
import dev.kordex.core.checks.types.CheckContextWithCache
import dev.kordex.core.types.EphemeralInteractionContext
import dev.kordex.core.utils.botHasPermissions
import dev.kordex.core.utils.getTopRole
import kotlinx.coroutines.flow.toList
import lilybot.i18n.Translations
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
			}
			val informChannel = getSystemChannelWithPerms(guild as Guild) ?: getFirstUsableChannel(guild)
			informChannel?.createMessage(
				Translations.Checks.LoggingChannelPerms.cannotGet.translate(channelType.toString().lowercase())
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
			fail(Translations.Checks.ChannelPerms.incorrectPerms.withOrdinalPlaceholders(permissionsSet, eventChannel.mention))
		}
	} else if (eventChannel is NewsChannel) {
		if (eventChannel.asChannelOfOrNull<NewsChannel>()?.getEffectivePermissions(event.kord.selfId)
				?.contains(Permissions(permissions)) == true
		) {
			pass()
		} else {
			fail(Translations.Checks.ChannelPerms.incorrectPerms.withOrdinalPlaceholders(permissionsSet, eventChannel.mention))
		}
	} else if (eventChannel is ThreadChannel) {
		if (eventChannel.asChannelOfOrNull<ThreadChannel>()?.getParent()?.getEffectivePermissions(event.kord.selfId)
				?.contains(Permissions(permissions)) == true
		) {
			pass()
		} else {
			fail(Translations.Checks.ChannelPerms.incorrectPerms.withOrdinalPlaceholders(permissionsSet, eventChannel.mention))
		}
	} else {
		fail(Translations.Checks.ChannelPerms.unableToPerms)
	}
}

/**
 * This function runs a check to see if the target user is a bot or moderator in an [EphemeralInteractionContext],
 * before responding accordingly. It takes the target user as an input, allowing said user to pass through the checks.
 * It also takes in the command name to make the response more detailed to the command it is called from. If at any
 * point the check fails, null is returned. This should be handled with an elvis operator to return the action in the
 * code.
 *
 * @param kord The kord instance so the self of the bot can be got if needed
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
	val translations = Translations.Checks.BotOrMod
	if (guild == null) {
		respond { content = translations.noGuild.translate() }
		return null
	}

	val moderationConfig = ModerationConfigCollection().getConfig(guild.id)

	moderationConfig ?: run {
		respond { content = translations.unableToAccess.translate() }
		return null
	}

	val member = user.asMemberOrNull(guild.id) ?: run {
		utilsLogger.debug { "isBotOrModerator skipped on $commandName due to this user not being a member" }
		return "skip"
	}
	val self = kord.getSelf().asMemberOrNull(guild.id) ?: run {
		respond { content = translations.lilyError.translate() }
		return null
	}
	// Get the users roles into a List of Snowflakes
	val roles = member.roles.toList().map { it.id }
	// If the user is a bot, return
	if (member.isBot) {
		respond { content = translations.cantBot.translate(commandName) }
		return null
		// If the moderator ping role is in roles, return
	} else if (moderationConfig.role in roles) {
		respond { content = translations.cantMod.translate(commandName) }
		return null
	} else if (member.getTopRole()?.getPosition() != null && self.getTopRole()?.getPosition() == null) {
		respond { content = translations.lilyNoRole.translate(commandName) }
		return null
	} else if ((member.getTopRole()?.getPosition() ?: 0) > (self.getTopRole()?.getPosition() ?: 0)) {
		respond { content = translations.userHigher.translate(commandName) }
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

/**
 * Formats [Permissions] into a readable string list, returning "None" if there are no permissions there.
 *
 * @param permissions The [Permissions] to format
 * @return A string containing the permissions
 * @author NoComment1105
 * @since 5.0.0
 */
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
