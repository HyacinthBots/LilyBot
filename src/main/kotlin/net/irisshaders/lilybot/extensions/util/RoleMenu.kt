package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLURPLE
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.Permission
import net.irisshaders.lilybot.utils.configPresent

class RoleMenu : Extension() {
	override val name = "role-menu"

	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "role-menu"
			description = "The parent command for managing role menus."

			ephemeralSubCommand(::RoleMenuCreateArgs) {
				name = "create"
				description = "Create a new role menu in this channel. To edit roles, use the edit sub command."

				check {
					anyGuild()
					hasPermission(Permission.ManageMessages)
					configPresent()
				}

				action {
					respond {
						content = "Create role menu"
					}
				}
			}

			ephemeralSubCommand(::RoleMenuEditArgs) {
				name = "edit"
				description = "Edit the existing role menu in this channel. " +
						"To create a new role menu, use the create sub command."

				check {
					anyGuild()
					hasPermission(Permission.ManageMessages)
					configPresent()
				}

				action {
					respond {
						content = "Edit role menu"
					}
				}
			}
		}
	}

	inner class RoleMenuCreateArgs : Arguments() {
		val content by defaultingString {
			name = "content"
			description = "The content of the embed or message."
			defaultValue = " " // todo check that this doesn't error
		}
		val embed by optionalBoolean {
			name = "embed"
			description = "If the message containing the role menu should be sent as an embed."
		}
		val color by defaultingColor {
			name = "color"
			description = "The color for the message to be. Embed only."
			defaultValue = DISCORD_BLURPLE
		}
	}

	inner class RoleMenuEditArgs : Arguments() {
		val role by role {
			name = "role"
			description = "The role you'd like to add or remove from this channel's role menu."
		}
		val addOrRemove by boolean {
			name = "addOrRemove"
			description = "If the selected role should be added or removed from this channel's role menu."
		}
	}
}
