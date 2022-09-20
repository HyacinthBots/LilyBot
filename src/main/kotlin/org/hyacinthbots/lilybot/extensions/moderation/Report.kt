@file:OptIn(UnsafeAPI::class)

package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeMessageCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialMessageCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.modules.unsafe.types.ackEphemeral
import com.kotlindiscord.kord.extensions.modules.unsafe.types.respondEphemeral
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.behavior.interaction.ModalParentInteractionBehavior
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.DeferredEphemeralMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.edit
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.interaction.response.EphemeralMessageInteractionResponse
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.database.entities.ModerationConfigData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.requiredConfigs
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
		requiredConfigs(ConfigOptions.MODERATION_ENABLED, ConfigOptions.MODERATOR_ROLE, ConfigOptions.ACTION_LOG)
	}

	action {
		val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)!!
		val modLog = guild?.getChannelOf<GuildMessageChannel>(moderationConfig.channel!!)
		val reportedMessage: Message

		try {
			reportedMessage = event.interaction.getTarget()
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

		if (reportedMessage.author == event.interaction.user) {
			ackEphemeral()
			respondEphemeral {
				content = "You may not report your own message."
			}
			return@action
		}

		createReportModal(
			event.interaction as ModalParentInteractionBehavior,
			user,
			moderationConfig,
			modLog,
			reportedMessage,
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
		requiredConfigs(ConfigOptions.MODERATION_ENABLED, ConfigOptions.MODERATOR_ROLE, ConfigOptions.ACTION_LOG)
	}

	action {
		val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)!!
		val modLog = guild!!.getChannelOf<GuildMessageChannel>(moderationConfig.channel!!)
		val channel: MessageChannel
		val reportedMessage: Message

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

		if (reportedMessage.author == event.interaction.user) {
			ackEphemeral()
			respondEphemeral {
				content = "You may not report your own message."
			}
			return@action
		}

		createReportModal(
			event.interaction as ModalParentInteractionBehavior,
			user,
			moderationConfig,
			modLog,
			reportedMessage,
		)
	}
}

/**
 * A function to contain common code between [reportMessageCommand] and [reportSlashCommand].
 *
 * @param inputInteraction The interaction to create a modal in response to
 * @param user The user who created the [inputInteraction]
 * @param config The configuration from the database for the guild
 * @param modLog The channel for the guild that deleted messages are logged to
 * @param reportedMessage The message that was reported
 * @author tempest15
 * @since 3.3.0
 */
suspend fun createReportModal(
	inputInteraction: ModalParentInteractionBehavior,
	user: UserBehavior,
	config: ModerationConfigData,
	modLog: GuildMessageChannel?,
	reportedMessage: Message,
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
		modLog,
		reportedMessage,
		config.role!!,
		reason,
		modalResponse
	)
}

/**
 * Create an embed in the [modLog] for moderators to respond to with appropriate action.
 *
 * @param user The user that reported the message
 * @param modLog The channel to send the report embed to
 * @param reportedMessage The message being reported
 * @param moderatorRole The role to ping when a report is submitted
 * @param reportReason The reason provided from the modal for the report
 * @param modalResponse The modal interaction for the message
 * @author MissCorruption
 * @since 2.0
 */
private suspend inline fun createReport(
	user: UserBehavior,
	modLog: GuildMessageChannel?,
	reportedMessage: Message,
	moderatorRole: Snowflake,
	reportReason: String?,
	modalResponse: DeferredEphemeralMessageInteractionResponseBehavior
) {
	var reportResponse: EphemeralMessageInteractionResponse? = null

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

						modLog?.createMessage { content = "<@&$moderatorRole>" }

						modLog?.createMessage {
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

class ManualReportArgs : Arguments() {
	/** The link to the message being reported. */
	val message by string {
		name = "message-link"
		description = "Link to the message to report"
	}
}
