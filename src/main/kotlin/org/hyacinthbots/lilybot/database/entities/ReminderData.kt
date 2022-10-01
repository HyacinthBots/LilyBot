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
 * @property owner The user that set the reminder
 * @property subscribers Users that have subscribed to a reminder
 * @property channelId The ID of the channel the reminder was set in
 * @property setMessageUrl The URL to the message that is sent after the reminder
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
	val owner: Snowflake,
	val subscribers: List<Snowflake>,
	val channelId: Snowflake,
	val setMessageUrl: String,
	val customMessage: String?,
	val repeating: Boolean,
	val repeatingInterval: DateTimePeriod?,
	val id: Long
)
