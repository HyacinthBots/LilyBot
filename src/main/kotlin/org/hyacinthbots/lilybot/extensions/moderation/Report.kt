package org.hyacinthbots.lilybot.extensions.moderation

import com.kotlindiscord.kord.extensions.DISCORD_RED
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.string
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.ephemeralButton
import com.kotlindiscord.kord.extensions.components.forms.ModalForm
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralMessageCommand
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.types.edit
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.create.embed
import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs

/**
 * The message reporting feature in the bot.
 *
 * @since 2.0
 */
class Report : Extension() {
	override val name = "report"

	override suspend fun setup() {
		ephemeralMessageCommand(::ReportModal) {
			name = "Report"
			locking = true // To prevent the command from being run more than once concurrently

			check {
				anyGuild()
				requiredConfigs(
					ConfigOptions.MODERATION_ENABLED, ConfigOptions.MODERATOR_ROLE, ConfigOptions.ACTION_LOG
				)
			}

			action { modal ->
				val reportedMessage = event.interaction.getTargetOrNull()
				if (reportedMessage == null) {
					respond {
						content = "Sorry, I can't properly access this message. Please ping the moderators instead."
					}
					return@action
				}

				if (reportedMessage.author == event.interaction.user) {
					respond {
						content = "You may not report your own message."
					}
					return@action
				}

				val actionLog = getLoggingChannelWithPerms(ConfigOptions.ACTION_LOG, this.getGuild()!!) ?: return@action
				val config = ModerationConfigCollection().getConfig(guild!!.id) ?: return@action

				respond {
					createConfirmation(
						user,
						config.role!!,
						actionLog,
						reportedMessage,
						modal?.reason?.value ?: "No reason provided"
					)
				}
			}
		}

		ephemeralSlashCommand(::ManualReportArgs, ::ReportModal) {
			name = "manual-report"
			description = "Report a message, using a link instead of the message command"
			locking = true // To prevent the command from being run more than once concurrently

			check {
				anyGuild()
				requiredConfigs(
					ConfigOptions.MODERATION_ENABLED, ConfigOptions.MODERATOR_ROLE, ConfigOptions.ACTION_LOG
				)
			}

			action { modal ->
				val moderationConfig = ModerationConfigCollection().getConfig(guild!!.id)!!
				val modLog = guild!!.getChannelOfOrNull<GuildMessageChannel>(moderationConfig.channel!!)

				if (arguments.message.contains("/").not()) {
					respond {
						content = "The URL provided was malformed and the message could not be found!"
					}
					return@action
				}

				val channel =
					guild!!.getChannelOfOrNull<GuildMessageChannel>(Snowflake(arguments.message.split("/")[5]))

				if (channel == null) {
					respond {
						content = "Sorry, I can't properly access this message. Please ping the moderators instead."
					}
					return@action
				}

				val reportedMessage = channel.getMessageOrNull(Snowflake(arguments.message.split("/")[6]))

				if (reportedMessage == null) {
					respond {
						content = "Sorry, I can't find this message. Please ping the moderators instead."
					}
					return@action
				}

				if (reportedMessage.author == event.interaction.user) {
					respond {
						content = "You may not report your own message."
					}
					return@action
				}

				respond {
					createConfirmation(
						user,
						moderationConfig.role!!,
						modLog,
						reportedMessage,
						modal?.reason?.value ?: "No reason provided"
					)
				}
			}
		}
	}

	/**
	 * A function to apply the confirmation to a report.
	 *
	 * @param user The user who created the action
	 * @param moderatorRoleId The ID of the configured moderator role for the guild
	 * @param modLog The channel for the guild that deleted messages are logged to
	 * @param reportedMessage The message that was reported
	 * @param reason The reason for the report
	 * @author tempest15, NoComment1105
	 * @since 4.5.0
	 */
	private suspend fun FollowupMessageCreateBuilder.createConfirmation(
		user: UserBehavior,
		moderatorRoleId: Snowflake,
		modLog: GuildMessageChannel?,
		reportedMessage: Message,
		reason: String
	) {
		content = "Would you like to report this message? This will ping moderators, and false reporting will be " +
				"treated as spam and punished accordingly"
		components {
			ephemeralButton(0) {
				label = "Yes"
				style = ButtonStyle.Success

				action {
					this.edit {
						content = "Message reported to staff"
						components { removeAll() }

						modLog?.createMessage { content = "<@&$moderatorRoleId>" }

						modLog?.createMessage {
							embed {
								title = "Message reported"
								description = "A message was reported in ${
									reportedMessage.getChannelOrNull()?.mention ?: "`Unable to get channel`"
								}"
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
									value = reason
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
					this.edit {
						content = "Message not reported."
						components { removeAll() }
					}
				}
			}
		}
	}

	inner class ManualReportArgs : Arguments() {
		/** The link to the message being reported. */
		val message by string {
			name = "message-link"
			description = "Link to the message to report"
		}
	}

	inner class ReportModal : ModalForm() {
		override var title = "Report a message"

		val reason = paragraphText {
			label = "Why are you reporting this message"
			placeholder = "It violates rule X!"
		}
	}
}
