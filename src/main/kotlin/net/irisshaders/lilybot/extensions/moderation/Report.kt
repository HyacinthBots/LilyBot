@file:OptIn(UnsafeAPI::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeMessageCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialMessageCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.ModalParentInteractionBehavior
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import dev.kord.rest.request.RestRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.database.collections.LoggingConfigCollection
import net.irisshaders.lilybot.database.collections.ModerationConfigCollection
import net.irisshaders.lilybot.database.entities.ModerationConfigData
import net.irisshaders.lilybot.extensions.config.ConfigType
import net.irisshaders.lilybot.utils.configPresent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The message reporting feature in the bot.
 *
 * @since 2.0
 */
class Report : Extension() {
	override val name = "report"

	override suspend fun setup() {
		reportMessageCommand()
		reportSlashCommand()
	}
}

/**
 * The message command for the report functions in the bot.
 *
 * @author NoComment1105
 * @since 3.3.0
 */
suspend inline fun Report.reportMessageCommand() = unsafeMessageCommand {
	name = "Report"
	locking = true // To prevent the command from being run more than once concurrently

	initialResponse = InitialMessageCommandResponse.None

	check {
		anyGuild()
		configPresent()
	}

	action {
		val loggingConfig = LoggingConfigCollection().getConfig(guild!!.id)!!
		val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)!!
		val messageLog = guild!!.getChannelOf<GuildMessageChannel>(loggingConfig.messageChannel)
		val reportedMessage: Message
		val messageAuthor: Member?

		try {
			reportedMessage = event.interaction.getTarget()
			messageAuthor = reportedMessage.getAuthorAsMember()
		} catch (e: KtorRequestException) {
			ackEphemeral()
			respondEphemeral {
				content = "Sorry, I can't properly access this message. Please ping the moderators instead."
			}
			return@action
		} catch (e: EntityNotFoundException) {
			ackEphemeral()
			respondEphemeral {
				content = "Sorry, I can't find this message. Please ping the moderators instead."
			}
			return@action
		}

		createReportModal(
			event.interaction as ModalParentInteractionBehavior,
			user,
			moderationConfig,
			messageLog,
			reportedMessage,
			messageAuthor
		)
	}
}

/**
 * The slash command variant of [reportMessageCommand]. This is primarily here for mobile users, since context menu's
 * don't properly exist in the mobile apps yet. Should be removed when they're introduced.
 *
 * @author NoComment1105
 * @since 3.3.0
 */
suspend inline fun Report.reportSlashCommand() = unsafeSlashCommand(::ManualReportArgs) {
	name = "manual-report"
	description = "Report a message, using a link instead of the message command"
	locking = true // To prevent the command from being run more than once concurrently

	initialResponse = InitialSlashCommandResponse.None

	check {
		anyGuild()
		configPresent(ConfigType.LOGGING, ConfigType.MODERATION)
	}

	action {
		val loggingConfig = LoggingConfigCollection().getConfig(guild!!.id)!!
		val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)!!
		val messageLog = guild!!.getChannelOf<GuildMessageChannel>(loggingConfig.messageChannel)
		val channel: MessageChannel
		val reportedMessage: Message
		val messageAuthor: Member?

		if (arguments.message.contains("/").not()) {
			ackEphemeral()
			respondEphemeral {
				content = "The URL provided was malformed and the message could not be found!"
			}
			return@action
		}

		try {
			// Since this takes in a discord URL, we have to parse the channel and message ID out of it to use
			channel = guild?.getChannel(
				Snowflake(arguments.message.split("/")[5])
			) as MessageChannel
			reportedMessage = channel.getMessage(Snowflake(arguments.message.split("/")[6]))
			messageAuthor = reportedMessage.getAuthorAsMember()
		} catch (e: KtorRequestException) { // In the event of a report in a channel the bot can't see
			ackEphemeral()
			respondEphemeral {
				content = "Sorry, I can't properly access this message. Please ping the moderators instead."
			}
			return@action
		} catch (e: EntityNotFoundException) { // In the event of the message already being deleted.
			ackEphemeral()
			respondEphemeral {
				content = "Sorry, I can't find this message. Please ping the moderators instead."
			}
			return@action
		}

		createReportModal(
			event.interaction as ModalParentInteractionBehavior,
			user,
			moderationConfig,
			messageLog,
			reportedMessage,
			messageAuthor
		)
	}
}

/**
 * A function to contain common code between [reportMessageCommand] and [reportSlashCommand].
 *
 * @param inputInteraction The interaction to create a modal in response to
 * @param user The user who created the [inputInteraction]
 * @param config The configuration from the database for the guild
 * @param messageLog The channel for the guild that deleted messages are logged to
 * @param reportedMessage The message that was reported
 * @param messageAuthor The author of the reported message
 * @author tempest15
 * @since 3.3.0
 */
suspend fun createReportModal(
	inputInteraction: ModalParentInteractionBehavior,
	user: UserBehavior,
	config: ModerationConfigData,
	messageLog: GuildMessageChannel,
	reportedMessage: Message,
	messageAuthor: Member?,
) {
	val modal = inputInteraction.modal("Report a message", "reportModal") {
		actionRow {
			textInput(TextInputStyle.Paragraph, "reason", "Why are you reporting this message?") {
				placeholder = "It violates rule X!"
			}
		}
	}

	val interaction = modal.kord.waitFor<ModalSubmitInteractionCreateEvent>(120.seconds.inWholeMilliseconds) {
		interaction.modalId == "reportModal"
	}?.interaction

	if (interaction == null) {
		modal.createEphemeralFollowup {
			embed {
				description = "Report timed out"
			}
		}
		return
	}

	val reason = interaction.textInputs["reason"]!!.value!!
	val modalResponse = interaction.deferEphemeralResponse()

	createReport(
		user,
		messageLog,
		messageAuthor,
		reportedMessage,
		config.team,
		config.channel,
		reason,
		modalResponse
	)
}

/**
 * Create an embed in the [messageLog] for moderators to respond to with appropriate action.
 *
 * @param user The user that reported the message
 * @param messageLog The channel to send the report embed to
 * @param messageAuthor The author of the reported message
 * @param reportedMessage The message being reported
 * @param moderatorRole The role to ping when a report is submitted
 * @param modActionLog The channel so send punishment logs too
 * @param reportReason The reason provided from the modal for the report
 * @param modalResponse The modal interaction for the message
 * @author MissCorruption
 * @since 2.0
 */
private suspend inline fun createReport(
	user: UserBehavior,
	messageLog: GuildMessageChannel,
	messageAuthor: Member?,
	reportedMessage: Message,
	moderatorRole: Snowflake,
	modActionLog: Snowflake,
	reportReason: String?,
	modalResponse: DeferredEphemeralMessageInteractionResponseBehavior
) {
	var reportResponse: EphemeralMessageInteractionResponse? = null
	var reportEmbed: Message? = null

	reportResponse = modalResponse.respond {
		content = "Would you like to report this message? This will ping moderators, and false reporting will be " +
				"treated as spam and punished accordingly"
		components {
			ephemeralButton(0) {
				label = "Yes"
				style = ButtonStyle.Success

				action {
					reportResponse?.edit {
						content = "Message reported to staff"
						components { removeAll() }

						messageLog.createMessage { content = "<@&$moderatorRole>" }

						reportEmbed = messageLog.createMessage {
							embed {
								title = "Message reported"
								description = "A message was reported in ${reportedMessage.getChannel().mention}"
								field {
									name = "Message Content:"
									value =
										reportedMessage.content.ifEmpty {
											"Failed to get content of message"
										}
									inline = true
								}
								field {
									name = "Message Author:"
									value =
										reportedMessage.author?.mention ?: "Failed to get author of message"
								}
								field {
									name = "Report reason:"
									value = reportReason ?: "No reason provided"
								}
								footer {
									text = "Reported by: ${user.asUser().tag}"
									icon = user.asUser().avatar?.url
								}
								timestamp = Clock.System.now()
								color = DISCORD_RED
							}
							components {
								linkButton {
									label = "Jump to message"
									url = reportedMessage.getJumpUrl()
								}
								ephemeralButton(0) {
									label = "Delete the reported message"
									style = ButtonStyle.Danger

									action {
										try {
											reportedMessage.delete("Deleted via report.")
											// Remove components (buttons) to prevent errors if pressed after action was taken
											reportResponse?.edit { components { removeAll() } }
										} catch (e: RestRequestException) {
											respond {
												content =
													"The message you tried to delete may have already been deleted!"
											}
										}
									}
								}

								ephemeralSelectMenu(1) {
									placeholder = "Select a quick-action"
									option(
										label = "10-Minute Timeout",
										value = "10-timeout",
									) {
										description = "Timeout the user for ten minutes."
									}
									option(
										label = "20-Minute Timeout",
										value = "20-timeout",
									) {
										description = "Timeout the user for 20 minutes."
									}
									option(
										label = "30-Minute Timeout",
										value = "30-timeout",
									) {
										description = "Timeout the user for 30 minutes."
									}
									option(
										label = "Kick the user.",
										value = "kick-user",
									) {
										description = "Kick the user from the server."
									}
									option(
										label = "Soft-ban the user.",
										value = "soft-ban-user",
									) {
										description = "Soft-ban the user and delete all their messages."
									}
									option(
										label = "Ban the user.",
										value = "ban-user",
									) {
										description = "Ban the user and delete their messages."
									}
									action {
										val actionLog = guild?.getChannel(modActionLog) as GuildMessageChannelBehavior
										when (this.selected[0]) {
											"10-timeout" -> {
												reportEmbed?.edit { components { removeAll() } }
												guild?.getMember(messageAuthor!!.id)?.edit {
													timeoutUntil = Clock.System.now().plus(Duration.parse("PT10M"))
												}
												respond {
													content = "Timed out user for 10 minutes"
												}
												messageAuthor!!.asUser().dm {
													embed {
														title = "You have been timed out in ${guild?.fetchGuild()?.name}"
														description = "**Duration:**\n10 minutes\n**Reason:**\nTimed-out via report"
													}
												}
												quickTimeoutEmbed(actionLog, messageAuthor.asUser(), 10)
											}
											"20-timeout" -> {
												reportEmbed?.edit { components { removeAll() } }
												guild?.getMember(messageAuthor!!.id)?.edit {
													timeoutUntil = Clock.System.now().plus(Duration.parse("PT20M"))
												}
												respond {
													content = "Timed out user for 20 minutes"
												}
												messageAuthor!!.asUser().dm {
													embed {
														title = "You have been timed out in ${guild?.fetchGuild()?.name}"
														description = "**Duration:**\n20 minutes\n**Reason:**\nTimed-out via report"
													}
												}
												quickTimeoutEmbed(actionLog, messageAuthor.asUser(), 20)
											}
											"30-timeout" -> {
												reportEmbed?.edit { components { removeAll() } }
												guild?.getMember(messageAuthor!!.id)?.edit {
													timeoutUntil = Clock.System.now().plus(Duration.parse("PT30M"))
												}
												respond {
													content = "Timed out user for 30 minutes"
												}
												messageAuthor!!.asUser().dm {
													embed {
														title = "You have been timed out in ${guild?.fetchGuild()?.name}"
														description = "**Duration:**\n30 minutes\n**Reason:**\nTimed-out via report"
													}
												}
												quickTimeoutEmbed(actionLog, messageAuthor.asUser(), 30)
											}
											"kick-user" -> {
												reportEmbed?.edit { components { removeAll() } }
												messageAuthor!!.asUser().dm {
													embed {
														title = "You have been kicked from ${guild?.fetchGuild()?.name}"
														description = "**Reason:**\nKicked via report"
													}
												}
												messageAuthor.kick(reason = "Kicked via report")
												quickLogEmbed("Kicked a User", actionLog, messageAuthor.asUser())
											}
											"soft-ban-user" -> {
												reportEmbed?.edit { components { removeAll() } }
												messageAuthor!!.asUser().dm {
													embed {
														title = "You have been soft-banned from ${guild?.fetchGuild()?.name}"
														description = "**Reason:**\nSoft-banned via report\n\n" +
																"You are free to rejoin without the need to be unbanned"
													}
												}
												messageAuthor.ban {
													this.reason = "Soft-Banned via report."
													this.deleteMessagesDays = 1
												}
												reportedMessage.getGuild().unban(messageAuthor.id, reason = "Soft-ban")
												quickLogEmbed("Soft-Banned a User", actionLog, messageAuthor.asUser())
											}
											"ban-user" -> {
												reportEmbed?.edit { components { removeAll() } }
												messageAuthor!!.asUser().dm {
													embed {
														title = "You have been banned from ${guild?.fetchGuild()?.name}"
														description = "**Reason:**\nBanned via report"
													}
												}
												messageAuthor.ban {
													this.reason = "Banned via report"
													this.deleteMessagesDays = 1
												}
												quickLogEmbed("Banned a user!", actionLog, messageAuthor.asUser())
											}
										}
									}
								}
							}
						}
					}
				}
			}
			ephemeralButton(0) {
				label = "No"
				style = ButtonStyle.Danger

				action {
					reportResponse?.edit {
						content = "Message not reported."
						components { removeAll() }
					}
				}
			}
		}
	}
}

/**
 * A quick function for creating timeout embeds after actions have been performed.
 *
 * @param actionLog The channel for sending the embed too
 * @param user The user being sanctioned
 * @param duration The time they're to be timed out for
 * @return The embed
 * @author MissCorruption
 * @since 2.0
 */
private suspend inline fun quickTimeoutEmbed(
	actionLog: GuildMessageChannelBehavior,
	user: User,
	duration: Int
) = actionLog.createEmbed {
	title = "Timeout"

	field {
		name = "User"
		value = "${user.tag}\n${user.id}"
		inline = false
	}
	field {
		name = "Duration"
		value = "$duration minutes \n ${Clock.System.now().plus(Duration.parse("PT${duration}M"))}"
		inline = false
	}
	field {
		name = "Reason"
		value = "Timed-out via report"
		inline = false
	}
}

/**
 * A quick function for creating embeds after actions have been performed.
 *
 * @param moderationAction The action taken by the moderator
 * @param actionLog The channel for sending the embed too
 * @param user The user being sanctioned
 * @return The embed
 * @author MissCorruption
 * @since 2.0
 */
private suspend inline fun quickLogEmbed(
	moderationAction: String,
	actionLog: GuildMessageChannelBehavior,
	user: User
) = actionLog.createEmbed {
	title = moderationAction

	field {
		name = "User"
		value = "${user.tag}\n${user.id}"
		inline = false
	}
	field {
		name = "Reason"
		value = "Via report"
		inline = false
	}
}

class ManualReportArgs : Arguments() {
	/** The link to the message being reported. */
	val message by string {
		name = "messageLink"
		description = "Link to the message to report"
	}
}
