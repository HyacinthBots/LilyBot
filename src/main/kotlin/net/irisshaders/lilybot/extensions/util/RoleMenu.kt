package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_BLACK
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
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
import com.kotlindiscord.kord.extensions.utils.ackEphemeral
import com.kotlindiscord.kord.extensions.utils.hasRole
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.entity.Role
import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.event.interaction.InteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.ComponentData
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent

class RoleMenu : Extension() {
	override val name = "role-menu"

	private fun descriptionAppendix(role: Role) =
		"\n\nUse the button below to add/remove the ${role.mention} role."

	override suspend fun setup() {
		ephemeralSlashCommand(::RoleMenuArgs) {
			name = "role-menu"
			description = "Creates a menu that allows users to select a role."

			check {
				anyGuild()
				hasPermission(Permission.ManageMessages)
				configPresent()
			}

			action {
				val targetChannel =
					if (arguments.channel != null) {
						guild!!.getChannelOf(arguments.channel!!.id)
					} else {
						this.channel
					}

				targetChannel.createMessage {
					embed {
						title = arguments.title
						description = arguments.description + descriptionAppendix(arguments.role)
						color = arguments.color
					}

					val components = components {
						ephemeralButton(0) {
							label = "Add role"
							style = ButtonStyle.Success
							id = arguments.role.name + "add"
							DatabaseHelper.setComponent(ComponentData(guild!!.id, id, arguments.role.id, "add"))

							action { }
						}

						ephemeralButton(0) {
							label = "Remove role"
							style = ButtonStyle.Danger
							id = arguments.role.name + "remove"
							DatabaseHelper.setComponent(ComponentData(guild!!.id, id, arguments.role.id, "remove"))

							action { }
						}
					}

					// Unregister the built-in listener for the unused action callback
					components.removeAll()

					respond { content = "Role menu created." }
				}

				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val actionLog = guild!!.getChannel(config.modActionLog) as GuildMessageChannelBehavior

				actionLog.createEmbed {
					color = DISCORD_BLACK
					title = "Role menu created."
					description =
						"A role menu for the ${arguments.role.mention} role was created in ${targetChannel.mention}"

					field {
						name = "Embed title"
						value = arguments.title
						inline = false
					}
					field {
						name = "Embed description:"
						value = arguments.description + descriptionAppendix(arguments.role)
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

		event<InteractionCreateEvent> {
			check {
				anyGuild()
				failIfNot {
					event.interaction is ButtonInteraction
				}
			}

			action {
				val interaction = event.interaction as ButtonInteraction
				val guild = kord.getGuild(interaction.data.guildId.value!!)!!
				val member = guild.getMember(event.interaction.user.id)

				// this is  a very dirty fix, so it doesn't conflict with log uploading
				val roleId: Snowflake? = try {
					DatabaseHelper.getComponent(guildFor(event)!!.id, interaction.componentId)?.roleId
				} catch (e: NullPointerException) {
					return@action
				}
				val addOrRemove: String? = try {
					DatabaseHelper.getComponent(guildFor(event)!!.id, interaction.componentId)?.addOrRemove
				} catch (e: NullPointerException) {
					return@action
				}

				roleId ?: return@action
				addOrRemove ?: return@action

				val role = guild.getRole(roleId)
				val response = interaction.ackEphemeral()

				if (addOrRemove == "add") {
					if (member.hasRole(role)) {
						response.createPublicFollowup {
							content = "You already have the ${role.mention} role, so it cannot be added. " +
									"If you **do not** have the role, please contact a staff member."
						}
					} else if (!member.hasRole(role)) {
						try {
							member.addRole(role.id)
						} catch (e: KtorRequestException) {
							response.createEphemeralFollowup {
								content = "I was unable to add the ${role.mention} role to you, due to a permissions " +
										"error. Please contact a staff member."
							}
							return@action
						}

						response.createEphemeralFollowup { content = "Added the ${role.mention} role." }
					}
				} else if (addOrRemove == "remove") {
					if (member.hasRole(role)) {
						try {
							member.removeRole(role.id)
						} catch (e: KtorRequestException) {
							response.createEphemeralFollowup {
								content = "I was unable to remove the ${role.mention} role from you, due to a " +
										"permissions error. Please contact a staff member."
							}
							return@action
						}

						response.createEphemeralFollowup { content = "Remove the ${role.mention} role." }
					} else if (!member.hasRole(role)) {
						response.createEphemeralFollowup {
							content = "You do not have the ${role.mention} role, so it cannot be removed. " +
									"If you **do** have the role, please contact a staff member."
						}
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

			mutate {
				it.replace("\\n", "\n")
					.replace("\n", "\n")
			}
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
