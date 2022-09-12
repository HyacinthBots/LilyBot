package org.hyacinthbots.lilybot.database.migrations.main

import kotlinx.datetime.Clock
import org.hyacinthbots.lilybot.database.entities.CleanupsData
import org.litote.kmongo.coroutine.CoroutineDatabase
import kotlin.time.Duration.Companion.days

suspend fun mainV2(db: CoroutineDatabase) {
	db.createCollection("cleanupsData")
	db.getCollection<CleanupsData>("cleanupsData").insertOne(
		CleanupsData(Clock.System.now().plus(30.days), Clock.System.now().plus(7.days))
	)
}
