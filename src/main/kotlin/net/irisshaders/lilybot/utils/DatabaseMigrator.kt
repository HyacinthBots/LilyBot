package net.irisshaders.lilybot.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import dev.kord.common.entity.Snowflake
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.DriverManager

/**
 * A system to migrate a database from SQLite to MongoDB. Based on code by chalkyjeans.
 * @author tempest15
 */
object OldDatabaseManager {
	private val logger = KotlinLogging.logger { }
	private val config = HikariConfig()
	private var dataSource: HikariDataSource

	init {
		config.jdbcUrl = JDBC_URL
		config.connectionTestQuery = "SELECT 1"
		config.addDataSourceProperty("cachePrepStmts", "true")
		config.addDataSourceProperty("prepStmtCacheSize", "250")
		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

		dataSource = HikariDataSource(config)
		Database.connect(dataSource)

		logger.info("Connected to database.")
	}

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

	fun startDatabase() {
		DriverManager.getConnection(JDBC_URL)

		transaction {
			SchemaUtils.createMissingTablesAndColumns(Config, Components)
		}
	}
}

fun migrateDatabase() {
	val oldConfigs = transaction { OldDatabaseManager.Config.selectAll() }
	val oldComponents = transaction { OldDatabaseManager.Components.selectAll() }

	transaction {
		for(cfg in oldConfigs) {
			val supportChannel: Snowflake? =
				if (cfg[OldDatabaseManager.Config.supportChannel] != "null") {
					Snowflake(cfg[OldDatabaseManager.Config.supportChannel]!!)
				} else {
					null
				}

			val supportTeam: Snowflake? =
				if (cfg[OldDatabaseManager.Config.supportTeam] != "null") {
					Snowflake(cfg[OldDatabaseManager.Config.supportTeam]!!)
				} else {
					null
				}

			val newConfig = ConfigData(
				Snowflake(cfg[OldDatabaseManager.Config.guildId]),
				Snowflake(cfg[OldDatabaseManager.Config.moderatorsPing]),
				Snowflake(cfg[OldDatabaseManager.Config.modActionLog]),
				Snowflake(cfg[OldDatabaseManager.Config.messageLogs]),
				Snowflake(cfg[OldDatabaseManager.Config.joinChannel]),
				supportChannel,
				supportTeam
				)

			runBlocking { DatabaseHelper.setConfig(newConfig) }
		}

		for(cmp in oldComponents) {
			val newComponent = ComponentData(
				cmp[OldDatabaseManager.Components.componentId],
				Snowflake(cmp[OldDatabaseManager.Components.roleId]),
				cmp[OldDatabaseManager.Components.addOrRemove]
			)

			runBlocking { DatabaseHelper.setComponent(newComponent) }
		}
	}
}
