package org.hyacinthbots.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The data for reminders set by users.
 *
 * @property initialSetTime The time the reminder was set
 * @property guildId The ID of the guild the reminder was set in
 * @property userId The ID of the user that would like to be reminded
 * @property channelId The ID of the channel the reminder was set in
 * @property remindTime The time the user would like to be reminded at
 * @property originalMessageUrl The URL to the original message that set the reminder
 * @property customMessage A custom message to attach to the reminder
 * @property repeating Whether the reminder should repeat
 * @property id The numerical ID of the reminder
 *
 * @since 3.3.2
 */
@Serializable
data class RemindMeData(
	val initialSetTime: Instant,
	val guildId: Snowflake,
	val userId: Snowflake,
	val channelId: Snowflake,
	val remindTime: Instant,
	val originalMessageUrl: String,
	val customMessage: String?,
	val repeating: Boolean,
	val repeatingInterval: DateTimePeriod?,
	val id: Int
)
