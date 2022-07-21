package net.irisshaders.lilybot.database.entities

import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * The data for reminders set by users.
 *
 * @param initialSetTime The time the reminder was set
 * @param guildId The ID of the guild the reminder was set in
 * @param userId The ID of the user that would like to be reminded
 * @param channelId The ID of the channel the reminder was set in
 * @param remindTime The time the user would like to be reminded at
 * @param originalMessageUrl The URL to the original message that set the reminder
 * @param customMessage A custom message to attach to the reminder
 * @param repeating Whether the reminder should repeat
 * @param id The numerical ID of the reminder
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
