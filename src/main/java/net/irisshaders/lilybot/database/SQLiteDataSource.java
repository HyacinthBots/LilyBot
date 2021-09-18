package net.irisshaders.lilybot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class SQLiteDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteDataSource.class);
    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource dataSource;

    static {
        try {
            File database = new File("database.db");
            if (!database.exists()) {
                if (database.createNewFile()) {
                    LOGGER.info("Created database file!");
                } else {
                    LOGGER.error("Could not create database file!");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        config.setJdbcUrl("jdbc:sqlite:database.db");
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        dataSource = new HikariDataSource(config);

        try (Statement statement = getConnection().createStatement()) {
            @Language("SQL")
            String tableString = "CREATE TABLE IF NOT EXISTS warn(id INTEGER UNIQUE, points INTEGER)";
            statement.execute(tableString);
            LOGGER.info("Table initialised!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
