package org.hyacinthbots.lilybot.utils

import com.kotlindiscord.kord.extensions.builders.ExtensibleBotBuilder
import com.kotlindiscord.kord.extensions.checks.channelFor
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.types.CheckContext
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.extra.pluralkit.api.PKMessage
import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import com.kotlindiscord.kord.extensions.utils.getTopRole
import com.kotlindiscord.kord.extensions.utils.loadModule
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.GuildBehavior
import dev.kord.core.behavior.RoleBehavior
import dev.kord.core.behavior.channel.asChannelOf
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.NewsChannel
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kord.rest.builder.message.EmbedBuilder
import io.github.nocomment1105.discordmoderationactions.enums.DmResult
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DateTimePeriod
import mu.KotlinLogging
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.collections.ConfigMetaCollection
import org.hyacinthbots.lilybot.database.collections.GalleryChannelCollection
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.database.collections.GuildLeaveTimeCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.MainMetaCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.StatusCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.TagsCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.collections.WarnCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
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
suspend inline fun CheckContext<*>.requiredConfigs(vararg configOptions: ConfigOptions) {
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
 * This function checks if a single config exists and is valid. Returns true if it is or false otherwise.
 *
 * @param option The config option to check the database for. Only takes a single option.
 * @return True if the selected [option] is valid and enabled and false if it isn't
 * @author NoComment1105
 * @since 3.2.0
 */
suspend inline fun configIsUsable(option: ConfigOptions, guildId: Snowflake): Boolean {
	when (option) {
		ConfigOptions.SUPPORT_ENABLED -> return SupportConfigCollection().getConfig(guildId)?.enabled ?: false

		ConfigOptions.SUPPORT_CHANNEL -> {
			val supportConfig = SupportConfigCollection().getConfig(guildId) ?: return false
			return supportConfig.channel != null
		}

		ConfigOptions.SUPPORT_ROLE -> {
			val supportConfig = SupportConfigCollection().getConfig(guildId) ?: return false
			return supportConfig.role != null
		}

		ConfigOptions.MODERATION_ENABLED -> return ModerationConfigCollection().getConfig(guildId)?.enabled ?: false

		ConfigOptions.MODERATOR_ROLE -> {
			val moderationConfig = ModerationConfigCollection().getConfig(guildId) ?: return false
			return moderationConfig.role != null
		}

		ConfigOptions.ACTION_LOG -> {
			val moderationConfig = ModerationConfigCollection().getConfig(guildId) ?: return false
			return moderationConfig.channel != null
		}

		ConfigOptions.LOG_PUBLICLY -> {
			val moderationConfig = ModerationConfigCollection().getConfig(guildId) ?: return false
			return moderationConfig.publicLogging != null
		}

		ConfigOptions.MESSAGE_DELETE_LOGGING_ENABLED ->
			return LoggingConfigCollection().getConfig(guildId)?.enableMessageDeleteLogs ?: false

		ConfigOptions.MESSAGE_EDIT_LOGGING_ENABLED ->
			return LoggingConfigCollection().getConfig(guildId)?.enableMessageEditLogs ?: false

		ConfigOptions.MESSAGE_LOG -> {
			val loggingConfig = LoggingConfigCollection().getConfig(guildId) ?: return false
			return loggingConfig.messageChannel != null
		}

		ConfigOptions.MEMBER_LOGGING_ENABLED -> return LoggingConfigCollection().getConfig(guildId)?.enableMemberLogs
			?: false

		ConfigOptions.MEMBER_LOG -> {
			val loggingConfig = LoggingConfigCollection().getConfig(guildId) ?: return false
			return loggingConfig.memberLog != null
		}

		ConfigOptions.LOG_UPLOADS_ENABLED -> {
			val utilityConfig = UtilityConfigCollection().getConfig(guildId) ?: return false
			return utilityConfig.disableLogUploading
		}

		ConfigOptions.UTILITY_LOG -> {
			val utilityConfig = UtilityConfigCollection().getConfig(guildId) ?: return false
			return utilityConfig.utilityLogChannel != null
		}
	}
}

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

	if (!configIsUsable(channelType, guildId)) return null

	val channelId = when (channelType) {
		ConfigOptions.SUPPORT_CHANNEL -> SupportConfigCollection().getConfig(guildId)?.channel ?: return null
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
				ConfigOptions.SUPPORT_CHANNEL -> SupportConfigCollection().clearConfig(guildId)
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
suspend inline fun getFirstUsableChannel(inputGuild: GuildBehavior): GuildMessageChannel? =
	inputGuild.channels.first {
		it.botHasPermissions(Permission.ViewChannel, Permission.SendMessages)
	}.asChannelOfOrNull()

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
	) return null
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
	val moderatorRoleId = ModerationConfigCollection().getConfig(guild!!.id)?.role
	ModerationConfigCollection().getConfig(guild.id) ?: run {
		respond {
			content = "**Error:** Unable to access configuration for this guild! Is your configuration set?"
		}
		return null
	}

	val member = user.asMemberOrNull(guild.id) ?: return null
	val self = kord.getSelf().asMemberOrNull(guild.id) ?: return null
	// Get the users roles into a List of Snowflakes
	val roles = member.roles.toList().map { it.id }
	// If the user is a bot, return
	if (member.isBot) {
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
	} else if (member.getTopRole()?.getPosition() != null && self.getTopRole()?.getPosition() == null) {
		respond {
			content = "This user has a role and Lily does not, therefore she cannot $commandName them."
		}
		return null
	} else if (member.getTopRole()?.getPosition()!! > self.getTopRole()?.getPosition()!!) {
		respond {
			content = "This users highest role is above Lily's, therefore she cannot $commandName them."
		}
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
				single { GithubCollection() } bind GithubCollection::class
				single { GuildLeaveTimeCollection() } bind GuildLeaveTimeCollection::class
				single { MainMetaCollection() } bind MainMetaCollection::class
				single { ConfigMetaCollection() } bind ConfigMetaCollection::class
				single { ReminderCollection() } bind ReminderCollection::class
				single { RoleMenuCollection() } bind RoleMenuCollection::class
				single { StatusCollection() } bind StatusCollection::class
				single { TagsCollection() } bind TagsCollection::class
				single { ThreadsCollection() } bind ThreadsCollection::class
				single { WarnCollection() } bind WarnCollection::class
				single { WelcomeChannelCollection() } bind WelcomeChannelCollection::class
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

/**
 * @see trimmedContents
 * @author trainb0y
 * @since 4.2.0
 */
fun String?.trimmedContents(): String? {
	this ?: return null
	return if (this.length > 1024) {
		this.substring(0, 1020) + " ..."
	} else this
}

/**
 * Get this message's contents, trimmed to the [desiredLength] of characters.
 * If the message exceeds that length, it will be truncated and an ellipsis appended.
 * If the message is smaller than the [desiredLength], the content length is used and an elipsis appended
 *
 * @param desiredLength The desired length to limit the string too
 * @author NoComment1105
 * @since 4.2.0
 */
fun Message?.trimmedContents(desiredLength: Int): String? {
	this ?: return null
	val useRegularLength = this.content.length < desiredLength.coerceIn(1, 1020)
	return if (this.content.length > desiredLength.coerceIn(1, 1020)) {
		this.content.substring(0, if (useRegularLength) this.content.length else desiredLength) + "..."
	} else this.content
}

/**
 * @see trimmedContents
 * @author NoComment1105
 * @since 4.2.0
 */
fun String?.trimmedContents(desiredLength: Int): String? {
	this ?: return null
	val useRegularLength = this.length < desiredLength.coerceIn(1, 1020)
	return if (this.length > desiredLength.coerceIn(1, 1020)) {
		this.substring(0, if (useRegularLength) this.length else desiredLength) + "..."
	} else this
}

/**
 * Checks a string to see if it fits the in a discord embed field.
 *
 * @return True, if the given string fits and embed, false if not
 * @author NoComment1105
 * @since 4.2.0
 */
fun String?.fitsEmbed(): Boolean? {
	this ?: return null
	return this.length <= 1024
}

/**
 * This function removed duplicated code from MessageDelete and MessageEdit.
 * It holds attachment and PluralKit info fields for the logging embeds.
 * @author tempest15
 * @since 4.1.0
 */
suspend fun EmbedBuilder.attachmentsAndProxiedMessageInfo(
	guild: Guild,
	message: Message,
	proxiedMessage: PKMessage?
) {
	if (message.attachments.isNotEmpty()) {
		field {
			name = "Attachments"
			value = message.attachments.joinToString(separator = "\n") { it.url }
			inline = false
		}
	}
	if (proxiedMessage != null) {
		field {
			name = "Message Author:"
			value = "System Member: ${proxiedMessage.member?.name}\n" +
					"Account: ${guild.getMember(proxiedMessage.sender).tag} " +
					guild.getMember(proxiedMessage.sender).mention
			inline = true
		}

		field {
			name = "Author ID:"
			value = proxiedMessage.sender.toString()
		}
	} else {
		field {
			name = "Message Author:"
			value =
				"${message.author?.tag ?: "Failed to get author of message"} ${message.author?.mention ?: ""}"
			inline = true
		}

		field {
			name = "Author ID:"
			value = message.author?.id.toString()
		}
	}
}

/**
 * Check if a role is mentionable by Lily.
 *
 * @param role The role to check
 * @return A Boolean of whether it is pingable or not
 *
 * @author NoComment1105
 * @since 4.1.0
 */
suspend inline fun canPingRole(role: RoleBehavior?) = role != null && role.guild.getRole(role.id).mentionable

fun getDmResult(shouldDm: Boolean, dm: Message?): DmResult {
	@Suppress("KotlinConstantConditions") // Yes, but I want to be 100% sure
	return if (shouldDm && dm != null) {
		DmResult.DM_SUCCESS
	} else if (shouldDm && dm == null) {
		DmResult.DM_FAIL
	} else {
		DmResult.DM_NOT_SENT
	}
}

/**
 * Converts a [DateTimePeriod] into a [String] interval at which it repeats at.
 *
 * @return The string interval the DateTimePeriod repeats at
 * @author NoComment1105
 * @since 4.2.0
 */
fun DateTimePeriod?.interval(): String? {
	this ?: return null
	return this.toString().lowercase().replace("pt", "").replace("p", "")
}
