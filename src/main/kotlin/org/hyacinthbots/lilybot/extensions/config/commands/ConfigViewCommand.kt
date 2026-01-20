package org.hyacinthbots.lilybot.extensions.config.commands

import dev.kord.common.entity.Permission
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.collections.UtilityConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigType
import org.hyacinthbots.lilybot.utils.interval
import kotlin.time.Clock

suspend fun SlashCommand<*, *, *>.configViewCommand() = ephemeralSubCommand(::ViewArgs) {
	name = Translations.Config.View.name
	description = Translations.Config.View.description

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
						content = Translations.Config.View.noModConfig.translate()
					}
					return@action
				}

				respond {
					val obj = Translations.Config.Moderation.Embed
					embed {
						title = Translations.Config.View.CurrentConfig.modTitle.translate()
						description = Translations.Config.View.CurrentConfig.modDescription.translate()
						field {
							name =
								Translations.Basic.enabled.translate() + "/" + Translations.Basic.disabled.translate()
							value =
								if (config.enabled) {
									Translations.Basic.enabled.translate()
								} else {
									Translations.Basic.disabled.translate()
								}
						}
						field {
							name = obj.moderatorsFieldName.translate()
							value = config.role?.let { guild!!.getRoleOrNull(it)?.mention }
								?: Translations.Basic.disabled.translate()
						}
						field {
							name = obj.actionLogFieldName.translate()
							value =
								config.channel?.let { guild!!.getChannelOrNull(it)?.mention }
									?: Translations.Basic.disabled.translate()
						}
						field {
							name = obj.logPubliclyFieldName.translate()
							value = when (config.publicLogging) {
								true -> Translations.Basic.enabled
								false -> Translations.Basic.disabled
								null -> Translations.Basic.disabled
							}.translate()
						}
						field {
							name = Translations.Config.Moderation.Embed.QuickTimeoutLength.name.translate()
							value = config.quickTimeoutLength.interval()
								?: Translations.Config.Moderation.Embed.QuickTimeoutLength.disabled.translate()
						}
						field {
							name = obj.warningAutoPunishmentsName.translate()
							value = when (config.autoPunishOnWarn) {
								true -> Translations.Basic.enabled
								false -> Translations.Basic.disabled
								null -> Translations.Basic.disabled
							}.translate()
						}
						field {
							name = Translations.Config.Moderation.Embed.BanDmMessage.name.translate()
							value = config.banDmMessage
								?: Translations.Config.Moderation.Embed.BanDmMessage.disabled.translate()
						}
						field {
							name = obj.autoInviteRoleName.translate()
							value = when (config.autoInviteModeratorRole) {
								true -> Translations.Basic.enabled
								false -> Translations.Basic.disabled
								null -> Translations.Basic.disabled
							}.translate()
						}
						field {
							name = obj.memberRoleChangesName.translate()
							value = when (config.logMemberRoleChanges) {
								true -> Translations.Basic.enabled
								false -> Translations.Basic.disabled
								null -> Translations.Basic.disabled
							}.translate()
						}
						timestamp = Clock.System.now()
					}
				}
			}

			ConfigType.LOGGING.name -> {
				val config = LoggingConfigCollection().getConfig(guild!!.id)
				if (config == null) {
					respond {
						content = Translations.Config.View.noLoggingConfig.translate()
					}
					return@action
				}

				respond {
					val obj = Translations.Config.Logging.Embed
					embed {
						title = Translations.Config.View.CurrentConfig.loggingTitle.translate()
						description = Translations.Config.View.CurrentConfig.loggingDescription.translate()
						field {
							name = obj.messageDeleteFieldName.translate()
							value = if (config.enableMessageDeleteLogs) {
								"${Translations.Basic.enabled.translate()}\n" +
									"* ${guild!!.getChannelOrNull(config.messageChannel!!)?.mention
										?: Translations.Config.UnableTo.mention.translate()} (" +
									"${guild!!.getChannelOrNull(config.messageChannel)?.name
										?: Translations.Config.UnableTo.name.translate()})"
							} else {
								Translations.Basic.disabled.translate()
							}
						}
						field {
							name = obj.messageEditFieldName.translate()
							value = if (config.enableMessageEditLogs) {
								"${Translations.Basic.enabled.translate()}\n" +
									"* ${guild!!.getChannelOrNull(config.messageChannel!!)?.mention
										?: Translations.Config.UnableTo.mention.translate()} (" +
									"${guild!!.getChannelOrNull(config.messageChannel)?.name
										?: Translations.Config.UnableTo.name.translate()})"
							} else {
								Translations.Basic.disabled.translate()
							}
						}
						field {
							name = obj.memberFieldName.translate()
							value = if (config.enableMemberLogs) {
								"${Translations.Basic.enabled.translate()}\n" +
									"* ${guild!!.getChannelOrNull(config.memberLog!!)?.mention
										?: Translations.Config.UnableTo.mention.translate()} (" +
									"${guild!!.getChannelOrNull(config.memberLog)?.name
										?: Translations.Config.UnableTo.name.translate()})"
							} else {
								Translations.Basic.disabled.translate()
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
						content = Translations.Config.View.noUtilityConfig.translate()
					}
					return@action
				}

				respond {
					val obj = Translations.Config.Utility.Embed
					embed {
						title = Translations.Config.View.CurrentConfig.utilityTitle.translate()
						description = Translations.Config.View.CurrentConfig.utilityDescription.translate()
						field {
							name = obj.utilityFieldName.translate()
							value =
								"${
									config.utilityLogChannel?.let { guild!!.getChannelOrNull(it)?.mention }
										?: Translations.Basic.none.translate()
								} ${config.utilityLogChannel?.let { guild!!.getChannelOrNull(it)?.name } ?: ""}"
						}
						field {
							name = obj.channelUpdates.translate()
							value = config.logChannelUpdates.toString()
						}
						field {
							name = obj.eventUpdates.translate()
							value = config.logEventUpdates.toString()
						}
						field {
							name = obj.inviteUpdates.translate()
							value = config.logInviteUpdates.toString()
						}
						field {
							name = obj.roleUpdates.translate()
							value = config.logRoleUpdates.toString()
						}
						timestamp = Clock.System.now()
					}
				}
			}
		}
	}
}

class ViewArgs : Arguments() {
	val config by stringChoice {
		name = Translations.Config.Arguments.Clear.name
		description = Translations.Config.Arguments.View.description
		choices = mutableMapOf(
			Translations.Config.Arguments.Clear.Choice.moderation to ConfigType.MODERATION.name,
			Translations.Config.Arguments.Clear.Choice.logging to ConfigType.LOGGING.name,
			Translations.Config.Arguments.Clear.Choice.utility to ConfigType.UTILITY.name,
		)
	}
}
