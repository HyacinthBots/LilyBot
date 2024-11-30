package org.hyacinthbots.lilybot.extensions.utility.commands

import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import dev.kordex.core.checks.anyGuild
import dev.kordex.core.checks.guildFor
import dev.kordex.core.checks.hasPermission
import dev.kordex.core.commands.Arguments
import dev.kordex.core.commands.application.slash.converters.impl.stringChoice
import dev.kordex.core.commands.application.slash.ephemeralSubCommand
import dev.kordex.core.commands.application.slash.publicSubCommand
import dev.kordex.core.commands.converters.impl.boolean
import dev.kordex.core.commands.converters.impl.coalescingDuration
import dev.kordex.core.commands.converters.impl.coalescingOptionalDuration
import dev.kordex.core.commands.converters.impl.defaultingBoolean
import dev.kordex.core.commands.converters.impl.long
import dev.kordex.core.commands.converters.impl.optionalString
import dev.kordex.core.commands.converters.impl.user
import dev.kordex.core.extensions.Extension
import dev.kordex.core.extensions.publicSlashCommand
import dev.kordex.core.pagination.EphemeralResponsePaginator
import dev.kordex.core.pagination.pages.Page
import dev.kordex.core.pagination.pages.Pages
import dev.kordex.core.time.TimestampType
import dev.kordex.core.time.toDiscord
import dev.kordex.core.utils.botHasPermissions
import dev.kordex.core.utils.dm
import dev.kordex.core.utils.scheduling.Scheduler
import dev.kordex.core.utils.scheduling.Task
import dev.kordex.core.utils.toDuration
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import lilybot.i18n.Translations
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.fitsEmbedField
import org.hyacinthbots.lilybot.utils.interval
import org.hyacinthbots.lilybot.utils.statusEnv

class Reminders : Extension() {
	override val name = "reminders"

	/** Logger for status ping. */
	private val statusLogger = KotlinLogging.logger("Status ping")

	/** Client for status ping. */
	private val statusClient = HttpClient {}

	/** The scheduler that will track the time for reminder posting. */
	private val reminderScheduler = Scheduler()

	/** The task that will run the [reminderScheduler]. */
	private lateinit var reminderTask: Task

	override suspend fun setup() {
		reminderTask = reminderScheduler.schedule(30, repeat = true, callback = ::postReminders)

		publicSlashCommand {
			name = Translations.Utility.Reminders.Reminder.name
			description = Translations.Utility.Reminders.Reminder.description

			/*
			Reminder set
			 */
			publicSubCommand(::ReminderSetArgs) {
				name = Translations.Utility.Reminders.Reminder.Set.name
				description = Translations.Utility.Reminders.Reminder.Set.description

				check {
					anyGuild()
					requireBotPermissions(Permission.ViewChannel, Permission.SendMessages, Permission.EmbedLinks)
					botHasChannelPerms(
						Permissions(
							Permission.ViewChannel, Permission.SendMessages,
							Permission.EmbedLinks
						)
					)
				}

				action {
					val translations = Translations.Utility.Reminders.Reminder.Set
					val setTime = Clock.System.now()
					val remindTime = Clock.System.now().plus(arguments.time.toDuration(TimeZone.UTC))

					if (arguments.customMessage != null && arguments.customMessage.fitsEmbedField() == false) {
						respond { content = translations.messageTooLong.translate() }
						return@action
					}

					if (arguments.repeating && arguments.repeatingInterval == null) {
						respond { content = translations.noRepeatingInt.translate() }
						return@action
					}

					if (arguments.repeatingInterval != null && arguments.repeatingInterval!!.toDuration(TimeZone.UTC) <
						DateTimePeriod(hours = 1).toDuration(TimeZone.UTC)
					) {
						respond { content = translations.tooShort.translate() }
						return@action
					}

					val reminderEmbed = respond {
						embed {
							if (arguments.customMessage.isNullOrEmpty() && !arguments.repeating) {
								title = translations.embedTitle.translate()
							} else if (arguments.customMessage.isNullOrEmpty() && arguments.repeating) {
								title = translations.repeatingEmbedTitle.translate()
								field {
									name = translations.embedRepeatingInt.translate()
									value = arguments.repeatingInterval.toString().lowercase()
										.replace("pt", "")
										.replace("p", "")
								}
							} else if (arguments.customMessage != null && !arguments.repeating) {
								title = translations.embedTitle.translate()
								field {
									name = translations.embedCustomMessage.translate()
									value = arguments.customMessage!!
								}
							} else if (arguments.customMessage != null && arguments.repeating) {
								title = translations.repeatingEmbedTitle.translate()
								field {
									name = translations.embedCustomMessage.translate()
									value = arguments.customMessage!!
								}
								field {
									name = translations.embedRepeatingInt.translate()
									value = arguments.repeatingInterval.interval()!!
								}
							}

							description = translations.embedDesc.translateNamed(
								"long" to remindTime.toDiscord(TimestampType.LongDateTime),
								"relative" to remindTime.toDiscord(TimestampType.RelativeTime)
							)

							if (arguments.dm) {
								field {
									value = translations.reminderToAll.translate()
								}
							}

							footer {
								text = Translations.Utility.Reminders.Reminder.Embed.footer.translate()
							}
						}
					}

					val id = (ReminderCollection().getRemindersForUser(user.id).maxByOrNull { it.id }?.id ?: 0) + 1

					ReminderCollection().setReminder(
						ReminderData(
							guild!!.id,
							remindTime,
							setTime,
							user.id,
							channel.id,
							reminderEmbed.message.asMessageOrNull().id,
							arguments.dm,
							arguments.customMessage,
							arguments.repeating,
							arguments.repeatingInterval,
							id
						)
					)
				}
			}

			/*
			Reminder List
			 */
			ephemeralSubCommand {
				name = Translations.Utility.Reminders.Reminder.List.name
				description = Translations.Utility.Reminders.Reminder.List.description

				check {
					anyGuild()
				}

				action {
					val reminders = userReminders(event)

					val paginator = EphemeralResponsePaginator(
						pages = reminders,
						owner = event.interaction.user,
						timeoutSeconds = 500,
						interaction = interactionResponse
					)

					paginator.send()
				}
			}

			/*
			Reminder Remove
			 */
			ephemeralSubCommand(::ReminderRemoveArgs) {
				name = Translations.Utility.Reminders.Reminder.Remove.name
				description = Translations.Utility.Reminders.Reminder.description

				check {
					anyGuild()
				}

				action {
					val translations = Translations.Utility.Reminders.Reminder.Remove
					val reminders = ReminderCollection().getRemindersForUser(user.id)

					val reminder = reminders.find { it.id == arguments.reminder }

					if (reminder == null) {
						respond {
							content = translations.notFound.translate()
						}
						return@action
					}

					respond {
						embed {
							title = translations.embedTitle.translate()
							field {
								name = translations.reminderField.translate()
								value = reminder.getContent()!!
							}
						}
					}

					ReminderCollection().removeReminder(user.id, arguments.reminder)
					markReminderCompleteOrCancelled(reminder.guildId, reminder.channelId, reminder.messageId, true)
				}
			}

			/*
			Reminder Remove all
			 */
			ephemeralSubCommand(::ReminderRemoveAllArgs) {
				name = Translations.Utility.Reminders.Reminder.RemoveAll.name
				description = Translations.Utility.Reminders.Reminder.RemoveAll.description

				check {
					anyGuild()
				}

				action {
					val translations = Translations.Utility.Reminders.Reminder.RemoveAll
					val reminders = ReminderCollection().getRemindersForUserInGuild(user.id, guild!!.id)

					if (reminders.isEmpty()) {
						respond {
							content = when (arguments.type) {
								"all" -> translations.noReminders
								"repeating" -> translations.noRepeating
								"non-repeating" -> translations.noNonRepeating
								// This is impossible but the compiler complains otherwise
								else -> translations.noReminders
							}.translate()
						}
						return@action
					}

					when (arguments.type) {
						"all" -> {
							reminders.forEach {
								ReminderCollection().removeReminder(it.userId, it.id)
								markReminderCompleteOrCancelled(it.guildId, it.channelId, it.messageId, true)
							}

							respond {
								content = translations.removedAll.translate()
							}
						}

						"repeating" -> {
							reminders.forEach {
								if (it.repeating) {
									ReminderCollection().removeReminder(it.userId, it.id)
									markReminderCompleteOrCancelled(it.guildId, it.channelId, it.messageId, true)
								}
							}

							respond {
								content = translations.removedRepeating.translate()
							}
						}

						"non-repeating" -> {
							reminders.forEach {
								if (!it.repeating) {
									ReminderCollection().removeReminder(it.userId, it.id)
									markReminderCompleteOrCancelled(it.guildId, it.channelId, it.messageId, true)
								}
							}

							respond {
								content = translations.removedNonRepeating.translate()
							}
						}
					}
				}
			}

			/*
			Reminder Mod List
			 */
			ephemeralSubCommand(::ReminderModListArgs) {
				name = Translations.Utility.Reminders.Reminder.ModList.name
				description = Translations.Utility.Reminders.Reminder.ModList.description

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val reminders = userReminders(event, arguments.user.id)

					val paginator = EphemeralResponsePaginator(
						pages = reminders,
						owner = event.interaction.user,
						timeoutSeconds = 500,
						interaction = interactionResponse
					)

					paginator.send()
				}
			}

			/*
			Reminder Mod Remove
			 */
			ephemeralSubCommand(::ReminderModRemoveArgs) {
				name = Translations.Utility.Reminders.Reminder.ModRemove.name
				description = Translations.Utility.Reminders.Reminder.ModRemove.description

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val translations = Translations.Utility.Reminders.Reminder.ModRemove
					val reminders = ReminderCollection().getRemindersForUser(arguments.user.id)

					val reminder = reminders.find { it.id == arguments.reminder }

					if (reminder == null) {
						respond { content = translations.notFound.translate() }
						return@action
					}

					respond {
						embed {
							title = translations.embedTitle.translate()
							field {
								name = Translations.Utility.Reminders.Reminder.Remove.reminderField.translate()
								value = reminder.getContent()!!
							}
						}
					}

					ReminderCollection().removeReminder(arguments.user.id, arguments.reminder)
					markReminderCompleteOrCancelled(
						reminder.guildId, reminder.channelId, reminder.messageId,
						wasCancelled = true,
						byModerator = true
					)
				}
			}

			/*
			Reminder Mod Remove All
			 */
			ephemeralSubCommand(::ReminderModRemoveAllArgs) {
				name = Translations.Utility.Reminders.Reminder.ModRemoveAll.name
				description = Translations.Utility.Reminders.Reminder.ModRemoveAll.description

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val translations = Translations.Utility.Reminders.Reminder.ModRemoveAll
					val reminders = ReminderCollection().getRemindersForUserInGuild(arguments.user.id, guild!!.id)
					val target = guild!!.getMemberOrNull(arguments.user.id)?.username

					if (reminders.isEmpty()) {
						respond {
							content = when (arguments.type) {
								"all" -> translations.noReminders
								"repeating" -> translations.noRepeating
								"non-repeating" -> translations.noNonRepeating
								// This is impossible but the compiler complains otherwise
								else -> translations.noReminders
							}.translate(target)
						}
						return@action
					}

					when (arguments.type) {
						"all" -> {
							reminders.forEach {
								ReminderCollection().removeReminder(it.userId, it.id)
								markReminderCompleteOrCancelled(
									it.guildId, it.channelId, it.messageId,
									wasCancelled = true,
									byModerator = true
								)
							}

							respond { content = translations.removedAll.translate(target) }
						}

						"repeating" -> {
							reminders.forEach {
								if (it.repeating) {
									ReminderCollection().removeReminder(it.userId, it.id)
									markReminderCompleteOrCancelled(
										it.guildId, it.channelId, it.messageId,
										wasCancelled = true,
										byModerator = true
									)
								}
							}

							respond { content = translations.removedRepeating.translate(target) }
						}

						"non-repeating" -> {
							reminders.forEach {
								if (!it.repeating) {
									ReminderCollection().removeReminder(it.userId, it.id)
									markReminderCompleteOrCancelled(
										it.guildId, it.channelId, it.messageId,
										wasCancelled = true,
										byModerator = true
									)
								}
							}

							respond { content = translations.removedNonRepeating.translate(target) }
						}
					}
				}
			}
		}
	}

	/**
	 * Checks the database to see if reminders need posting and posts them if necessary.
	 *
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	private suspend fun postReminders() {
		// Start status ping

		// Doing this status ping in here as this is an already running scheduler and can easily have this added
		// to avoid duplicating the scheduler. This should always ping if the bots up too so why make another
		statusLogger.debug { "Pinging!" }
		statusEnv?.let { statusClient.post(it) }
		// End status ping
		val reminders = ReminderCollection().getAllReminders()
		val dueReminders =
			reminders.filter { it.remindTime.toEpochMilliseconds() - Clock.System.now().toEpochMilliseconds() <= 0 }

		for (it in dueReminders) {
			var guild: Guild?
			try {
				guild = kord.getGuildOrNull(it.guildId)
			} catch (_: KtorRequestException) {
				ReminderCollection().removeReminder(it.userId, it.id)
				continue
			}

			if (guild == null) {
				ReminderCollection().removeReminder(it.userId, it.id)
				continue
			}

			val channel = guild.getChannelOfOrNull<GuildMessageChannel>(it.channelId)
			if (channel == null) {
				ReminderCollection().removeReminder(it.userId, it.id)
				continue
			}

			val hasPerms =
				channel.botHasPermissions(Permission.ViewChannel, Permission.EmbedLinks, Permission.SendMessages)

			if (it.dm || !hasPerms) {
				guild.getMemberOrNull(it.userId)?.dm {
					if (!it.dm) {
						content = Translations.Utility.Reminders.Reminder.unableToAccess.translate(guild)
					}
					reminderEmbed(it)
				}
			} else {
				channel.createMessage {
					content = guild.getMemberOrNull(it.userId)?.mention
					reminderEmbed(it)
				}
				markReminderCompleteOrCancelled(it.guildId, it.channelId, it.messageId, false, it.repeating)
			}

			if (it.repeating) {
				ReminderCollection().repeatReminder(it, it.repeatingInterval!!)
			} else {
				ReminderCollection().removeReminder(it.userId, it.id)
			}
		}
	}

	/**
	 * Creates an embed for a reminder based on the provided [data].
	 *
	 * @param data The data for the reminder
	 *
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	private fun MessageCreateBuilder.reminderEmbed(data: ReminderData) {
		val translations = Translations.Utility.Reminders.Reminder.Embed
		embed {
			title = translations.title.translate()
			if (data.customMessage != null) description = data.customMessage
			field {
				name = translations.set.translate()
				value = data.setTime.toDiscord(TimestampType.LongDateTime)
			}
			if (data.repeating) {
				field {
					name = translations.repeatingTitle.translate()
					value = translations.repeatingValue.translate(data.repeatingInterval.interval())
				}

				footer {
					text = translations.footer.translate()
				}
			}
		}
	}

	/**
	 * Edits an original reminder message to state whether it was completed or cancelled and whether the cancellation
	 * was carried out by a moderator.
	 *
	 * @param guildId The ID of the guild the reminder was in
	 * @param channelId THe ID of the channel the reminder was in
	 * @param messageId The ID of the message for the reminder
	 * @param wasCancelled Whether the reminder was cancelled or not
	 * @param isRepeating Whether this was a repeating reminder or not. Leave null if irrelevant in the context
	 * @param byModerator Whether the reminder was cancelled by a moderator, defaults false
	 *
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	private suspend fun markReminderCompleteOrCancelled(
		guildId: Snowflake,
		channelId: Snowflake,
		messageId: Snowflake,
		wasCancelled: Boolean,
		isRepeating: Boolean? = null,
		byModerator: Boolean = false
	) {
		if (isRepeating == true) return
		val guild = kord.getGuildOrNull(guildId) ?: return
		val channel = guild.getChannelOfOrNull<GuildMessageChannel>(channelId) ?: return
		val message = channel.getMessageOrNull(messageId) ?: return
		message.edit {
			content = "${message.content} **${Translations.Utility.Reminders.Reminder.Embed.title.translate()} " +
				if (wasCancelled) {
					"${Translations.Utility.Reminders.Reminder.OriginalMessage.canc.translate()}${
						if (byModerator) {
							" ${Translations.Utility.Reminders.Reminder.OriginalMessage.byMod.translate()}"
						} else {
						    ""
						}
					}."
				} else {
					Translations.Utility.Reminders.Reminder.OriginalMessage.complete.translate()
				} + "**"
		}
	}

	/**
	 * Gets all a users reminders and formats them into a Page for a paginator.
	 *
	 * @param event The event that requires this information
	 * @param userId The user that is being looked for, or null to use the integration
	 *
	 * @return A [Pages] object of containing reminder information
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	private suspend inline fun userReminders(
		event: ChatInputCommandInteractionCreateEvent,
		userId: Snowflake? = null
	): Pages {
		val translations = Translations.Utility.Reminders.Reminder.ListPage
		val pagesObj = Pages()
		val userReminders = ReminderCollection().getRemindersForUserInGuild(
			userId ?: event.interaction.user.id,
			guildFor(event)!!.id
		)

		if (userReminders.isEmpty()) {
			pagesObj.addPage(
				Page {
					description = if (userId == null) {
						translations.desc
					} else {
						translations.modDesc
					}.translate(userId)
				}
			)
		} else {
			userReminders.chunked(4).forEach { reminder ->
				var response = ""
				reminder.forEach {
					response += it.getContent()
				}

				pagesObj.addPage(
					Page {
						title = translations.title.translate(guildFor(event)?.asGuildOrNull()?.name)
						description = response
					}
				)
			}
		}

		return pagesObj
	}

	/**
	 * Gets the content of a reminder and formats it for display.
	 *
	 * @return The formatted string of the Reminder data
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	private fun ReminderData?.getContent(): String? {
		val translations = Translations.Utility.Reminders.Reminder.Content
		this ?: return null
		return "${translations.line1.translate()}: ${this.id}\n" +
			"${translations.line2.translate()}: ${this.setTime.toDiscord(TimestampType.ShortDateTime)},\n" +
			"${translations.line3.translate()}: ${this.remindTime.toDiscord(TimestampType.RelativeTime)} (${
				this.remindTime.toDiscord(TimestampType.ShortDateTime)
			}),\n" +
			"${translations.line4.translate()}: ${
				if (this.customMessage != null && this.customMessage.length >= 150) {
					this.customMessage.substring(0..150)
				} else {
					this.customMessage ?: Translations.Basic.none
				}
			}\n---\n"
	}

	inner class ReminderSetArgs : Arguments() {
		/** The time until the reminding should happen. */
		val time by coalescingDuration {
			name = Translations.Utility.Reminders.Reminder.Set.Arguments.Time.name
			description = Translations.Utility.Reminders.Reminder.Set.Arguments.Time.description
		}

		val dm by boolean {
			name = Translations.Utility.Reminders.Reminder.Set.Arguments.Dm.name
			description = Translations.Utility.Reminders.Reminder.Set.Arguments.Dm.description
		}

		/** An optional message to attach to the reminder. */
		val customMessage by optionalString {
			name = Translations.Utility.Reminders.Reminder.Set.Arguments.Message.name
			description = Translations.Utility.Reminders.Reminder.Set.Arguments.Message.description
		}

		/** Whether to repeat the reminder or have it run once. */
		val repeating by defaultingBoolean {
			name = Translations.Utility.Reminders.Reminder.Set.Arguments.Repeating.name
			description = Translations.Utility.Reminders.Reminder.Set.Arguments.Repeating.description
			defaultValue = false
		}

		/** The interval for the repeating reminder to run at. */
		val repeatingInterval by coalescingOptionalDuration {
			name = Translations.Utility.Reminders.Reminder.Set.Arguments.RepeatingInt.name
			description = Translations.Utility.Reminders.Reminder.Set.Arguments.RepeatingInt.description
		}
	}

	inner class ReminderRemoveArgs : Arguments() {
		/** The number of the reminder to remove. */
		val reminder by long {
			name = Translations.Utility.Reminders.Reminder.Remove.Arguments.Reminder.name
			description = Translations.Utility.Reminders.Reminder.Remove.Arguments.Reminder.description
		}
	}

	inner class ReminderRemoveAllArgs : Arguments() {
		/** The type of reminder to remove. */
		val type by stringChoice {
			name = Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.name
			description = Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.description
			choices = mutableMapOf(
				Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.Choices.repeating to "repeating",
				Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.Choices.nonRepeating to "non-repeating",
				Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.Choices.all to "all"
			)
		}
	}

	inner class ReminderModListArgs : Arguments() {
		/** The user whose reminders are being viewed. */
		val user by user {
			name = Translations.Utility.Reminders.Reminder.ModList.Arguments.User.name
			description = Translations.Utility.Reminders.Reminder.ModList.Arguments.User.description
		}
	}

	inner class ReminderModRemoveArgs : Arguments() {
		/** The user whose reminders need removing. */
		val user by user {
			name = Translations.Utility.Reminders.Reminder.ModRemove.Arguments.User.name
			description = Translations.Utility.Reminders.Reminder.ModRemove.Arguments.User.description
		}

		/** The number of the reminder to remove. */
		val reminder by long {
			name = Translations.Utility.Reminders.Reminder.ModRemove.Arguments.Reminder.name
			description = Translations.Utility.Reminders.Reminder.ModRemove.Arguments.Reminder.description
		}
	}

	inner class ReminderModRemoveAllArgs : Arguments() {
		/** The user whose reminders need removing. */
		val user by user {
			name = Translations.Utility.Reminders.Reminder.ModRemoveAll.Arguments.User.name
			description = Translations.Utility.Reminders.Reminder.ModRemoveAll.Arguments.User.description
		}

		/** The type of reminder to remove. */
		val type by stringChoice {
			name = Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.name
			description = Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.description
			choices = mutableMapOf(
				Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.Choices.repeating to "repeating",
				Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.Choices.nonRepeating to "non-repeating",
				Translations.Utility.Reminders.Reminder.RemoveAll.Arguments.Type.Choices.all to "all"
			)
		}
	}
}
