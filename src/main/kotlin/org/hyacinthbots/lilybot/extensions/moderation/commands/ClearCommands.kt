package org.hyacinthbots.lilybot.extensions.moderation.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.supplier.EntitySupplyStrategy
import dev.kordex.core.DISCORD_BLACK
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.EphemeralSlashCommandContext
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.int
import dev.kordex.core.commands.converters.impl.optionalInt
import dev.kordex.core.commands.converters.impl.optionalUser
import dev.kordex.core.commands.converters.impl.snowflake
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toSet
import lilybot.i18n.Translations
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
            name = Translations.Moderation.ClearCommands.Clear.name
            description = Translations.Moderation.ClearCommands.Clear.description

            ephemeralSubCommand(ClearCommandArgs::Count) {
                name = Translations.Moderation.ClearCommands.Clear.Count.name
                description = Translations.Moderation.ClearCommands.Clear.Count.description

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
                name = Translations.Moderation.ClearCommands.Clear.Before.name
                description = Translations.Moderation.ClearCommands.Clear.Before.description

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
                name = Translations.Moderation.ClearCommands.Clear.After.name
                description = Translations.Moderation.ClearCommands.Clear.After.description

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
                name = Translations.Moderation.ClearCommands.Clear.Between.name
                description = Translations.Moderation.ClearCommands.Clear.Between.description

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
                name = Translations.Moderation.ClearCommands.Arguments.Count.name
                description = Translations.Moderation.ClearCommands.Arguments.Count.description
            }

            /** The author of the messages that need clearing. */
            val author by optionalUser {
                name = Translations.Moderation.ClearCommands.Arguments.Author.name
                description = Translations.Moderation.ClearCommands.Arguments.Author.description
            }
        }

        /** Clear messages after a specific one. */
        internal class After : Arguments() {
            /** The ID of the message to start clearing from. */
            val after by snowflake {
                name = Translations.Moderation.ClearCommands.Clear.After.Arguments.After.name
                description = Translations.Moderation.ClearCommands.Clear.After.Arguments.After.description
            }

            /** The number of messages the user wants to remove. */
            val count by optionalInt {
                name = Translations.Moderation.ClearCommands.Arguments.Count.name
                description = Translations.Moderation.ClearCommands.Arguments.Count.description
            }

            /** The author of the messages that need clearing. */
            val author by optionalUser {
                name = Translations.Moderation.ClearCommands.Arguments.Author.name
                description = Translations.Moderation.ClearCommands.Arguments.Author.description
            }
        }

        /** Clear messages before a specific one. */
        internal class Before : Arguments() {
            /** The ID of the message to start clearing before. */
            val before by snowflake {
                name = Translations.Moderation.ClearCommands.Clear.Before.Arguments.Before.name
                description = Translations.Moderation.ClearCommands.Clear.Before.Arguments.Before.description
            }

            /** The number of messages the user wants to remove. */
            val count by optionalInt {
                name = Translations.Moderation.ClearCommands.Arguments.Count.name
                description = Translations.Moderation.ClearCommands.Arguments.Count.description
            }

            /** The author of the messages that need clearing. */
            val author by optionalUser {
                name = Translations.Moderation.ClearCommands.Arguments.Author.name
                description = Translations.Moderation.ClearCommands.Arguments.Author.description
            }
        }

        /** Clear messages between 2 specific ones. */
        internal class Between : Arguments() {
            /** The ID of the message to start clearing from. */
            val after by snowflake {
                name = Translations.Moderation.ClearCommands.Clear.After.Arguments.After.name
                description = Translations.Moderation.ClearCommands.Clear.After.Arguments.After.description
            }

            /** The ID of the message to start clearing before. */
            val before by snowflake {
                name = Translations.Moderation.ClearCommands.Clear.Before.Arguments.Before.name
                description = Translations.Moderation.ClearCommands.Clear.Before.Arguments.Before.description
            }

            /** The author of the messages that need clearing. */
            val author by optionalUser {
                name = Translations.Moderation.ClearCommands.Arguments.Author.name
                description = Translations.Moderation.ClearCommands.Arguments.Author.description
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
            content = Translations.Moderation.ClearCommands.Error.noChannel.translate()
        }
        return
    }

    if ((before != null && after != null) && (before < after)) {
        respond {
            content = Translations.Moderation.ClearCommands.Error.beforeAfter.translate()
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
        content = Translations.Moderation.ClearCommands.cleared.translate()
    }

    if (config.publicLogging != null && config.publicLogging == true) {
        channel.createEmbed {
            title = Translations.Moderation.ClearCommands.numberCleared.translate(count)
            color = DISCORD_BLACK
        }
    }

    val actionLog =
        getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return
    actionLog.createEmbed {
        title = Translations.Moderation.ClearCommands.numberCleared.translate(count ?: messages.size)
        description = Translations.Moderation.ClearCommands.occurredIn.translate()
        footer {
            text = user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
            icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
        }
        color = DISCORD_BLACK
    }
}
