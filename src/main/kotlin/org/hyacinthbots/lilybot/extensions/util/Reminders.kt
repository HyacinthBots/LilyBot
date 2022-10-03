package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.EphemeralSlashCommandContext
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingOptionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.int
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.snowflake
import com.kotlindiscord.kord.extensions.components.components
import com.kotlindiscord.kord.extensions.components.linkButton
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.EphemeralResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.types.respond
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.getJumpUrl
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import com.kotlindiscord.kord.extensions.utils.toDuration
import dev.kord.common.Locale
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOf
import dev.kord.core.entity.Message
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.core.exception.EntityNotFoundException
import dev.kord.rest.builder.message.create.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.hyacinthbots.lilybot.database.collections.RemindMeCollection
import org.hyacinthbots.lilybot.database.entities.RemindMeData
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.utilsLogger

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

	/** The choices of reminder type. For use in `stringChoice` arguments. */
	private val reminderChoices = mutableMapOf(
		"all" to "all",
		"repeating" to "repeating",
		"non-repeating" to "non-repeating"
	)

	override suspend fun setup() {
		/** Set the task to run every 30 seconds. */
		task = scheduler.schedule(30, pollingSeconds = 1, repeat = true, callback = ::postReminders)

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
					} else if (arguments.customMessage != null) {
						if (arguments.customMessage!!.contains("@everyone") ||
							arguments.customMessage!!.contains("@here")
						) {
							respond { content = "You can't use `@everyone` or `@here` in your message" }
							return@action
						}
					}

					if (arguments.repeating && arguments.repeatingInterval == null) {
						respond {
							content = "You must specify a repeating interval if you are setting a repeating reminder"
						}
						return@action
					}

					if (arguments.repeatingInterval != null && arguments.repeatingInterval!!.toDuration(TimeZone.UTC) <=
						DateTimePeriod(hours = 1).toDuration(TimeZone.UTC)
					) {
						respond {
							content = "Repeating interval cannot be less than or equal to one hour!\n\n" +
									"This is to prevent spam and abuse of the system."
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
					// To allow the paginated to be made immediately rather than waiting for the function to run
					val reminders = userReminders(event)

					val paginator = EphemeralResponsePaginator(
						pages = reminders,
						owner = event.interaction.user,
						timeoutSeconds = 500,
						locale = Locale.ENGLISH_GREAT_BRITAIN.asJavaLocale(),
						interaction = interactionResponse
					)

					paginator.send()
				}
			}

			ephemeralSubCommand(::ModReminderListArgs) {
				name = "mod-list"
				description = "List the reminders of a user in this guild."

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					// To allow the paginated to be made immediately rather than waiting for the function to run
					val reminders = userReminders(event, arguments.userID)

					val paginator = EphemeralResponsePaginator(
						pages = reminders,
						owner = event.interaction.user,
						timeoutSeconds = 500,
						locale = Locale.ENGLISH_GREAT_BRITAIN.asJavaLocale(),
						interaction = interactionResponse
					)

					paginator.send()
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
							@Suppress("DuplicatedCode")
							response = reminderContent(it)
							RemindMeCollection().removeReminder(guild!!.id, user.id, arguments.reminder)
							try {
								val message = event.kord.getGuild(it.guildId)!!
									.getChannelOf<GuildMessageChannel>(it.channelId)
									.getMessageOrNull(Snowflake(it.originalMessageUrl.split("/")[6]))
								message?.edit {
									content = "${message.content} ${
										if (it.repeating) "**Repeating" else "**"
									} Reminder cancelled.**"
								}
							} catch (_: EntityNotFoundException) {
							} catch (_: KtorRequestException) {
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

			ephemeralSubCommand(::ModRemoveRemindArgs) {
				name = "mod-remove"
				description = "A command for moderators to remove reminders to prevent spam"

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val reminders = RemindMeCollection().getReminders()

					var response = ""

					reminders.forEach {
						if (it.guildId == guild?.id && it.userId == arguments.userID && it.id == arguments.reminder) {
							response = reminderContent(it)
							RemindMeCollection().removeReminder(guild!!.id, arguments.userID, arguments.reminder)
							try {
								val message = event.kord.getGuild(it.guildId)!!
									.getChannelOf<GuildMessageChannel>(it.channelId)
									.getMessage(Snowflake(it.originalMessageUrl.split("/")[6]))
								message.edit {
									content = "${message.content} ${
										if (it.repeating) "**Repeating" else "**"
									} Reminder cancelled by a moderator.**"
								}
							} catch (_: EntityNotFoundException) {
							} catch (_: KtorRequestException) {
							}
						}
					}

					if (response.isEmpty()) {
						response += "Reminder not found. Please use `/reminder mod-list` to find out the correct " +
								"reminder number"
					}

					respond {
						embed {
							title = "Reminder cancelled by moderator"
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
					val reminders = RemindMeCollection().getUserReminders(user.id).filter { it.guildId == guild!!.id }

					if (reminders.isEmpty()) {
						respond {
							content = "You do not have any reminders for this guild!"
						}
						return@action
					}

					when (arguments.reminderType) {
						"all" -> {
							reminders.forEach {
								editAndRemoveReminder(it)
							}

							respond {
								content = "Removed all your reminders for this guild."
							}
						}

						"repeating" -> {
							reminders.forEach {
								if (it.repeating) {
									editAndRemoveReminder(it)
								}
							}

							respond {
								content = "Removed all your repeating reminders for this guild."
							}
						}

						"non-repeating" -> {
							reminders.forEach {
								if (!it.repeating) {
									editAndRemoveReminder(it)
								}
							}

							respond {
								content = "Removed all your non-repeating reminders for this guild."
							}
						}
					}
				}
			}

			ephemeralSubCommand(::ModRemoveAllArgs) {
				name = "mod-remove-all"
				description = "Remove all a users reminders of a specific type for this guild"

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val reminders = RemindMeCollection().getUserReminders(user.id).filter { it.guildId == guild!!.id }

					if (reminders.isEmpty()) {
						respond {
							content = "This user does not have any reminders in this guild!"
						}
						return@action
					}

					when (arguments.reminderType) {
						"all" -> {
							reminders.forEach {
								editAndRemoveReminder(it, arguments.userID)
							}

							respond {
								content = "Removed all ${
									guild!!.getMember(arguments.userID).mention
								}'s reminders for this guild."
							}
						}

						"repeating" -> {
							reminders.forEach {
								if (it.repeating) {
									editAndRemoveReminder(it, arguments.userID)
								}
							}

							respond {
								content = "Removed all ${
									guild!!.getMember(arguments.userID).mention
								}'s repeating reminders for this guild."
							}
						}

						"non-repeating" -> {
							reminders.forEach {
								if (!it.repeating) {
									editAndRemoveReminder(it, arguments.userID)
								}
							}

							respond {
								content = "Removed all ${
									guild!!.getMember(arguments.userID).mention
								}'s non-repeating reminders for this guild."
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Edits a reminder message to contain cancelled and removes it from the database.
	 *
	 * @param reminder The reminder to edit and remove
	 * @param userId The user to remove the reminder from. Will use interaction author if null
	 * @since 4.0.0
	 * @author NoComment1105
	 */
	private suspend inline fun EphemeralSlashCommandContext<*>.editAndRemoveReminder(
		reminder: RemindMeData,
		userId: Snowflake? = null
	) {
		val messageId = Snowflake(reminder.originalMessageUrl.split("/")[6])
		val message = event.kord.getGuild(reminder.guildId)!!
			.getChannelOf<GuildMessageChannel>(reminder.channelId)
			.getMessageOrNull(messageId)

		completeOrCancelReminder(message, true)

		RemindMeCollection().removeReminder(guild!!.id, userId ?: user.id, reminder.id)
	}

	/**
	 * Gets a reminders information and produces a formatted string to send in an embed.
	 *
	 * @param reminder The reminder to get the information from
	 * @return A formatted string to send in an embed
	 * @since 3.5.2
	 * @author NoComment1105
	 */
	private fun reminderContent(reminder: RemindMeData) = "Reminder ${reminder.id}\nTime set: ${
		reminder.initialSetTime.toDiscord(TimestampType.ShortDateTime)
	},\nTime until " +
			"reminder: ${reminder.remindTime.toDiscord(TimestampType.RelativeTime)} (${
				reminder.remindTime.toDiscord(TimestampType.ShortDateTime)
			}),\nCustom Message: ${
				if (reminder.customMessage != null && reminder.customMessage.length >= 1024) {
					reminder.customMessage.substring(0..1000)
				} else {
					reminder.customMessage ?: "none"
				}
			}\n---\n"

	/**
	 * Collect a String of reminders that a user has for this guild and return it.
	 *
	 * @param event The event of from the slash command
	 * @param userId If you'd like to get the reminders for a specific user, other than the interaction user.
	 * @return Pages of reminders for hte provided [userId]
	 * @since 3.3.4
	 * @author NoComment1105
	 */
	private suspend inline fun userReminders(
		event: ChatInputCommandInteractionCreateEvent,
		userId: Snowflake? = null
	): Pages {
		val pagesObj = Pages()
		val allUserReminders = RemindMeCollection().getUserReminders(userId ?: event.interaction.user.id)
		val guildUserReminders = allUserReminders.filter { it.guildId == guildFor(event)?.id }

		if (guildUserReminders.isEmpty()) {
			pagesObj.addPage(
				Page {
					description = "There are no reminders set for this guild."
				}
			)
		} else {
			guildUserReminders.chunked(4).forEach { reminder ->
				var response = ""
				reminder.forEach {
					response +=
						"Reminder ID: ${it.id}\nTime set: ${it.initialSetTime.toDiscord(TimestampType.ShortDateTime)},\n" +
								"Time until reminder: ${it.remindTime.toDiscord(TimestampType.RelativeTime)} (${
									it.remindTime.toDiscord(TimestampType.ShortDateTime)
								}),\nCustom Message: ${
									if (it.customMessage != null && it.customMessage.length >= 150) {
										it.customMessage.substring(0..150)
									} else {
										it.customMessage ?: "none"
									}
								}\n---\n"
				}

				pagesObj.addPage(
					Page {
						title = "Reminders for ${guildFor(event)?.asGuild()?.name ?: "this guild"}"
						description = response
					}
				)
			}
		}

		return pagesObj
	}

	/**
	 * Sends reminders if the time for the reminder to be sent has passed.
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	private suspend fun postReminders() {
		val reminders = RemindMeCollection().getReminders()

		for (it in reminders) {
			if (it.remindTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0) {
				var channel: GuildMessageChannel?
				try {
					channel = kord.getGuild(it.guildId)!!.getChannelOf(it.channelId)
				} catch (e: EntityNotFoundException) {
					kord.getUser(it.userId)?.dm {
						content = "I was unable to send your reminder in <#${it.channelId}> from ${
							kord.getGuild(it.guildId)?.name
						}. due to channel access issues.\n\n${
							if (it.repeating) {
								"Repeating reminder for <@${it.userId}>"
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
					updateReminderDb(it)
					continue
				}

				val message = channel.getMessageOrNull(Snowflake(it.originalMessageUrl.split("/")[6]))
				if (it.customMessage.isNullOrEmpty()) {
					try {
						channel.createMessage {
							content = if (it.repeating) {
								"Repeating reminder for <@${it.userId}>\n" +
										"This reminder will repeat every `${
											it.repeatingInterval.toString().lowercase()
												.replace("pt", "")
												.replace("p", "")
										}`"
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

							if (message != null) {
								components {
									linkButton {
										label = "Jump to message"
										url = it.originalMessageUrl
									}
								}
							} else {
								content += "\nOriginal message not found."
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
						updateReminderDb(it)
						continue
					}

					if (!it.repeating) {
						completeOrCancelReminder(message, false)
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
							if (message != null) {
								components {
									linkButton {
										label = "Jump to message"
										url = it.originalMessageUrl
									}
								}
							} else {
								content += "\nOriginal message not found."
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
							if (message != null) {
								components {
									linkButton {
										label = "Jump to message"
										url = it.originalMessageUrl
									}
								}
							} else {
								content += "\nOriginal message not found."
							}
						}
						updateReminderDb(it)
						continue
					}

					if (!it.repeating) {
						completeOrCancelReminder(message, false)
					}
				}

				// Remove the old reminder from the database
				updateReminderDb(it)
			}
		}
	}

	/**
	 * Removes or updates a reminder in the database.
	 *
	 * @param reminder The reminder to update
	 *
	 * @since 4.2.0
	 * @author NoComment1105
	 */
	private suspend inline fun updateReminderDb(reminder: RemindMeData) {
		if (reminder.repeating) {
			RemindMeCollection().removeReminder(reminder.guildId, reminder.userId, reminder.id)
			RemindMeCollection().setReminder(
				Clock.System.now(),
				reminder.guildId,
				reminder.userId,
				reminder.channelId,
				reminder.remindTime.plus(reminder.repeatingInterval!!.toDuration(TimeZone.UTC)),
				reminder.originalMessageUrl,
				reminder.customMessage,
				true,
				reminder.repeatingInterval,
				reminder.id
			)
		} else {
			RemindMeCollection().removeReminder(reminder.guildId, reminder.userId, reminder.id)
		}
	}

	/**
	 * Edits the reminders initial message to detail whether it has been completed or cancelled.
	 *
	 * @param message The message to edit
	 * @param cancel Whether the reminder has been cancelled or not
	 * @since 4.0.0
	 * @author NoComment1105
	 */
	private suspend inline fun completeOrCancelReminder(message: Message?, cancel: Boolean) {
		if (cancel) {
			try {
				message?.edit {
					content = "${message.content} **Cancelled**"
				} ?: utilsLogger.debug { "Unable to find original message" }
			} catch (e: KtorRequestException) {
				utilsLogger.debug { "Unable to edit original message" }
			}
		} else {
			try {
				message?.edit {
					content = "${message.content} **Completed**"
				} ?: utilsLogger.debug { "Unable to find original message" }
			} catch (e: KtorRequestException) {
				utilsLogger.debug { "Unable to edit original message" }
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
			name = "custom-message"
			description = "Add a custom message to your reminder"
		}

		/** Whether the reminder should repeat daily or not. */
		val repeating by defaultingBoolean {
			name = "repeating"
			description = "Would you like this reminder to repeat?"
			defaultValue = false
		}

		/** The interval at which you want the reminder to repeat. */
		val repeatingInterval by coalescingOptionalDuration {
			name = "repeating-interval"
			description = "How often should the reminder repeat?"
		}
	}

	inner class RemoveRemindArgs : Arguments() {
		/** The numeric ID of the reminder you want to delete. */
		val reminder by int {
			name = "reminder-number"
			description = "The number of the reminder to remove. Use `/reminders list` to find out the reminder."
		}
	}

	inner class ModRemoveRemindArgs : Arguments() {
		/** The ID of the user whose reminders you'd like to remove. */
		val userID by snowflake {
			name = "userid"
			description = "The user id of the user to remove the reminder from"
		}

		/** The reminder number you would like to remove. */
		val reminder by int {
			name = "reminder-number"
			description = "The number of the reminder to remove. Use `/reminders list` to find out the reminder."
		}
	}

	inner class ModReminderListArgs : Arguments() {
		/** The ID of the user whose reminders you'd like to remove. */
		val userID by snowflake {
			name = "user-id"
			description = "The user id of the user to list the reminders of"
		}
	}

	inner class RemoveAllArgs : Arguments() {
		/** The type of the reminder to delete all of. */
		val reminderType by stringChoice {
			name = "type"
			description = "Choose which reminder type to remove all of"
			choices = reminderChoices
		}
	}

	inner class ModRemoveAllArgs : Arguments() {
		/** The ID of the user whose reminders you'd like to remove. */
		val userID by snowflake {
			name = "userid"
			description = "The user id of the user to remove the reminder from"
		}

		/** The type of the reminder to delete all of. */
		val reminderType by stringChoice {
			name = "type"
			description = "Choose which reminder type to remove all of"
			choices = reminderChoices
		}
	}
}
