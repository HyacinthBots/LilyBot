package net.irisshaders.lilybot

import com.github.jezza.Toml
import com.github.jezza.TomlTable
import com.kotlindiscord.kord.extensions.ExtensibleBot
import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import mu.KotlinLogging
import net.irisshaders.lilybot.utils.BOT_TOKEN
import net.irisshaders.lilybot.utils.CUSTOM_COMMANDS_PATH
import net.irisshaders.lilybot.utils.DatabaseHelper
import net.irisshaders.lilybot.utils.MODE
import net.irisshaders.lilybot.utils.MONGO_URI
import net.irisshaders.lilybot.utils.Mode
import net.irisshaders.lilybot.utils.TEST_GUILD_ID
import net.irisshaders.lilybot.utils.addExtensions
import net.irisshaders.lilybot.utils.common
import org.bson.UuidRepresentation
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.reactivestreams.KMongo
import java.nio.file.Files
import java.nio.file.Path

val config: TomlTable = Toml.from(Files.newInputStream(Path.of(CUSTOM_COMMANDS_PATH)))

// Connect to the database
private val settings = MongoClientSettings
	.builder()
	.uuidRepresentation(UuidRepresentation.STANDARD)
	.applyConnectionString(ConnectionString(MONGO_URI))
	.build()

private val client = KMongo.createClient(settings).coroutine
val database = client.getDatabase("LilyBot")
private val lilylogger = KotlinLogging.logger { }

suspend fun main() {
	val bot = when (MODE) {
		"production" -> setupProduction()
		"development" -> setupDevelopment()

		else -> error("Invalid mode: $MODE")
	}

	bot.start()
}

/**
 * This functions adds all the features and settings that are independent of the [common] function and required for
 * running the bot in production.
 *
 * @author NoComment1105
 * @since 3.2.0
 */
suspend fun setupProduction() = ExtensibleBot(BOT_TOKEN) {
	applicationCommands {
		enabled = true
	}

	common()
	addExtensions(Mode.PRODUCTION)

	presence { playing(DatabaseHelper.getStatus()) }

	lilylogger.info("Loaded into PRODUCTION mode")
}

/**
 * This function adds all the features and settings that are independent of the [common] function and required for
 * running the bot in development.
 *
 * @author NoComment1105
 * @since 3.2.0
 */
suspend fun setupDevelopment() = ExtensibleBot(BOT_TOKEN) {
	applicationCommands {
		defaultGuild = TEST_GUILD_ID // Set default guild so we aren't waiting years for commands to appear
		enabled = true
	}

	common()
	addExtensions(Mode.DEVELOPMENT)

	presence { playing("in development") }

	lilylogger.info("Loaded into DEVELOPMENT mode")
}
