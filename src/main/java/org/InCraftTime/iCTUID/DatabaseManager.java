package org.InCraftTime.iCTUID;

import java.sql.*;

public class DatabaseManager {

    private final ICTUID plugin;
    private Connection connection;

    public DatabaseManager(ICTUID plugin) {
        this.plugin = plugin;
        setupDatabase();
    }

 private void setupDatabase() {
    try {
        connection = DriverManager.getConnection("jdbc:sqlite:plugins/ICTUID/players.db");
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, uid TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS uid_history (uuid TEXT, old_uid TEXT, new_uid TEXT, change_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    public Connection getConnection() {
        return connection;
    }

    public void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}