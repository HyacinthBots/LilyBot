package org.hyacinthbots.lilybot.extensions.logging.config

import dev.kord.common.entity.Permission
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.entity.channel.TextChannel
import dev.kord.rest.builder.message.EmbedBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.application.slash.SlashCommand
import dev.kordex.core.utils.botHasPermissions
import dev.kordex.modules.dev.unsafe.annotations.UnsafeAPI
import dev.kordex.modules.dev.unsafe.commands.slash.InitialSlashCommandResponse
import dev.kordex.modules.dev.unsafe.extensions.unsafeSubCommand
import org.hyacinthbots.lilybot.database.collections.LoggingConfigCollection
import org.hyacinthbots.lilybot.database.entities.LoggingConfigData
import org.hyacinthbots.lilybot.database.entities.PublicMemberLogData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.trimmedContents

@OptIn(UnsafeAPI::class)
suspend fun SlashCommand<*, *, *>.loggingCommand() = unsafeSubCommand(::LoggingArgs) {
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
				text = "Configured by ${user.asUserOrNull()?.username}"
				icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
			}
		}

		var publicMemberLogData: PublicMemberLogData? = null
		if (arguments.enablePublicMemberLogging) {
			val modalObj = PublicLoggingModal()

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
