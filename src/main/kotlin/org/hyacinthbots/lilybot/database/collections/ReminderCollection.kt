package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.kotlindiscord.kord.extensions.utils.toDuration
import dev.kord.common.entity.Snowflake
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.koin.core.component.inject
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

/**
 * This class contains the functions for interacting with []the reminder database][ReminderData]. This
 * class contains functions for setting reminders, getting reminders based of various parameters, removing reminders and
 * repeating them.
 *
 * @since 4.2.0
 * @see getAllReminders
 * @see getRemindersForUser
 * @see getRemindersForUserInGuild
 * @see setReminder
 * @see removeReminder
 * @see repeatReminder
 */
class ReminderCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ReminderData>()

	/**
	 * Gets all the reminders currently in the database.
	 *
	 * @return A list of reminders in the database
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun getAllReminders(): List<ReminderData> = collection.find().toList()

	/**
	 * Gets all the reminders in the database for a specific user.
	 *
	 * @param userId The ID of the user to get reminders for
	 * @return A list of reminders for the given [userId]
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun getRemindersForUser(userId: Snowflake): List<ReminderData> =
		collection.find(ReminderData::userId eq userId).toList()

	/**
	 * Gets all the reminders in the database for a specific user, in a specific guild.
	 *
	 * @param userId The ID of the user to get reminders for
	 * @param guildId The ID of the guild the reminders should be in
	 * @return A list of reminders for the given [userId] in the given [guildId]
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun getRemindersForUserInGuild(userId: Snowflake, guildId: Snowflake): List<ReminderData> =
		getRemindersForUser(userId).filter { it.guildId == guildId }

	/**
	 * Sets a reminder.
	 *
	 * @param reminderData The data for the reminder
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun setReminder(reminderData: ReminderData) = collection.insertOne(reminderData)

	/**
	 * Removes a reminder from the database.
	 *
	 * @param number The reminder to remove
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun removeReminder(number: Long) = collection.deleteOne(ReminderData::id eq number)

	/**
	 * Updates a repeating reminder to be extended by the given [repeatingInterval].
	 *
	 * @param originalTime The original time the reminder was set
	 * @param repeatingInterval The repeating interval to extend the reminder by
	 * @param number The ID of the reminder to update
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun repeatReminder(originalTime: Instant, repeatingInterval: DateTimePeriod, number: Long) =
		collection.updateOne(
			ReminderData::id eq number,
			setValue(ReminderData::remindTime, originalTime.plus(repeatingInterval.toDuration(TimeZone.UTC)))
		)
}
