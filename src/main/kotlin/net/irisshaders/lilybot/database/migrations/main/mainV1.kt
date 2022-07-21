package net.irisshaders.lilybot.database.migrations.main

import com.mongodb.client.model.BulkWriteOptions
import com.mongodb.client.model.ReplaceOneModel
import kotlinx.datetime.DateTimePeriod
import net.irisshaders.lilybot.database.entities.RemindMeData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq
import org.litote.kmongo.replaceOne
import org.litote.kmongo.replaceUpsert

suspend fun mainV1(db: CoroutineDatabase) {
	val reminders = db.getCollection<RemindMeData>("remindMeData")

	val repeating = mutableListOf<ReplaceOneModel<RemindMeData>>()
	val nonRepeating = mutableListOf<ReplaceOneModel<RemindMeData>>()

	reminders.find().consumeEach {
		if (it.repeating) {
			repeating.add(
				replaceOne(
					RemindMeData::initialSetTime eq it.initialSetTime,

					RemindMeData(
						it.initialSetTime,
						it.guildId,
						it.userId,
						it.channelId,
						it.remindTime,
						it.originalMessageUrl,
						it.customMessage,
						true,
						DateTimePeriod(days = 1),
						it.id
					),

					replaceUpsert()
				)
			)
		} else {
			nonRepeating.add(
				replaceOne(
					RemindMeData::initialSetTime eq it.initialSetTime,

					RemindMeData(
						it.initialSetTime,
						it.guildId,
						it.userId,
						it.channelId,
						it.remindTime,
						it.originalMessageUrl,
						it.customMessage,
						false,
						null,
						it.id
					),

					replaceUpsert()
				)
			)
		}
	}

	if (repeating.isNotEmpty()) {
		reminders.bulkWrite(requests = repeating, BulkWriteOptions().ordered(true))
	}
	if (nonRepeating.isNotEmpty()) {
		reminders.bulkWrite(requests = nonRepeating, BulkWriteOptions().ordered(true))
	}
}
