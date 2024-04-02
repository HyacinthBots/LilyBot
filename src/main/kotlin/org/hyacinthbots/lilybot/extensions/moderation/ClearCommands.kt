package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalInt
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalUser
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.modCommandChecks
import org.hyacinthbots.lilybot.utils.requiredConfigs
import kotlin.math.min

class ClearCommands : Extension() {
	override val name = "clear"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "clear"
			description = "Parent command for clear commands"

			ephemeralSubCommand(ClearCommandArgs::Count) {
				name = "count"
				description = "Clear a specific count of messages"

				requirePermission(Permission.ManageMessages)

				check {
					modCommandChecks(Permission.ManageMessages)
					requireBotPermissions(Permission.ManageMessages)
					botHasChannelPerms(Permissions(Permission.ManageMessages))
				}

				action {
					clearMessages(arguments.count, null, null, arguments.author)
				}
			}

			ephemeralSubCommand(ClearCommandArgs::Before) {
				name = "before"
				description = "Clear messages before a given message ID"

				requirePermission(Permission.ManageMessages)

				check {
					modCommandChecks(Permission.ManageMessages)
					requireBotPermissions(Permission.ManageMessages)
					botHasChannelPerms(Permissions(Permission.ManageMessages))
				}

				action {
					clearMessages(arguments.count, Snowflake(arguments.before.value + 1u), null, arguments.author)
				}
			}

			ephemeralSubCommand(ClearCommandArgs::After) {
				name = "after"
				description = "Clear messages before a given message ID"

				requirePermission(Permission.ManageMessages)

				check {
					modCommandChecks(Permission.ManageMessages)
					requireBotPermissions(Permission.ManageMessages)
					botHasChannelPerms(Permissions(Permission.ManageMessages))
				}

				action {
					clearMessages(arguments.count, null, Snowflake(arguments.after.value - 1u), arguments.author)
				}
			}

			ephemeralSubCommand(ClearCommandArgs::Between) {
				name = "between"
				description = "Clear messages between 2 message IDs"

				requirePermission(Permission.ManageMessages)

				check {
					anyGuild()
					requiredConfigs(ConfigOptions.MODERATION_ENABLED)
					hasPermission(Permission.ManageMessages)
					requireBotPermissions(Permission.ManageMessages)
					botHasChannelPerms(Permissions(Permission.ManageMessages))
				}

				action {
					clearMessages(
						null,
						Snowflake(arguments.before.value - 1u),
						Snowflake(arguments.after.value + 1u),
						arguments.author
					)
				}
			}
		}
	}

	/**
	 * An object containing the arguments for clear commands.
	 *
	 * @since 4.8.6
	 */
	@Suppress("MemberNameEqualsClassName") // Cope
	internal object ClearCommandArgs {
		/** Clear a specific count of messages. */
		internal class Count : Arguments() {
			/** The number of messages the user wants to remove. */
			val count by int {
				name = "messages"
				description = "Number of messages to delete"
			}

			/** The author of the messages that need clearing. */
			val author by optionalUser {
				name = "author"
				description = "The author of the messages to clear"
			}
		}

		/** Clear messages after a specific one. */
		internal class After : Arguments() {
			/** The ID of the message to start clearing from. */
			val after by snowflake {
				name = "after"
				description = "The ID of the message to clear after"
			}

			/** The number of messages the user wants to remove. */
			val count by optionalInt {
				name = "message-count"
				description = "The number of messages to clear"
			}

			/** The author of the messages that need clearing. */
			val author by optionalUser {
				name = "author"
				description = "The author of the messages to clear"
			}
		}

		/** Clear messages before a specific one. */
		internal class Before : Arguments() {
			/** The ID of the message to start clearing before. */
			val before by snowflake {
				name = "before"
				description = "The ID of the message to clear before"
			}

			/** The number of messages the user wants to remove. */
			val count by optionalInt {
				name = "message-count"
				description = "The number of messages to clear"
			}

			/** The author of the messages that need clearing. */
			val author by optionalUser {
				name = "author"
				description = "The author of the messages to clear"
			}
		}

		/** Clear messages between 2 specific ones. */
		internal class Between : Arguments() {
			/** The ID of the message to start clearing from. */
			val after by snowflake {
				name = "after"
				description = "The ID of the message to clear after"
			}

			/** The ID of the message to start clearing before. */
			val before by snowflake {
				name = "before"
				description = "The ID of the message to clear before"
			}

			/** The author of the messages that need clearing. */
			val author by optionalUser {
				name = "author"
				description = "The author of the messages to clear"
			}
		}
	}
}

/**
 * A function to use clear messages based on the count, before and after, as well as a user.
 *
 * @param count The number of messages to clear, or null
 * @param before The ID of the message to clear messages before
 * @param after The ID of the message to clear messages after
 * @param author The author of the messages that should be cleared
 * @author NoComment1105
 * @since 4.8.6
 */
private suspend fun EphemeralSlashCommandContext<*, *>.clearMessages(
	count: Int?,
	before: Snowflake?,
	after: Snowflake?,
	author: User?
) {
	val config = ModerationConfigCollection().getConfig(guild!!.id)!!
	val textChannel = channel.asChannelOfOrNull<GuildMessageChannel>()

	if (textChannel == null) {
		respond {
			content = "Could not get the channel to clear messages from."
		}
		return
	}

	if ((before != null && after != null) && (before < after)) {
		respond {
			content = "Before cannot be more recent than after!"
		}
		return
	}

	// Get the specified amount of messages into an array list of Snowflakes and delete them
	// Send help
	val messageFlow = if (before == null && after == null) {
		channel.withStrategy(EntitySupplyStrategy.rest)
			.getMessagesBefore(Snowflake.max, count?.let { min(it, 100) })
	} else if (after != null && before == null) {
		channel.withStrategy(EntitySupplyStrategy.rest).getMessagesAfter(after, count?.let { min(it, 100) })
	} else if (after == null && before != null) {
		channel.withStrategy(EntitySupplyStrategy.rest).getMessagesBefore(before, count?.let { min(it, 100) })
	} else if (after != null && before != null) {
		channel.withStrategy(EntitySupplyStrategy.rest).getMessagesBefore(before, count?.let { min(it, 100) })
			.filter { it.id > after }
	} else {
		flowOf()
	}

	val messages = if (author == null) {
		messageFlow.map { it.id }.toSet()
	} else {
		messageFlow.filter { it.author == author }.map { it.id }.toSet()
	}

	textChannel.bulkDelete(messages)

	respond {
		content = "Messages cleared."
	}

	if (config.publicLogging != null && config.publicLogging == true) {
		channel.createEmbed {
			title = "$count messages have been cleared."
			color = DISCORD_BLACK
		}
	}

	val actionLog =
		getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return
	actionLog.createEmbed {
		title = "${count ?: messages.size} messages have been cleared."
		description = "Action occurred in ${textChannel.mention}"
		footer {
			text = user.asUserOrNull()?.username ?: "Unable to get username"
			icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
		}
		color = DISCORD_BLACK
	}
}
