package org.InCraftTime.iCTUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.UUID;

public final class ICTUID extends JavaPlugin {

    private Connection connection;
    private UIDLogger uidLogger;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        log("插件已启动");
        // 插件启动逻辑
        setupDatabase();
        uidLogger = new UIDLogger(this);
        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this, uidLogger), this);
        getCommand("reload").setExecutor(this);
        getCommand("changeuid").setExecutor(this);

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ICTUIDPlaceholderExpansion(this).register();
        }
    }

    @Override
    public void onDisable() {
        log("插件已关闭");
        // 插件关闭逻辑
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void setupDatabase() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:plugins/ICTUID/players.db");
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("CREATE TABLE IF NOT EXISTS players (uuid TEXT PRIMARY KEY, uid TEXT)");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public String getMessage(String key) {
        return getConfig().getString("messages." + key);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ictuid.reload")) {
                sender.sendMessage(getMessage("permission_message"));
                return true;
            }
            reloadConfig();
            sender.sendMessage(getMessage("reload"));
            return true;
        } else if (command.getName().equalsIgnoreCase("changeuid")) {
            if (!sender.hasPermission("ictuid.changeuid")) {
                sender.sendMessage(getMessage("permission_message"));
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(getMessage("changeuid_usage"));
                return false;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(getMessage("player_not_found"));
                return false;
            }
            String newUID = args[1];
            changePlayerUIDAsync(player.getUniqueId(), newUID);
            sender.sendMessage(getMessage("uid_changed").replace("%player%", player.getName()).replace("%newUID%", newUID));
            return true;
        }
        return false;
    }

    private void changePlayerUIDAsync(UUID playerUUID, String newUID) {
        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
            try (Connection connection = getConnection();
                 PreparedStatement updateStatement = connection.prepareStatement("UPDATE players SET uid = ? WHERE uuid = ?")) {
                updateStatement.setString(1, newUID);
                updateStatement.setString(2, playerUUID.toString());
                updateStatement.executeUpdate();
                log("玩家 " + playerUUID + " 的 UID 已更改为 " + newUID);

                Player player = Bukkit.getPlayer(playerUUID);
                if (player != null) {
                    uidLogger.writeUIDToFile(playerUUID, player.getName(), newUID);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private void log(String message) {
        getLogger().info(message);
    }
}