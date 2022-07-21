package net.irisshaders.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingOptionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import com.kotlindiscord.kord.extensions.utils.toDuration
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import net.irisshaders.lilybot.database.collections.RemindMeCollection
import net.irisshaders.lilybot.utils.botHasChannelPerms

/**
 * The class that contains the reminding functions in the bot.
 * @since 3.3.2
 */
class Reminders : Extension() {
	override val name = "reminds"

	/** The timer for checking reminders. */
	private val scheduler = Scheduler()

	/** The task to attach the [scheduler] to. */
	private lateinit var task: Task

	override suspend fun setup() {
		/** Set the task to run every 30 seconds. */
		task = scheduler.schedule(30, pollingSeconds = 30, repeat = true, callback = ::postReminders)

		/**
		 * The command for reminders
		 *
		 * @since 3.3.2
		 * @author NoComment1105
		 */
		publicSlashCommand {
			name = "reminder"
			description = "The parent command for all reminder commands"

			publicSubCommand(::RemindArgs) {
				name = "set"
				description = "Remind me after a certain amount of time"

				check {
					anyGuild()
					requireBotPermissions(Permission.SendMessages)
					botHasChannelPerms(Permissions(Permission.SendMessages))
				}

				action {
					val setTime = Clock.System.now()
					val remindTime = Clock.System.now().plus(arguments.time, TimeZone.UTC)
					val duration = arguments.time

					if (arguments.customMessage != null && arguments.customMessage!!.length >= 1024) {
						respond { content = "Message is too long. Message must be 1024 characters or fewer" }
						return@action
					}

					if (arguments.repeating && arguments.repeatingInterval == null) {
						respond {
							content = "You must specify a repeating interval if you are setting a repeating reminder"
						}
						return@action
					}

					val response = respond {
						content = if (arguments.customMessage.isNullOrEmpty()) {
							if (arguments.repeating) {
								"Repeating reminder set!\nI will remind you ${
									remindTime.toDiscord(TimestampType.RelativeTime)
								} at ${remindTime.toDiscord(TimestampType.ShortTime)} " +
										"everyday unless cancelled. This reminder will repeat every `${
											arguments.repeatingInterval.toString().lowercase()
												.replace("pt", "")
												.replace("p", "")
										}`. Use `/remove remove` to cancel"
							} else {
								"Reminder set!\nI will remind you ${
									remindTime.toDiscord(TimestampType.RelativeTime)
								} at ${remindTime.toDiscord(TimestampType.ShortTime)}."
							}
						} else {
							if (arguments.repeating) {
								"Repeating reminder set with message ${arguments.customMessage}!\nI will remind you ${
									remindTime.toDiscord(TimestampType.RelativeTime)
								} at ${remindTime.toDiscord(TimestampType.ShortTime)}. This reminder will repeat every `${
									arguments.repeatingInterval.toString().lowercase()
										.replace("pt", "")
										.replace("p", "")
								}`. Use `/reminder remove` to cancel"
							} else {
								"Reminder set with message ${arguments.customMessage}!\nI will remind you ${
									remindTime.toDiscord(TimestampType.RelativeTime)
								} at ${remindTime.toDiscord(TimestampType.ShortTime)}. That's `${
									duration.toDuration(TimeZone.UTC)
								}` after this message was sent."
							}
						}
					}

					var counter = 0
					RemindMeCollection().getReminders().forEach {
						if (it.userId == user.id) {
							counter += 1
						}
					}
					counter++ // Add one to the final counter, since we're adding a new one to the list of reminders

					RemindMeCollection().setReminder(
						setTime,
						guild!!.id,
						user.id,
						channel.id,
						remindTime,
						response.message.getJumpUrl(),
						arguments.customMessage,
						arguments.repeating,
						arguments.repeatingInterval,
						counter
					)
				}
			}

			/**
			 * The command that allows users to see the reminders they have set in this server.
			 *
			 * @since 3.3.4
			 * @author NoComment1105
			 */
			ephemeralSubCommand {
				name = "list"
				description = "See the reminders you have set for yourself in this guild."

				check {
					anyGuild()
				}

				action {
					respond {
						embed {
							title = "Your reminders"
							field {
								value = userReminders(event)
							}
						}
					}
				}
			}

			/**
			 * Remove reminder command. Brings up a modal to allow the user to specify the reminder to delete.
			 * @author NoComment1105
			 * @since 3.3.4
			 */
			ephemeralSubCommand(::RemoveRemindArgs) {
				name = "remove"
				description = "Remove a reminder you have set yourself"

				check {
					anyGuild()
				}

				action {
					val reminders = RemindMeCollection().getReminders()

					var response = ""

					reminders.forEach {
						if (it.guildId == guild?.id && it.userId == user.id && it.id == arguments.reminder) {
							response = "Reminder ${it.id}\nTime set: ${
								it.initialSetTime.toDiscord(TimestampType.ShortDateTime)
							},\nTime until " +
									"reminder: ${it.remindTime.toDiscord(TimestampType.RelativeTime)} (${
										it.remindTime.toDiscord(TimestampType.ShortDateTime)
									}),\nCustom Message: ${
										if (it.customMessage != null && it.customMessage.length >= 1024) {
											it.customMessage.substring(0..1000)
										} else {
											it.customMessage ?: "none"
										}
									}\n---\n"

							val messageId = Snowflake(it.originalMessageUrl.split("/")[6])
							RemindMeCollection().removeReminder(guild!!.id, user.id, arguments.reminder)
							@Suppress("DuplicatedCode")
							val message = this@ephemeralSubCommand.kord.getGuild(it.guildId)!!
								.getChannelOf<GuildMessageChannel>(it.channelId)
								.getMessage(messageId)
							message.edit {
								content = "${message.content} ${
									if (it.repeating) "**Repeating" else "**"
								} Reminder cancelled.**"
							}
						}
					}

					if (response.isEmpty()) {
						response += "Reminder not found. Please use `/reminder list` to find out the correct " +
								"reminder number"
					}

					respond {
						embed {
							title = "Reminder cancelled"
							field {
								name = "Reminder"
								value = response
							}
						}
					}
				}
			}

			ephemeralSubCommand(::RemoveAllArgs) {
				name = "remove-all"
				description = "Remove all of a specific type of reminder that you have set for this guild"

				check {
					anyGuild()
				}

				action {
					val reminders = RemindMeCollection().getReminders()

					// FIXME Duplicaten't the code
					@Suppress("DuplicatedCode")
					reminders.forEach {
						when (arguments.reminderType) {
							"all" -> {
								if (it.guildId == guild?.id && it.userId == user.id) {
									RemindMeCollection().removeReminder(guild!!.id, user.id, it.id)
									val messageId = Snowflake(it.originalMessageUrl.split("/")[6])
									val message = this@ephemeralSubCommand.kord.getGuild(it.guildId)!!
										.getChannelOf<GuildMessageChannel>(it.channelId)
										.getMessage(messageId)

									message.edit {
										content = "${message.content} ${
											if (it.repeating) "**Repeating" else "**"
										} Reminder cancelled.**"
									}
								}

								respond {
									content = "Removed all your reminders for this guild."
								}
							}

							"repeating" -> {
								if (it.guildId == guild?.id && it.userId == user.id && it.repeating) {
									RemindMeCollection().removeReminder(guild!!.id, user.id, it.id)
									val messageId = Snowflake(it.originalMessageUrl.split("/")[6])
									val message = this@ephemeralSubCommand.kord.getGuild(it.guildId)!!
										.getChannelOf<GuildMessageChannel>(it.channelId)
										.getMessage(messageId)

									message.edit {
										content = "${message.content} **Repeating Reminder cancelled.**"
									}
								}

								respond {
									content = "Removed all your repeating reminders for this guild."
								}
							}

							"non-repeating" -> {
								if (it.guildId == guild?.id && it.userId == user.id && !it.repeating) {
									RemindMeCollection().removeReminder(guild!!.id, user.id, it.id)
									val messageId = Snowflake(it.originalMessageUrl.split("/")[6])
									val message = this@ephemeralSubCommand.kord.getGuild(it.guildId)!!
										.getChannelOf<GuildMessageChannel>(it.channelId)
										.getMessage(messageId)

									message.edit {
										content = "${message.content} **Reminder cancelled.**"
									}
								}

								respond {
									content = "Removed all your non-repeating reminders for this guild."
								}
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Collect a String of reminders that a user has for this guild and return it.
	 *
	 * @param event The event of from the slash command
	 * @since 3.3.4
	 * @author NoComment1105
	 */
	private suspend inline fun userReminders(event: ChatInputCommandInteractionCreateEvent): String {
		val reminders = RemindMeCollection().getReminders()
		var response = ""
		reminders.forEach {
			if (it.userId == event.interaction.user.id && it.guildId == guildFor(event)!!.id) {
				response +=
					"Reminder ID: ${it.id}\nTime set: ${it.initialSetTime.toDiscord(TimestampType.ShortDateTime)},\n" +
							"Time until reminder: ${it.remindTime.toDiscord(TimestampType.RelativeTime)} (${
								it.remindTime.toDiscord(TimestampType.ShortDateTime)
							}),\nCustom Message: ${
								if (it.customMessage != null && it.customMessage.length >= 1024) {
									it.customMessage.substring(0..1000)
								} else {
									it.customMessage ?: "none"
								}
							}\n${
								if (it.repeating) {
									"This reminder will repeat every `${
										it.repeatingInterval.toString().lowercase()
											.replace("pt", "")
											.replace("p", "")
									}`"
								} else ""
							}" +
							"\n---\n"
			}
		}

		if (response.isEmpty()) {
			response = "You do not have any reminders set."
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
		val reminders = RemindMeCollection().getReminders()

		reminders.forEach {
			if (it.remindTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0) {
				val channel = kord.getGuild(it.guildId)!!.getChannelOf<GuildMessageChannel>(it.channelId)
				if (it.customMessage.isNullOrEmpty()) {
					try {
						channel.createMessage {
							content = "${if (it.repeating) "Repeating" else ""} Reminder for <@${it.userId}> set ${
								it.initialSetTime.toDiscord(
									TimestampType.RelativeTime
								)
							} at ${
								it.initialSetTime.toDiscord(
									TimestampType.ShortDateTime
								)
							}${
								if (it.repeating) {
									"This reminder will repeat every `${
										it.repeatingInterval.toString().lowercase()
											.replace("pt", "")
											.replace("p", "")
									}`"
								} else ""
							}"
							components {
								linkButton {
									label = "Jump to message"
									url = it.originalMessageUrl
								}
							}
						}
					} catch (e: KtorRequestException) {
						kord.getUser(it.userId)?.dm {
							content = "I was unable to send your reminder in <#${it.channelId}> from ${
								kord.getGuild(it.guildId)?.name
							}.\n\n${
								if (it.repeating) {
									"Repeating reminder for <@${it.userId}>. This reminder will repeat every `${
										it.repeatingInterval.toString().lowercase()
											.replace("pt", "")
											.replace("p", "")
									}`. Use `/reminder remove` to cancel."
								} else {
									"Reminder for <@${it.userId}> set ${it.initialSetTime.toDiscord(TimestampType.RelativeTime)} at ${
										it.initialSetTime.toDiscord(
											TimestampType.ShortDateTime
										)
									}"
								}
							}"
							components {
								linkButton {
									label = "Jump to message"
									url = it.originalMessageUrl
								}
							}
						}
					}

					if (!it.repeating) {
						val messageId = Snowflake(it.originalMessageUrl.split("/")[6])
						kord.getGuild(it.guildId)!!.getChannelOf<GuildMessageChannel>(it.channelId)
							.getMessage(messageId)
							.edit {
								content = "Reminder completed!"
							}
					}
				} else {
					// FIXME Maybe duplicaten't?
					@Suppress("DuplicatedCode")
					try {
						channel.createMessage {
							content = if (it.repeating) {
								"Repeating reminder for <@${it.userId}>\n> ${
									if (it.customMessage.length >= 1024) {
										it.customMessage.substring(0..1000)
									} else {
										it.customMessage
									}
								}.\nThis reminder will repeat every `${
									it.repeatingInterval.toString().lowercase()
										.replace("pt", "")
										.replace("p", "")
								}`. Use `/reminder remove` to cancel."
							} else {
								"Reminder for <@${it.userId}> set ${
									it.initialSetTime.toDiscord(TimestampType.RelativeTime)
								} at ${
									it.initialSetTime.toDiscord(TimestampType.ShortDateTime)
								}\n> ${
									if (it.customMessage.length >= 1024) {
										it.customMessage.substring(0..1000)
									} else {
										it.customMessage
									}
								}"
							}
							components {
								linkButton {
									label = "Jump to message"
									url = it.originalMessageUrl
								}
							}
						}
					} catch (e: KtorRequestException) {
						kord.getUser(it.userId)?.dm {
							content = "I was unable to send your reminder in <#${it.channelId}> from ${
								kord.getGuild(it.guildId)?.name
							}.\n\n${
								if (it.repeating) {
									"Repeating reminder for <@${it.userId}>\n> ${
										if (it.customMessage.length >= 1024) {
											it.customMessage.substring(0..1000)
										} else {
											it.customMessage
										}
									}.\nThis reminder will repeat every `${
										it.repeatingInterval.toString().lowercase()
											.replace("pt", "")
											.replace("p", "")
									}`. Use `/reminder remove` to cancel"
								} else {
									"Reminder for <@${it.userId}> set ${
										it.initialSetTime.toDiscord(TimestampType.RelativeTime)
									} at ${
										it.initialSetTime.toDiscord(TimestampType.ShortDateTime)
									}\n> ${
										if (it.customMessage.length >= 1024) {
											it.customMessage.substring(0..1000)
										} else {
											it.customMessage
										}
									}"
								}
							}"
							components {
								linkButton {
									label = "Jump to message"
									url = it.originalMessageUrl
								}
							}
						}
					}

					if (!it.repeating) {
						val messageId = Snowflake(it.originalMessageUrl.split("/")[6])
						kord.getGuild(it.guildId)!!.getChannelOf<GuildMessageChannel>(it.channelId)
							.getMessage(messageId)
							.edit {
								content = "Reminder completed!"
							}
					}
				}

				// Remove the old reminder from the database
				if (it.repeating) {
					RemindMeCollection().setReminder(
						Clock.System.now(),
						it.guildId,
						it.userId,
						it.channelId,
						it.remindTime.plus(it.repeatingInterval!!.toDuration(TimeZone.UTC)),
						it.originalMessageUrl,
						it.customMessage,
						true,
						it.repeatingInterval,
						it.id
					)
					RemindMeCollection().removeReminder(it.guildId, it.userId, it.id)
				} else {
					RemindMeCollection().removeReminder(it.guildId, it.userId, it.id)
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

		/** Whether the reminder should repeat daily or not. */
		val repeating by defaultingBoolean {
			name = "repeating"
			description = "Would you like this reminder to repeat daily?"
			defaultValue = false
		}

		val repeatingInterval by coalescingOptionalDuration {
			name = "repeatingInterval"
			description = "How often should the reminder repeat?"
		}
	}

	inner class RemoveRemindArgs : Arguments() {
		val reminder by int {
			name = "reminderNumber"
			description = "The number of the reminder to remove. Use `/reminders list` to find out the reminder."
		}
	}

	inner class RemoveAllArgs : Arguments() {
		val reminderType by stringChoice {
			name = "type"
			description = "Choose which reminder type to remove all of"
			choices = mutableMapOf(
				"all" to "All",
				"repeating" to "Repeating",
				"non-repeating" to "Non-repeating"
			)
		}
	}
}
