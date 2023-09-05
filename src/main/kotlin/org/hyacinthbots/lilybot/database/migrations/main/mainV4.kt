package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.entities.GithubData
import org.hyacinthbots.lilybot.database.entities.WelcomeChannelData

suspend fun mainV4(db: MongoDatabase) {
	db.createCollection(WelcomeChannelData.name)
	db.createCollection(GithubData.name)
}
