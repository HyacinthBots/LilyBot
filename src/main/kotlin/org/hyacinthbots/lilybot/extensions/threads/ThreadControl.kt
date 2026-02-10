/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package org.hyacinthbots.lilybot.extensions.threads

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.channel.threads.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.channel.thread.ThreadUpdateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_FUCHSIA
import dev.kordex.core.checks.isInThread
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.EphemeralSlashCommandContext
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.defaultingBoolean
import dev.kordex.core.commands.converters.impl.member
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.hasPermission
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import kotlin.time.Clock

class ThreadControl : Extension() {

    override val name = "thread-control"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = Translations.Threads.ThreadControl.name
            description = Translations.Threads.ThreadControl.description

            ephemeralSubCommand(::ThreadRenameArgs) {
                name = Translations.Threads.ThreadControl.Rename.name
                description = Translations.Threads.ThreadControl.Rename.description

                check {
                    isInThread()
                    requireBotPermissions(Permission.ManageThreads)
                    botHasChannelPerms(Permissions(Permission.ManageThreads))
                }

                action {
                    val threadChannel = getAsThreadChannel()

                    val member = user.asMemberOrNull(guild!!.id) ?: return@action
                    if (!ownsThreadOrModerator(threadChannel!!, member)) return@action

                    threadChannel.edit {
                        name = arguments.newThreadName
                        reason = Translations.Threads.ThreadControl.Rename.renamedBy.translate(member.username)
                    }

                    respond {
                        content = Translations.Threads.ThreadControl.Rename.renamed.translate()
                    }
                }
            }

            ephemeralSubCommand(::ThreadArchiveArgs) {
                name = Translations.Threads.ThreadControl.Archive.name
                description = Translations.Threads.ThreadControl.Archive.description

                check {
                    isInThread()
                    requireBotPermissions(Permission.ManageThreads)
                    botHasChannelPerms(Permissions(Permission.ManageThreads))
                }

                action {
                    val threadChannel = getAsThreadChannel()

                    val member = user.asMemberOrNull(guild!!.id) ?: return@action
                    if (!ownsThreadOrModerator(threadChannel!!, member)) return@action

                    ThreadsCollection().getAllThreads().forEach {
                        if (it.threadId == threadChannel.id) {
                            val preventingArchiving = ThreadsCollection().getThread(it.threadId)?.preventArchiving
                            ThreadsCollection().removeThread(it.threadId)
                            ThreadsCollection().setThreadOwner(it.guildId, it.threadId, it.ownerId, it.parentChannelId)
                            if (preventingArchiving == true) {
                                guild!!.getChannelOfOrNull<GuildMessageChannel>(
                                    ModerationConfigCollection().getConfig(guild!!.id)!!.channel!!
                                )?.createEmbed {
                                    title =
                                        Translations.Threads.ThreadControl.Archive.PreventionDisabled.title.translate()
                                    description = Translations.Threads.ThreadControl.Archive.PreventionDisabled
                                        .description.translate()
                                    color = DISCORD_FUCHSIA

                                    field {
                                        name = Translations.Threads.ThreadControl.Archive.user.translate()
                                        value = user.asUserOrNull()?.username
                                            ?: Translations.Threads.ThreadControl.Archive.user.translate()
                                    }
                                    field {
                                        name = Translations.Threads.ThreadControl.Archive.thread.translate()
                                        value = "${threadChannel.mention} ${threadChannel.name}"
                                    }
                                }
                            }
                        }
                    }

                    if (threadChannel.isArchived) {
                        edit { content = Translations.Threads.ThreadControl.Archive.already.translate() }
                        return@action
                    }

                    threadChannel.edit {
                        this.archived = true
                        this.locked = arguments.lock && member.hasPermission(Permission.ModerateMembers)

                        reason = Translations.Threads.ThreadControl.Archive.archivedBy
                            .translate(user.asUserOrNull()?.username)
                    }

                    respond {
                        content = Translations.Threads.ThreadControl.Archive.response.translate()
                        if (arguments.lock && member.hasPermission(Permission.ModerateMembers)) {
                            content += Translations.Threads.ThreadControl.Archive.responseLock.translate()
                        }
                        content += "!"
                    }
                }
            }

            ephemeralSubCommand(::ThreadTransferArgs) {
                name = Translations.Threads.ThreadControl.Transfer.name
                description = Translations.Threads.ThreadControl.Transfer.description

                check {
                    isInThread()
                    requireBotPermissions(Permission.ManageThreads)
                    botHasChannelPerms(Permissions(Permission.ManageThreads))
                }

                action {
                    val threadChannel = getAsThreadChannel()
                    val member = user.asMemberOrNull(guild!!.id) ?: return@action

                    val oldOwnerId = ThreadsCollection().getThread(threadChannel!!.id)?.ownerId ?: threadChannel.ownerId
                    val oldOwner = guild!!.getMemberOrNull(oldOwnerId)

                    if (!ownsThreadOrModerator(threadChannel, member)) return@action

                    if (arguments.newOwner.id == oldOwnerId) {
                        respond { content = Translations.Threads.ThreadControl.Transfer.alreadyOwns.translate() }
                        return@action
                    }

                    if (arguments.newOwner.isBot) {
                        respond { content = Translations.Threads.ThreadControl.Transfer.cannotBot.translate() }
                        return@action
                    }

                    ThreadsCollection().setThreadOwner(
                        guild!!.id,
                        threadChannel.id,
                        arguments.newOwner.id,
                        threadChannel.parentId
                    )

                    respond { content = Translations.Threads.ThreadControl.Transfer.success.translate() }

                    var content = Translations.Threads.ThreadControl.Transfer.fromTo.translateNamed(
                        "old" to oldOwner?.mention,
                        "new" to arguments.newOwner.mention
                    )

                    if (member != oldOwner) {
                        content += Translations.Threads.ThreadControl.Transfer.transferredBy.translate(
                            member.mention
                        )
                    }

                    threadChannel.createMessage(content)

                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                        ?: return@action
                    utilityLog.createMessage {
                        val obj = Translations.Threads.ThreadControl.Transfer.Embed
                        embed {
                            title = obj.title.translate()
                            field {
                                name = obj.prevOwner.translate()
                                value =
                                    "${oldOwner?.mention ?: obj.cannotFind.translate()} ${oldOwner?.username ?: ""}"
                            }
                            field {
                                name = obj.newOwner.translate()
                                value = "${arguments.newOwner.mention} ${arguments.newOwner.username}"
                            }
                            if (member != oldOwner) {
                                footer {
                                    text = Translations.Threads.ThreadControl.Transfer.transferredBy
                                        .translate(member.mention)
                                    icon = member.avatar?.cdnUrl?.toUrl()
                                }
                            }
                            timestamp = Clock.System.now()
                            color = DISCORD_FUCHSIA
                        }
                    }
                }
            }

            ephemeralSubCommand {
                name = Translations.Threads.ThreadControl.PreventArchiving.name
                description = Translations.Threads.ThreadControl.PreventArchiving.description

                check {
                    isInThread()
                    requireBotPermissions(Permission.ManageThreads)
                    botHasChannelPerms(Permissions(Permission.ManageThreads))
                }

                action {
                    val threadChannel = getAsThreadChannel()
                    val member = user.asMemberOrNull(guild!!.id) ?: return@action
                    if (!ownsThreadOrModerator(threadChannel!!, member)) return@action

                    if (threadChannel.isArchived) {
                        threadChannel.edit {
                            archived = false
                            reason = "`/thread prevent-archiving` run by ${member.username}"
                        }
                    }

                    val threads = ThreadsCollection().getAllThreads()
                    var message: EphemeralMessageInteractionResponse? = null
                    var thread = threads.firstOrNull { it.threadId == threadChannel.id }
                    if (thread == null) {
                        ThreadsCollection().setThreadOwner(
                            threadChannel.guildId,
                            threadChannel.id,
                            threadChannel.ownerId,
                            threadChannel.parentId
                        )
                        thread = threads.firstOrNull { it.threadId == threadChannel.id }
                    }
                    if (thread?.preventArchiving == true) {
                        message = edit {
                            content =
                                Translations.Threads.ThreadControl.PreventArchiving.alreadyPreventedWarning.translate()
                        }.edit {
                            components {
                                ephemeralButton {
                                    label = Translations.Basic.yes
                                    style = ButtonStyle.Primary

                                    action button@{
                                        ThreadsCollection().setThreadOwner(
                                            thread.guildId,
                                            thread.threadId,
                                            thread.ownerId,
                                            threadChannel.parentId
                                        )
                                        edit {
                                            content =
                                                Translations.Threads.ThreadControl.PreventArchiving.noLongerPrevented
                                                    .translate()
                                        }
                                        val utilityLog = getLoggingChannelWithPerms(
                                            ConfigOptions.UTILITY_LOG,
                                            this.getGuild()!!
                                        ) ?: return@button
                                        utilityLog.createMessage {
                                            embed {
                                                title = Translations.Threads.ThreadControl.Archive.PreventionDisabled
                                                    .title.translate()
                                                color = DISCORD_FUCHSIA

                                                field {
                                                    name = Translations.Threads.ThreadControl.Archive.user.translate()
                                                    value = user.asUserOrNull()?.username
                                                        ?: Translations.Basic.UnableTo.tag.translate()
                                                }
                                                field {
                                                    name = "Thread"
                                                    value = threadChannel.mention
                                                }
                                            }
                                        }
                                        message!!.edit { components { removeAll() } }
                                    }
                                }
                                ephemeralButton {
                                    label = Translations.Basic.no
                                    style = ButtonStyle.Secondary

                                    action {
                                        edit {
                                            content =
                                                Translations.Threads.ThreadControl.PreventArchiving.stillPrevented
                                                    .translate()
                                        }
                                        message!!.edit { components { removeAll() } }
                                    }
                                }
                            }
                        }
                        return@action
                    } else if (thread?.preventArchiving == false) {
                        ThreadsCollection().setThreadOwner(
                            thread.guildId,
                            thread.threadId,
                            thread.ownerId,
                            threadChannel.parentId
                        )
                        try {
                            val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                                ?: return@action
                            utilityLog.createMessage {
                                embed {
                                    title = Translations.Threads.ThreadControl.PreventArchiving.Embed.title.translate()
                                    color = DISCORD_FUCHSIA

                                    field {
                                        name = Translations.Threads.ThreadControl.Archive.user.translate()
                                        value =
                                            user.asUserOrNull()?.username ?: Translations.Basic.UnableTo.tag.translate()
                                    }
                                    field {
                                        name = Translations.Threads.ThreadControl.Archive.thread.translate()
                                        value = threadChannel.mention
                                    }
                                }
                            }
                            edit {
                                content = Translations.Threads.ThreadControl.PreventArchiving.nowPrevented.translate()
                            }
                        } catch (_: EntityNotFoundException) {
                            edit {
                                content =
                                    Translations.Threads.ThreadControl.PreventArchiving.nowPreventedNoLog.translate()
                            }
                        }
                    }
                }
            }
        }

        event<ThreadUpdateEvent> {
            action {
                val channel = event.channel
                val ownedThread = ThreadsCollection().getThread(channel.id)

                if (channel.isArchived && ownedThread != null && ownedThread.preventArchiving) {
                    channel.edit {
                        archived = false
                        reason = "Preventing thread from being archived."
                    }
                }
            }
        }
    }

    /**
     * Gets the event channel as a thread channel, and responds appropriately if it cannot be gotten.
     *
     * @return The channel as a [ThreadChannel]
     *
     * @author NoComment1105
     * @since 4.8.0
     */
    private suspend inline fun EphemeralSlashCommandContext<*, *>.getAsThreadChannel(): ThreadChannel? {
        val threadChannel = channel.asChannelOfOrNull<ThreadChannel>()
        if (threadChannel == null) {
            respond {
                content = Translations.Threads.ThreadControl.fetchIssue.translate()
            }
            return null
        }

        return threadChannel
    }

    inner class ThreadRenameArgs : Arguments() {
        /** The new name for the thread. */
        val newThreadName by string {
            name = Translations.Threads.ThreadControl.Arguments.Rename.NewName.name
            description = Translations.Threads.ThreadControl.Arguments.Rename.NewName.description
        }
    }

    inner class ThreadArchiveArgs : Arguments() {
        /** Whether to lock the thread or not. */
        val lock by defaultingBoolean {
            name = Translations.Threads.ThreadControl.Arguments.Archive.Lock.name
            description = Translations.Threads.ThreadControl.Arguments.Archive.Lock.description
            defaultValue = false
        }
    }

    inner class ThreadTransferArgs : Arguments() {
        /** The new thread owner. */
        val newOwner by member {
            name = Translations.Threads.ThreadControl.Arguments.Transfer.NewOwner.name
            description = Translations.Threads.ThreadControl.Arguments.Transfer.NewOwner.name
        }
    }

    /**
     * Run a check to see if the provided [Member] owns this [ThreadChannel].
     *
     * @param inputThread The thread being checked
     * @param inputMember The Member to check
     * @return [Boolean]. whether the [inputMember] owns the [inputThread]
     * @author tempest15
     * @since 3.2.0
     */
    private suspend fun EphemeralSlashCommandContext<*, *>.ownsThreadOrModerator(
        inputThread: ThreadChannel,
        inputMember: Member
    ): Boolean {
        val databaseThreadOwner = ThreadsCollection().getThread(inputThread.id)?.ownerId

        if (inputMember.hasPermission(Permission.ModerateMembers) || databaseThreadOwner == inputMember.id) {
            return true
        }

        respond { content = Translations.Threads.ThreadControl.notYours.translate() }
        return false
    }
}
