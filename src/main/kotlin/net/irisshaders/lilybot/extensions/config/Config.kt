package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
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
import net.irisshaders.lilybot.database.collections.LoggingConfigCollection
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.collections.SupportConfigCollection
import net.irisshaders.lilybot.database.entities.LoggingConfigData
import net.irisshaders.lilybot.database.entities.ModerationConfigData
import net.irisshaders.lilybot.database.entities.SupportConfigData
import kotlin.time.Duration.Companion.seconds

class Config : Extension() {
	override val name: String = "config"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		configCommand()
	}
}

@OptIn(UnsafeAPI::class)
suspend fun Config.configCommand() = unsafeSlashCommand {
	name = "config"
	description = "Configuring Lily's settings"

	unsafeSubCommand(::SupportArgs) {
		name = "support"
		description = "Configure Lily's support system"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		// TODO Audit Log logging
		action {
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
						title = "Configuration: Support"
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

				SupportConfigCollection().setConfig(
					SupportConfigData(
						guild!!.id,
						arguments.enable,
						arguments.channel.id,
						arguments.role.id,
						supportMsg
					)
				)
			} else {
				event.interaction.respondEphemeral {
					embed {
						title = "Configuration: Support"
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

				SupportConfigCollection().setConfig(
					SupportConfigData(
						guild!!.id,
						arguments.enable,
						arguments.channel.id,
						arguments.role.id,
						null
					)
				)
			}
		}
	}

	unsafeSubCommand(::ModerationArgs) {
		name = "moderation"
		description = "Configure Lily's moderation system"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		// TODO Audit Log logging
		action {
			event.interaction.respondEphemeral {
				embed {
					title = "Configuration: Moderation"
					field {
						name = "Moderators"
						value = arguments.moderatorRole.mention
					}
					field {
						name = "Action log"
						value = arguments.modActionLog.mention
					}
					footer {
						text = "Configured by ${user.asUser().tag}"
					}
				}
			}

			ModerationConfigCollection().setConfig(
				ModerationConfigData(
					guild!!.id,
					arguments.enabled,
					arguments.modActionLog.id,
					arguments.moderatorRole.id
				)
			)
		}
	}

	unsafeSubCommand(::LoggingArgs) {
		name = "logging"
		description = "Configure Lily's logging system"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			// hasPermission(Permission.ManageGuild)
		}

		// TODO Audit Log logging
		action {
			event.interaction.respondEphemeral {
				embed {
					title = "Configuration: Logging"
					field {
						name = "Message Logs"
						value = arguments.messageLogs.mention
					}
					field {
						name = "Join/Leave Logs"
						value = arguments.joinChannel.mention
					}
					footer {
						text = "Configured by ${user.asUser().tag}"
					}
				}
			}

			LoggingConfigCollection().setConfig(
				LoggingConfigData(
					guild!!.id,
					arguments.enableMessageLogs,
					arguments.messageLogs.id,
					arguments.enableJoinChannel,
					arguments.joinChannel.id
				)
			)
		}
	}

	// TODO Validate the presence of database values
	unsafeSubCommand(::ClearArgs) {
		name = "clear"
		description = "Clear a config type"

		initialResponse = InitialSlashCommandResponse.None

		action {
			when (arguments.config) {
				ConfigType.MODERATION.name -> {
					ModerationConfigCollection().clearConfig(guild!!.id)
					event.interaction.respondEphemeral {
						embed {
							title = "Config cleared: Moderation"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
							}
						}
					}
				}
				ConfigType.LOGGING.name -> {
					LoggingConfigCollection().clearConfig(guild!!.id)
					event.interaction.respondEphemeral {
						embed {
							title = "Config cleared: Logging"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
							}
						}
					}
				}
				ConfigType.SUPPORT.name -> {
					SupportConfigCollection().clearConfig(guild!!.id)
					event.interaction.respondEphemeral {
						embed {
							title = "Config cleared: Support"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
							}
						}
					}
				}
				ConfigType.ALL.name -> {
					ModerationConfigCollection().clearConfig(guild!!.id)
					LoggingConfigCollection().clearConfig(guild!!.id)
					SupportConfigCollection().clearConfig(guild!!.id)
					event.interaction.respondEphemeral {
						embed {
							title = "Config cleared"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
							}
						}
					}
				}
			}
		}
	}
}

class SupportArgs : Arguments() {
	val enable by boolean {
		name = "enable"
		description = "Whether to enable the support system"
	}

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
		description = "True if you'd like to add a custom message, false if you'd like the default."
	}
}

class ModerationArgs : Arguments() {
	val enabled by boolean {
		name = "enable"
		description = "Whether to enable the moderation system"
	}

	val moderatorRole by role {
		name = "moderatorrole"
		description = "The role of your moderators, used for pinging in message logs."
	}

	val modActionLog by channel {
		name = "actionlog"
		description = "The channel used to store moderator actions."
	}
}

class LoggingArgs : Arguments() {
	val enableMessageLogs by boolean {
		name = "enablemesssgelogs"
		description = "Enable logging of message deletions"
	}

	val enableJoinChannel by boolean {
		name = "enablejoinchannel"
		description = "Enable logging of joins and leaves"
	}

	val messageLogs by channel {
		name = "messagelogs"
		description = "The channel for logging message deletions"
	}

	val joinChannel by channel {
		name = "joinchannel"
		description = "The channel for logging member joins/leaves"
	}
}

class ClearArgs : Arguments() {
	val config by stringChoice {
		name = "configType"
		description = "The type of config to clear"
		choices = mutableMapOf(
			"support" to ConfigType.SUPPORT.name,
			"moderation" to ConfigType.MODERATION.name,
			"logging" to ConfigType.LOGGING.name,
			"all" to ConfigType.ALL.name
		)
	}
}
