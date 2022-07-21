package net.irisshaders.lilybot.utils

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.ChannelType
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.PresenceStatus
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.collections.ConfigMetaCollection
import net.irisshaders.lilybot.database.collections.GalleryChannelCollection
import net.irisshaders.lilybot.database.collections.GuildLeaveTimeCollection
import net.irisshaders.lilybot.database.collections.LoggingConfigCollection
import net.irisshaders.lilybot.database.collections.MainMetaCollection
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.RemindMeCollection
import net.irisshaders.lilybot.database.collections.RoleMenuCollection
import net.irisshaders.lilybot.database.collections.StatusCollection
import net.irisshaders.lilybot.database.collections.SupportConfigCollection
import net.irisshaders.lilybot.database.collections.TagsCollection
import net.irisshaders.lilybot.database.collections.ThreadsCollection
import net.irisshaders.lilybot.database.collections.WarnCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import org.koin.dsl.bind

@PublishedApi
internal val utilsLogger = KotlinLogging.logger("Checks Logger")

/**
 * This is a check to verify that no element of the guild config is null, since these are all non-nullable values, if
 * any one of them is null, we fail with the unable to access config error message.
 *
 * @author NoComment1105
 * @since 3.2.0
 */
suspend inline fun CheckContext<*>.configPresent(vararg configType: ConfigType) {
	if (!passed) {
		return
	}

	// Prevent commands being run in DMs, although [anyGuild] should still be used as backup
	guildFor(event) ?: fail("Must be in a server")

	if (configType.isEmpty()) {
		fail("There is no config type provided in the code. Please inform the developers immediately!")
	}

	// Look at the config type and check the presence of the config in the database.
	configType.forEach {
		when (it) {
			ConfigType.SUPPORT ->
				if (SupportConfigCollection().getConfig(guildFor(event)!!.id) == null) {
					fail("Unable to access Support config for this guild! Please inform a member of staff.")
				} else {
					pass()
				}

			ConfigType.MODERATION ->
				if (ModerationConfigCollection().getConfig(guildFor(event)!!.id) == null) {
					fail("Unable to access Moderation config for this guild! Please inform a member of staff.")
				} else {
					pass()
				}

			ConfigType.LOGGING ->
				if (LoggingConfigCollection().getConfig(guildFor(event)!!.id) == null) {
					fail("Unable to access Logging config for this guild! Please inform a member of staff.")
				} else {
					pass()
				}

			ConfigType.ALL ->
				if (SupportConfigCollection().getConfig(guildFor(event)!!.id) == null ||
					ModerationConfigCollection().getConfig(guildFor(event)!!.id) == null ||
					LoggingConfigCollection().getConfig(guildFor(event)!!.id) == null
				) {
					fail("Unable to access config for this guild! Please inform a member of staff.")
				} else {
					pass()
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
	if (channelFor(event)!!.asChannel().type == ChannelType.GuildText) {
		if (channelFor(event)!!.asChannelOf<TextChannel>().getEffectivePermissions(event.kord.selfId)
				.contains(Permissions(permissions))
		) {
			pass()
		} else {
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${channelFor(event)?.mention}"
			)
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
			fail(
				"Incorrect permissions!\nI do not have the $permissionsSet permissions for ${channelFor(event)?.mention}"
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
	val moderatorRoleId = ModerationConfigCollection().getConfig(guild!!.id)?.team
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
	if (StatusCollection().getStatus() != "default") {
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
