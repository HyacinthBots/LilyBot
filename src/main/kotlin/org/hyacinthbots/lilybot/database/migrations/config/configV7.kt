package org.hyacinthbots.lilybot.database.migrations.config

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import kotlinx.coroutines.flow.toList
import org.hyacinthbots.lilybot.database.entities.AdaptedData

suspend fun configV7(db: MongoDatabase, tempKeDb: MongoDatabase) {
	// Done twice due to updates in the data-adapter
	with(db.getCollection<AdaptedData>("ext-pluralkit")) {
		val data = find().toList()

		tempKeDb.createCollection("data-ext-pluralkit") // create new with correct name
		tempKeDb.getCollection<AdaptedData>("data-ext-pluralkit").insertMany(data) // add the data
		drop() // drop old
	}

	with(db.getCollection<AdaptedData>("data-ext-pluralkit")) {
		val data = find().toList()

		tempKeDb.createCollection("data-ext-pluralkit") // create new
		tempKeDb.getCollection<AdaptedData>("data-ext-pluralkit").insertMany(data) // add the data
		drop() // drop old
	}
}
