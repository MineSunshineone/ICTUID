package org.InCraftTime.iCTUID;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public final class ICTUID extends JavaPlugin {

    private DatabaseManager databaseManager;
    private UIDLogger uidLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        log("插件已启动");
        databaseManager = new DatabaseManager(this);
        uidLogger = new UIDLogger(this);
        UIDCache uidCache = new UIDCache(); // 初始化 UIDCache
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, uidLogger, uidCache), this);
        CommandHandler commandHandler = new CommandHandler(this, databaseManager, uidLogger, uidCache); // 传递 UIDCache
        getCommand("reload").setExecutor(commandHandler);
        getCommand("changeuid").setExecutor(commandHandler);
        getCommand("backupdata").setExecutor(commandHandler);
        getCommand("restoredata").setExecutor(commandHandler);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ICTUIDPlaceholderExpansion(this).register();
        }
    }
    @Override
    public void onDisable() {
        log("插件已关闭");
        databaseManager.closeDataSource();
    }

    public void log(String message) {
        getLogger().info(message);
    }

    public void backupPlayerData() {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement("SELECT uuid, uid FROM players");
             BufferedWriter writer = new BufferedWriter(new FileWriter("plugins/ICTUID/player_data_backup.txt"))) {
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                writer.write(resultSet.getString("uuid") + " - " + resultSet.getString("uid"));
                writer.newLine();
            }
            log("玩家数据已备份");
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
    }

    public void restorePlayerData() {
        try (BufferedReader reader = new BufferedReader(new FileReader("plugins/ICTUID/player_data_backup.txt"));
             Connection connection = databaseManager.getConnection()) {
            String line;
            Map<String, String> playerData = new HashMap<>();
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" - ");
                if (parts.length == 2) {
                    playerData.put(parts[0], parts[1]);
                }
            }

            try (PreparedStatement deleteStatement = connection.prepareStatement("DELETE FROM players")) {
                deleteStatement.executeUpdate();
            }

            try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO players (uuid, uid) VALUES (?, ?)")) {
                for (Map.Entry<String, String> entry : playerData.entrySet()) {
                    insertStatement.setString(1, entry.getKey());
                    insertStatement.setString(2, entry.getValue());
                    insertStatement.executeUpdate();
                }
            }
            log("玩家数据已恢复");
        } catch (IOException | SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() throws SQLException {
        return databaseManager.getConnection();
    }

    public String getMessage(String key) {
        return getConfig().getString(key, "默认消息");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
}