package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * THe data for reminders in a guild.
 *
 * @property guildId The ID of the guild the reminder was set in
 * @property remindTime The time the reminder will be sent
 * @property setTime The time the reminder was set
 * @property userId The user that set the reminder
 * @property channelId The ID of the channel the reminder was set in
 * @property messageId The ID of the message
 * @property dm Whether to DM the reminder or not
 * @property customMessage A message to send with the reminder
 * @property repeating Whether the reminder should repeat or not
 * @property repeatingInterval The interval to repeat the reminder at, if repeating is true
 * @property id The numerical ID of the reminder
 *
 * @since 4.2.0
 */
@Serializable
data class ReminderData(
	val guildId: Snowflake,
	val remindTime: Instant,
	val setTime: Instant,
	val userId: Snowflake,
	val channelId: Snowflake,
	val messageId: Snowflake,
	val dm: Boolean,
	val customMessage: String?,
	val repeating: Boolean,
	val repeatingInterval: DateTimePeriod?,
	val id: Long
)
