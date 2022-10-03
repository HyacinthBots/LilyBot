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

// TODO Doc this to the moon!
class ReminderCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ReminderData>()

	suspend fun getAllReminders(): List<ReminderData> = collection.find().toList()

	suspend fun getRemindersForUser(userId: Snowflake): List<ReminderData> =
		collection.find(ReminderData::userId eq userId).toList()

	suspend fun getRemindersForUserInGuild(userId: Snowflake, guildId: Snowflake): List<ReminderData> =
		getRemindersForUser(userId).filter { it.guildId == guildId }

	suspend fun setReminder(reminderData: ReminderData) = collection.insertOne(reminderData)

	suspend fun removeReminder(number: Long) = collection.deleteOne(ReminderData::id eq number)

	suspend fun repeatReminder(originalTime: Instant, repeatingInterval: DateTimePeriod, number: Long) =
		collection.updateOne(
			ReminderData::id eq number,
			setValue(ReminderData::remindTime, originalTime.plus(repeatingInterval.toDuration(TimeZone.UTC)))
		)
}
