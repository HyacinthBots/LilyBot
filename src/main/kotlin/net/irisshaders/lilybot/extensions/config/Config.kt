package net.irisshaders.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.Permission
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.GuildMessageChannel
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

// TODO Do some wizard magic and duplicaten't?
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
			hasPermission(Permission.ManageGuild)
		}

		@Suppress("DuplicatedCode")
		action {
			if (arguments.customMessage) {
				val response = event.interaction.modal("Support Module", "supportModuleModal") {
					actionRow {
						textInput(TextInputStyle.Paragraph, "msgInput", "Support Message") {
							placeholder = "Input the content of the message you would like sent when a support thread" +
									"is created"
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
							value = arguments.role?.mention ?: "Disabled"
						}
						field {
							name = "Support Channel"
							value = arguments.channel?.mention ?: "Disabled"
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
						arguments.channel?.id,
						arguments.role?.id,
						supportMsg
					)
				)
			} else {
				event.interaction.respondEphemeral {
					embed {
						title = "Configuration: Support"
						field {
							name = "Support Team"
							value = arguments.role?.mention ?: "Disabled"
						}
						field {
							name = "Support Channel"
							value = arguments.channel?.mention ?: "Disabled"
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
						arguments.channel?.id,
						arguments.role?.id,
						null
					)
				)
			}

			if (ModerationConfigCollection().getConfig(guild!!.id) == null) {
				guild!!.asGuild().getSystemChannel()
			} else {
				guild!!.getChannelOf<GuildMessageChannel>(ModerationConfigCollection().getConfig(guild!!.id)!!.channel!!)
			}?.createMessage {
				embed {
					title = "Configuration: Support"
					ModerationConfigCollection().getConfig(guild!!.id) ?: run {
						description = "Consider setting the moderation configuration to receive configuration updates" +
								"where you want them!"
					}
					field {
						name = "Support Team"
						value = arguments.role?.mention ?: "Disable"
					}
					field {
						name = "Support Channel"
						value = arguments.channel?.mention ?: "Disable"
					}
					field {
						name = "Message"
						value = "default"
					}
					footer {
						text = "Configured by: ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}
	}

	ephemeralSubCommand(::ModerationArgs) {
		name = "moderation"
		description = "Configure Lily's moderation system"

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		@Suppress("DuplicatedCode")
		action {
			respond {
				embed {
					title = "Configuration: Moderation"
					field {
						name = "Moderators"
						value = arguments.moderatorRole?.mention ?: "Disabled"
					}
					field {
						name = "Action log"
						value = arguments.modActionLog?.mention ?: "Disabled"
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
					arguments.modActionLog?.id,
					arguments.moderatorRole?.id
				)
			)

			if (arguments.modActionLog == null) {
				guild!!.asGuild().getSystemChannel()
			} else {
				guild!!.getChannelOf<GuildMessageChannel>(arguments.modActionLog!!.id)
			}?.createMessage {
				embed {
					title = "Configuration: Moderation"
					field {
						name = "Moderators"
						value = arguments.moderatorRole?.mention ?: "Disabled"
					}
					field {
						name = "Action log"
						value = arguments.modActionLog?.mention ?: "Disabled"
					}
					footer {
						text = "Configured by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}
	}

	ephemeralSubCommand(::LoggingArgs) {
		name = "logging"
		description = "Configure Lily's logging system"

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		@Suppress("DuplicatedCode")
		action {
			if (arguments.enableJoinChannel && arguments.joinChannel == null) {
				respond { content = "You must specify a channel to log joins to!" }
				return@action
			} else if (arguments.enableMessageLogs && arguments.messageLogs == null) {
				respond { content = "You must specify a channel to message deletes joins to!" }
				return@action
			}

			respond {
				embed {
					title = "Configuration: Logging"
					field {
						name = "Message Logs"
						value = if (!arguments.enableMessageLogs || arguments.messageLogs?.mention == null) {
							arguments.messageLogs!!.mention
						} else {
							"Disabled"
						}
					}
					field {
						name = "Join/Leave Logs"
						value = if (!arguments.enableJoinChannel || arguments.joinChannel?.mention == null) {
							arguments.joinChannel!!.mention
						} else {
							"Disabled"
						}
					}
				}
			}

			LoggingConfigCollection().setConfig(
				LoggingConfigData(
					guild!!.id,
					arguments.enableMessageLogs,
					arguments.messageLogs?.id,
					arguments.enableJoinChannel,
					arguments.joinChannel?.id
				)
			)

			if (ModerationConfigCollection().getConfig(guild!!.id) == null ||
				!ModerationConfigCollection().getConfig(guild!!.id)!!.enabled
			) {
				guild!!.asGuild().getSystemChannel()
			} else {
				guild!!.getChannelOf<GuildMessageChannel>(ModerationConfigCollection().getConfig(guild!!.id)!!.channel!!)
			}?.createMessage {
				embed {
					title = "Configuration: Logging"
					ModerationConfigCollection().getConfig(guild!!.id) ?: run {
						description = "Consider setting the moderation configuration to receive configuration updates" +
								"where you want them!"
					}
					field {
						name = "Message Logs"
						value = if (!arguments.enableMessageLogs || arguments.messageLogs?.mention == null) {
							arguments.messageLogs!!.mention
						} else {
							"Disabled"
						}
					}
					field {
						name = "Join/Leave Logs"
						value = if (!arguments.enableJoinChannel || arguments.joinChannel?.mention == null) {
							arguments.joinChannel!!.mention
						} else {
							"Disabled"
						}
					}
					footer {
						text = "Configured by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
					}
				}
			}
		}
	}

	// TODO Validate the presence of database values
	ephemeralSubCommand(::ClearArgs) {
		name = "clear"
		description = "Clear a config type"

		action {
			when (arguments.config) {
				ConfigType.MODERATION.name -> {
					ModerationConfigCollection().clearConfig(guild!!.id)
					event.interaction.respondEphemeral {
						embed {
							title = "Config cleared: Moderation"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
						}
					}
				}

				ConfigType.LOGGING.name -> {
					LoggingConfigCollection().clearConfig(guild!!.id)
					respond {
						embed {
							title = "Config cleared: Logging"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
						}
					}
				}

				ConfigType.SUPPORT.name -> {
					SupportConfigCollection().clearConfig(guild!!.id)
					respond {
						embed {
							title = "Config cleared: Support"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
						}
					}
				}
				// FIXME Apparently this is broken. Do something idk
				ConfigType.ALL.name -> {
					ModerationConfigCollection().clearConfig(guild!!.id)
					LoggingConfigCollection().clearConfig(guild!!.id)
					SupportConfigCollection().clearConfig(guild!!.id)
					respond {
						embed {
							title = "Config cleared"
							footer {
								text = "Config cleared by ${user.asUser().tag}"
								icon = user.asUser().avatar?.url
							}
						}
					}
				}
			}
			if (ModerationConfigCollection().getConfig(guild!!.id) == null ||
				!ModerationConfigCollection().getConfig(guild!!.id)!!.enabled
			) {
				guild!!.asGuild().getSystemChannel()
			} else {
				guild!!.getChannelOf<GuildMessageChannel>(ModerationConfigCollection().getConfig(guild!!.id)!!.channel!!)
			}?.createMessage {
				embed {
					title = "Configuration Cleared: ${arguments.config}"
					ModerationConfigCollection().getConfig(guild!!.id) ?: run {
						description =
							"Consider setting the moderation configuration to receive configuration updates" +
									"where you want them!"
					}
					footer {
						text = "Config cleared by ${user.asUser().tag}"
						icon = user.asUser().avatar?.url
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

	val customMessage by boolean {
		name = "custommessage"
		description = "True if you'd like to add a custom message, false if you'd like the default."
	}

	val channel by optionalChannel {
		name = "channel"
		description = "The channel to be used for creating support threads in."
	}

	val role by optionalRole {
		name = "role"
		description = "The role to add to support threads, when one is created."
	}
}

class ModerationArgs : Arguments() {
	val enabled by boolean {
		name = "enable"
		description = "Whether to enable the moderation system"
	}

	val moderatorRole by optionalRole {
		name = "moderatorrole"
		description = "The role of your moderators, used for pinging in message logs."
	}

	val modActionLog by optionalChannel {
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

	val messageLogs by optionalChannel {
		name = "messagelogs"
		description = "The channel for logging message deletions"
	}

	val joinChannel by optionalChannel {
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
