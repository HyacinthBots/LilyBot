package org.hyacinthbots.lilybot.database.migrations.main

import dev.kord.common.entity.Snowflake
import org.hyacinthbots.lilybot.database.entities.ReminderData
import org.litote.kmongo.coroutine.CoroutineDatabase
import org.litote.kmongo.eq

@Suppress("UnderscoresInNumericLiterals")
suspend fun mainV5(db: CoroutineDatabase) {
	db.getCollection<ReminderData>().deleteMany(ReminderData::userId eq Snowflake(462348944173957120))
}
