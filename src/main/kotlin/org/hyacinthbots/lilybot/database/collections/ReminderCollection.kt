package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.flow.toList
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.koin.core.component.inject

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
 * @see removeGuildReminders
 * @see repeatReminder
 */
class ReminderCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ReminderData>(ReminderData.name)

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
		collection.find(eq(ReminderData::userId.name, userId)).toList()

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
	 * @param userId The ID of the user the reminder belongs too
	 * @param number The reminder to remove
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun removeReminder(userId: Snowflake, number: Long) =
		collection.deleteOne(and(eq(ReminderData::userId.name, userId), eq(ReminderData::id.name, number)))

	/**
	 * Removes all the reminders for a given guild.
	 *
	 * @param guildId The guild to remove reminders for
	 * @author NoComment1105
	 * @since 4.2.0
	 */
	suspend fun removeGuildReminders(guildId: Snowflake) = collection.deleteMany(eq(ReminderData::guildId.name, guildId))

	/**
	 * Updates a repeating reminder to be extended by the given [repeatingInterval].
	 *
	 * @param originalData The original time the reminder data
	 * @param repeatingInterval The repeating interval to extend the reminder by
	 * @author NoComment1105
	 * @since 4.5.0
	 */
	suspend fun repeatReminder(originalData: ReminderData, repeatingInterval: DateTimePeriod) {
		removeReminder(originalData.userId, originalData.id)

		collection.insertOne(
			ReminderData(
				originalData.guildId,
				originalData.remindTime.plus(repeatingInterval, TimeZone.UTC),
				originalData.setTime,
				originalData.userId,
				originalData.channelId,
				originalData.messageId,
				originalData.dm,
				originalData.customMessage,
				originalData.repeating,
				originalData.repeatingInterval,
				originalData.id
			)
		)
	}
}
