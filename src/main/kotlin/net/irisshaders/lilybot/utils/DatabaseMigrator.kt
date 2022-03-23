package net.irisshaders.lilybot.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

/**
 * The Database system within the bot
 * @author chalkyjeans
 */

val logger = KotlinLogging.logger { }
val config = HikariConfig()
lateinit  var dataSource: HikariDataSource

fun migrateDatabase() {

	// start the database
	DriverManager.getConnection("jdbc:sqlite:./data/database.db")

	transaction {
		SchemaUtils.createMissingTablesAndColumns(
			OldDatabaseManager.Config,
			OldDatabaseManager.Components,
		)
	}

	// connect to the database
	config.jdbcUrl = "jdbc:sqlite:./data/database.db"
	config.connectionTestQuery = "SELECT 1"
	config.addDataSourceProperty("cachePrepStmts", "true")
	config.addDataSourceProperty("prepStmtCacheSize", "250")
	config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

	dataSource = HikariDataSource(config)
	Database.connect(dataSource)

	logger.info("Connected to old database.")

	// get all old values
	val oldConfigs = transaction {
		OldDatabaseManager.Config.selectAll()
	}

	val oldComponents = transaction {
		OldDatabaseManager.Components.selectAll()
	}

	// map old values to new type

	// put old values into new database
}

object OldDatabaseManager {
	object Config : Table("config") {
		val guildId = text("guildId")
		val moderatorsPing = text("moderatorsPing")
		val modActionLog = text("modActionLog")
		val messageLogs = text("messageLogs")
		val joinChannel = text("joinChannel")
		val supportChannel = text("supportChannel").nullable()
		val supportTeam = text("supportTeam").nullable()

		override val primaryKey = PrimaryKey(guildId)
	}

	object Components : Table("components") {
		val componentId = text("componentId")
		val roleId = text("roleId")
		val addOrRemove = text("addOrRemove")

		override val primaryKey = PrimaryKey(componentId)
	}
}
