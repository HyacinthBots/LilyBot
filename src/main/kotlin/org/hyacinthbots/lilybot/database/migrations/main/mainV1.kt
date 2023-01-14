package org.hyacinthbots.lilybot.database.migrations.main

import org.hyacinthbots.lilybot.database.entities.StatusData
import org.litote.kmongo.coroutine.CoroutineDatabase

@Suppress("UnusedPrivateMember")
// This was commented out due to the remindme data class being removed
suspend fun mainV1(db: CoroutineDatabase, configDb: CoroutineDatabase) {
// 	val reminders = db.getCollection<RemindMeData>("remindMeData")
//
// 	val repeating = mutableListOf<ReplaceOneModel<RemindMeData>>()
// 	val nonRepeating = mutableListOf<ReplaceOneModel<RemindMeData>>()

// 	reminders.find().consumeEach {
// 		if (it.repeating) {
// 			repeating.add(
// 				replaceOne(
// 					RemindMeData::initialSetTime eq it.initialSetTime,
//
// 					RemindMeData(
// 						it.initialSetTime,
// 						it.guildId,
// 						it.userId,
// 						it.channelId,
// 						it.remindTime,
// 						it.originalMessageUrl,
// 						it.customMessage,
// 						true,
// 						DateTimePeriod(days = 1),
// 						it.id
// 					),
//
// 					replaceUpsert()
// 				)
// 			)
// 		} else {
// 			nonRepeating.add(
// 				replaceOne(
// 					RemindMeData::initialSetTime eq it.initialSetTime,
//
// 					RemindMeData(
// 						it.initialSetTime,
// 						it.guildId,
// 						it.userId,
// 						it.channelId,
// 						it.remindTime,
// 						it.originalMessageUrl,
// 						it.customMessage,
// 						false,
// 						null,
// 						it.id
// 					),
//
// 					replaceUpsert()
// 				)
// 			)
// 		}
// 	}

// 	if (repeating.isNotEmpty()) {
// 		reminders.bulkWrite(requests = repeating, BulkWriteOptions().ordered(true))
// 	}
// 	if (nonRepeating.isNotEmpty()) {
// 		reminders.bulkWrite(requests = nonRepeating, BulkWriteOptions().ordered(true))
// 	}

	db.dropCollection("statusData")
	db.createCollection("statusData")
	db.getCollection<StatusData>("statusData").insertOne(StatusData(null))
}
