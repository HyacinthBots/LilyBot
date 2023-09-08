package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.AdaptedData

suspend fun configV7(db: MongoDatabase) {
	db.getCollection<AdaptedData>("ext-pluralkit").drop()
	db.getCollection<AdaptedData>("data-ext-pluralkit").drop()
}
