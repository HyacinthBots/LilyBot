package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingColor
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingString
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.event
import com.kotlindiscord.kord.extensions.types.respond
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.rest.builder.message.create.embed
import com.kotlindiscord.kord.extensions.utils.hasRole
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.DatabaseManager
import net.irisshaders.lilybot.utils.MOD_ACTION_LOG
import org.jetbrains.exposed.sql.insertIgnore
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction

class RoleMenu : Extension() {
    override val name = "rolemenu"

	override suspend fun setup() {
		ephemeralSlashCommand(::RoleMenuArgs) {
			name = "role-menu"
			description = "Creates a menu that allows users to select a role."

			check { hasPermission(Permission.ManageMessages) }

			action {
				val descriptionAppendix = "\n\nUse the button below to add/remove the ${arguments.role.mention} role."

				val targetChannel: GuildMessageChannelBehavior =
					if (arguments.channel != null) {
						val targetChannelId = arguments.channel!!.id
						guild?.getChannel(targetChannelId) as GuildMessageChannelBehavior
					} else {
						this.channel as GuildMessageChannelBehavior
					}

				targetChannel.createMessage {
					embed {
						title = arguments.title
						description = arguments.description + descriptionAppendix
						color = arguments.color
					}

					val components = components {
						ephemeralButton(row = 0) {
							label = "Add role"
							style = ButtonStyle.Success
							id = arguments.role.name + "add"

							newSuspendedTransaction {
								DatabaseManager.Components.insertIgnore {
									it[componentId] = arguments.role.name + "add"
									it[roleId] = arguments.role.id.toString()
									it[addOrRemove] = "add"
								}
							}

							action { }
						}

						ephemeralButton(row = 0) {
							label = "Remove role"
							style = ButtonStyle.Danger
							id = arguments.role.name + "remove"

							newSuspendedTransaction {
								DatabaseManager.Components.insertIgnore {
									it[componentId] = arguments.role.name + "remove"
									it[roleId] = arguments.role.id.toString()
									it[addOrRemove] = "remove"
								}
							}

							action { }
						}
					}

					components.removeAll()

					respond { content = "Role menu created." }
					val actionLog = guild?.getChannel(MOD_ACTION_LOG) as GuildMessageChannelBehavior
					actionLog.createEmbed {
						color = DISCORD_BLACK
						title = "Role menu created."
						description =
							"A role menu for the ${arguments.role.mention} role was created in ${targetChannel.mention}"

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

		event<InteractionCreateEvent> {
			check { failIfNot(event.interaction is ButtonInteraction) }
			
			action {
				val interaction = event.interaction as ButtonInteraction
				val guild = kord.getGuild(interaction.data.guildId.value!!)!!
				val member = guild.getMember(interaction.user.id)

				var roleId: String? = null
				var addOrRemove: String? = null

				newSuspendedTransaction {
					roleId = DatabaseManager.Components.select {
						DatabaseManager.Components.componentId eq interaction.componentId
					}.single()[DatabaseManager.Components.roleId]

					addOrRemove = DatabaseManager.Components.select {
						DatabaseManager.Components.componentId eq interaction.componentId
					}.single()[DatabaseManager.Components.addOrRemove]
				}

				val role = guild.getRole(Snowflake(roleId!!))

				if (addOrRemove == "add") {
					if (!member.hasRole(role)) {
						member.addRole(role.id)
						interaction.respondEphemeral {content = "Added the ${role.mention} role."}
					}
					else if (member.hasRole(role)) { interaction.respondEphemeral {content = "You already have the ${role.mention} role so it can't be added. If you don't, please contact a staff member."} }
				} else if (addOrRemove == "remove") {
					if (!member.hasRole(role)) { interaction.respondEphemeral {content = "You don't have the ${role.mention} role so it can't be removed. If you do, please contact a staff member."} }
					else if (member.hasRole(role)) {
						member.removeRole(role.id)
						interaction.respondEphemeral {content = "Removed the ${role.mention} role."}
					}
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
