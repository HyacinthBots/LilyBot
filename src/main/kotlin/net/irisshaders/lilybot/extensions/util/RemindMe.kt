package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.ephemeralSlashCommand
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.annotations.UnsafeAPI
import com.kotlindiscord.kord.extensions.modules.unsafe.extensions.unsafeSlashCommand
import com.kotlindiscord.kord.extensions.modules.unsafe.types.InitialSlashCommandResponse
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import com.kotlindiscord.kord.extensions.utils.waitFor
import dev.kord.common.entity.TextInputStyle
import dev.kord.core.behavior.channel.GuildMessageChannelBehavior
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.interaction.modal
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.event.interaction.ModalSubmitInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.builder.message.modify.embed
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.utils.DatabaseHelper
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// TODO Migrate the current DB reminders over to the new system
/** The class that contains the reminding functions in the bot. */
class RemindMe : Extension() {
	override val name = "remind-me"

	/** The timer for checking reminders. */
	private val scheduler = Scheduler()

	/** The task to attach the [scheduler] to. */
	private lateinit var task: Task

	@OptIn(UnsafeAPI::class)
	override suspend fun setup() {
		/** Set the task to run every 30 seconds. */
		task = scheduler.schedule(30, pollingSeconds = 30, repeat = true, callback = ::postReminders)

		/**
		 * The command for reminders
		 *
		 * @since 3.3.2
		 * @author NoComment1105
		 */
		publicSlashCommand(::RemindArgs) {
			name = "remind"
			description = "Remind me after a certain amount of time"

			check {
				anyGuild()
			}

			action {
				val setTime = Clock.System.now()
				val remindTime = Clock.System.now().plus(arguments.time, TimeZone.UTC)
				val duration = arguments.time

				val response = respond {
					content = if (arguments.customMessage.isNullOrEmpty()) {
						"${if (arguments.repeating) "Repeating" else ""} Reminder set!\nI will remind you ${
							remindTime.toDiscord(TimestampType.RelativeTime)
						} at ${remindTime.toDiscord(TimestampType.ShortTime)} everyday unless cancelled. Use" +
								"`/remove-reminder` to cancel"
					} else {
						"${if (arguments.repeating) "Repeating" else ""} Reminder set with message ${
							arguments.customMessage
						}!\nI will remind you ${
							remindTime.toDiscord(TimestampType.RelativeTime)
						} at ${remindTime.toDiscord(TimestampType.ShortTime)}. That's `${
							Duration.parse(duration.toString())
						}` after this message was sent."
					}
				}

				var counter = 0
				DatabaseHelper.getReminders().forEach {
					if (it.userId == user.id) {
						counter += 1
					}
				}
				counter++ // Add one to the final counter, since we're adding a new one to the list of reminders

				DatabaseHelper.setReminder(
					setTime,
					guild!!.id,
					user.id,
					channel.id,
					remindTime,
					response.message.getJumpUrl(),
					arguments.customMessage,
					arguments.repeating,
					counter
				)
			}
		}

		ephemeralSlashCommand {
			name = "reminders"
			description = "See the reminders you have set for yourself in this guild."

			check {
				anyGuild()
			}

			action {
				respond {
					embed {
						title = "Your reminders"
						description = "These are the reminders you have set in this guild"
						field {
							value = userReminders(event)
						}
					}
				}
			}
		}

		unsafeSlashCommand {
			name = "remove-reminder"
			description = "Remove a reminder you have set yourself"

			initialResponse = InitialSlashCommandResponse.None

			check {
				anyGuild()
			}

			action {
				val reminders = DatabaseHelper.getReminders()

				val modal = event.interaction.modal("Delete a Reminder", "deleteModal") {
					actionRow {
						textInput(TextInputStyle.Short, "reminder", "Reminder Number") {
							placeholder = "Use `/reminders` to find out the reminder number for deletion"
						}
					}
				}
				val interaction =
					modal.kord.waitFor<ModalSubmitInteractionCreateEvent>(120.seconds.inWholeMilliseconds) {
						interaction.modalId == "deleteModal"
					}?.interaction

				if (interaction == null) {
					modal.createEphemeralFollowup {
						embed {
							description = "Reminder timed out"
						}
					}
					return@action
				}

				val id = interaction.textInputs["reminder"]!!.value!!
				val modalResponse = interaction.deferEphemeralResponse()

				reminders.forEach {
					if (it.id == id.toInt()) {
						return@forEach
					} else {
						modalResponse.respond {
							embed {
								title = "Invalid reminder number."
								description = "Use `/reminders` to see your reminders!"
							}
						}
						return@action
					}
				}

				modalResponse.respond {
					var response = ""
					reminders.forEach {
						if (it.userId == interaction.user.id && it.id == id.toInt()) {
							response = "Reminder ${it.id}\nTime set: ${
								it.initialSetTime.toDiscord(TimestampType.ShortDateTime)
							},\nTime until " +
									"reminder: ${it.remindTime.toDiscord(TimestampType.RelativeTime)} (${
										it.remindTime.toDiscord(TimestampType.ShortDateTime)
									}),\nCustom Message: ${
										it.customMessage ?: "none"
									}\n---\n"

							DatabaseHelper.removeReminder(it.guildId, it.userId, it.id)
						}
					}

					embed {
						title = "Reminder deleted"
						field {
							name = "Reminder"
							value = response
						}
					}
				}
			}
		}
	}

	private suspend fun userReminders(event: ChatInputCommandInteractionCreateEvent): String {
		val reminders = DatabaseHelper.getReminders()
		var response = ""
		reminders.forEach {
			if (it.userId == event.interaction.user.id && it.guildId == guildFor(event)!!.id) {
				response +=
					"Reminder ID: ${it.id}\nTime set: ${it.initialSetTime.toDiscord(TimestampType.ShortDateTime)},\nTime until " +
							"reminder: ${it.remindTime.toDiscord(TimestampType.RelativeTime)} (${
								it.remindTime.toDiscord(TimestampType.ShortDateTime)
							}),\nCustom Message: ${
								it.customMessage ?: "none"
							}\n---\n"
			}
		}

		if (response.isEmpty()) {
			response = "You have no reminders set."
		}

		return response
	}

	/**
	 * Sends reminders if the time for the reminder to be sent has passed.
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	private suspend fun postReminders() {
		val reminders = DatabaseHelper.getReminders()

		reminders.forEach {
			if (it.remindTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0) {
				val channel = kord.getGuild(it.guildId)!!.getChannel(it.channelId) as GuildMessageChannelBehavior
				if (it.customMessage.isNullOrEmpty()) {
					channel.createMessage {
						content = if (it.repeating!!) {
							"Repeating reminder for <@${it.userId}> set ${
								it.initialSetTime.toDiscord(
									TimestampType.RelativeTime
								)
							} at ${it.initialSetTime.toDiscord(TimestampType.ShortDateTime)}"
						} else {
							"Reminder for <@${it.userId}> set ${
								it.initialSetTime.toDiscord(
									TimestampType.RelativeTime
								)
							} at ${
								it.initialSetTime.toDiscord(
									TimestampType.ShortDateTime
								)
							}"
						}
						components {
							linkButton {
								label = "Jump to message"
								url = it.originalMessageUrl
							}
						}
					}
				} else {
					channel.createMessage {
						content = if (it.repeating!!) {
							"Repeating reminder for <@${it.userId}> set ${
								it.initialSetTime.toDiscord(
									TimestampType.RelativeTime
								)
							} at ${
								it.initialSetTime.toDiscord(
									TimestampType.ShortDateTime
								)
							}\n> ${it.customMessage}"
						} else {
							"Reminder for <@${it.userId}> set ${
								it.initialSetTime.toDiscord(
									TimestampType.RelativeTime
								)
							} at ${
								it.initialSetTime.toDiscord(
									TimestampType.ShortDateTime
								)
							}\n> ${it.customMessage}"
						}
						components {
							linkButton {
								label = "Jump to message"
								url = it.originalMessageUrl
							}
						}
					}
				}

				// Remove the old reminder from the database
				if (it.repeating!!) {
					DatabaseHelper.setReminder(
						Clock.System.now(),
						it.guildId,
						it.userId,
						it.channelId,
						it.remindTime.plus(30.seconds),
						it.originalMessageUrl,
						it.customMessage,
						true,
						it.id!!
					)
					DatabaseHelper.removeReminder(it.guildId, it.userId, it.id)
				} else {
					DatabaseHelper.removeReminder(it.guildId, it.userId, it.id!!)
				}
			}
		}
	}

	inner class RemindArgs : Arguments() {
		/** The time until the user should be reminded. */
		val time by coalescingDuration {
			name = "time"
			description = "How long until reminding?"
		}

		/** A custom message the user may want to provide. */
		val customMessage by optionalString {
			name = "customMessage"
			description = "Add a custom message to your reminder"
		}

		val repeating by defaultingBoolean {
			name = "repeating"
			description = "Would you like this reminder to repeat?"
			defaultValue = false
		}
	}
}
