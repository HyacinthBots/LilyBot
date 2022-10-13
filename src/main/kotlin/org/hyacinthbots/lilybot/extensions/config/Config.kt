package org.hyacinthbots.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
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
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.Permission
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.channel.TextChannel
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.database.entities.SupportConfigData
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.hyacinthbots.lilybot.utils.canPingRole
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
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
					content = "You already have a support configuration set. " +
							"Please clear it before attempting to set a new one."
				}
				return@action
			}

			if (!canPingRole(arguments.role)) {
				ackEphemeral()
				respondEphemeral {
					content =
						"I cannot use the role: ${arguments.role!!.mention}, because it is not mentionable by" +
								"regular users. Please enable this in the role settings, or use a different role."
				}
				return@action
			}

			val supportChannel: TextChannel?
			if (arguments.enable && arguments.channel != null) {
				supportChannel = guild!!.getChannelOfOrNull(arguments.channel!!.id)
				if (supportChannel?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
					ackEphemeral()
					respondEphemeral {
						content = "The mod action log you've selected is invalid, or I can't view it. " +
								"Please attempt to resolve this and try again."
					}
					return@action
				}
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

			val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

			if (utilityLog == null) {
				ackEphemeral()
				respondEphemeral {
					content = "Consider setting a utility config to log changes to configurations."
				}
				return@action
			}

			utilityLog.createMessage {
				embed {
					supportEmbed()
					field {
						name = "Message"
						value = SupportConfigCollection().getConfig(guild!!.id)?.message ?: "default"
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

			if (
				arguments.moderatorRole != null && arguments.modActionLog == null ||
				arguments.moderatorRole == null && arguments.modActionLog != null
			) {
				respond {
					content =
						"You must set both the moderator role and the action log channel to use the moderation configuration."
				}
				return@action
			}

			if (!canPingRole(arguments.moderatorRole)) {
				respond {
					content =
						"I cannot use the role: ${arguments.moderatorRole!!.mention}, because it is not mentionable by" +
								"regular users. Please enable this in the role settings, or use a different role."
				}
				return@action
			}

			val modActionLog: TextChannel?
			if (arguments.enabled && arguments.modActionLog != null) {
				modActionLog = guild!!.getChannelOfOrNull(arguments.modActionLog!!.id)
				if (modActionLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
					respond {
						content = "The mod action log you've selected is invalid, or I can't view it. " +
								"Please attempt to resolve this and try again."
					}
					return@action
				}
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
				field {
					name = "Log publicly"
					value = when (arguments.logPublicly) {
						true -> "True"
						false -> "Disabled"
						null -> "Disabled"
					}
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

			ModerationConfigCollection().setConfig(
				ModerationConfigData(
					guild!!.id,
					arguments.enabled,
					arguments.modActionLog?.id,
					arguments.moderatorRole?.id,
					arguments.logPublicly
				)
			)

			val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

			if (utilityLog == null) {
				respond {
					content = "Consider setting a utility config to log changes to configurations."
				}
				return@action
			}

			utilityLog.createMessage {
				embed {
					moderationEmbed()
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
			} else if ((arguments.enableMessageDeleteLogs || arguments.enableMessageEditLogs) && arguments.messageLogs == null) {
				respond { content = "You must specify a channel to log deleted/edited messages to!" }
				return@action
			}

			val memberLog: TextChannel?
			if (arguments.enableMemberLogging && arguments.memberLog != null) {
				memberLog = guild!!.getChannelOfOrNull(arguments.memberLog!!.id)
				if (memberLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
					respond {
						content = "The member log you've selected is invalid, or I can't view it. " +
								"Please attempt to resolve this and try again."
					}
					return@action
				}
			}

			val messageLog: TextChannel?
			if ((arguments.enableMessageDeleteLogs || arguments.enableMessageEditLogs) && arguments.messageLogs != null) {
				messageLog = guild!!.getChannelOfOrNull(arguments.messageLogs!!.id)
				if (messageLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
					respond {
						content = "The message log you've selected is invalid, or I can't view it. " +
								"Please attempt to resolve this and try again."
					}
					return@action
				}
			}

			suspend fun EmbedBuilder.loggingEmbed() {
				title = "Configuration: Logging"
				field {
					name = "Message Delete Logs"
					value = if (arguments.enableMessageDeleteLogs && arguments.messageLogs?.mention != null) {
						arguments.messageLogs!!.mention
					} else {
						"Disabled"
					}
				}
				field {
					name = "Message Edit Logs"
					value = if (arguments.enableMessageEditLogs && arguments.messageLogs?.mention != null) {
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
					arguments.enableMessageDeleteLogs,
					arguments.enableMessageEditLogs,
					arguments.messageLogs?.id,
					arguments.enableMemberLogging,
					arguments.memberLog?.id
				)
			)

			val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

			if (utilityLog == null) {
				respond {
					content = "Consider setting a utility config to log changes to configurations."
				}
				return@action
			}

			utilityLog.createMessage {
				embed {
					loggingEmbed()
				}
			}
		}
	}

	ephemeralSubCommand(::UtilityArgs) {
		name = "utility"
		description = "Configure Lily's utility settings"

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		action {
			val utilityConfig = UtilityConfigCollection().getConfig(guild!!.id)

			if (utilityConfig != null) {
				respond {
					content = "You already have a utility configuration set. " +
							"Please clear it before attempting to set a new one."
				}
				return@action
			}

			var utilityLog: TextChannel? = null
			if (arguments.utilityLogChannel != null) {
				utilityLog = guild!!.getChannelOfOrNull(arguments.utilityLogChannel!!.id)
				if (utilityLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
					respond {
						content = "The utility log you've selected is invalid, or I can't view it. " +
								"Please attempt to resolve this and try again."
					}
					return@action
				}
			}

			suspend fun EmbedBuilder.utilityEmbed() {
				title = "Configuration: Utility"
				field {
					name = "Disable log uploading"
					value = if (arguments.disableLogUploading) {
						"True"
					} else {
						"false"
					}
				}
				field {
					name = "Utility Log"
					value = if (arguments.utilityLogChannel != null) {
						"${arguments.utilityLogChannel!!.mention} ${arguments.utilityLogChannel!!.data.name.value}"
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
					utilityEmbed()
				}
			}

			UtilityConfigCollection().setConfig(
				UtilityConfigData(
					guild!!.id,
					arguments.disableLogUploading,
					arguments.utilityLogChannel?.id
				)
			)

			utilityLog?.createMessage {
				embed {
					utilityEmbed()
				}
			}
		}
	}

	ephemeralSubCommand(::ClearArgs) {
		name = "clear"
		description = "Clear a config type"

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		action {
			suspend fun logClear() {
				val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

				if (utilityLog == null) {
					respond {
						content = "Consider setting a utility config to log changes to configurations."
					}
					return
				}

				utilityLog.createMessage {
					embed {
						title = "Configuration Cleared: ${arguments.config[0]}${
							arguments.config.substring(1, arguments.config.length).lowercase()
						}"
						footer {
							text = "Config cleared by ${user.asUser().tag}"
							icon = user.asUser().avatar?.url
						}
					}
				}
			}

			when (arguments.config) {
				ConfigType.MODERATION.name -> {
					ModerationConfigCollection().getConfig(guild!!.id) ?: run {
						respond {
							content = "No moderation configuration exists to clear!"
						}
						return@action
					}

					logClear()

					ModerationConfigCollection().clearConfig(guild!!.id)
					respond {
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
					LoggingConfigCollection().getConfig(guild!!.id) ?: run {
						respond {
							content = "No logging configuration exists to clear!"
						}
						return@action
					}

					logClear()

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
					SupportConfigCollection().getConfig(guild!!.id) ?: run {
						respond {
							content = "No support configuration exists to clear!"
						}
						return@action
					}

					logClear()

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

				ConfigType.UTILITY.name -> {
					UtilityConfigCollection().getConfig(guild!!.id) ?: run {
						respond {
							content = "No utility configuration exists to clear"
						}
						return@action
					}

					logClear()

					UtilityConfigCollection().clearConfig(guild!!.id)
					respond {
						embed {
							title = "Config cleared: Utility"
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
					UtilityConfigCollection().clearConfig(guild!!.id)
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
		}
	}

	ephemeralSubCommand(::ViewArgs) {
		name = "view"
		description = "View the current config that you have set"

		check {
			anyGuild()
			hasPermission(Permission.ManageGuild)
		}

		action {
			when (arguments.config) {
				ConfigType.MODERATION.name -> {
					val config = ModerationConfigCollection().getConfig(guild!!.id)
					if (config == null) {
						respond {
							content = "There is no moderation config for this guild"
						}
						return@action
					}

					respond {
						embed {
							title = "Current moderation config"
							description = "This is the current moderation config for this guild"
							field {
								name = "Enabled/Disabled"
								value = if (config.enabled) "Enabled" else "Disabled"
							}
							field {
								name = "Moderators"
								value = config.role?.let { guild!!.getRoleOrNull(it)?.mention } ?: "Disabled"
							}
							field {
								name = "Action log"
								value = config.channel?.let { guild!!.getChannelOrNull(it)?.mention } ?: "Disabled"
							}
							field {
								name = "Log publicly"
								value = when (config.publicLogging) {
									true -> "True"
									false -> "Disabled"
									null -> "Disabled"
								}
							}
							timestamp = Clock.System.now()
						}
					}
				}

				ConfigType.LOGGING.name -> {
					val config = LoggingConfigCollection().getConfig(guild!!.id)
					if (config == null) {
						respond {
							content = "There is no logging config for this guild"
						}
						return@action
					}

					respond {
						embed {
							title = "Current logging config"
							description = "This is the current logging config for this guild"
							field {
								name = "Message delete logs"
								value = if (config.enableMessageDeleteLogs) {
									"Enabled\n" +
											"${guild!!.getChannel(config.messageChannel!!).mention} (" +
											"${guild!!.getChannel(config.messageChannel).name })"
								} else {
									"Disabled"
								}
							}
							field {
								name = "Message edit logs"
								value = if (config.enableMessageEditLogs) {
									"Enabled\n" +
											"${guild!!.getChannel(config.messageChannel!!).mention } (" +
											"${guild!!.getChannel(config.messageChannel).name })"
								} else {
									"Disabled"
								}
							}
							field {
								name = "Member logs"
								value = if (config.enableMemberLogs) {
									"Enabled\n" +
											"${guild!!.getChannel(config.memberLog!!).mention } (" +
											"${guild!!.getChannel(config.memberLog).name })"
								} else {
									"Disabled"
								}
							}
							timestamp = Clock.System.now()
						}
					}
				}

				ConfigType.SUPPORT.name -> {
					val config = SupportConfigCollection().getConfig(guild!!.id)
					if (config == null) {
						respond {
							content = "There is no support config for this guild"
						}
						return@action
					}

					respond {
						embed {
							title = "Current support config"
							description = "This is the current support config for this guild"
							field {
								name = "Enabled/Disabled"
								value = if (config.enabled) "Enabled" else "Disabled"
							}
							field {
								name = "Channel"
								value = "${config.channel?.let { guild!!.getChannelOrNull(it)?.mention }} " +
										"${config.channel?.let { guild!!.getChannelOrNull(it)?.name }}"
							}
							field {
								name = "Role"
								value = "${config.role?.let { guild!!.getRoleOrNull(it)?.mention }} " +
										"${config.role?.let { guild!!.getRoleOrNull(it)?.name }}"
							}
							field {
								name = "Custom message"
								value =
									if (config.message != null) "${config.message.substring(0, 500)} ..." else "Default"
							}
							timestamp = Clock.System.now()
						}
					}
				}

				ConfigType.UTILITY.name -> {
					val config = UtilityConfigCollection().getConfig(guild!!.id)
					if (config == null) {
						respond {
							content = "There is no utility config for this guild"
						}
						return@action
					}

					respond {
						embed {
							title = "Current utility config"
							description = "This is the current utility config for this guild"
							field {
								name = "Log uploading"
								value = if (config.disableLogUploading) "Disabled" else "Enabled"
							}
							field {
								name = "Channel"
								value =
									"${
										config.utilityLogChannel?.let { guild!!.getChannelOrNull(it)?.mention } ?: "None"
									} ${config.utilityLogChannel?.let { guild!!.getChannelOrNull(it)?.name } ?: ""}"
							}
							timestamp = Clock.System.now()
						}
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

	val logPublicly by optionalBoolean {
		name = "log-publicly"
		description = "Whether to log moderation publicly or not."
	}
}

class LoggingArgs : Arguments() {
	val enableMessageDeleteLogs by boolean {
		name = "enable-delete-logs"
		description = "Enable logging of message deletions"
	}

	val enableMessageEditLogs by boolean {
		name = "enable-edit-logs"
		description = "Enable logging of message edits"
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

class UtilityArgs : Arguments() {
	val disableLogUploading by boolean {
		name = "disable-log-uploading"
		description = "Enable or disable log uploading for this guild"
	}
	val utilityLogChannel by optionalChannel {
		name = "utility-log"
		description = "The channel to log various utility actions too."
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
			"utility" to ConfigType.UTILITY.name,
			"all" to ConfigType.ALL.name
		)
	}
}

class ViewArgs : Arguments() {
	val config by stringChoice {
		name = "config-type"
		description = "The type of config to clear"
		choices = mutableMapOf(
			"support" to ConfigType.SUPPORT.name,
			"moderation" to ConfigType.MODERATION.name,
			"logging" to ConfigType.LOGGING.name,
			"utility" to ConfigType.UTILITY.name,
		)
	}
}
