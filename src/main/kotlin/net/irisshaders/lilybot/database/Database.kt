package net.irisshaders.lilybot.database

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import net.irisshaders.lilybot.database.migrations.Migrator
import net.irisshaders.lilybot.utils.MONGO_URI
import org.bson.UuidRepresentation
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo

class Database {
	// Connect to the database using the provided connection URL
	private val settings = MongoClientSettings
		.builder()
		.uuidRepresentation(UuidRepresentation.STANDARD)
		.applyConnectionString(ConnectionString(MONGO_URI))
		.build()

	private val client = KMongo.createClient(settings).coroutine

	/** The main database for storing data. */
	val mainDatabase get() = client.getDatabase("LilyBot")

	/** The database for storing per guild configuration data. */
	val configDatabase get() = client.getDatabase("LilyBotConfig")

	suspend fun migrate() {
		Migrator.migrateMain()
		Migrator.migrateConfig()
	}
}
