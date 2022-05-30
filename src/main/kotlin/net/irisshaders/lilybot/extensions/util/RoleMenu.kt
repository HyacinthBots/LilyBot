package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalEmoji
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import net.irisshaders.lilybot.utils.configPresent

class RoleMenu : Extension() {
	override val name = "role-menu"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "role-menu"
			description = "The parent command for managing role menus."

			ephemeralSubCommand(::RoleMenuCreateArgs) {
				name = "create"
				description = "Create a new role menu in this channel."

				check {
					anyGuild()
					hasPermission(Permission.ManageMessages)
					configPresent()
				}

				action {
					// todo check if there's already a role menu in this channel

					channel.createMessage {
						if (arguments.embed) {
							embed {
								description = arguments.content
								color = arguments.color
							}
						} else {
							content = arguments.content
						}

						components {
							ephemeralButton {
								label = "Select roles"
								style = ButtonStyle.Primary

								action {
									respond {
										// check if there are actually roles associated
										content = "There wil be a menu here."
									}
								}
							}
						}
					}

					respond {
						content = "Role menu created. Be sure to add roles to it using the `/role-menu add` command."
					}
				}
			}

			ephemeralSubCommand(::RoleMenuAddArgs) {
				name = "add"
				description = "Add a role to the existing role menu in this channel."

				check {
					anyGuild()
					hasPermission(Permission.ManageMessages)
					configPresent()
				}

				action {
					respond {
						content = "Edited role menu."
					}
				}
			}

			ephemeralSubCommand(::RoleMenuRemoveArgs) {
				name = "remove"
				description = "Remove a role from the existing role menu in this channel."

				check {
					anyGuild()
					hasPermission(Permission.ManageMessages)
					configPresent()
				}

				action {
					respond {
						content = "Edited role menu."
					}
				}
			}
		}
	}

	inner class RoleMenuCreateArgs : Arguments() {
		val content by string {
			name = "content"
			description = "The content of the embed or message."
		}
		val embed by boolean {
			name = "embed"
			description = "If the message containing the role menu should be sent as an embed."
		}
		val color by defaultingColor {
			name = "color"
			description = "The color for the message to be. Embed only."
			defaultValue = DISCORD_BLURPLE
		}
	}

	inner class RoleMenuAddArgs : Arguments() {
		val role by role {
			name = "role"
			description = "The role you'd like to add or remove from this channel's role menu."
		}
		val emoji by optionalEmoji {
			name = "emoji"
			description = "The emoji, if any, that should be associated with the selected role."
		}
	}

	inner class RoleMenuRemoveArgs : Arguments() {
		val role by role {
			name = "role"
			description = "The role you'd like to add or remove from this channel's role menu."
		}
	}
}
