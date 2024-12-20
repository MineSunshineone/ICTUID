package org.InCraftTime.iCTUID;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class CommandHandler implements CommandExecutor {

    private final ICTUID plugin;
    private final DatabaseManager databaseManager;
    private final UIDLogger uidLogger;

    public CommandHandler(ICTUID plugin, DatabaseManager databaseManager, UIDLogger uidLogger) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.uidLogger = uidLogger;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("ictuid.reload")) {
                sender.sendMessage(plugin.getMessage("permission_message"));
                return true;
            }
            plugin.reloadConfig();
            sender.sendMessage(plugin.getMessage("reload"));
            return true;
        } else if (command.getName().equalsIgnoreCase("changeuid")) {

            if (!sender.hasPermission("ictuid.changeuid")) {
                sender.sendMessage(plugin.getMessage("permission_message"));
                return true;
            }
            if (!isValidUID(args[1])) {
                sender.sendMessage("无效的UID格式，UID格式应该为4~10位");
                return false;
            }
            if (args.length != 2) {
                sender.sendMessage(plugin.getMessage("changeuid_usage"));
                return false;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage(plugin.getMessage("player_not_found"));
                return false;
            }
            String newUID = args[1];
            changePlayerUIDAsync(player.getUniqueId(), newUID);
            sender.sendMessage(plugin.getMessage("uid_changed").replace("%player%", player.getName()).replace("%newUID%", newUID));
            return true;
        } else if (command.getName().equalsIgnoreCase("backupdata")) {
            if (!sender.hasPermission("ictuid.backupdata")) {
                sender.sendMessage(plugin.getMessage("permission_message"));
                return true;
            }
            plugin.backupPlayerData();
            sender.sendMessage("玩家数据已备份");
            return true;
        } else if (command.getName().equalsIgnoreCase("restoredata")) {
            if (!sender.hasPermission("ictuid.restoredata")) {
                sender.sendMessage(plugin.getMessage("permission_message"));
                return true;
            }
            plugin.restorePlayerData();
            sender.sendMessage("玩家数据已恢复");
            return true;
        }
        if (command.getName().equalsIgnoreCase("queryuid")) {
            if (args.length != 1) {
                sender.sendMessage("Usage: /queryuid <player>");
                return false;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage("玩家不存在");
                return false;
            }
            String uid = getPlayerUID(player.getUniqueId());
            sender.sendMessage("玩家 " + player.getName() + " 的UID是这个: " + uid);
            return true;
        }
        if (command.getName().equalsIgnoreCase("queryuidhistory")) {
            if (args.length != 1) {
                sender.sendMessage("Usage: /queryuidhistory <player>");
                return false;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null) {
                sender.sendMessage("Player not found");
                return false;
            }
            queryUIDHistory(sender, player.getUniqueId());
            return true;
        }
        return false;
    }

    private void changePlayerUIDAsync(UUID playerUUID, String newUID) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection connection = databaseManager.getConnection();
                 PreparedStatement selectStatement = connection.prepareStatement("SELECT uid FROM players WHERE uuid = ?");
                 PreparedStatement updateStatement = connection.prepareStatement("UPDATE players SET uid = ? WHERE uuid = ?");
                 PreparedStatement insertHistoryStatement = connection.prepareStatement("INSERT INTO uid_history (uuid, old_uid, new_uid) VALUES (?, ?, ?)")) {
                selectStatement.setString(1, playerUUID.toString());
                ResultSet resultSet = selectStatement.executeQuery();
                if (resultSet.next()) {
                    String oldUID = resultSet.getString("uid");
                    updateStatement.setString(1, newUID);
                    updateStatement.setString(2, playerUUID.toString());
                    updateStatement.executeUpdate();
                    insertHistoryStatement.setString(1, playerUUID.toString());
                    insertHistoryStatement.setString(2, oldUID);
                    insertHistoryStatement.setString(3, newUID);
                    insertHistoryStatement.executeUpdate();
                    plugin.log("玩家 " + playerUUID + " 的 UID 已更改为 " + newUID);
                    Player player = Bukkit.getPlayer(playerUUID);
                    if (player != null) {
                        uidLogger.writeUIDToFile(playerUUID, player.getName(), newUID);
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    private String getPlayerUID(UUID playerUUID) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement("SELECT uid FROM players WHERE uuid = ?")) {
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString("uid");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "UID not found";
    }

    private void queryUIDHistory(CommandSender sender, UUID playerUUID) {
        try (Connection connection = databaseManager.getConnection();
             PreparedStatement selectStatement = connection.prepareStatement("SELECT old_uid, new_uid, change_time FROM uid_history WHERE uuid = ?")) {
            selectStatement.setString(1, playerUUID.toString());
            ResultSet resultSet = selectStatement.executeQuery();
            while (resultSet.next()) {
                String oldUID = resultSet.getString("old_uid");
                String newUID = resultSet.getString("new_uid");
                String changeTime = resultSet.getString("change_time");
                sender.sendMessage("Old UID: " + oldUID + ", New UID: " + newUID + ", Change Time: " + changeTime);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidUID(String uid) {
        return uid.matches("[A-Za-z0-9]{4,10}");
    }
}