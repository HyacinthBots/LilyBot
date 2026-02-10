package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import dev.kord.core.event.interaction.GuildButtonInteractionCreateEvent
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_BLACK
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.checks.hasPermissions
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.EphemeralSlashCommandContext
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.converters.impl.*
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.components.ephemeralStringSelectMenu
import dev.kordex.core.components.linkButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.extensions.event
import dev.kordex.core.i18n.toKey
import dev.kordex.core.utils.getJumpUrl
import dev.kordex.core.utils.getTopRole
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.RoleMenuCollection
import org.hyacinthbots.lilybot.database.collections.RoleSubscriptionCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.HYACINTH_GITHUB
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.utilsLogger

/**
 * The class the holds the systems allowing for role menus to function.
 *
 * @since 3.4.0
 */
class RoleMenu : Extension() {
    override val name = "role-menu"

    override suspend fun setup() {
        /**
         * Role menu commands.
         * @author tempest15
         * @since 3.4.0
         */
        ephemeralSlashCommand {
            name = Translations.Utility.RoleMenu.name
            description = Translations.Utility.RoleMenu.description

            /**
             * The command to create a new role menu.
             */
            ephemeralSubCommand(::RoleMenuCreateArgs) {
                name = Translations.Utility.RoleMenu.Create.name
                description = Translations.Utility.RoleMenu.Create.description

                requirePermission(Permission.ManageRoles)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageRoles)
                    requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
                    botHasChannelPerms(
                        Permissions(Permission.SendMessages, Permission.EmbedLinks)
                    )
                }

                var menuMessage: Message?
                action {
                    val translations = Translations.Utility.RoleMenu.Create
                    val kord = this@ephemeralSlashCommand.kord

                    if (!botCanAssignRole(kord, arguments.initialRole)) return@action

                    menuMessage = channel.createMessage {
                        if (arguments.embed) {
                            embed {
                                description = arguments.content
                                color = arguments.color
                            }
                        } else {
                            content = arguments.content
                        }
                    }

                    // While we don't normally edit in components, in this case we need the message ID.
                    menuMessage.edit {
                        val components = components {
                            ephemeralButton {
                                label = translations.selectButton
                                style = ButtonStyle.Primary

                                id = "role-menu${menuMessage.id}"

                                action { }
                            }
                        }

                        components.removeAll()
                    }

                    RoleMenuCollection().setRoleMenu(
                        menuMessage.id,
                        channel.id,
                        guild!!.id,
                        mutableListOf(arguments.initialRole.id)
                    )

                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                        ?: return@action

                    utilityLog.createMessage {
                        embed {
                            title = translations.embedTitle.translate()
                            description =
                                translations.embedDesc.translate(arguments.initialRole.mention, channel.mention)

                            field {
                                name = translations.embedContent.translate()
                                value = "```${arguments.content}```"
                                inline = false
                            }
                            field {
                                name = translations.embedColor.translate()
                                value = arguments.color.toString()
                                inline = true
                            }
                            field {
                                name = translations.embedEmbed.translate()
                                value = arguments.embed.toString()
                                inline = true
                            }
                            footer {
                                text = translations.createdBy.translate(user.asUserOrNull()?.username)
                                icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                            }
                        }
                        components {
                            linkButton {
                                label = Translations.Utility.RoleMenu.jumpButton
                                url = menuMessage.getJumpUrl()
                            }
                        }
                    }

                    respond {
                        content = translations.response.translate()
                    }
                }
            }

            /**
             * The command to add a role to an existing role menu.
             */
            ephemeralSubCommand(::RoleMenuAddArgs) {
                name = Translations.Utility.RoleMenu.Add.name
                description = Translations.Utility.RoleMenu.Add.description

                requirePermission(Permission.ManageRoles)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageRoles)
                    requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
                    botHasChannelPerms(
                        Permissions(Permission.SendMessages, Permission.EmbedLinks)
                    )
                }

                action {
                    val kord = this@ephemeralSlashCommand.kord

                    val translations = Translations.Utility.RoleMenu.Add

                    if (!botCanAssignRole(kord, arguments.role)) return@action

                    val message = channel.getMessageOrNull(arguments.messageId)
                    if (!roleMenuExists(message, arguments.messageId)) return@action

                    val data = RoleMenuCollection().getRoleData(arguments.messageId)!!

                    if (arguments.role.id in data.roles) {
                        respond { content = translations.alreadyGot.translate() }
                        return@action
                    }

                    if (data.roles.size == 24) {
                        respond { content = translations.max24.translate() }
                        return@action
                    }

                    data.roles.add(arguments.role.id)
                    RoleMenuCollection().setRoleMenu(
                        data.messageId,
                        data.channelId,
                        data.guildId,
                        data.roles
                    )

                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                        ?: return@action
                    utilityLog.createMessage {
                        embed {
                            title = translations.embedTitle.translate()
                            description = translations.embedDesc.translate(arguments.role.mention, channel.mention)
                            footer {
                                text = translations.addedBy.translate(user.asUserOrNull()?.username)
                                icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                            }
                        }
                        components {
                            linkButton {
                                label = Translations.Utility.RoleMenu.jumpButton
                                url = message!!.getJumpUrl()
                            }
                        }
                    }

                    respond {
                        content = translations.response.translate(arguments.role.mention)
                    }
                }
            }

            /**
             * The command to remove a role from an existing role menu.
             */
            ephemeralSubCommand(::RoleMenuRemoveArgs) {
                name = Translations.Utility.RoleMenu.Remove.name
                description = Translations.Utility.RoleMenu.Remove.description

                requirePermission(Permission.ManageMessages)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageMessages)
                    requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
                    botHasChannelPerms(
                        Permissions(Permission.SendMessages, Permission.EmbedLinks)
                    )
                }

                action {
                    val menuMessage = channel.getMessageOrNull(arguments.messageId)
                    if (!roleMenuExists(menuMessage, arguments.messageId)) return@action

                    val translations = Translations.Utility.RoleMenu.Remove

                    val data = RoleMenuCollection().getRoleData(arguments.messageId)!!

                    if (arguments.role.id !in data.roles) {
                        respond { content = translations.cantRemove.translate() }
                        return@action
                    }

                    if (data.roles.size == 1) {
                        respond { content = translations.cantRemoveLast.translate() }
                        return@action
                    }

                    RoleMenuCollection().removeRoleFromMenu(menuMessage!!.id, arguments.role.id)

                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                        ?: return@action
                    utilityLog.createMessage {
                        embed {
                            title = translations.embedTitle.translate()
                            description = translations.embedDesc.translate(arguments.role.mention, channel.mention)
                            footer {
                                text = translations.removedBy.translate(user.asUserOrNull()?.username)
                                icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                            }
                        }
                        components {
                            linkButton {
                                label = Translations.Utility.RoleMenu.jumpButton
                                url = menuMessage.getJumpUrl()
                            }
                        }
                    }

                    respond {
                        content = translations.response.translate(arguments.role.mention)
                    }
                }
            }

            /**
             * A command that creates a new role menu specifically for selecting pronouns.
             */
            ephemeralSubCommand {
                name = Translations.Utility.RoleMenu.Pronouns.name
                description = Translations.Utility.RoleMenu.Pronouns.description

                requirePermission(Permission.ManageMessages)

                check {
                    anyGuild()
                    hasPermission(Permission.ManageMessages)
                    requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
                    botHasChannelPerms(
                        Permissions(Permission.SendMessages, Permission.EmbedLinks)
                    )
                }

                action {
                    val translations = Translations.Utility.RoleMenu.Pronouns
                    respond {
                        content = translations.response.translate()
                    }

                    val menuMessage = channel.createMessage {
                        content = translations.message.translate()
                    }

                    // While we don't normally edit in components, in this case we need the message ID.
                    menuMessage.edit {
                        val components = components {
                            ephemeralButton {
                                label = Translations.Utility.RoleMenu.Create.selectButton
                                style = ButtonStyle.Primary

                                this.id = "role-menu${menuMessage.id}"

                                action { }
                            }
                        }

                        components.removeAll()
                    }

                    val pronouns = listOf(
                        "he/him",
                        "she/her",
                        "they/them",
                        "it/its",
                        "no pronouns (use name)",
                        "any pronouns",
                        "ask for pronouns"
                    )

                    val roles = mutableListOf<Snowflake>()

                    for (pronoun in pronouns) {
                        val existingRole = guild!!.roles.firstOrNull { it.name == pronoun }
                        if (existingRole == null) {
                            val newRole = guild!!.createRole {
                                name = pronoun
                            }

                            roles.add(newRole.id)
                        } else {
                            utilsLogger.debug { "skipped creating new roles" }
                            roles.add(existingRole.id)
                        }
                    }

                    RoleMenuCollection().setRoleMenu(
                        menuMessage.id,
                        channel.id,
                        guild!!.id,
                        roles
                    )

                    val guildRoles = guild!!.roles
                        .filter { role -> role.id in roles.map { it }.toList().associateBy { it } }
                        .toList()
                        .associateBy { it.id }

                    guildRoles.forEach {
                        if (it.value.name == "she/her") event.kord.getSelf().asMemberOrNull(guild!!.id)?.addRole(it.key)
                        if (it.value.name == "it/its") event.kord.getSelf().asMemberOrNull(guild!!.id)?.addRole(it.key)
                    }

                    val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)
                        ?: return@action
                    utilityLog.createMessage {
                        embed {
                            title = translations.embedTitle.translate()
                            description = translations.embedDesc.translate(channel.mention)
                            footer {
                                text = translations.createdBy.translate(user.asUserOrNull()?.username)
                                icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                            }
                        }
                        components {
                            linkButton {
                                label = Translations.Utility.RoleMenu.jumpButton
                                url = menuMessage.getJumpUrl()
                            }
                        }
                    }
                }
            }
        }

        /**
         * The button event that allows the user to select roles.
         */
        event<GuildButtonInteractionCreateEvent> {
            check {
                anyGuild()
                failIfNot {
                    event.interaction.componentId.contains("role-menu")
                }
            }

            action Button@{
                val data = RoleMenuCollection().getRoleData(event.interaction.message.id)
                val translations = Translations.Utility.RoleMenu.Interaction

                if (data == null) {
                    event.interaction.respondEphemeral {
                        content = translations.broken.translate(HYACINTH_GITHUB)
                    }
                    return@Button
                }

                if (data.roles.isEmpty()) {
                    event.interaction.respondEphemeral {
                        content = translations.noRoles.translate(HYACINTH_GITHUB)
                    }
                    return@Button
                }

                val guild = kord.getGuildOrNull(data.guildId)
                if (guild == null) {
                    event.interaction.respondEphemeral {
                        content = translations.serverError.translate(HYACINTH_GITHUB)
                    }
                    return@Button
                }

                val roles = mutableListOf<Role>()
                data.roles.forEach {
                    val role = guild.getRoleOrNull(it)
                    if (role == null) {
                        RoleMenuCollection().removeRoleFromMenu(event.interaction.message.id, it)
                    } else {
                        roles.add(role)
                    }
                }

                if (roles.isEmpty()) {
                    event.interaction.respondEphemeral {
                        content = translations.noRoles.translate(HYACINTH_GITHUB)
                    }
                    return@Button
                }

                val guildRoles = guild.roles
                    .filter { role -> role.id in data.roles.map { it }.toList().associateBy { it } }
                    .toList()
                    .associateBy { it.id }
                val member = event.interaction.user.asMemberOrNull(guild.id)
                val userRoles = member.roleIds.filter { it in guildRoles.keys }

                event.interaction.respondEphemeral {
                    content = translations.menuMessage.translate()
                    components {
                        // TODO Update to ephemeralRoleSelectMenu
                        ephemeralStringSelectMenu {
                            placeholder = translations.placeholder
                            maximumChoices = roles.size
                            minimumChoices = 0

                            roles.forEach {
                                option(
                                    label = "@${it.name}".toKey(),
                                    value = it.id.toString()
                                ) {
                                    default = it.id in userRoles
                                }
                            }

                            action SelectMenu@{
                                val selectedRoles = event.interaction.values.toList().map { Snowflake(it) }
                                    .filter { it in guildRoles.keys }

                                if (event.interaction.values.isEmpty()) {
                                    member.edit {
                                        roles.forEach {
                                            member.removeRole(it.id)
                                        }
                                    }
                                    respond { content = translations.response.translate() }
                                    return@SelectMenu
                                }

                                val rolesToAdd = selectedRoles.filterNot { it in userRoles }
                                val rolesToRemove = userRoles.filterNot { it in selectedRoles }

                                if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) {
                                    respond { content = translations.noChanges.translate() }
                                    return@SelectMenu
                                }

                                member.edit {
                                    this@edit.roles = member.roleIds.toMutableSet()

                                    // toSet() to increase performance. Idea advised this.
                                    this@edit.roles!!.addAll(rolesToAdd.toSet())
                                    this@edit.roles!!.removeAll(rolesToRemove.toSet())
                                }
                                respond { content = translations.response.translate() }
                            }
                        }
                    }
                }
            }
        }

        ephemeralSlashCommand {
            name = Translations.Utility.RoleMenu.RoleSubscription.name
            description = Translations.Utility.RoleMenu.RoleSubscription.description

            ephemeralSubCommand {
                name = Translations.Utility.RoleMenu.RoleSubscription.Update.name
                description = Translations.Utility.RoleMenu.RoleSubscription.Update.description

                check {
                    anyGuild()
                }

                action {
                    val guild = guild ?: return@action
                    val translations = Translations.Utility.RoleMenu.RoleSubscription.Update
                    val data = RoleSubscriptionCollection().getSubscribableRoles(guild.id)

                    if (data == null) {
                        respond {
                            content = translations.noSubs.translate()
                        }
                        return@action
                    }

                    val subscribableRoles = mutableListOf<Role>()
                    data.subscribableRoles.forEach {
                        val role = guild.getRoleOrNull(it)
                        if (role == null) {
                            RoleSubscriptionCollection().removeSubscribableRole(guild.id, it)
                        } else {
                            subscribableRoles.add(role)
                        }
                    }

                    val guildRoles = guild.roles
                        .filter { role -> role.id in data.subscribableRoles.map { it }.toList().associateBy { it } }
                        .toList()
                        .associateBy { it.id }
                    val member = user.asMemberOrNull(guild.id)
                    val userRoles = member?.roleIds?.filter { it in guildRoles.keys }

                    respond {
                        content = translations.useMenu.translate()
                        components {
                            // TODO Update to ephemeralRoleSelectMenu
                            ephemeralStringSelectMenu {
                                placeholder = translations.placeholder
                                minimumChoices = 0
                                maximumChoices = subscribableRoles.size

                                subscribableRoles.forEach {
                                    option(
                                        label = "@${it.name}".toKey(),
                                        value = it.id.toString()
                                    ) {
                                        if (userRoles != null) {
                                            default = it.id in userRoles
                                        }
                                    }
                                }

                                action SelectMenu@{
                                    val selectedRoles = selected.map { Snowflake(it) }.toList()
                                        .filter { it in guildRoles.keys }

                                    if (selectedRoles.isEmpty()) {
                                        member?.edit {
                                            subscribableRoles.forEach {
                                                member.removeRole(it.id)
                                            }
                                        }
                                        respond { content = translations.adjusted.translate() }
                                        return@SelectMenu
                                    }

                                    val rolesToAdd = if (userRoles == null) {
                                        emptyList()
                                    } else {
                                        selectedRoles.filterNot { it in userRoles }
                                    }

                                    val rolesToRemove = userRoles?.filterNot { it in selectedRoles }

                                    if (rolesToAdd.isEmpty() && rolesToRemove?.isEmpty() == true) {
                                        respond {
                                            content = Translations.Utility.RoleMenu.Interaction.noChanges.translate()
                                        }
                                        return@SelectMenu
                                    }

                                    member?.edit {
                                        this@edit.roles = member.roleIds.toMutableSet()

                                        // toSet() to increase performance. Idea advised this.
                                        this@edit.roles!!.addAll(rolesToAdd.toSet())
                                        rolesToRemove?.toSet()?.let { this@edit.roles!!.removeAll(it) }
                                    }
                                    respond { content = translations.adjusted.translate() }
                                }
                            }
                        }
                    }
                }
            }

            ephemeralSubCommand(::RoleSubscriptionRoleArgs) {
                name = Translations.Utility.RoleMenu.RoleSubscription.Add.name
                description = Translations.Utility.RoleMenu.RoleSubscription.Add.description

                requirePermission(Permission.ManageRoles, Permission.ManageGuild)

                check {
                    anyGuild()
                    hasPermissions(Permissions(Permission.ManageRoles, Permission.ManageGuild))
                }

                action {
                    val guild = guild ?: return@action
                    var config = RoleSubscriptionCollection().getSubscribableRoles(guild.id)
                    val utilityConfig = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)
                    if (config == null) {
                        RoleSubscriptionCollection().createSubscribableRoleRecord(guild.id)
                    }

                    RoleSubscriptionCollection().addSubscribableRole(guild.id, arguments.role.id)
                    config = RoleSubscriptionCollection().getSubscribableRoles(guild.id)!!

                    val formattedRoleList = config.subscribableRoles.map { guild.getRoleOrNull(it)?.mention }

                    val translations = Translations.Utility.RoleMenu.RoleSubscription.Add

                    respond {
                        content = translations.response.translate(
                            arguments.role.mention,
                            formattedRoleList.joinToString("\n")
                        )
                    }

                    utilityConfig?.createEmbed {
                        title = translations.embedTitle.translate()
                        description = translations.embedDesc.translate(arguments.role.mention)
                        footer {
                            text = translations.addedBy.translate(user.asUserOrNull()?.username)
                            icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                        }
                    }
                }
            }

            ephemeralSubCommand(::RoleSubscriptionRoleArgs) {
                name = Translations.Utility.RoleMenu.RoleSubscription.Remove.name
                description = Translations.Utility.RoleMenu.RoleSubscription.Remove.description

                requirePermission(Permission.ManageRoles, Permission.ManageGuild)

                check {
                    anyGuild()
                    hasPermissions(Permissions(Permission.ManageRoles, Permission.ManageGuild))
                }

                action {
                    val guild = guild ?: return@action
                    var config = RoleSubscriptionCollection().getSubscribableRoles(guild.id)
                    val utilityConfig = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild)

                    val translations = Translations.Utility.RoleMenu.RoleSubscription.Remove

                    if (config == null) {
                        respond {
                            content = Translations.Utility.RoleMenu.RoleSubscription.Update.noSubs.translate()
                        }
                        return@action
                    }

                    if (!config.subscribableRoles.contains(arguments.role.id)) {
                        respond {
                            content = translations.notSubable.translate()
                        }
                        return@action
                    }

                    RoleSubscriptionCollection().removeSubscribableRole(guild.id, arguments.role.id)
                    config = RoleSubscriptionCollection().getSubscribableRoles(guild.id)

                    val formattedRoleList = config!!.subscribableRoles.map { guild.getRoleOrNull(it)?.mention }

                    respond {
                        content = translations.response.translate(
                            arguments.role.mention,
                            if (formattedRoleList.isNotEmpty()) {
                                formattedRoleList.joinToString("\n")
                            } else {
                                Translations.Basic.none.translate()
                            }
                        )
                    }

                    utilityConfig?.createEmbed {
                        title = translations.embedTitle.translate()
                        description = translations.embedDesc.translate(arguments.role.mention)
                        footer {
                            text = translations.removedBy.translate(user.asUserOrNull()?.username)
                            icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
                        }
                    }
                }
            }
        }
    }

    /**
     * This function checks if a given [inputMessage] with an associated [argumentMessageId] exists and is a role menu.
     *
     * @param inputMessage The message to check.
     * @param argumentMessageId The ID given as an argument for the command this function is called within.
     *
     * @return `true` if the message exists and is a role menu, `false` if not.
     * @author tempest15
     * @since 3.4.0
     */
    private suspend inline fun EphemeralSlashCommandContext<*, *>.roleMenuExists(
        inputMessage: Message?,
        argumentMessageId: Snowflake
    ): Boolean {
        if (inputMessage == null) {
            respond {
                content = Translations.Utility.RoleMenu.Check.cantFind.translate()
            }
            return false
        }

        val data = RoleMenuCollection().getRoleData(argumentMessageId)
        if (data == null) {
            respond {
                content = Translations.Utility.RoleMenu.Check.notRole.translate()
            }
            return false
        }

        return true
    }

    /**
     * This function checks if the bot can assign a given [role].
     *
     * @param role The role to check.
     * @param kord The kord instance to check.
     *
     * @return `true` if the proper permissions exist, `false` if not.
     * @author tempest15
     * @since 3.4.0
     */
    private suspend inline fun EphemeralSlashCommandContext<*, *>.botCanAssignRole(kord: Kord, role: Role): Boolean {
        val self = guild?.getMemberOrNull(kord.selfId)!!
        if (self.getTopRole()!! < role) {
            respond {
                content = Translations.Utility.RoleMenu.Check.higherRole.translate()
            }
            return false
        }
        return true
    }

    inner class RoleMenuCreateArgs : Arguments() {
        /** The initial role for a new role menu. */
        val initialRole by role {
            name = Translations.Utility.RoleMenu.Create.Arguments.Role.name
            description = Translations.Utility.RoleMenu.Create.Arguments.Role.description
        }

        /** The content of the embed or message to attach the role menu to. */
        val content by string {
            name = Translations.Utility.RoleMenu.Create.Arguments.Content.name
            description = Translations.Utility.RoleMenu.Create.Arguments.Content.description

            // Fix newline escape characters
            mutate {
                it.replace("\\n", "\n")
                    .replace("\n ", "\n")
            }
        }

        /** If the message the role menu is attached to should be an embed. */
        val embed by defaultingBoolean {
            name = Translations.Utility.RoleMenu.Create.Arguments.Embed.name
            description = Translations.Utility.RoleMenu.Create.Arguments.Embed.description
            defaultValue = true
        }

        /** If the message the role menu is attached to is an embed, the color that embed should be. */
        val color by defaultingColor {
            name = Translations.Utility.RoleMenu.Create.Arguments.Color.name
            description = Translations.Utility.RoleMenu.Create.Arguments.Color.description
            defaultValue = DISCORD_BLACK
        }
    }

    inner class RoleMenuAddArgs : Arguments() {
        /** The message ID of the role menu being edited. */
        val messageId by snowflake {
            name = Translations.Utility.RoleMenu.Add.Arguments.Id.name
            description = Translations.Utility.RoleMenu.Add.Arguments.Id.description
        }

        /** The role to add to the role menu. */
        val role by role {
            name = Translations.Utility.RoleMenu.Add.Arguments.Role.name
            description = Translations.Utility.RoleMenu.Add.Arguments.Role.description
        }
    }

    inner class RoleMenuRemoveArgs : Arguments() {
        /** The message ID of the role menu being edited. */
        val messageId by snowflake {
            name = Translations.Utility.RoleMenu.Add.Arguments.Id.name
            description = Translations.Utility.RoleMenu.Add.Arguments.Id.description
        }

        /** The role to remove from the role menu. */
        val role by role {
            name = Translations.Utility.RoleMenu.Add.Arguments.Role.name
            description = Translations.Utility.RoleMenu.Remove.Arguments.Role.description
        }
    }

    inner class RoleSubscriptionRoleArgs : Arguments() {
        val role by role {
            name = Translations.Utility.RoleMenu.Add.Arguments.Role.name
            description = Translations.Utility.RoleMenu.RoleSubscription.Arguments.Role.description
        }
    }
}
