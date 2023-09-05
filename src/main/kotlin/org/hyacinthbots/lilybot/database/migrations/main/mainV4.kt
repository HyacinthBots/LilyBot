package org.hyacinthbots.lilybot.database.migrations.main

import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.hyacinthbots.lilybot.database.collections.GithubCollection
import org.hyacinthbots.lilybot.database.collections.WelcomeChannelCollection

suspend fun mainV4(db: MongoDatabase) {
	db.createCollection(WelcomeChannelCollection.name)
	db.createCollection(GithubCollection.name)
}
