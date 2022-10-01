package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.koin.core.component.inject
import org.litote.kmongo.eq

class ReminderCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ReminderData>()

	suspend fun getAllReminders(): List<ReminderData?> = collection.find().toList()

	suspend fun getRemindersForGuild(guildId: Snowflake): List<ReminderData?> =
		collection.find(ReminderData::guildId eq guildId).toList()

	suspend fun getRemindersForUser(userId: Snowflake): List<ReminderData?> =
		collection.find(ReminderData::owner eq userId).toList()

	suspend fun setReminder(reminderData: ReminderData) = collection.insertOne(reminderData)

	suspend fun removeReminder(number: Long) = collection.deleteOne(ReminderData::id eq number)

	suspend fun removeAllReminders() = collection.deleteMany()

	suspend fun removeRemindersForGuild(guildId: Snowflake) =
		collection.deleteMany(ReminderData::guildId eq guildId)

	suspend fun removeRemindersForUser(userId: Snowflake) =
		collection.deleteMany(ReminderData::owner eq userId)
}
