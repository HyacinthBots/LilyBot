package org.hyacinthbots.lilybot.extensions.moderation.commands

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.UserBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.rest.builder.message.create.FollowupMessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kordex.core.DISCORD_RED
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.converters.impl.string
import dev.kordex.core.components.components
import dev.kordex.core.components.ephemeralButton
import dev.kordex.core.components.forms.ModalForm
import dev.kordex.core.components.linkButton
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.ephemeralMessageCommand
import dev.kordex.core.extensions.ephemeralSlashCommand
import dev.kordex.core.utils.getJumpUrl
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.ModerationConfigCollection
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.requiredConfigs
import org.hyacinthbots.lilybot.utils.trimmedContents
import kotlin.time.Clock

private val noAccess = Translations.Moderation.Report.noAccess.translate()

private val ownMessage = Translations.Moderation.Report.ownMessage.translate()

/**
 * The message reporting feature in the bot.
 *
 * @since 2.0
 */
class Report : Extension() {
	override val name = "report"

	override suspend fun setup() {
		ephemeralMessageCommand(::ReportModal) {
			name = Translations.Moderation.Report.MessageCommand.name
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
					respond { content = noAccess }
					return@action
				}

				if (reportedMessage.author == event.interaction.user) {
					respond { content = ownMessage }
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
						modal?.reason?.value ?: Translations.Basic.noReason.translate()
					)
				}
			}
		}

		ephemeralSlashCommand(::ManualReportArgs, ::ReportModal) {
			name = Translations.Moderation.Report.SlashCommand.name
			description = Translations.Moderation.Report.SlashCommand.description
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
						content = Translations.Moderation.Report.SlashCommand.malformedLink.translate()
					}
					return@action
				}

				val channel =
					guild!!.getChannelOfOrNull<GuildMessageChannel>(Snowflake(arguments.message.split("/")[5]))

				if (channel == null) {
					respond { content = noAccess }
					return@action
				}

				val reportedMessage = channel.getMessageOrNull(Snowflake(arguments.message.split("/")[6]))

				if (reportedMessage == null) {
					respond { content = noAccess }
					return@action
				}

				if (reportedMessage.author == event.interaction.user) {
					respond { content = ownMessage }
					return@action
				}

				respond {
					createConfirmation(
						user,
						moderationConfig.role!!,
						modLog,
						reportedMessage,
						modal?.reason?.value ?: Translations.Basic.noReason.translate()
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
		val translations = Translations.Moderation.Report.Confirmation
		content = translations.content.translate()
		components {
			ephemeralButton(0) {
				label = Translations.Basic.yes
				style = ButtonStyle.Success

				action {
					this.edit {
						content = translations.response.translate()
						components { removeAll() }

						modLog?.createMessage { content = "<@&$moderatorRoleId>" }

						modLog?.createMessage {
							embed {
								title = translations.embedTitle.translate()
								description = translations.embedDesc.translate(
									reportedMessage.getChannelOrNull()?.mention
										?: Translations.Basic.UnableTo.channel.translate()
								)

								field {
									name = translations.embedContentField.translate()
									value =
										reportedMessage.content.ifEmpty {
											Translations.Basic.UnableTo.getContents.translate()
										}.trimmedContents()!!
									inline = true
								}
								field {
									name = translations.embedAuthorField.translate()
									value =
										reportedMessage.author?.mention ?: Translations.Basic.UnableTo.author.translate()
								}
								field {
									name = translations.embedReasonField.translate()
									value = reason.trimmedContents()!!
								}
								footer {
									text = translations.embedReportedBy.translate(user.asUserOrNull()?.username)
									icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
								}
								timestamp = Clock.System.now()
								color = DISCORD_RED
							}
							components {
								linkButton {
									label = translations.jumpButton
									url = reportedMessage.getJumpUrl()
								}
							}
						}
					}
				}
			}
			ephemeralButton(0) {
				label = Translations.Basic.no
				style = ButtonStyle.Danger

				action {
					this.edit {
						content = translations.notReported.translate()
						components { removeAll() }
					}
				}
			}
		}
	}

	inner class ManualReportArgs : Arguments() {
		/** The link to the message being reported. */
		val message by string {
			name = Translations.Moderation.Report.SlashCommand.Arguments.Message.name
			description = Translations.Moderation.Report.SlashCommand.Arguments.Message.description
		}
	}

	inner class ReportModal : ModalForm() {
		override var title = Translations.Moderation.Report.Modal.title

		val reason = paragraphText {
			label = Translations.Moderation.Report.Modal.label
			placeholder = Translations.Moderation.Report.Modal.placeholder
		}
	}
}
