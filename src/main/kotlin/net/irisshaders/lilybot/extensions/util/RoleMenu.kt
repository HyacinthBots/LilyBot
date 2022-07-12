package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.getTopRole
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.createRole
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.Message
import dev.kord.core.entity.Role
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.ButtonInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.RoleMenuCollection
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.botHasChannelPerms
import net.irisshaders.lilybot.utils.configPresent

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
			name = "role-menu"
			description = "The parent command for managing role menus."

			/**
			 * The command to create a new role menu.
			 */
			ephemeralSubCommand(::RoleMenuCreateArgs) {
				name = "create"
				description = "Create a new role menu in this channel. A channel can have any number of role menus."

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ManageRoles)
					requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				var menuMessage: Message?
				action {
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
					menuMessage!!.edit {
						val components = components {
							ephemeralButton {
								label = "Select roles"
								style = ButtonStyle.Primary

								id = "role-menu${menuMessage!!.id}"

								action { }
							}
						}

						components.removeAll()
					}

					RoleMenuCollection().setRoleMenu(
						menuMessage!!.id,
						channel.id,
						guild!!.id,
						mutableListOf(arguments.initialRole.id)
					)

					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

					actionLog.createMessage {
						embed {
							title = "Role Menu Created"
							description = "A role menu for the ${arguments.initialRole.mention} role was created in " +
									"${channel.mention}."

							field {
								name = "Content:"
								value = "```${arguments.content}```"
								inline = false
							}
							field {
								name = "Color:"
								value = arguments.color.toString()
								inline = true
							}
							field {
								name = "Embed:"
								value = arguments.embed.toString()
								inline = true
							}
						}
						components {
							linkButton {
								label = "Jump to role menu"
								url = menuMessage!!.getJumpUrl()
							}
						}
					}

					respond {
						content = "Role menu created. You can add more roles using the `/role-menu add` command."
					}
				}
			}

			/**
			 * The command to add a role to an existing role menu.
			 */
			ephemeralSubCommand(::RoleMenuAddArgs) {
				name = "add"
				description = "Add a role to the existing role menu in this channel."

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ManageRoles)
					requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				action {
					val kord = this@ephemeralSlashCommand.kord

					if (!botCanAssignRole(kord, arguments.role)) return@action

					val message = channel.getMessageOrNull(arguments.messageId)
					if (!roleMenuExists(message, arguments.messageId)) return@action

					val data = RoleMenuCollection().getRoleData(arguments.messageId)!!

					if (arguments.role.id in data.roles) {
						respond {
							content = "This menu already contains that role."
						}
						return@action
					}

					if (data.roles.size == 24) {
						respond {
							content = "You can't have more than 24 roles in a role menu. This is a Discord limitation."
						}
						return@action
					}

					data.roles.add(arguments.role.id)
					RoleMenuCollection().setRoleMenu(
						data.messageId,
						data.channelId,
						data.guildId,
						data.roles
					)

					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

					actionLog.createMessage {
						embed {
							title = "Role Added to Role Menu"
							description = "The ${arguments.role.mention} role was added to a role menu in " +
									"${channel.mention}."
						}
						components {
							linkButton {
								label = "Jump to role menu"
								url = message!!.getJumpUrl()
							}
						}
					}

					respond {
						content = "Added the ${arguments.role.mention} role to the specified role menu."
					}
				}
			}

			/**
			 * The command to remove a role from an existing role menu.
			 */
			ephemeralSubCommand(::RoleMenuRemoveArgs) {
				name = "remove"
				description = "Remove a role from the existing role menu in this channel."

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ManageMessages)
					requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				action {
					val menuMessage = channel.getMessageOrNull(arguments.messageId)
					if (!roleMenuExists(menuMessage, arguments.messageId)) return@action

					val data = RoleMenuCollection().getRoleData(arguments.messageId)!!

					if (arguments.role.id !in data.roles) {
						respond {
							content = "You can't remove a role from a menu it's not in."
						}
						return@action
					}

					if (data.roles.size == 1) {
						respond {
							content = "You can't remove the last role from a role menu."
						}
						return@action
					}

					RoleMenuCollection().removeRoleFromMenu(menuMessage!!.id, arguments.role.id)
					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

					actionLog.createMessage {
						embed {
							title = "Role Removed from Role Menu"
							description = "The ${arguments.role.mention} role was removed from a role menu in " +
									"${channel.mention}."
						}
						components {
							linkButton {
								label = "Jump to role menu"
								url = menuMessage.getJumpUrl()
							}
						}
					}

					respond {
						content = "Removed the ${arguments.role.mention} role from the specified role menu."
					}
				}
			}

			/**
			 * A command that creates a new role menu specifically for selecting pronouns.
			 */
			ephemeralSubCommand {
				name = "pronouns"
				description = "Create a pronoun selection role menu and the roles to go with it."

				check {
					anyGuild()
					configPresent(ConfigType.MODERATION)
					hasPermission(Permission.ManageMessages)
					requireBotPermissions(Permission.SendMessages, Permission.ManageRoles)
					botHasChannelPerms(
						Permissions(Permission.SendMessages, Permission.EmbedLinks)
					)
				}

				action {
					respond {
						content = "Pronoun role menu created."
					}

					val menuMessage = channel.createMessage {
						content = "Select pronoun roles from the menu below!"
					}

					// While we don't normally edit in components, in this case we need the message ID.
					menuMessage.edit {
						val components = components {
							ephemeralButton {
								label = "Select roles"
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
							println("skipped creating new roles")
							roles.add(existingRole.id)
						}
					}

					RoleMenuCollection().setRoleMenu(
						menuMessage.id,
						channel.id,
						guild!!.id,
						roles
					)

					val config = ModerationConfigCollection().getConfig(guild!!.id)!!
					val actionLog = guild!!.getChannelOf<GuildMessageChannel>(config.channel)

					actionLog.createMessage {
						embed {
							title = "Pronoun Role Menu Created"
							description = "A pronoun role menu was created in ${channel.mention}."
						}
						components {
							linkButton {
								label = "Jump to role menu"
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
		event<ButtonInteractionCreateEvent> {
			check {
				anyGuild()
				failIfNot {
					event.interaction.componentId.contains("role-menu")
				}
			}

			action {
				val data = RoleMenuCollection().getRoleData(event.interaction.message.id)

				if (data == null) {
					event.interaction.respondEphemeral {
						content = "This role menu seems to be broken, please ask staff to recreate it. " +
								"If this isn't a role menu, or if the issue persists, open a report at " +
								"<https://github.com/IrisShaders/LilyBot/issues>"
					}
					return@action
				}

				if (data.roles.isEmpty()) {
					event.interaction.respondEphemeral {
						content = "Could not find any roles associated with this menu. Please ask staff to add some " +
								"If this isn't a role menu, or if the issue persists, open a report at " +
								"<https://github.com/IrisShaders/LilyBot/issues>"
					}
					return@action
				}

				val guild = kord.getGuild(data.guildId)!!

				val roles = mutableListOf<Role>()
				data.roles.forEach {
					val role = guild.getRoleOrNull(it)
					if (role == null) {
						RoleMenuCollection().removeRoleFromMenu(event.interaction.message.id, it)
					} else {
						roles.add(role)
					}
				}

				val guildRoles = guild.roles
					.filter { role -> role.id in data.roles.map { it }.toList().associateBy { it } }
					.toList()
					.associateBy { it.id }
				val member = event.interaction.user.asMember(guild.id)
				val userRoles = member.roleIds.filter { it in guildRoles.keys }

				event.interaction.respondEphemeral {
					content = "Use the menu below to select roles."
					components {
						ephemeralSelectMenu {
							placeholder = "Select roles..."
							maximumChoices = roles.size
							minimumChoices = 0

							roles.forEach {
								option(
									label = "@${it.name}",
									value = it.id.toString()
								) {
									default = it.id in userRoles
								}
							}

							action {
								val selectedRoles = event.interaction.values.toList().map { Snowflake(it) }.filter { it in guildRoles.keys }

								if (event.interaction.values.isEmpty()) {
									member.edit {
										roles.forEach {
											member.removeRole(it.id)
										}
									}
									respond { content = "Your roles have been adjusted" }
									return@action
								}

								val rolesToAdd = selectedRoles.filterNot { it in userRoles }
								val rolesToRemove = userRoles.filterNot { it in selectedRoles }

								if (rolesToAdd.isEmpty() && rolesToRemove.isEmpty()) {
									respond {
										content = "You didn't select any different roles, so no changes were made."
									}
									return@action
								}

								member.edit {
									this@edit.roles = member.roleIds.toMutableSet()

									// toSet() to increase performance. Idea advised this.
									this@edit.roles!!.addAll(rolesToAdd.toSet())
									this@edit.roles!!.removeAll(rolesToRemove.toSet())
								}
								respond { content = "Your roles have been adjusted." }
							}
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
	private suspend inline fun EphemeralSlashCommandContext<*>.roleMenuExists(
		inputMessage: Message?,
		argumentMessageId: Snowflake
	): Boolean {
		if (inputMessage == null) {
			respond {
				content = "I couldn't find that message in this channel. Make sure it exists."
			}
			return false
		}

		val data = RoleMenuCollection().getRoleData(argumentMessageId)
		if (data == null) {
			respond {
				content = "That message doesn't seem to be a role menu."
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
	private suspend inline fun EphemeralSlashCommandContext<*>.botCanAssignRole(kord: Kord, role: Role): Boolean {
		val self = guild?.getMember(kord.selfId)!!
		if (self.getTopRole()!! < role) {
			respond {
				content = "The selected role is higher than me in the role hierarchy. " +
						"Please move it and try again."
			}
			return false
		}
		return true
	}

	inner class RoleMenuCreateArgs : Arguments() {
		/** The initial role for a new role menu. */
		val initialRole by role {
			name = "role"
			description = "The first role to start the menu with. Add more via `/role-menu add`"
		}

		/** The content of the embed or message to attach the role menu to. */
		val content by string {
			name = "content"
			description = "The content of the embed or message."

			// Fix newline escape characters
			mutate {
				it.replace("\\n", "\n")
					.replace("\n ", "\n")
			}
		}

		/** If the message the role menu is attached to should be an embed. */
		val embed by defaultingBoolean {
			name = "embed"
			description = "If the message containing the role menu should be sent as an embed."
			defaultValue = true
		}

		/** If the message the role menu is attached to is an embed, the color that embed should be. */
		val color by defaultingColor {
			name = "color"
			description = "The color for the message to be. Embed only."
			defaultValue = DISCORD_BLACK
		}
	}

	inner class RoleMenuAddArgs : Arguments() {
		/** The message ID of the role menu being edited. */
		val messageId by snowflake {
			name = "menuId"
			description = "The message ID of the role menu you'd like to edit."
		}

		/** The role to add to the role menu. */
		val role by role {
			name = "role"
			description = "The role you'd like to add to the selected role menu."
		}
	}

	inner class RoleMenuRemoveArgs : Arguments() {
		/** The message ID of the role menu being edited. */
		val messageId by snowflake {
			name = "menuId"
			description = "The message ID of the menu you'd like to edit."
		}

		/** The role to remove from the role menu. */
		val role by role {
			name = "role"
			description = "The role you'd like to remove from the selected role menu."
		}
	}
}
