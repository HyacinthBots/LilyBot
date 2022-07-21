package net.irisshaders.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import net.irisshaders.lilybot.database.Database
import net.irisshaders.lilybot.database.entities.RemindMeData
import org.koin.core.component.inject
import org.litote.kmongo.eq

/**
 * This object contains the functions or interacting with the [Reminder Database][RemindMeData]. This object contains
 * functions for getting, setting and removing reminders.
 *
 * @since 4.0.0
 * @see getReminders
 * @see setReminder
 * @see removeReminder
 */
class RemindMeCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<RemindMeData>()

	/**
	 * Gets every reminder in the database.
	 *
	 * @return A [List] of reminders from the database
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	suspend inline fun getReminders(): List<RemindMeData> =
		collection.find().toList()

	/**
	 * Stores a reminder in the database.
	 *
	 * @param initialSetTime The time the reminder was set
	 * @param inputGuildId The ID of the guild the reminder was set in
	 * @param inputUserId The ID of the user that would like to be reminded
	 * @param inputChannelId The ID of the channel the reminder was set in
	 * @param remindTime The time the user would like to be reminded at
	 * @param originalMessageUrl The URL to the original message that set the reminder
	 * @param customMessage A custom message to attach to the reminder
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	suspend inline fun setReminder(
		initialSetTime: Instant,
		inputGuildId: Snowflake,
		inputUserId: Snowflake,
		inputChannelId: Snowflake,
		remindTime: Instant,
		originalMessageUrl: String,
		customMessage: String?,
		repeating: Boolean,
		repeatingInterval: DateTimePeriod?,
		id: Int
	) =
		collection.insertOne(
			RemindMeData(
				initialSetTime,
				inputGuildId,
				inputUserId,
				inputChannelId,
				remindTime,
				originalMessageUrl,
				customMessage,
				repeating,
				repeatingInterval,
				id
			)
		)

	/**
	 * Removes old reminders from the Database.
	 *
	 * @param inputGuildId The ID of the guild the reminder was set in
	 * @param inputUserId The ID of the user the reminder was set by
	 * @param id The numerical ID of the reminder
	 *
	 * @since 3.3.2
	 * @author NoComment1105
	 */
	suspend inline fun removeReminder(
		inputGuildId: Snowflake,
		inputUserId: Snowflake,
		id: Int
	) =
		collection.deleteOne(
			RemindMeData::guildId eq inputGuildId,
			RemindMeData::userId eq inputUserId,
			RemindMeData::id eq id
		)
}
