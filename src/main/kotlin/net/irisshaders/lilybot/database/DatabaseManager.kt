package net.irisshaders.lilybot.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.utils.io.errors.*
import net.irisshaders.lilybot.utils.JDBC_URL
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path

object DatabaseManager {

    private val LOGGER: Logger = LoggerFactory.getLogger("Database")
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
        LOGGER.info("Connected to Database")
    }

    object Warn: Table("warn") {
        val id = text("id")
        val points = text("points").nullable()
        override val primaryKey = PrimaryKey(id)
    }

    fun startDatabase() {
        try {
            val database = Path.of("database.db")
            if (Files.notExists(database)) {
                Files.createFile(database)
                LOGGER.info("Created database file!")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        transaction {
            SchemaUtils.createMissingTablesAndColumns(Warn)
        }
    }

}