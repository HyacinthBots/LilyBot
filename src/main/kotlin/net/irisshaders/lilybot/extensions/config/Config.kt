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
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
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
import dev.kord.rest.builder.message.EmbedBuilder
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
	description = "Configure Lily's settings"

	unsafeSubCommand(::SupportArgs) {
		name = "support"
		description = "Configure Lily's support system"

		initialResponse = InitialSlashCommandResponse.None

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		action {
			val supportConfig = SupportConfigCollection().getConfig(guild!!.id)
			if (supportConfig != null) {
				ackEphemeral()
				respondEphemeral {
					content = "You already have a moderation configuration set. " +
							"Please clear it before attempting to set a new one."
				}
				return@action
			}

			suspend fun EmbedBuilder.supportEmbed() {
				title = "Configuration: Support"
				field {
					name = "Support Team"
					value = arguments.role?.mention ?: "Disabled"
				}
				field {
					name = "Support Channel"
					value = arguments.channel?.mention ?: "Disabled"
				}
				footer {
					text = "Configured by: ${user.asUser().tag}"
				}
			}

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
						supportEmbed()
						field {
							name = "Message"
							value = supportMsg
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
						supportEmbed()
						field {
							name = "Message"
							value = "default"
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
					supportEmbed()
					field {
						name = "Message"
						value = SupportConfigCollection().getConfig(guild!!.id)?.message ?: "default"
					}
					ModerationConfigCollection().getConfig(guild!!.id) ?: run {
						description = "Consider setting the moderation configuration to receive configuration " +
								"updates where you want them!"
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

		action {
			val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)
			if (moderationConfig != null) {
				respond {
					content = "You already have a moderation configuration set. " +
							"Please clear it before attempting to set a new one."
				}
				return@action
			}

			suspend fun EmbedBuilder.moderationEmbed() {
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

			respond {
				embed {
					moderationEmbed()
				}
			}

			if (arguments.modActionLog == null) {
				guild!!.asGuild().getSystemChannel()
			} else {
				guild!!.getChannelOf<GuildMessageChannel>(arguments.modActionLog!!.id)
			}?.createMessage {
				embed {
					moderationEmbed()
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
		}
	}

	ephemeralSubCommand(::LoggingArgs) {
		name = "logging"
		description = "Configure Lily's logging system"

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		action {
			val loggingConfig = LoggingConfigCollection().getConfig(guild!!.id)
			if (loggingConfig != null) {
				respond {
					content = "You already have a logging configuration set. " +
							"Please clear it before attempting to set a new one."
				}
				return@action
			}

			if (arguments.enableMemberLogging && arguments.memberLog == null) {
				respond { content = "You must specify a channel to log members joining and leaving to!" }
				return@action
			} else if (arguments.enableMessageLogs && arguments.messageLogs == null) {
				respond { content = "You must specify a channel to log deleted messages to!" }
				return@action
			}

			suspend fun EmbedBuilder.loggingEmbed() {
				title = "Configuration: Logging"
				field {
					name = "Message Logs"
					value = if (arguments.enableMessageLogs && arguments.messageLogs?.mention != null) {
						arguments.messageLogs!!.mention
					} else {
						"Disabled"
					}
				}
				field {
					name = "Member Logs"
					value = if (arguments.enableMemberLogging && arguments.memberLog?.mention != null) {
						arguments.memberLog!!.mention
					} else {
						"Disabled"
					}
				}
				footer {
					text = "Configured by ${user.asUser().tag}"
					icon = user.asUser().avatar?.url
				}
			}

			respond {
				embed {
					loggingEmbed()
				}
			}

			LoggingConfigCollection().setConfig(
				LoggingConfigData(
					guild!!.id,
					arguments.enableMessageLogs,
					arguments.messageLogs?.id,
					arguments.enableMemberLogging,
					arguments.memberLog?.id
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
					loggingEmbed()
					ModerationConfigCollection().getConfig(guild!!.id) ?: run {
						description = "Consider setting the moderation configuration to receive configuration " +
								"updates where you want them!"
					}
				}
			}
		}
	}

	ephemeralSubCommand(::ClearArgs) {
		name = "clear"
		description = "Clear a config type"

		action {
			when (arguments.config) {
				ConfigType.MODERATION.name -> {
					val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)
					if (moderationConfig == null) {
						respond {
							content = "No moderation configuration exists to clear!"
						}
						return@action
					}

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
					val loggingConfig = LoggingConfigCollection().getConfig(guild!!.id)
					if (loggingConfig == null) {
						respond {
							content = "No logging configuration exists to clear!"
						}
						return@action
					}

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
					val supportConfig = SupportConfigCollection().getConfig(guild!!.id)
					if (supportConfig == null) {
						respond {
							content = "No support configuration exists to clear!"
						}
						return@action
					}

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
				ConfigType.ALL.name -> {
					ModerationConfigCollection().clearConfig(guild!!.id)
					LoggingConfigCollection().clearConfig(guild!!.id)
					SupportConfigCollection().clearConfig(guild!!.id)
					respond {
						embed {
							title = "All configs cleared"
							footer {
								text = "Configs cleared by ${user.asUser().tag}"
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
						description = "Consider setting the moderation configuration to receive configuration " +
								"updates where you want them!"
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
		name = "enable-support"
		description = "Whether to enable the support system"
	}

	val customMessage by boolean {
		name = "custom-message"
		description = "True if you'd like to add a custom message, false if you'd like the default."
	}

	val channel by optionalChannel {
		name = "support-channel"
		description = "The channel to be used for creating support threads in."
	}

	val role by optionalRole {
		name = "support-role"
		description = "The role to add to support threads, when one is created."
	}
}

class ModerationArgs : Arguments() {
	val enabled by boolean {
		name = "enable-moderation"
		description = "Whether to enable the moderation system"
	}

	val moderatorRole by optionalRole {
		name = "moderator-role"
		description = "The role of your moderators, used for pinging in message logs."
	}

	val modActionLog by optionalChannel {
		name = "action-log"
		description = "The channel used to store moderator actions."
	}
}

class LoggingArgs : Arguments() {
	val enableMessageLogs by boolean {
		name = "enable-message-logs"
		description = "Enable logging of message deletions"
	}

	val enableMemberLogging by boolean {
		name = "enable-member-logging"
		description = "Enable logging of members joining and leaving the guild"
	}

	val messageLogs by optionalChannel {
		name = "message-logs"
		description = "The channel for logging message deletions"
	}

	val memberLog by optionalChannel {
		name = "member-log"
		description = "The channel for logging members joining and leaving the guild"
	}
}

class ClearArgs : Arguments() {
	val config by stringChoice {
		name = "config-type"
		description = "The type of config to clear"
		choices = mutableMapOf(
			"support" to ConfigType.SUPPORT.name,
			"moderation" to ConfigType.MODERATION.name,
			"logging" to ConfigType.LOGGING.name,
			"all" to ConfigType.ALL.name
		)
	}
}
