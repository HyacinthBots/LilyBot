package net.irisshaders.lilybot.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates the database and the table.
 */
public class SQLiteDataSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLiteDataSource.class);
    private static final HikariConfig config = new HikariConfig();
    private static final HikariDataSource dataSource;

    static {
        try {
            Path database = Path.of("database.db");
            if (Files.notExists(database)) {
                Files.createFile(database);
                LOGGER.info("Created database file!");
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
            LOGGER.info("Warn table initialised!");
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    /**
     * A method to use wherever any SQL requests are made.
     * @return The SQL connection.
     * @throws SQLException if a database access error occurs.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

}
