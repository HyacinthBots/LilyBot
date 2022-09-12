package org.hyacinthbots.lilybot.utils

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.FollowupPermittingInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.collections.ConfigMetaCollection
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.database.collections.GuildLeaveTimeCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.MainMetaCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.RemindMeCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.StatusCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.koin.dsl.bind

@PublishedApi
internal val utilsLogger = KotlinLogging.logger("Checks Logger")

/**
 * This is a check to verify that no element of the guild config is null, since these are all non-nullable values, if
 * any one of them is null, we fail with the unable to access config error message.
 *
 * @param configOptions The config options to check the database for.
 * @author NoComment1105
 * @since 3.2.0
 */
suspend inline fun CheckContext<*>.configPresent(vararg configOptions: ConfigOptions) {
	if (!passed) {
		return
	}

	// Prevent commands being run in DMs, although [anyGuild] should still be used as backup
	guildFor(event) ?: fail("Must be in a server")

	if (configOptions.isEmpty()) {
		fail("There are no config options provided in the code. Please inform the developers immediately!")
	}

	// Look at the config options and check the presence of the config in the database.
	for (option in configOptions) {
		when (option) {
			ConfigOptions.SUPPORT_ENABLED -> {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				if (supportConfig == null) {
					fail("Unable to access support config for this guild! Please inform a member of staff.")
					break
				} else if (!supportConfig.enabled) {
					fail("Support is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.SUPPORT_CHANNEL -> {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				if (supportConfig == null) {
					fail("Unable to access support config for this guild! Please inform a member of staff.")
					break
				} else if (supportConfig.channel == null) {
					fail("A support channel has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.SUPPORT_ROLE -> {
				val supportConfig = SupportConfigCollection().getConfig(guildFor(event)!!.id)
				if (supportConfig == null) {
					fail("Unable to access support config for this guild! Please inform a member of staff.")
					break
				} else if (supportConfig.role == null) {
					fail("A support role has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MODERATION_ENABLED -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (!moderationConfig.enabled) {
					fail("Moderation is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MODERATOR_ROLE -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (moderationConfig.role == null) {
					fail("A moderator role has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.ACTION_LOG -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (moderationConfig.channel == null) {
					fail("An action log has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.LOG_PUBLICLY -> {
				val moderationConfig = ModerationConfigCollection().getConfig(guildFor(event)!!.id)
				if (moderationConfig == null) {
					fail("Unable to access moderation config for this guild! Please inform a member of staff.")
					break
				} else if (moderationConfig.publicLogging == null) {
					fail("Public logging has not been enabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (!loggingConfig.enableMessageDeleteLogs) {
					fail("Message delete logging is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (!loggingConfig.enableMessageEditLogs) {
					fail("Message edit logging is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MESSAGE_LOG -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (loggingConfig.messageChannel == null) {
					fail("A message log has not been set for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MEMBER_LOGGING_ENABLED -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (!loggingConfig.enableMemberLogs) {
					fail("Member logging is disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.MEMBER_LOG -> {
				val loggingConfig = LoggingConfigCollection().getConfig(guildFor(event)!!.id)
				if (loggingConfig == null) {
					fail("Unable to access logging config for this guild! Please inform a member of staff.")
					break
				} else if (loggingConfig.memberLog == null) {
					fail("A member log has not been set for this guild")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.LOG_UPLOADS_ENABLED -> {
				val utilityConfig = UtilityConfigCollection().getConfig(guildFor(event)!!.id)
				if (utilityConfig == null) {
					fail("Unable to access utility config for this guild! Please inform a member of staff.")
					break
				} else if (utilityConfig.disableLogUploading) {
					fail("Log uploads are disabled for this guild!")
					break
				} else {
					pass()
				}
			}

			ConfigOptions.UTILITY_LOG -> {
				val utilityConfig = UtilityConfigCollection().getConfig(guildFor(event)!!.id)
				if (utilityConfig == null) {
					fail("Unable to access utility config for this guild! Please inform a member of staff.")
					break
				} else if (utilityConfig.utilityLogChannel == null) {
					fail("A utility log has not been set for this guild")
					break
				} else {
					pass()
				}
			}
		}
	}
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
	val moderatorRoleId = ModerationConfigCollection().getConfig(guild!!.id)?.role
	ModerationConfigCollection().getConfig(guild!!.id) ?: run {
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
	if (StatusCollection().getStatus() != null) {
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
 * This function loads the database and checks if it is up-to-date. If it isn't, it will update the database via
 * migrations.
 *
 * @since 4.0.0
 */
suspend inline fun ExtensibleBotBuilder.database(migrate: Boolean) {
	val db = Database()

	hooks {
		beforeKoinSetup {
			loadModule {
				single { db } bind Database::class
			}

			loadModule {
				single { ModerationConfigCollection() } bind ModerationConfigCollection::class
				single { SupportConfigCollection() } bind SupportConfigCollection::class
				single { LoggingConfigCollection() } bind LoggingConfigCollection::class
				single { UtilityConfigCollection() } bind UtilityConfigCollection::class
				single { GalleryChannelCollection() } bind GalleryChannelCollection::class
				single { GuildLeaveTimeCollection() } bind GuildLeaveTimeCollection::class
				single { MainMetaCollection() } bind MainMetaCollection::class
				single { ConfigMetaCollection() } bind ConfigMetaCollection::class
				single { RemindMeCollection() } bind RemindMeCollection::class
				single { RoleMenuCollection() } bind RoleMenuCollection::class
				single { StatusCollection() } bind StatusCollection::class
				single { TagsCollection() } bind TagsCollection::class
				single { ThreadsCollection() } bind ThreadsCollection::class
				single { WarnCollection() } bind WarnCollection::class
			}

			if (migrate) {
				runBlocking {
					db.migrate()
				}
			}
		}
	}
}

/**
 * Get the first text channel the bot can send a message in.
 *
 * @param inputGuild The guild in which to get the channel.
 * @return The first text channel the bot can send a message in or null if there isn't one.
 * @author tempest15
 * @since 3.5.4
 */
suspend inline fun getFirstUsableChannel(inputGuild: Guild): GuildMessageChannel? = inputGuild.channels.first {
	it.botHasPermissions(Permission.ViewChannel, Permission.SendMessages)
}.fetchChannelOrNull()?.asChannelOfOrNull()

/**
 * Check if the bot can send messages in a guild's configured logging channel.
 * If the bot can't, reset a config and send a message in the top usable channel saying that the config was reset or
 * if this function is in a command, an [interactionResponse] is provided, allowing a response to be given on the
 * command.
 * If the bot can, return the channel.
 *
 * **DO NOT USE THIS FUNCTION ON NON-MODERATION CHANNELS!** Use the [botHasChannelPerms] check instead.
 *
 * @param inputGuild The guild to check in.
 * @param targetChannel The channel to check permissions for
 * @param configType The config the channel will be in
 * @param interactionResponse The interactionResponse to respond to if this function is in a command.
 * @return The channel or null if it does not have the correct permissions.
 * @author tempest15
 * @since 3.5.4
 */
suspend inline fun <T : FollowupPermittingInteractionResponseBehavior?> getLoggingChannelWithPerms(
	inputGuild: Guild,
	targetChannel: Snowflake?,
	configType: ConfigType,
	interactionResponse: T? = null
): GuildMessageChannel? {
	val channel = targetChannel?.let { inputGuild.getChannelOfOrNull<GuildMessageChannel>(it) }

	// Check each permission in a separate check because all in one expects all to be there or not. This allows for
	// some permissions to be false and some to be true while still producing the correct result.
	if (channel?.botHasPermissions(Permission.ViewChannel) != true ||
		!channel.botHasPermissions(Permission.SendMessages) ||
		!channel.botHasPermissions(Permission.EmbedLinks)
	) {
		val usableChannel = getFirstUsableChannel(inputGuild) ?: return null

		if (interactionResponse == null) {
			usableChannel.createMessage(
				"Lily cannot send messages in ${channel?.mention}. " +
						"As a result, your config has been reset. " +
						"Please fix the permissions before setting a new config."
			)
		} else {
			interactionResponse.createEphemeralFollowup {
				content = "Lily cannot send messages in ${channel?.mention}. " +
						"As a result, your config has been reset. " +
						"Please fix the permissions before setting a new config."
			}
		}

		delay(3000) // So that other events may finish firing
		when (configType) {
			ConfigType.MODERATION -> ModerationConfigCollection().clearConfig(usableChannel.guildId)
			ConfigType.LOGGING -> LoggingConfigCollection().clearConfig(usableChannel.guildId)
			ConfigType.SUPPORT -> SupportConfigCollection().clearConfig(usableChannel.guildId)
			ConfigType.UTILITY -> UtilityConfigCollection().clearConfig(usableChannel.guildId)
			ConfigType.ALL -> {
				ModerationConfigCollection().clearConfig(usableChannel.guildId)
				LoggingConfigCollection().clearConfig(usableChannel.guildId)
				SupportConfigCollection().clearConfig(usableChannel.guildId)
				UtilityConfigCollection().clearConfig(usableChannel.guildId)
			}
		}

		return null
	}

	return channel
}

/**
 * Overload function for [getLoggingChannelWithPerms] that does not take an interaction response allowing the type
 * variable not be specified in the function.
 *
 * **DO NOT USE THIS FUNCTION ON NON-MODERATION CHANNELS!** Use the [botHasChannelPerms] check instead.
 *
 * @see getLoggingChannelWithPerms
 *
 * @param inputGuild The guild to check in.
 * @param targetChannel The channel to check permissions for
 * @param configType The config the channel will be in
 * @return The channel or null if it does not have the correct permissions.
 * @author NoComment1105
 */
suspend inline fun getLoggingChannelWithPerms(
	inputGuild: Guild,
	targetChannel: Snowflake?,
	configType: ConfigType
): GuildMessageChannel? =
	getLoggingChannelWithPerms(inputGuild, targetChannel, configType, null)

/**
 * A small function to get a log of a guild or the first available channel.
 *
 * @param configOption The option to get the channel of
 * @param guild The guild for the channel
 * @return The utility log or the first usable channel
 * @throws IllegalArgumentException when the [configOption] is invalid
 * @author NoComment1105
 * @since 4.0.1
 */
suspend inline fun getChannelOrFirstUsable(configOption: ConfigOptions, guild: GuildBehavior?): GuildMessageChannel? {
	val channel = when (configOption) {
		ConfigOptions.ACTION_LOG -> ModerationConfigCollection().getConfig(guild!!.id)?.channel
		ConfigOptions.MESSAGE_LOG -> LoggingConfigCollection().getConfig(guild!!.id)?.messageChannel
		ConfigOptions.MEMBER_LOG -> LoggingConfigCollection().getConfig(guild!!.id)?.memberLog
		ConfigOptions.UTILITY_LOG -> UtilityConfigCollection().getConfig(guild!!.id)?.utilityLogChannel
		else -> throw IllegalArgumentException("Config Option $configOption does not point to a channel.")
	}
	return if (channel != null) {
		guild.getChannelOf(channel)
	} else {
		guild.asGuild().getSystemChannel() ?: getFirstUsableChannel(guild.asGuild())
	}
}

/**
 * Utility to get a string or a default value.
 * Basically String.ifEmpty but works with nullable strings
 *
 * @return This, or defaultValue if this is null or empty
 * @author trainb0y
 * @since 4.1.0
 * @see String.ifEmpty
 */
fun String?.ifNullOrEmpty(defaultValue: () -> String): String =
	if (this.isNullOrEmpty()) {
		defaultValue()
	} else {
		this
	}

/**
 * Get this message's contents, trimmed to 1024 characters.
 * If the message exceeds that length, it will be truncated and an ellipsis appended.
 * @author trainb0y
 * @since 4.1.0
 */
fun Message?.trimmedContents(): String? {
	this ?: return null
	return if (this.content.length > 1024) {
		this.content.substring(0, 1020) + " ..."
	} else this.content
}
