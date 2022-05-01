package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.EphemeralInteractionContext
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.timeoutUntil
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.ban
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.interaction.followup.edit
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.entity.interaction.followup.EphemeralFollowupMessage
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.request.KtorRequestException
import dev.kord.rest.request.RestRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.configPresent
import net.irisshaders.lilybot.utils.userDMEmbed
import kotlin.time.Duration

/**
 * The message reporting feature in the bot.
 * @since 2.0
 */
class Report : Extension() {
	override val name = "report"

	override suspend fun setup() {
		ephemeralMessageCommand {
			name = "Report"
			locking = true // To prevent the command from being run more than once concurrently

			check { anyGuild() }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val messageLog = guild?.getChannel(config.messageLogs) as GuildMessageChannelBehavior
				val reportedMessage: Message
				val messageAuthor: Member?

				try {
					reportedMessage = event.interaction.getTarget()
					messageAuthor = reportedMessage.getAuthorAsMember()
				} catch (e: KtorRequestException) {
					respond {
						content = "Sorry, I can't properly access this message. Please ping the moderators instead."
					}
					return@action
				} catch (e: EntityNotFoundException) {
					respond {
						content = "Sorry, I can't find this message. Please ping the moderators instead."
					}
					return@action
				}

				confirmationMessage(
					user,
					messageLog,
					messageAuthor,
					reportedMessage,
					config.moderatorsPing,
					config.modActionLog
				)
			}
		}

		ephemeralSlashCommand(::ManualReportArgs) {
			name = "manual-report"
			description = "Manually report a message"
			locking = true

			check { anyGuild() }
			check { configPresent() }

			action {
				val config = DatabaseHelper.getConfig(guild!!.id)!!
				val messageLog = guild?.getChannel(config.messageLogs) as GuildMessageChannelBehavior
				val channel: MessageChannel
				val reportedMessage: Message
				val messageAuthor: Member?

				try {
					// Since this takes in a discord URL, we have to parse the channel and message ID out of it to use
					channel = guild?.getChannel(
						Snowflake(arguments.message.split("/")[5])
					) as MessageChannel
					reportedMessage = channel.getMessage(Snowflake(arguments.message.split("/")[6]))
					messageAuthor = reportedMessage.getAuthorAsMember()
				} catch (e: KtorRequestException) { // In the event of a report in a channel the bot can't see
					respond {
						content = "Sorry, I can't properly access this message. Please ping the moderators instead."
					}
					return@action
				} catch (e: EntityNotFoundException) { // In the event of the message already being deleted.
					respond {
						content = "Sorry, I can't find this message. Please ping the moderators instead."
					}
					return@action
				}

				confirmationMessage(
					user,
					messageLog,
					messageAuthor,
					reportedMessage,
					config.moderatorsPing,
					config.modActionLog
				)
			}
		}
	}

	/**
	 * Create an [EphemeralFollowupMessage] for the user to provide confirmation on whether they want to report the
	 * message, to save fake moderator pings.
	 *
	 * @param user The user that reported the message
	 * @param messageLog The channel to send the report embed to
	 * @param messageAuthor The author of the reported message
	 * @param reportedMessage The message being reported
	 * @param moderatorRole The role to ping when a report is submitted
	 * @param modActionLog The channel so send punishment logs too
	 * @since 3.1.0
	 */
	private suspend fun EphemeralInteractionContext.confirmationMessage(
		user: UserBehavior,
		messageLog: GuildMessageChannelBehavior,
		messageAuthor: Member?,
		reportedMessage: Message,
		moderatorRole: Snowflake,
		modActionLog: Snowflake
	) {
		var response: EphemeralFollowupMessage? = null
		response = respond {
			content = "Would you like to report this message? This will ping moderators, and false reporting" +
					" will be treated as spam and punished accordingly."
		}.edit {
			components {
				ephemeralButton(0) {
					label = "Yes"
					style = ButtonStyle.Success

					action {
						response?.edit {
							content = "Message reported to staff."
							components { removeAll() }
						}

						// Call the create report function with the provided information
						createReport(
							user,
							messageLog,
							messageAuthor,
							reportedMessage,
							moderatorRole,
							modActionLog
						)
					}
				}
				ephemeralButton(0) {
					label = "No"
					style = ButtonStyle.Danger

					action {
						response?.edit {
							content = "Message not reported."
							components { removeAll() }
						}
					}
				}
			}
		}
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
	 * @since 2.0
	 */
	private suspend fun createReport(
		user: UserBehavior,
		messageLog: GuildMessageChannelBehavior,
		messageAuthor: Member?,
		reportedMessage: Message,
		moderatorRole: Snowflake,
		modActionLog: Snowflake
	) {
		var response: Message? = null // Create this here to allow us to edit within the variable
		messageLog.createMessage { content = "<@&$moderatorRole>" }

		response = messageLog.createEmbed {
			color = DISCORD_RED
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
				name = "Message Link:"
				value = reportedMessage.getJumpUrl()
				inline = false
			}
			footer {
				text = "Reported by: ${user.asUser().tag}"
				icon = user.asUser().avatar?.url
			}
			timestamp = Clock.System.now()
		}.edit {
			components {
				ephemeralButton(0) {
					label = "Delete the reported message"
					style = ButtonStyle.Danger

					action {
						// TODO once modals become a thing, avoid just consuming this error
						try {
							reportedMessage.delete("Deleted via report.")
							// Remove components (buttons) to prevent errors if pressed after action was taken
							response?.edit { components { removeAll() } }
						} catch (e: RestRequestException) {
							messageLog.createMessage {
								content = "The message you tried to delete may have already been deleted!"
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
								// Remove components (buttons) to prevent errors if pressed after action was taken
								response?.edit { components { removeAll() } }
								guild?.getMember(messageAuthor!!.id)?.edit {
									timeoutUntil = Clock.System.now().plus(Duration.parse("PT10M"))
								}
								respond {
									content = "Timed out user for 10 minutes"
								}
								userDMEmbed(
									messageAuthor!!.asUser(),
									"You have been timed out in ${guild?.fetchGuild()?.name}",
									"**Duration:**\n10 minutes\n**Reason:**\nTimed-out via report",
									null
								)
								quickTimeoutEmbed(actionLog, messageAuthor.asUser(), 10)
							}
							"20-timeout" -> {
								// Remove components (buttons) to prevent errors if pressed after action was taken
								response?.edit { components { removeAll() } }
								guild?.getMember(messageAuthor!!.id)?.edit {
									timeoutUntil = Clock.System.now().plus(Duration.parse("PT20M"))
								}
								respond {
									content = "Timed out user for 20 minutes"
								}
								userDMEmbed(
									messageAuthor!!.asUser(),
									"You have been timed out in ${guild?.fetchGuild()?.name}",
									"**Duration:**\n20 minutes\n**Reason:**\nTimed-out via report",
									null
								)
								quickTimeoutEmbed(actionLog, messageAuthor.asUser(), 20)
							}
							"30-timeout" -> {
								// Remove components (buttons) to prevent errors if pressed after action was taken
								response?.edit { components { removeAll() } }
								guild?.getMember(messageAuthor!!.id)?.edit {
									timeoutUntil = Clock.System.now().plus(Duration.parse("PT30M"))
								}
								respond {
									content = "Timed out user for 30 minutes"
								}
								userDMEmbed(
									messageAuthor!!.asUser(),
									"You have been timed out in ${guild?.fetchGuild()?.name}",
									"**Duration:**\n30 minutes\n**Reason:**\nTimed-out via report",
									null
								)
								quickTimeoutEmbed(actionLog, messageAuthor.asUser(), 30)
							}
							"kick-user" -> {
								// Remove components (buttons) to prevent errors if pressed after action was taken
								response?.edit { components { removeAll() } }
								userDMEmbed(
									messageAuthor!!.asUser(),
									"You have been kicked from ${guild?.fetchGuild()?.name}",
									"**Reason:**\nKicked via report",
									null
								)
								messageAuthor.kick(reason = "Kicked via report")
								quickLogEmbed("Kicked a User", actionLog, messageAuthor.asUser())
							}
							"soft-ban-user" -> {
								// Remove components (buttons) to prevent errors if pressed after action was taken
								response?.edit { components { removeAll() } }
								userDMEmbed(
									messageAuthor!!.asUser(),
									"You have been soft-banned from ${guild?.fetchGuild()?.name}",
									"**Reason:**\nSoft-banned via report\n\n" +
											"You are free to rejoin without the need to be unbanned",
									null
								)
								messageAuthor.ban {
									this.reason = "Soft-Banned via report."
									this.deleteMessagesDays = 1
								}
								reportedMessage.getGuild().unban(messageAuthor.id, reason = "Soft-ban")
								quickLogEmbed("Soft-Banned a User", actionLog, messageAuthor.asUser())
							}
							"ban-user" -> {
								// Remove components (buttons) to prevent errors if pressed after action was taken
								response?.edit { components { removeAll() } }
								userDMEmbed(
									messageAuthor!!.asUser(),
									"You have been banned from ${guild?.fetchGuild()?.name}",
									"**Reason:**\nBanned via report",
									null
								)
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

	/**
	 * A quick function for creating timeout embeds after actions have been performed.
	 *
	 * @param actionLog The channel for sending the embed too
	 * @param user The user being sanctioned
	 * @param duration The time they're to be timed out for
	 * @return The embed
	 * @since 2.0
	 */
	private suspend fun quickTimeoutEmbed(
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
	 * @since 2.0
	 */
	private suspend fun quickLogEmbed(
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

	inner class ManualReportArgs : Arguments() {
		/** The link to the message being reported. */
		val message by string {
			name = "messageLink"
			description = "Link to the message to report"
		}
	}
}
