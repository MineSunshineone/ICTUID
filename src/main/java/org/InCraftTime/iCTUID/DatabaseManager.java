package org.InCraftTime.iCTUID;

import java.sql.*;
import java.util.concurrent.CompletableFuture;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseManager {

    private final ICTUID plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ICTUID plugin) {
        this.plugin = plugin;
        setupDataSource();
    }

    private void setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:plugins/ICTUID/players.db");
        config.setMaximumPoolSize(10);
        dataSource = new HikariDataSource(config);
    }

    public CompletableFuture<Connection> getConnectionAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return dataSource.getConnection();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void closeDataSource() {
        if (dataSource != null) {
            dataSource.close();
        }
    }

    public Connection getConnection() {
        try {
            return dataSource.getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}