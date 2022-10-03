package org.hyacinthbots.lilybot.database.collections

import com.kotlindiscord.kord.extensions.koin.KordExKoinComponent
import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.Database
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.koin.core.component.inject
import org.litote.kmongo.eq

// TODO Doc this to the moon!
class ReminderCollection : KordExKoinComponent {
	private val db: Database by inject()

	@PublishedApi
	internal val collection = db.mainDatabase.getCollection<ReminderData>()

	suspend fun getAllReminders(): List<ReminderData> = collection.find().toList()

	suspend fun getRemindersForUser(userId: Snowflake): List<ReminderData> =
		collection.find(ReminderData::owner eq userId).toList()

	suspend fun getReminderFromMessageId(messageId: Snowflake): ReminderData? =
		collection.findOne(ReminderData::messageId eq messageId)

	suspend fun getRemindersForUserInGuild(userId: Snowflake, guildId: Snowflake): List<ReminderData> =
		getRemindersForUser(userId).filter { it.guildId == guildId }

	suspend fun setReminder(reminderData: ReminderData) = collection.insertOne(reminderData)

	suspend fun updateReminder(messageId: Snowflake, oldData: ReminderData, newSubscribers: MutableList<Snowflake>) =
		collection.replaceOne(
			ReminderData::messageId eq messageId,
			ReminderData(
				oldData.guildId,
				oldData.remindTime,
				oldData.setTime,
				oldData.owner,
				newSubscribers,
				oldData.channelId,
				oldData.messageId,
				oldData.dm,
				oldData.customMessage,
				oldData.repeating,
				oldData.repeatingInterval,
				oldData.id
			)
		)

	suspend fun removeReminder(number: Long) = collection.deleteOne(ReminderData::id eq number)
}
