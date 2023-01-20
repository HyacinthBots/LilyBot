package org.hyacinthbots.lilybot.extensions.config

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingOptionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalRole
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSubCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.AutoThreadingCollection
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.SupportConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.database.entities.AutoThreadingData
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.database.entities.PublicMemberLogData
import org.hyacinthbots.lilybot.database.entities.UtilityConfigData
import org.hyacinthbots.lilybot.utils.canPingRole
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.interval
import org.hyacinthbots.lilybot.utils.trimmedContents

class Config : Extension() {
	override val name: String = "config"

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		ephemeralSlashCommand {
			name = "config"
			description = "Configure Lily's settings"

			unsafeSubCommand(::SupportArgs) {
				name = "support"
				description = "Deprecated: Configure Lily's support system"

				initialResponse = InitialSlashCommandResponse.None

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				val deprecationNotice = "This command is deprecated and will be removed in version v4.7.0! Please use" +
						" the `/autothreading` command to fully configure thread inviting for a channel!"

				action {
					val supportConfig = SupportConfigCollection().getConfig(guild!!.id)
					if (supportConfig != null) {
						ackEphemeral()
						respondEphemeral {
							content = "You already have a support configuration set. " +
									"Please clear it before attempting to set a new one.\n$deprecationNotice"
						}
						return@action
					}

					if (!arguments.enable) {
						// We don't want this getting set because it doesn't get used
						// SupportConfigCollection().setConfig(SupportConfigData(guild!!.id, false, null, null, null))
						ackEphemeral()
						respondEphemeral {
							content = "Support system disabled.\n$deprecationNotice"
						}
						return@action
					}

					if (!canPingRole(arguments.role) && arguments.role != null) {
						ackEphemeral()
						respondEphemeral {
							content =
								"I cannot use the role: ${arguments.role!!.mention}, because it is not mentionable by " +
										"regular users. Please enable this in the role settings, or use a different " +
										"role.\n$deprecationNotice"
						}
						return@action
					}

					val supportChannel: TextChannel?
					if (arguments.enable && arguments.channel != null) {
						supportChannel = guild!!.getChannelOfOrNull(arguments.channel!!.id)
						if (supportChannel?.botHasPermissions(
								Permission.ViewChannel,
								Permission.SendMessages
							) != true
						) {
							ackEphemeral()
							respondEphemeral {
								content = "The mod action log you've selected is invalid, or I can't view it. " +
										"Please attempt to resolve this and try again.\n$deprecationNotice"
							}
							return@action
						}
					}

					suspend fun EmbedBuilder.supportEmbed() {
						title = "Configuration: Support"
						description = deprecationNotice
						field {
							name = "Support Team"
							value = arguments.role?.mention ?: "Disabled"
						}
						field {
							name = "Support Channel"
							value = arguments.channel?.mention ?: "Disabled"
						}
						footer {
							text = "Configured by: ${user.asUserOrNull()?.tag}"
						}
					}

					if (arguments.customMessage) {
						val modalObj = SupportModal()

						this@unsafeSubCommand.componentRegistry.register(modalObj)

						event.interaction.modal(
							modalObj.title,
							modalObj.id
						) {
							modalObj.applyToBuilder(this, getLocale(), null)
						}

						modalObj.awaitCompletion { modalSubmitInteraction ->
							interactionResponse = modalSubmitInteraction?.deferEphemeralMessageUpdate()
						}

						interactionResponse?.createEphemeralFollowup {
							embed {
								supportEmbed()
								field {
									name = "Message"
									value = modalObj.msgInput.value!!
								}
							}
						}

						AutoThreadingCollection().setAutoThread(
							AutoThreadingData(
								guild!!.id,
								arguments.channel?.id!!,
								arguments.role?.id,
								preventDuplicates = true,
								archive = false,
								contentAwareNaming = false,
								mention = true,
								creationMessage = modalObj.msgInput.value!!,
								addModsAndRole = false
							)
						)
					} else {
						ackEphemeral()
						respondEphemeral {
							embed {
								supportEmbed()
								field {
									name = "Message"
									value = "default"
								}
							}
						}

						AutoThreadingCollection().setAutoThread(
							AutoThreadingData(
								guild!!.id,
								arguments.channel?.id!!,
								arguments.role?.id,
								preventDuplicates = true,
								archive = false,
								contentAwareNaming = false,
								mention = true,
								creationMessage = null,
								addModsAndRole = false
							)
						)
					}

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, this.getGuild()!!)

					if (utilityLog == null) {
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

				requirePermission(Permission.ManageGuild)

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

					if (!arguments.enabled) {
						ModerationConfigCollection().setConfig(
							ModerationConfigData(
								guild!!.id,
								false,
								null,
								null,
								null,
								null,
								null
							)
						)
						respond {
							content = "Moderation system disabled."
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

					if (!canPingRole(arguments.moderatorRole) && arguments.moderatorRole != null) {
						respond {
							content =
								"I cannot use the role: ${arguments.moderatorRole!!.mention}, because it is not mentionable by " +
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
						field {
							name = "Quick timeout length"
							value = arguments.quickTimeoutLength.interval() ?: "No quick timeout length set"
						}
						field {
							name = "Warning Auto-punishments"
							value = when (arguments.warnAutoPunishments) {
								true -> "Enabled"
								false -> "Disabled"
								null -> "Disabled"
							}
						}
						footer {
							text = "Configured by ${user.asUserOrNull()?.tag}"
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
							arguments.quickTimeoutLength,
							arguments.warnAutoPunishments,
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

			unsafeSubCommand(::LoggingArgs) {
				name = "logging"
				description = "Configure Lily's logging system"

				initialResponse = InitialSlashCommandResponse.None

				requirePermission(Permission.ManageGuild)

				check {
					anyGuild()
					hasPermission(Permission.ManageGuild)
				}

				action {
					val loggingConfig = LoggingConfigCollection().getConfig(guild!!.id)
					if (loggingConfig != null) {
						ackEphemeral()
						respondEphemeral {
							content = "You already have a logging configuration set. " +
									"Please clear it before attempting to set a new one."
						}
						return@action
					}

					if (arguments.enableMemberLogging && arguments.memberLog == null) {
						ackEphemeral()
						respondEphemeral {
							content = "You must specify a channel to log members joining and leaving to!"
						}
						return@action
					} else if ((arguments.enableMessageDeleteLogs || arguments.enableMessageEditLogs) &&
						arguments.messageLogs == null
					) {
						ackEphemeral()
						respondEphemeral { content = "You must specify a channel to log deleted/edited messages to!" }
						return@action
					} else if (arguments.enablePublicMemberLogging && arguments.publicMemberLog == null) {
						ackEphemeral()
						respondEphemeral {
							content = "You must specify a channel to publicly log members joining and leaving to!"
						}
						return@action
					}

					val memberLog: TextChannel?
					if (arguments.enableMemberLogging && arguments.memberLog != null) {
						memberLog = guild!!.getChannelOfOrNull(arguments.memberLog!!.id)
						if (memberLog?.botHasPermissions(Permission.ViewChannel, Permission.SendMessages) != true) {
							ackEphemeral()
							respondEphemeral {
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
							ackEphemeral()
							respondEphemeral {
								content = "The message log you've selected is invalid, or I can't view it. " +
										"Please attempt to resolve this and try again."
							}
							return@action
						}
					}

					val publicMemberLog: TextChannel?
					if (arguments.enablePublicMemberLogging && arguments.publicMemberLog != null) {
						publicMemberLog = guild!!.getChannelOfOrNull(arguments.publicMemberLog!!.id)
						if (publicMemberLog?.botHasPermissions(
								Permission.ViewChannel,
								Permission.SendMessages
							) != true
						) {
							ackEphemeral()
							respondEphemeral {
								content = "The public member log you've selected is invalid, or I can't view it. " +
										"Please attempt to resolve this and try again."
							}
							return@action
						}
					}

					suspend fun EmbedBuilder.loggingEmbed() {
						title = "Configuration: Logging"
						field {
							name = "Message Delete Logs"
							value = if (arguments.enableMessageDeleteLogs && arguments.messageLogs != null) {
								arguments.messageLogs!!.mention
							} else {
								"Disabled"
							}
						}
						field {
							name = "Message Edit Logs"
							value = if (arguments.enableMessageEditLogs && arguments.messageLogs != null) {
								arguments.messageLogs!!.mention
							} else {
								"Disabled"
							}
						}
						field {
							name = "Member Logs"
							value = if (arguments.enableMemberLogging && arguments.memberLog != null) {
								arguments.memberLog!!.mention
							} else {
								"Disabled"
							}
						}

						field {
							name = "Public Member logs"
							value = if (arguments.enablePublicMemberLogging && arguments.publicMemberLog != null) {
								arguments.publicMemberLog!!.mention
							} else {
								"Disabled"
							}
						}
						if (arguments.enableMemberLogging && arguments.publicMemberLog != null) {
							val config = LoggingConfigCollection().getConfig(guild!!.id)
							if (config != null) {
								field {
									name = "Join Message"
									value = config.publicMemberLogData?.joinMessage.trimmedContents(256)!!
								}
								field {
									name = "Leave Message"
									value = config.publicMemberLogData?.leaveMessage.trimmedContents(256)!!
								}
								field {
									name = "Ping on join"
									value = config.publicMemberLogData?.pingNewUsers.toString()
								}
							}
						}

						footer {
							text = "Configured by ${user.asUserOrNull()?.tag}"
							icon = user.asUserOrNull()?.avatar?.url
						}
					}

					var publicMemberLogData: PublicMemberLogData? = null
					if (arguments.enablePublicMemberLogging) {
						val modalObj = LoggingModal()

						this@unsafeSubCommand.componentRegistry.register(modalObj)

						event.interaction.modal(
							modalObj.title,
							modalObj.id
						) {
							modalObj.applyToBuilder(this, getLocale(), null)
						}

						modalObj.awaitCompletion { modalSubmitInteraction ->
							interactionResponse = modalSubmitInteraction?.deferEphemeralMessageUpdate()
						}

						publicMemberLogData = PublicMemberLogData(
							modalObj.ping.value == "yes",
							modalObj.joinMessage.value,
							modalObj.leaveMessage.value
						)
					}

					LoggingConfigCollection().setConfig(
						LoggingConfigData(
							guild!!.id,
							arguments.enableMessageDeleteLogs,
							arguments.enableMessageEditLogs,
							arguments.messageLogs?.id,
							arguments.enableMemberLogging,
							arguments.memberLog?.id,
							arguments.enablePublicMemberLogging,
							arguments.publicMemberLog?.id,
							publicMemberLogData
						)
					)

					if (!arguments.enablePublicMemberLogging) {
						ackEphemeral()
					}
					respondEphemeral {
						embed { loggingEmbed() }
					}

					val utilityLog = getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, guild!!)

					if (utilityLog == null) {
						respondEphemeral {
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

				requirePermission(Permission.ManageGuild)

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
							text = "Configured by ${user.asUserOrNull()?.tag}"
							icon = user.asUserOrNull()?.avatar?.url
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

				requirePermission(Permission.ManageGuild)

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
									text = "Config cleared by ${user.asUserOrNull()?.tag}"
									icon = user.asUserOrNull()?.avatar?.url
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
										text = "Config cleared by ${user.asUserOrNull()?.tag}"
										icon = user.asUserOrNull()?.avatar?.url
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
										text = "Config cleared by ${user.asUserOrNull()?.tag}"
										icon = user.asUserOrNull()?.avatar?.url
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
										text = "Config cleared by ${user.asUserOrNull()?.tag}"
										icon = user.asUserOrNull()?.avatar?.url
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
										text = "Config cleared by ${user.asUserOrNull()?.tag}"
										icon = user.asUserOrNull()?.avatar?.url
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
										text = "Configs cleared by ${user.asUserOrNull()?.tag}"
										icon = user.asUserOrNull()?.avatar?.url
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

				requirePermission(Permission.ManageGuild)

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
										value =
											config.channel?.let { guild!!.getChannelOrNull(it)?.mention } ?: "Disabled"
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
													"* ${guild!!.getChannelOrNull(config.messageChannel!!)?.mention ?: "Unable to get channel mention"} (" +
													"${guild!!.getChannelOrNull(config.messageChannel)?.name ?: "Unable to get channel name"})"
										} else {
											"Disabled"
										}
									}
									field {
										name = "Message edit logs"
										value = if (config.enableMessageEditLogs) {
											"Enabled\n" +
													"* ${guild!!.getChannelOrNull(config.messageChannel!!)?.mention ?: "Unable to get channel mention"} (" +
													"${guild!!.getChannelOrNull(config.messageChannel)?.name ?: "Unable to get channel mention"})"
										} else {
											"Disabled"
										}
									}
									field {
										name = "Member logs"
										value = if (config.enableMemberLogs) {
											"Enabled\n" +
													"* ${guild!!.getChannelOrNull(config.memberLog!!)?.mention ?: "Unable to get channel mention"} (" +
													"${guild!!.getChannelOrNull(config.memberLog)?.name ?: "Unable to get channel mention."})"
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
											if (config.message != null) {
												"${
													config.message.substring(
														0,
														500
													)
												} ..."
											} else {
												"Default"
											}
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
	}

	inner class SupportArgs : Arguments() {
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

	inner class ModerationArgs : Arguments() {
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

		val quickTimeoutLength by coalescingOptionalDuration {
			name = "quick-timeout-length"
			description = "The length of timeouts to use for quick timeouts"
		}

		val warnAutoPunishments by optionalBoolean {
			name = "warn-auto-punishments"
			description = "Whether to automatically punish users for reach a certain threshold on warns"
		}

		val logPublicly by optionalBoolean {
			name = "log-publicly"
			description = "Whether to log moderation publicly or not."
		}
	}

	inner class LoggingArgs : Arguments() {
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

		val enablePublicMemberLogging by boolean {
			name = "enable-public-member-logging"
			description =
				"Enable logging of members joining and leaving the guild with a public message and ping if enabled"
		}

		val messageLogs by optionalChannel {
			name = "message-logs"
			description = "The channel for logging message deletions"
		}

		val memberLog by optionalChannel {
			name = "member-log"
			description = "The channel for logging members joining and leaving the guild"
		}

		val publicMemberLog by optionalChannel {
			name = "public-member-log"
			description = "The channel for the public logging of members joining and leaving the guild"
		}
	}

	inner class UtilityArgs : Arguments() {
		val disableLogUploading by boolean {
			name = "disable-log-uploading"
			description = "Enable or disable log uploading for this guild"
		}
		val utilityLogChannel by optionalChannel {
			name = "utility-log"
			description = "The channel to log various utility actions too."
		}
	}

	inner class ClearArgs : Arguments() {
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

	inner class ViewArgs : Arguments() {
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

	inner class SupportModal : ModalForm() {
		override var title = "Support Message Configuration"

		val msgInput = paragraphText {
			label = "Support Message"
			placeholder = "Input the content of the message you would like sent when a support thread is created"
			required = true
		}
	}

	inner class LoggingModal : ModalForm() {
		override var title = "Public logging configuration"

		val joinMessage = paragraphText {
			label = "What would you like sent when a user joins"
			placeholder = "Welcome to the server!"
			required = true
		}

		val leaveMessage = paragraphText {
			label = "What would you like sent when a user leaves"
			placeholder = "Adiós amigo!"
			required = true
		}

		val ping = lineText {
			label = "Type `yes` to ping new users when they join"
			placeholder = "Defaults to false if input is invalid or not `yes`"
		}
	}
}
