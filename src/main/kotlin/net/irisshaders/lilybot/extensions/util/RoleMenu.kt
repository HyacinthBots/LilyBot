package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import net.irisshaders.lilybot.utils.FULLMODERATORS
import com.kotlindiscord.kord.extensions.utils.hasRole
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.MOD_ACTION_LOG

//todo role doesn't retain value between reboots

class RoleMenu : Extension() {
    override val name = "rolemenu"

	override suspend fun setup() {
        ephemeralSlashCommand(::RoleMenuArgs) {
            name = "role-menu"
            description = "Creates a menu that allows users to select a role."

            allowRole(FULLMODERATORS)

            action {

				val descriptionAppendix = "\n\nUse the buttons below to add/remove the ${arguments.role.mention} role."
				val targetChannel: GuildMessageChannelBehavior =
					if (arguments.channel != null) {
						val targetChannelId = arguments.channel!!.id
						guild?.getChannel(targetChannelId) as GuildMessageChannelBehavior
					}
					else {
						this.channel as GuildMessageChannelBehavior
					}

				targetChannel.createMessage {
					embed {
						title = arguments.title
						description = arguments.description + descriptionAppendix
						color = arguments.color
					}
					components {
						ephemeralButton (row = 0) {
							label = "Add"
							style = ButtonStyle.Success
							action {
								val member = guild?.getMember(member!!.id)
								if (member?.hasRole(arguments.role) == false) {
									member.addRole(arguments.role.id)
									respond {content = "Role added."}
								}
								else {
									respond {content = "You already have this role. If you don't, please alert a staff member."}
								}
							}
						}
						ephemeralButton (row = 0) {
							label = "Remove"
							style = ButtonStyle.Danger
							action {
								val member = guild?.getMember(member!!.id)
								if (member?.hasRole(arguments.role) == true) {
									member.removeRole(arguments.role.id)
									respond {content = "Role removed."}
								}
								else {
									respond {content = "You don't have this role. If you do, please alert a staff member."}
								}
							}
						}
					}
				}
				respond { content = "Role menu created." }
				val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
				actionLog.createEmbed {
					color = DISCORD_BLACK
					title = "Role menu created."
					description = "A role menu for the ${arguments.role.mention} role was created in ${targetChannel.mention}"

					field {
						name = "Embed title:"
						value = arguments.title
						inline = false
					}
					field {
						name = "Embed description:"
						value = arguments.description + descriptionAppendix
						inline = false
					}
					footer {
						text = "Requested by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
					timestamp = Clock.System.now()
				}
            }
        }
    }

    inner class RoleMenuArgs : Arguments() {
		val role by role {
			name = "roles"
			description = "The roles to be selected."
		}
        val title by defaultingString {
            name = "title"
            description = "The title of the embed. Defaults to \"Role Selection Menu\""
			defaultValue = "Role selection menu"
        }
        val description by defaultingString {
            name = "description"
            description = "Text for the embed. Will be appended with a description of how to use the buttons."
			defaultValue = " "
        }
		val channel by optionalChannel {
			name = "channel"
			description = "The channel for the message to be sent in. Defaults to the channel executed in."
		}
		val color by defaultingColor {
			name = "color"
			description = "The color for the embed menu to be."
			defaultValue = DISCORD_BLACK
		}
    }
}
