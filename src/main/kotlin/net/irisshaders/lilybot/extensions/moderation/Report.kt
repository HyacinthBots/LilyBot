@file:OptIn(ExperimentalTime::class)

package net.irisshaders.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.ephemeralSelectMenu
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
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
import dev.kord.core.entity.Member
import dev.kord.core.entity.Message
import dev.kord.core.entity.User
import dev.kord.core.entity.channel.MessageChannel
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.request.KtorRequestException
import dev.kord.rest.request.RestRequestException
import kotlinx.datetime.Clock
import net.irisshaders.lilybot.utils.getFromConfigPublicResponse
import net.irisshaders.lilybot.utils.userDMEmbed
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * The message reporting feature in the bot
 * @author NoComment1105
 */
@Suppress("DuplicatedCode")
class Report : Extension() {
	override val name = "report"

	override suspend fun setup() {
		ephemeralMessageCommand {
			name = "Report"
			locking = true // To prevent the command from being run more than once concurrently

			action {
				val messageLogId = getFromConfigPublicResponse("messageLogs") ?: return@action
				val actionLogId = getFromConfigPublicResponse("modActionLog") ?: return@action
				val moderatorRoleId = getFromConfigPublicResponse("moderatorsPing") ?: return@action

				val messageLog = guild?.getChannel(messageLogId) as GuildMessageChannelBehavior
				try {
					val reportedMessage = event.interaction.getTarget()
					val messageAuthor = reportedMessage.getAuthorAsMember()

					respond {
						content = "Message reported to staff."
					}

					// Call the create report function with the provided information
					createReport(user, messageLog, messageAuthor, reportedMessage, moderatorRoleId, actionLogId)

				} catch (e: KtorRequestException) {
					respond {
						content = "Sorry, I can't properly access this message. Please ping the moderators instead."
					}
				} catch (e: EntityNotFoundException) {
					respond {
						content = "Sorry, I can't find this message. Please ping the moderators instead."
					}
				}
			}
		}

		ephemeralSlashCommand(::ManualReportArgs) {
			name = "manual-report"
			description = "Manually report a message"
			locking = true

			action {
				val messageLogId = getFromConfigPublicResponse("messageLogs") ?: return@action
				val actionLogId = getFromConfigPublicResponse("modActionLog") ?: return@action
				val moderatorRoleId = getFromConfigPublicResponse("moderatorsPing") ?: return@action

				val messageLog = guild?.getChannel(messageLogId) as GuildMessageChannelBehavior

				try {
					// Since this takes in a discord URL, we have to parse the channel and message ID out of it to use
					val channel = (guild?.getChannel(
						Snowflake(arguments.message.split("/")[5])) as MessageChannel)
					val reportedMessage = channel.getMessage(Snowflake(arguments.message.split("/")[6]))
					val messageAuthor = reportedMessage.getAuthorAsMember()

					respond {
						content = "Message reported to staff"
					}

					// Create a report with the provided information
					createReport(user, messageLog, messageAuthor, reportedMessage, moderatorRoleId, actionLogId)

				} catch (e: KtorRequestException) {
					respond {
						content = "Sorry, I can't properly access this message. Please ping the moderators instead."
					}
				} catch (e: EntityNotFoundException) {
					respond {
						content = "Sorry, I can't find this message. Please ping the moderators instead."
					}
				}
			}
		}
	}

	private suspend fun createReport(
		user: UserBehavior,
		messageLog: GuildMessageChannelBehavior,
		messageAuthor: Member?,
		reportedMessage: Message,
		moderatorRole: Snowflake,
		modActionLog: Snowflake
	) {
		messageLog.createMessage { content = "<@&${moderatorRole}>" }

		messageLog.createEmbed {
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
				ephemeralButton(row = 0) {
					label = "Delete the reported message"
					style = ButtonStyle.Danger

					action {
						// TODO once modals become a thing, avoid just consuming this error
						try {
							reportedMessage.delete(reason = "Deleted via report.")
						} catch (e: RestRequestException) {
							messageLog.createMessage {
								content = "The message you tried to delete may have already been deleted!"
							}
						}
					}
				}

				ephemeralSelectMenu(row = 1) {
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
					)
					{
						description = "Timeout the user for 30 minutes."
					}
					option(
						label = "Kick the user.",
						value = "kick-user",
					)
					{
						description = "Kick the user from the server."
					}
					option(
						label = "Soft-ban the user.",
						value = "soft-ban-user",
					)
					{
						description = "Soft-ban the user and delete all their messages."
					}
					option(
						label = "Ban the user.",
						value = "ban-user",
					)
					{
						description = "Ban the user and delete their messages."
					}
					action {
						val actionLog = guild?.getChannel(modActionLog) as GuildMessageChannelBehavior
						when (this.selected[0]) {
							"10-timeout" -> {
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

	private suspend fun quickTimeoutEmbed(actionLog: GuildMessageChannelBehavior, user: User, duration: Int): Message {
		return actionLog.createEmbed {
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
	}

	private suspend fun quickLogEmbed(moderationAction: String,
									  actionLog: GuildMessageChannelBehavior, user: User): Message {
		return actionLog.createEmbed {
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
	}

	inner class ManualReportArgs : Arguments() {
		val message by string {
			name = "messageLink"
			description = "Link to the message to report"
		}
	}
}
