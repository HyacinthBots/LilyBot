package org.hyacinthbots.lilybot.extensions.util

import com.kotlindiscord.kord.extensions.DISCORD_YELLOW
import com.kotlindiscord.kord.extensions.checks.anyGuild
import com.kotlindiscord.kord.extensions.checks.guildFor
import com.kotlindiscord.kord.extensions.checks.hasPermission
import com.kotlindiscord.kord.extensions.commands.Arguments
import com.kotlindiscord.kord.extensions.commands.application.slash.converters.impl.stringChoice
import com.kotlindiscord.kord.extensions.commands.application.slash.ephemeralSubCommand
import com.kotlindiscord.kord.extensions.commands.application.slash.publicSubCommand
import com.kotlindiscord.kord.extensions.commands.converters.impl.boolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.coalescingOptionalDuration
import com.kotlindiscord.kord.extensions.commands.converters.impl.defaultingBoolean
import com.kotlindiscord.kord.extensions.commands.converters.impl.long
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalChannel
import com.kotlindiscord.kord.extensions.commands.converters.impl.optionalString
import com.kotlindiscord.kord.extensions.commands.converters.impl.user
import com.kotlindiscord.kord.extensions.extensions.Extension
import com.kotlindiscord.kord.extensions.extensions.publicSlashCommand
import com.kotlindiscord.kord.extensions.pagination.EphemeralResponsePaginator
import com.kotlindiscord.kord.extensions.pagination.pages.Page
import com.kotlindiscord.kord.extensions.pagination.pages.Pages
import com.kotlindiscord.kord.extensions.time.TimestampType
import com.kotlindiscord.kord.extensions.time.toDiscord
import com.kotlindiscord.kord.extensions.utils.botHasPermissions
import com.kotlindiscord.kord.extensions.utils.dm
import com.kotlindiscord.kord.extensions.utils.scheduling.Scheduler
import com.kotlindiscord.kord.extensions.utils.scheduling.Task
import com.kotlindiscord.kord.extensions.utils.toDuration
import dev.kord.common.entity.Permission
import dev.kord.common.entity.Permissions
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.createEmbed
import dev.kord.core.behavior.channel.createMessage
import dev.kord.core.behavior.edit
import dev.kord.core.behavior.getChannelOfOrNull
import dev.kord.core.entity.Guild
import dev.kord.core.entity.channel.GuildMessageChannel
import dev.kord.core.event.interaction.ChatInputCommandInteractionCreateEvent
import dev.kord.rest.builder.message.create.MessageCreateBuilder
import dev.kord.rest.builder.message.embed
import dev.kord.rest.request.KtorRequestException
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import org.hyacinthbots.lilybot.database.collections.ReminderCollection
import org.hyacinthbots.lilybot.database.collections.ReminderRestrictionCollection
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.hyacinthbots.lilybot.database.entities.ReminderRestrictionData
import org.hyacinthbots.lilybot.extensions.config.ConfigOptions
import org.hyacinthbots.lilybot.utils.botHasChannelPerms
import org.hyacinthbots.lilybot.utils.fitsEmbedField
import org.hyacinthbots.lilybot.utils.getLoggingChannelWithPerms
import org.hyacinthbots.lilybot.utils.interval

class Reminders : Extension() {
	override val name = "reminders"

	/** The scheduler that will track the time for reminder posting. */
	private val reminderScheduler = Scheduler()

	/** The task that will run the [reminderScheduler]. */
	private lateinit var reminderTask: Task

	override suspend fun setup() {
		reminderTask = reminderScheduler.schedule(30, repeat = true, callback = ::postReminders)

		publicSlashCommand {
			name = "reminder"
			description = "The parent command for all reminder commands"

			/*
			Reminder set
			 */
			publicSubCommand(::ReminderSetArgs) {
				name = "set"
				description = "Set a reminder for some time in the future!"

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
					val setTime = Clock.System.now()
					val remindTime = Clock.System.now().plus(arguments.time.toDuration(TimeZone.UTC))

					if (arguments.customMessage != null && arguments.customMessage.fitsEmbedField() == false) {
						respond { content = "Custom Message is too long. Message must be 1024 characters or fewer." }
						return@action
					}

					if (arguments.repeating && arguments.repeatingInterval == null) {
						respond {
							content = "You must specify a repeating interval if you are setting a repeating reminder."
						}
						return@action
					}

					if (arguments.repeatingInterval != null && arguments.repeatingInterval!!.toDuration(TimeZone.UTC) <
						DateTimePeriod(hours = 1).toDuration(TimeZone.UTC)
					) {
						respond {
							content = "The Repeating interval cannot be less than one hour!\n\n" +
								"This is to prevent spam and/or abuse of reminders."
						}
						return@action
					}

					val reminderEmbed = respond {
						embed {
							if (arguments.customMessage.isNullOrEmpty() && !arguments.repeating) {
								title = "Reminder Set!"
								description =
									"I will remind you at ${remindTime.toDiscord(TimestampType.LongDateTime)} (${
										remindTime.toDiscord(TimestampType.RelativeTime)
									})"
							} else if (arguments.customMessage.isNullOrEmpty() && arguments.repeating) {
								title = "Repeating Reminder Set!"
								description =
									"I will remind you at ${remindTime.toDiscord(TimestampType.LongDateTime)} (${
										remindTime.toDiscord(TimestampType.RelativeTime)
									})"
								field {
									name = "Repeating Interval"
									value = arguments.repeatingInterval.toString().lowercase()
										.replace("pt", "")
										.replace("p", "")
								}
							} else if (arguments.customMessage != null && !arguments.repeating) {
								title = "Reminder Set!"
								description =
									"I will remind you at ${remindTime.toDiscord(TimestampType.LongDateTime)} (${
										remindTime.toDiscord(TimestampType.RelativeTime)
									})"
								field {
									name = "Custom Message"
									value = arguments.customMessage!!
								}
							} else if (arguments.customMessage != null && arguments.repeating) {
								title = "Repeating Reminder Set!"
								description =
									"I will remind you at ${remindTime.toDiscord(TimestampType.LongDateTime)} (${
										remindTime.toDiscord(TimestampType.RelativeTime)
									})"
								field {
									name = "Custom Message"
									value = arguments.customMessage!!
								}
								field {
									name = "Repeating Interval"
									value = arguments.repeatingInterval.interval()!!
								}
							}

							if (arguments.dm) {
								field {
									value = "Reminder will be send via DM to all participants"
								}
							}

							footer {
								text = "Use `/reminder remove` to cancel"
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
				name = "list"
				description = "List your reminders for this guild"

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
				name = "remove"
				description = "Remove a reminder you have set from this guild"

				check {
					anyGuild()
				}

				action {
					val reminders = ReminderCollection().getRemindersForUser(user.id)

					val reminder = reminders.find { it.id == arguments.reminder }

					if (reminder == null) {
						respond {
							content = "Reminder not found. Please use `/reminder list` to find out the correct " +
								"reminder number"
						}
						return@action
					}

					respond {
						embed {
							title = "Reminder cancelled"
							field {
								name = "Reminder"
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
				name = "remove-all"
				description = "Remove all a specific type of reminder from this guild"

				check {
					anyGuild()
				}

				action {
					val reminders = ReminderCollection().getRemindersForUserInGuild(user.id, guild!!.id)

					if (reminders.isEmpty()) {
						respond {
							content = when (arguments.type) {
								"all" -> "You do not have any reminders for this guild!"
								"repeating" -> "You do not have any repeating reminders for this guild"
								"non-repeating" -> "You do not have any regular reminders for this guild"
								// This is impossible but the compiler complains otherwise
								else -> "You do not have any reminders for this guild"
							}
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
								content = "Removed all reminders for this guild."
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
								content = "Removed all repeating reminders for this guild."
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
								content = "Removed all non-repeating reminders for this guild."
							}
						}
					}
				}
			}

			/*
			Reminder Mod List
			 */
			ephemeralSubCommand(::ReminderModListArgs) {
				name = "mod-list"
				description = "List all reminders for a user, if you're a moderator"

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
				name = "mod-remove"
				description = "Remove a reminder for a user, if you're a moderator"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val reminders = ReminderCollection().getRemindersForUser(arguments.user.id)

					val reminder = reminders.find { it.id == arguments.reminder }

					if (reminder == null) {
						respond {
							content = "Reminder not found. Please use `/reminder mod-list` to find out the correct " +
								"reminder number"
						}
						return@action
					}

					respond {
						embed {
							title = "Reminder cancelled by moderator"
							field {
								name = "Reminder"
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
				name = "mod-remove-all"
				description = "Remove all a specific type of reminder for a user, if you're a moderator"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val reminders = ReminderCollection().getRemindersForUserInGuild(arguments.user.id, guild!!.id)

					if (reminders.isEmpty()) {
						respond {
							content = when (arguments.type) {
								"all" -> "${user.asUserOrNull()?.username} does not have any reminders for this guild!"
								"repeating" -> "${user.asUserOrNull()?.username} does not have any repeating reminders for this guild"
								"non-repeating" -> "${user.asUserOrNull()?.username} does not have any regular reminders for this guild"
								// This is impossible but the compiler complains otherwise
								else -> "You do not have any reminders for this guild"
							}
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

							respond {
								content = "Removed all ${
									guild!!.getMemberOrNull(arguments.user.id)?.mention
								}'s reminders for this guild."
							}
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

							respond {
								content = "Removed all ${
									guild!!.getMemberOrNull(arguments.user.id)?.mention
								}'s repeating reminders for this guild."
							}
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

							respond {
								content = "Removed all ${
									guild!!.getMemberOrNull(arguments.user.id)?.mention
								}'s non-repeating reminders for this guild."
							}
						}
					}
				}
			}
			ephemeralSubCommand(::ReminderRestrictionArgs) {
				name = "restrict"
				description = "Whether to restrict reminders to a specific channel/channels"

				requirePermission(Permission.ModerateMembers)

				check {
					anyGuild()
					hasPermission(Permission.ModerateMembers)
					requirePermission(Permission.ModerateMembers)
				}

				action {
					val restrictionData = ReminderRestrictionCollection().getRestrictionData(guild!!.id)

					if (restrictionData == null && !arguments.restrict) {
						respond { content = "**Error**: This server does not have reminder restrictions in place" }
						return@action
					}
					if (restrictionData != null && !restrictionData.restrict && !arguments.restrict) {
						respond { content = "**Error**: Restrictions are already disabled in this server" }
						return@action
					}
					if (arguments.restrict && arguments.freeChannel == null) {
						respond {
							content = "**Error**: You need to select a free channel before you can restrict reminders"
						}
					}

					if (arguments.restrict) {
						ReminderRestrictionCollection().addRestriction(
							ReminderRestrictionData(guild!!.id, true, mutableListOf(arguments.freeChannel!!.id))
						)
					} else {
						ReminderRestrictionCollection().removeRestriction(guild!!.id)
					}

					getLoggingChannelWithPerms(ConfigOptions.UTILITY_LOG, getGuild()!!)?.createEmbed {
						title = "Reminder restrictions updated"
						field {
							name = "Restriction enabled:"
							value = arguments.restrict.toString()
						}
						field {
							name = "Whitelisted Channels:"
							value = arguments.freeChannel!!.mention
						}
						footer {
							text = "Removed by ${user.asUserOrNull()?.username}"
							icon = user.asUserOrNull()?.avatar?.cdnUrl?.toUrl()
						}
						timestamp = Clock.System.now()
						color = DISCORD_YELLOW
					}

					respond { content = "Reminder restriction set to ${arguments.restrict}" }
				}
			}

			ephemeralSubCommand {
				name = "add-whitelisted-channel"
				description = "Add a channel to the reminder restriction whitelist"
			}

			ephemeralSubCommand {
				name = "remove-whitelisted-channel"
				description = "Remove a channel from the reminder restriction whitelist"
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
						content =
							"I was unable to find/access the channel from $guild that this" +
								"reminder was set in."
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
		embed {
			title = "Reminder"
			if (data.customMessage != null) description = data.customMessage
			field {
				name = "Set time"
				value = data.setTime.toDiscord(TimestampType.LongDateTime)
			}
			if (data.repeating) {
				field {
					name = "Repeating Interval"
					value = "This reminder repeats every ${data.repeatingInterval.interval()}"
				}

				footer {
					text = "Use `/reminder remove` to cancel this reminder"
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
			content = "${message.content} **Reminder " +
				"${
					if (wasCancelled) {
						"cancelled${if (byModerator) " by moderator" else ""}."
					} else {
						"completed."
					}
				}**"
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
		val pagesObj = Pages()
		val userReminders = ReminderCollection().getRemindersForUserInGuild(
			userId ?: event.interaction.user.id,
			guildFor(event)!!.id
		)

		if (userReminders.isEmpty()) {
			pagesObj.addPage(
				Page {
					description = if (userId == null) {
						"You have no reminders set for this guild."
					} else {
						"<@$userId> has no reminders set for this guild."
					}
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
						title = "Reminders for ${guildFor(event)?.asGuildOrNull()?.name ?: "this guild"}"
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
		this ?: return null
		return "Reminder ID: ${this.id}\nTime set: ${this.setTime.toDiscord(TimestampType.ShortDateTime)},\n" +
			"Time until reminder: ${this.remindTime.toDiscord(TimestampType.RelativeTime)} (${
				this.remindTime.toDiscord(TimestampType.ShortDateTime)
			}),\nCustom Message: ${
				if (this.customMessage != null && this.customMessage.length >= 150) {
					this.customMessage.substring(0..150)
				} else {
					this.customMessage ?: "none"
				}
			}\n---\n"
	}

	inner class ReminderSetArgs : Arguments() {
		/** The time until the reminding should happen. */
		val time by coalescingDuration {
			name = "time"
			description = "How long until reminding? Format: 1d12h30m / 3d / 26m30s"
		}

		val dm by boolean {
			name = "remind-in-dm"
			description = "Whether to remind in DMs or not"
		}

		/** An optional message to attach to the reminder. */
		val customMessage by optionalString {
			name = "custom-message"
			description = "A message to attach to your reminder"
		}

		/** Whether to repeat the reminder or have it run once. */
		val repeating by defaultingBoolean {
			name = "repeat"
			description = "Whether to repeat the reminder or not"
			defaultValue = false
		}

		/** The interval for the repeating reminder to run at. */
		val repeatingInterval by coalescingOptionalDuration {
			name = "repeat-interval"
			description = "The interval to repeat the reminder at. Format: 1d / 1h / 5d"
		}
	}

	inner class ReminderRemoveArgs : Arguments() {
		/** The number of the reminder to remove. */
		val reminder by long {
			name = "reminder-number"
			description = "The number of the reminder to remove. Use '/reminder list' to get this"
		}
	}

	inner class ReminderRemoveAllArgs : Arguments() {
		/** The type of reminder to remove. */
		val type by stringChoice {
			name = "reminder-type"
			description = "The type of reminder to remove"
			choices = mutableMapOf(
				"repeating" to "repeating",
				"non-repeating" to "non-repeating",
				"all" to "all"
			)
		}
	}

	inner class ReminderModListArgs : Arguments() {
		/** The user whose reminders are being viewed. */
		val user by user {
			name = "user"
			description = "The user to view reminders for"
		}
	}

	inner class ReminderModRemoveArgs : Arguments() {
		/** The user whose reminders need removing. */
		val user by user {
			name = "user"
			description = "The user to remove the reminder for"
		}

		/** The number of the reminder to remove. */
		val reminder by long {
			name = "reminder-number"
			description = "The number of the reminder to remove. Use '/reminder mod-list' to get this"
		}
	}

	inner class ReminderModRemoveAllArgs : Arguments() {
		/** The user whose reminders need removing. */
		val user by user {
			name = "user"
			description = "The user to remove the reminders for"
		}

		/** The type of reminder to remove. */
		val type by stringChoice {
			name = "reminder-type"
			description = "The type of reminder to remove"
			choices = mutableMapOf(
				"repeating" to "repeating",
				"non-repeating" to "non-repeating",
				"all" to "all"
			)
		}
	}

	inner class ReminderRestrictionArgs : Arguments() {
		val restrict by boolean {
			name = "restrict"
			description = "Whether to restrict reminders to a single channel/channels"
		}

		val freeChannel by optionalChannel {
			name = "free-channel"
			description = "The channel where reminders can freely be used"
		}
	}
}
