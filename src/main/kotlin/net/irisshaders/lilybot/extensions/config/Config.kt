package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.channel
import com.kotlindiscord.kord.extensions.commands.converters.impl.role
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import kotlin.time.Duration.Companion.seconds

class Config : Extension() {
	override val name: String = "config"
	override val bundle: String = "config"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		configCommand()
	}
}

@OptIn(UnsafeAPI::class)
suspend fun Config.configCommand() = unsafeSlashCommand {
	name = "config"
	description = "Configuring Lily's Modules"
	guild(TEST_GUILD_ID)

	unsafeSubCommand(::SupportModuleArgs) {
		name = "support"
		description = "Configure the support module"

		initialResponse = InitialSlashCommandResponse.None
		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		action {
			// TODO new database functions to store this stuff

			if (arguments.customMessage) {
				val response = event.interaction.modal("Support Module", "supportModuleModal") {
					actionRow {
						textInput(TextInputStyle.Paragraph, "msgInput", "Support Message") {
							placeholder = "This is where your ad could be!"
						}
					}
				}

				val interaction =
					response.kord.waitFor<ModalSubmitInteractionCreateEvent>(60.seconds.inWholeMilliseconds) {
						interaction.modalId == "supportModuleModal"
					}?.interaction
				if (interaction == null) {
					response.createEphemeralFollowup {
						embed {
							description = "Configuration timed out"
						}
					}
					return@action
				}

				val supportMsg = interaction.textInputs["msgInput"]!!.value!!
				val modalResponse = interaction.deferEphemeralResponse()

				modalResponse.respond {
					embed {
						title = "Module configured: Support"
						field {
							name = "Support Team"
							value = arguments.role.mention
						}
						field {
							name = "Support Channel"
							value = arguments.channel.mention
						}
						field {
							name = "Message"
							value = supportMsg
						}
						footer {
							text = "Configured by: ${user.asUser().tag}"
						}
					}
				}
			} else {
				event.interaction.respondEphemeral {
					embed {
						title = "Module configured: Support"
						field {
							name = "Support Team"
							value = arguments.role.mention
						}
						field {
							name = "Support Channel"
							value = arguments.channel.mention
						}
						field {
							name = "Message"
							value = "default"
						}
						footer {
							text = "Configured by: ${user.asUser().tag}"
						}
					}
				}
			}
		}
	}

	unsafeSubCommand {
		name = "moderation"
		description = "Configure the moderation module"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		action {
			val response = event.interaction.modal("Moderation Module", "moderationModal") {
				actionRow {
					textInput(TextInputStyle.Short, "moderatorRole", "Moderation Role") {
						placeholder = "Role ID or name"
					}
				}
				actionRow {
					textInput(TextInputStyle.Short, "actionLog", "Action Log") {
						placeholder = "Channel ID"
					}
				}
			}

			val interaction = response.kord.waitFor<ModalSubmitInteractionCreateEvent>(60.seconds.inWholeMilliseconds) {
				interaction.modalId == "moderationModal"
			}?.interaction
			if (interaction == null) {
				response.createEphemeralFollowup {
					embed {
						description = "Configuration timed out"
					}
				}
				return@action
			}

			val moderationRole = interaction.textInputs["moderatorRole"]!!.value!!
			val actionLog = interaction.textInputs["actionLog"]!!.value!!
			val modalResponse = interaction.deferEphemeralResponse()
			modalResponse.respond {
				embed {
					title = "Module configured: Moderation"
					field {
						name = "Moderators"
						value = moderationRole
					}
					field {
						name = "Action Log"
						value = actionLog
					}
					footer {
						text = "Configured by: ${user.asUser().tag}"
					}
				}
			}
		}
	}

	unsafeSubCommand {
		name = "logging"
		description = "Configure Lily's logging modules"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		action {
		}
	}

	unsafeSubCommand {
		name = "bot"
		description = "Configure general aspects of the bot"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		action {
		}
	}
}

class SupportModuleArgs : Arguments() {
	val channel by channel {
		name = "channel"
		description = "The channel to be used for creating support threads in."
	}

	val role by role {
		name = "role"
		description = "The role to add to support threads, when one is created."
	}

	val customMessage by boolean {
		name = "custommessage"
		description = "True if you'd like to add a custom message, false if you'd like the default"
	}
}
