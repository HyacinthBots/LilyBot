package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import kotlin.time.Duration.Companion.seconds

@OptIn(UnsafeAPI::class)
suspend fun ConfigExtension.configCommand() = unsafeSlashCommand {
	name = "config"
	description = "Configuring Lily's Modules"
	guild(TEST_GUILD_ID)

	unsafeSubCommand {
		name = "support"
		description = "Configure the support module"

		initialResponse = InitialSlashCommandResponse.None
		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		action {
			val response = event.interaction.modal("Support Module", "supportModuleModal") {
				actionRow {
					textInput(TextInputStyle.Paragraph, "msgInput", "Support Message") {
						placeholder = "This is where your ad could be!"
					}
				}
				actionRow {
					textInput(TextInputStyle.Short, "teamInput", "Support Team/Role") {
						placeholder = "Role ID or name"
					}
				}
				actionRow {
					textInput(TextInputStyle.Short, "supChannel", "Support Channel") {
						placeholder = "Channel ID"
					}
				}
			}

			val interaction = response.kord.waitFor<ModalSubmitInteractionCreateEvent>(60.seconds.inWholeMilliseconds) {
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
			val supportTeam = interaction.textInputs["teamInput"]!!.value!!
			val supportChannel = interaction.textInputs["supChannel"]!!.value!!
			val modalResponse = interaction.deferEphemeralResponse()

			modalResponse.respond {
				embed {
					title = "Module configured: Support"
					description = supportMsg
					field {
						name = "Support Team"
						value = supportTeam
					}
					field {
						name = "Support Channel"
						value = supportChannel
					}
					footer {
						text = "Configured by: ${user.asUser().tag}"
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
