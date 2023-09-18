package org.hyacinthbots.lilybot.database

import com.kotlindiscord.kord.extensions.adapters.mongodb.kordExCodecRegistry
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.kotlin.client.coroutine.MongoClient
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecRegistries
import org.hyacinthbots.lilybot.database.migrations.Migrator
import org.hyacinthbots.lilybot.utils.MONGO_URI

class Database {
	private val codecRegistries = CodecRegistries.fromRegistries(
		kordExCodecRegistry,
		MongoClientSettings.getDefaultCodecRegistry()
	)

	// Connect to the database using the provided connection URL
	private val settings = MongoClientSettings
		.builder()
		.uuidRepresentation(UuidRepresentation.STANDARD)
		.applyConnectionString(ConnectionString(MONGO_URI))
		.codecRegistry(codecRegistries)
		.build()

	private val client = MongoClient.create(settings)

	/** The main database for storing data. */
	val mainDatabase get() = client.getDatabase("LilyBot")

	/** The database for storing per guild configuration data. */
	val configDatabase get() = client.getDatabase("LilyBotConfig")

	/**
	 * Runs the migrations for both databases.
	 *
	 * @since 4.0.0
	 */
	suspend fun migrate() {
		Migrator.migrateMain()
		Migrator.migrateConfig()
	}
}
