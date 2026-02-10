package org.hyacinthbots.lilybot.extensions.threads

import dev.kord.common.entity.*
import dev.kord.core.behavior.channel.asChannelOfOrNull
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.entity.channel.thread.TextChannelThread
import dev.kord.core.entity.channel.thread.ThreadChannel
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_BLACK
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.channel
import dev.kordex.core.commands.converters.impl.defaultingBoolean
import dev.kordex.core.commands.converters.impl.optionalChannel
import dev.kordex.core.commands.converters.impl.optionalRole
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.utils.delete
import dev.kordex.core.utils.respond
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import dev.kordex.modules.dev.unsafe.commands.slash.InitialSlashCommandResponse
import dev.kordex.modules.dev.unsafe.extensions.unsafeSubCommand
import dev.kordex.modules.pluralkit.api.PKMessage
import dev.kordex.modules.pluralkit.events.PKMessageCreateEvent
import dev.kordex.modules.pluralkit.events.ProxiedMessageCreateEvent
import dev.kordex.modules.pluralkit.events.UnProxiedMessageCreateEvent
import lilybot.i18n.Translations
import org.hyacinthbots.docgenerator.subCommandAdditionalDocumentation
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.ThreadsCollection
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.canPingRole
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import kotlin.time.Clock

class AutoThreading : Extension() {
    override val name = "auto-threading"

    override suspend fun setup() {
        ephemeralSlashCommand {
            name = Translations.Threads.AutoThreading.name
            description = Translations.Threads.AutoThreading.description

            @OptIn(UnsafeAPI::class)
            unsafeSubCommand(::AutoThreadingArgs) {
                name = Translations.Threads.AutoThreading.Enable.name
                description = Translations.Threads.AutoThreading.Enable.description

                initialResponse = InitialSlashCommandResponse.None

                requirePermission(Permission.ManageChannels)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageChannels)
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(Permissions(Permission.SendMessages))
                }

                action {
                    // Check if the auto-threading is disabled
                    if (AutoThreadingCollection().getSingleAutoThread(channel.id) != null) {
                        ackEphemeral()
                        respondEphemeral {
                            content = Translations.Threads.AutoThreading.alreadyOn.translate()
                        }
                        return@action
                    }

                    // Check if the role can be pinged
                    if (!canPingRole(arguments.role, guild!!.id, this@unsafeSubCommand.kord)) {
                        ackEphemeral()
                        respondEphemeral {
                            content = Translations.Threads.AutoThreading.Enable.noMention.translate()
                        }
                        return@action
                    }

                    var message: String? = null

                    if (arguments.message) {
                        val modalObj = MessageModal()

                        this@unsafeSubCommand.componentRegistry.register(modalObj)

                        event.interaction.modal(
                            modalObj.title.translate(),
                            modalObj.id
                        ) {
                            modalObj.applyToBuilder(this, getLocale())
                        }

                        modalObj.awaitCompletion { modalSubmitInteraction ->
                            interactionResponse = modalSubmitInteraction?.deferEphemeralMessageUpdate()
                        }

                        message = modalObj.msgInput.value!!
                    } else {
                        ackEphemeral()
                    }

                    respondEphemeral {
                        content = Translations.Threads.AutoThreading.Enable.enabled.translate()
                    }

                    // Add the channel to the database as auto-threaded
                    AutoThreadingCollection().setAutoThread(
                        AutoThreadingData(
                            guildId = guild!!.id,
                            channelId = arguments.targetChannel?.id ?: channel.id,
                            roleId = arguments.role?.id,
                            preventDuplicates = arguments.preventDuplicates,
                            archive = arguments.archive,
                            contentAwareNaming = arguments.contentAwareNaming,
                            mention = arguments.mention,
                            creationMessage = message,
                            addModsAndRole = arguments.addModsAndRole,
                            extraRoleIds = mutableListOf()
                        )
                    )

                    // Log the change
                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!) ?: return@action

                    utilityLog.createEmbed {
                        val obj = Translations.Threads.AutoThreading.Enable.Embed
                        title = obj.title.translate()
                        description = null
                        field {
                            name = obj.channel.translate()
                            value = arguments.targetChannel?.mention ?: channel.mention
                            inline = true
                        }
                        field {
                            name = obj.role.translate()
                            value = arguments.role?.mention ?: Translations.Basic.`null`.translate()
                            inline = true
                        }
                        field {
                            name = obj.preventDuplicates.translate()
                            value = arguments.preventDuplicates.toString()
                            inline = true
                        }
                        field {
                            name = obj.beginArchived.translate()
                            value = arguments.archive.toString()
                            inline = true
                        }
                        field {
                            name = obj.smartNaming.translate()
                            value = arguments.contentAwareNaming.toString()
                            inline = true
                        }
                        field {
                            name = obj.mention.translate()
                            value = arguments.mention.toString()
                            inline = true
                        }
                        field {
                            name = obj.initialMessage.translate()
                            value = if (message != null) "```$message```" else Translations.Basic.`null`.translate()
                            inline = message == null
                        }
                        footer {
                            text = user.asUser().username
                            icon = user.asUser().avatar?.cdnUrl?.toUrl()
                        }
                        timestamp = Clock.System.now()
                        color = DISCORD_BLACK
                    }
                }
            }

            ephemeralSubCommand(::AutoThreadingRemoveArgs) {
                name = Translations.Threads.AutoThreading.Disable.name
                description = Translations.Threads.AutoThreading.Disable.description

                requirePermission(Permission.ManageChannels)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageChannels)
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(Permissions(Permission.SendMessages))
                }

                action {
                    // Check if auto-threading is enabled
                    if (AutoThreadingCollection().getSingleAutoThread(channel.id) == null) {
                        respond {
                            content = Translations.Threads.AutoThreading.alreadyOff.translate()
                        }
                        return@action
                    }

                    // Remove the channel from the database as auto-threaded
                    AutoThreadingCollection().deleteAutoThread(arguments.targetChannel?.id ?: channel.id)

                    // Respond to the user
                    respond {
                        content = Translations.Threads.AutoThreading.Disable.disabled.translate()
                    }

                    // Log the change
                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!) ?: return@action

                    utilityLog.createEmbed {
                        title = Translations.Threads.AutoThreading.Disable.Embed.title.translate()
                        description = null

                        field {
                            name = Translations.Threads.AutoThreading.Disable.Embed.channel.translate()
                            value = arguments.targetChannel?.mention ?: channel.mention
                            inline = true
                        }
                        footer {
                            text = user.asUser().username
                            icon = user.asUser().avatar?.cdnUrl?.toUrl()
                        }
                        timestamp = Clock.System.now()
                        color = DISCORD_BLACK
                    }
                }
            }

            ephemeralSubCommand {
                name = Translations.Threads.AutoThreading.List.name
                description = Translations.Threads.AutoThreading.List.description

                check {
                    anyGuild()
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(Permissions(Permission.SendMessages))
                }

                action {
                    val autoThreads = AutoThreadingCollection().getAllAutoThreads(guild!!.id)
                    var responseContent: String? = null
                    autoThreads.forEach {
                        responseContent += "\n<#${it.channelId}>"
                        if (responseContent.length > 4080) {
                            responseContent += Translations.Threads.AutoThreading.List.trimmed.translate()
                            return@forEach
                        }
                    }

                    respond {
                        embed {
                            if (responseContent == null) {
                                title = Translations.Threads.AutoThreading.List.noChannels.translate()
                                description = Translations.Threads.AutoThreading.List.addNewChannels.translate()
                            } else {
                                title = Translations.Threads.AutoThreading.List.channelList.translate()
                                description = responseContent.replace("null", "")
                            }
                        }
                    }
                }
            }

            ephemeralSubCommand(::AutoThreadingViewArgs) {
                name = Translations.Threads.AutoThreading.View.name
                description = Translations.Threads.AutoThreading.View.description

                requirePermission(Permission.ManageChannels)

                check {
                    anyGuild()
                    requirePermission(Permission.ManageChannels)
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(Permissions(Permission.SendMessages))
                }

                action {
                    val autoThread = AutoThreadingCollection().getSingleAutoThread(arguments.channel.id)
                    if (autoThread == null) {
                        respond {
                            content = Translations.Threads.AutoThreading.View.noThreaded.translate()
                        }
                        return@action
                    }

                    respond {
                        val obj = Translations.Threads.AutoThreading.View.Embed
                        embed {
                            title = obj.title.translate()
                            description = obj.description.translate(arguments.channel.mention)
                            field {
                                name = obj.role.translate()
                                value = if (autoThread.roleId != null) {
                                    guild!!.getRoleOrNull(autoThread.roleId)?.mention ?: obj.unable.translate()
                                } else {
                                    Translations.Basic.none.translate()
                                }
                            }
                            if (autoThread.extraRoleIds.isNotEmpty()) {
                                var mentions = ""
                                autoThread.extraRoleIds.forEach {
                                    mentions += "${guild!!.getRoleOrNull(it)?.mention} "
                                }
                                field {
                                    name = obj.extraRoles.translate()
                                    value = mentions
                                }
                            }
                            field {
                                name = obj.preventDuplicates.translate()
                                value = autoThread.preventDuplicates.toString()
                            }
                            field {
                                name = obj.archiveOnStart.translate()
                                value = autoThread.archive.toString()
                            }
                            field {
                                name = obj.contentAwareNaming.translate()
                                value = autoThread.contentAwareNaming.toString()
                            }
                            field {
                                name = obj.mentionCreator.translate()
                                value = autoThread.mention.toString()
                            }
                            field {
                                name = obj.creationMessage.translate()
                                value = autoThread.creationMessage ?: Translations.Basic.default.translate()
                            }
                            field {
                                name = obj.addMods.translate()
                                value = autoThread.addModsAndRole.toString()
                            }
                        }
                    }
                }
            }

            ephemeralSubCommand(::ExtraRolesArgs) {
                name = Translations.Threads.AutoThreading.AddRoles.name
                description = Translations.Threads.AutoThreading.AddRoles.description

                subCommandAdditionalDocumentation {
                    extraInformation = Translations.Threads.AutoThreading.AddRoles.extraInfo.translate()
                }

                requirePermission(Permission.ManageChannels)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageChannels)
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(Permissions(Permission.SendMessages))
                }

                action {
                    val autoThread = AutoThreadingCollection().getSingleAutoThread(
                        arguments.targetChannel?.id ?: event.interaction.channelId
                    )
                    if (autoThread == null) {
                        respond {
                            content = Translations.Threads.AutoThreading.AddRoles.noThreaded.translate()
                        }
                        return@action
                    }

                    if (!canPingRole(arguments.role, guild!!.id, this@ephemeralSubCommand.kord)) {
                        respond {
                            content = Translations.Threads.AutoThreading.Enable.noMention.translate()
                        }
                        return@action
                    }

                    if (autoThread.extraRoleIds.contains(arguments.role!!.id)) {
                        respond {
                            content = Translations.Threads.AutoThreading.AddRoles.alreadyAdded.translate()
                        }
                        return@action
                    }

                    val updatedRoles = autoThread.extraRoleIds
                    updatedRoles.add(arguments.role!!.id)

                    AutoThreadingCollection().updateExtraRoles(
                        arguments.targetChannel?.id ?: event.interaction.channelId, updatedRoles
                    )

                    respond {
                        content = Translations.Threads.AutoThreading.AddRoles.added.translate(arguments.role!!.mention)
                    }
                }
            }

            ephemeralSubCommand(::ExtraRolesArgs) {
                name = Translations.Threads.AutoThreading.RemoveRoles.name
                description = Translations.Threads.AutoThreading.RemoveRoles.description

                subCommandAdditionalDocumentation {
                    extraInformation = Translations.Threads.AutoThreading.RemoveRoles.extraInfo.translate()
                }

                requirePermission(Permission.ManageChannels)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageChannels)
                    requireBotPermissions(Permission.SendMessages)
                    botHasChannelPerms(Permissions(Permission.SendMessages))
                }

                action {
                    val autoThread = AutoThreadingCollection().getSingleAutoThread(
                        arguments.targetChannel?.id ?: event.interaction.channelId
                    )
                    if (autoThread == null) {
                        respond {
                            content = Translations.Threads.AutoThreading.AddRoles.noThreaded.translate()
                        }
                        return@action
                    }

                    if (!autoThread.extraRoleIds.contains(arguments.role!!.id)) {
                        respond {
                            content = Translations.Threads.AutoThreading.RemoveRoles.notAdded.translate()
                        }
                        return@action
                    }

                    val updatedRoles = autoThread.extraRoleIds
                    updatedRoles.remove(arguments.role!!.id)

                    AutoThreadingCollection().updateExtraRoles(
                        arguments.targetChannel?.id ?: event.interaction.channelId, updatedRoles
                    )

                    respond {
                        content =
                            Translations.Threads.AutoThreading.RemoveRoles.removed.translate(arguments.role!!.mention)
                    }
                }
            }
        }

        event<ProxiedMessageCreateEvent> {
            check {
                anyGuild()
                failIf {
                    event.pkMessage.sender == kord.selfId ||
                        listOf(
                            MessageType.ChatInputCommand,
                            MessageType.ThreadCreated,
                            MessageType.ThreadStarterMessage
                        ).contains(event.message.type) ||
                        listOf(
                            ChannelType.GuildNews,
                            ChannelType.GuildVoice,
                            ChannelType.PublicGuildThread,
                            ChannelType.PublicNewsThread
                        ).contains(event.message.getChannelOrNull()?.type)
                }
            }

            action {
                onMessageSend(event, event.channel.id, event.getMessageOrNull(), event.pkMessage)
            }
        }

        event<UnProxiedMessageCreateEvent> {
            check {
                anyGuild()
                failIf {
                    event.message.author?.id == kord.selfId ||
                        listOf(
                            MessageType.ChatInputCommand,
                            MessageType.ThreadCreated,
                            MessageType.ThreadStarterMessage
                        ).contains(event.message.type) ||
                        listOf(
                            ChannelType.GuildNews,
                            ChannelType.GuildVoice,
                            ChannelType.PublicGuildThread,
                            ChannelType.PublicNewsThread
                        ).contains(event.message.getChannelOrNull()?.type)
                }
            }

            action {
                onMessageSend(event, event.channel.id, event.getMessageOrNull())
            }
        }
    }

    inner class AutoThreadingArgs : Arguments() {
        val targetChannel by optionalChannel {
            name = Translations.Threads.AutoThreading.Arguments.Channel.name
            description = Translations.Threads.AutoThreading.Arguments.TargetChannel.description
        }

        val role by optionalRole {
            name = Translations.Threads.AutoThreading.Arguments.Role.name
            description = Translations.Threads.AutoThreading.Arguments.Role.description
        }

        val addModsAndRole by defaultingBoolean {
            name = Translations.Threads.AutoThreading.Arguments.AddMods.name
            description = Translations.Threads.AutoThreading.Arguments.AddMods.description
            defaultValue = false
        }

        val preventDuplicates by defaultingBoolean {
            name = Translations.Threads.AutoThreading.Arguments.PreventDuplicates.name
            description = Translations.Threads.AutoThreading.Arguments.PreventDuplicates.description
            defaultValue = false
        }

        val archive by defaultingBoolean {
            name = Translations.Threads.AutoThreading.Arguments.Archive.name
            description = Translations.Threads.AutoThreading.Arguments.Archive.description
            defaultValue = false
        }

        val contentAwareNaming by defaultingBoolean {
            name = Translations.Threads.AutoThreading.Arguments.ContentAware.name
            description = Translations.Threads.AutoThreading.Arguments.ContentAware.description
            defaultValue = false
        }

        val mention by defaultingBoolean {
            name = Translations.Threads.AutoThreading.Arguments.Mention.name
            description = Translations.Threads.AutoThreading.Arguments.Mention.description
            defaultValue = false
        }

        val message by defaultingBoolean {
            name = Translations.Threads.AutoThreading.Arguments.Message.name
            description = Translations.Threads.AutoThreading.Arguments.Message.description
            defaultValue = false
        }
    }

    inner class AutoThreadingRemoveArgs : Arguments() {
        val targetChannel by optionalChannel {
            name = Translations.Threads.AutoThreading.Arguments.Channel.name
            description = Translations.Threads.AutoThreading.Arguments.TargetChannel.description
        }
    }

    inner class ExtraRolesArgs : Arguments() {
        val targetChannel by optionalChannel {
            name = Translations.Threads.AutoThreading.Arguments.Channel.name
            description = Translations.Threads.AutoThreading.Arguments.TargetChannel.description
        }

        val role by optionalRole {
            name = Translations.Threads.AutoThreading.Arguments.Role.name
            description = Translations.Threads.AutoThreading.Arguments.ExtraRole.description
        }
    }

    inner class AutoThreadingViewArgs : Arguments() {
        val channel by channel {
            name = Translations.Threads.AutoThreading.Arguments.Channel.name
            description = Translations.Threads.AutoThreading.Arguments.Channel.description
        }
    }

    inner class MessageModal : ModalForm() {
        override var title = Translations.Threads.AutoThreading.Modal.title

        val msgInput = paragraphText {
            label = Translations.Threads.AutoThreading.Modal.MsgInput.name
            placeholder = Translations.Threads.AutoThreading.Modal.MsgInput.placeholder
            required = true
        }
    }

    /**
     * A single function for both Proxied and Non-Proxied message to be turned into threads.
     *
     * @param event The event for the message creation
     * @param channelId The channel ID, used to enable quick cancelling of the task if invalid.
     * @param message The original message that wasn't proxied
     * @param proxiedMessage The proxied message, if the message was proxied
     * @since 4.6.0
     * @author NoComment1105
     */
    private suspend fun <T : PKMessageCreateEvent> onMessageSend(
        event: T,
        channelId: Snowflake,
        message: Message?,
        proxiedMessage: PKMessage? = null
    ) {
        val options = AutoThreadingCollection().getSingleAutoThread(channelId) ?: return

        val memberFromPk = if (proxiedMessage != null) event.getGuild().getMemberOrNull(proxiedMessage.sender) else null

        val channel: TextChannel = if (proxiedMessage != null) {
            // Check the real message member too, despite the pk message not being null, we may still be
            // able to use the original
            message?.channel?.asChannelOfOrNull()
                ?: event.getGuild().getChannelOfOrNull(proxiedMessage.channel) ?: return
        } else {
            message?.channel?.asChannelOfOrNull() ?: return
        }

        val authorId: Snowflake = message?.author?.id ?: proxiedMessage?.sender ?: return

        var threadName: String? = event.message.content.trim().split("\n").firstOrNull()?.take(75)?.replace(
            "(<a?)?:\\w+:(\\d{18,19}>)?".toRegex(RegexOption.IGNORE_CASE), ""
        )

        if (!options.contentAwareNaming || threadName.isNullOrEmpty()) {
            threadName = Translations.Threads.AutoThreading.threadFor.translate(
                message?.author?.asUser()?.username ?: proxiedMessage?.member?.name?.take(75)
            )
        }

        if (options.preventDuplicates) {
            var previousThreadExists = false
            var previousUserThread: ThreadChannel? = null
            val ownerThreads = ThreadsCollection().getOwnerThreads(authorId)

            ownerThreads.forEach {
                val thread = try {
                    event.guild?.getChannelOfOrNull<ThreadChannel>(it.threadId)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (thread == null) {
                    ThreadsCollection().removeThread(it.threadId)
                } else if (thread.parentId == channel.id && !thread.isArchived) {
                    previousThreadExists = true
                    previousUserThread = thread
                }
            }

            if (previousThreadExists) {
                val response = event.message.respond {
                    // There is a not-null call because the compiler knows it's not null if the boolean is true.
                    content = Translations.Threads.AutoThreading.existingThread.translate(previousUserThread!!.mention)
                }
                event.message.delete("User already has a thread")
                response.delete(10000L, false)
                return
            }
        }

        // See if there is already a thread created with from the message id to avoid errors when trying to make another
        var thread: TextChannelThread? = message?.let { event.getGuild().getChannelOfOrNull<TextChannelThread>(it.id) }

        /** Flag to decide confirm whether the thread exists already. */
        var existing = false

        // If there was no thread, proceed as normal
        if (thread == null) {
            thread = channel.startPublicThreadWithMessage(
                message?.id ?: proxiedMessage!!.channel,
                threadName
            ) {
                autoArchiveDuration = channel.data.defaultAutoArchiveDuration.value ?: ArchiveDuration.Day
            }
        } else {
            existing = true
        }

        ThreadsCollection().setThreadOwner(event.getGuild().id, thread.id, event.member!!.id, channel.id)

        // If the thread didn't exist prior to trying to make one, proceed with the mod-inviting
        if (!existing) {
            ModThreadInviting().handleThreadCreation(
                options,
                thread,
                message?.author ?: memberFromPk!!.asUser()
            )
        }
    }
}
