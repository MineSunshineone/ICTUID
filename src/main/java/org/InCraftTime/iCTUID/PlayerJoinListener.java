package org.InCraftTime.iCTUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;

public class PlayerJoinListener implements Listener {

    private final ICTUID plugin;
    private final UIDLogger uidLogger;
    private final Random random = new Random();

    public PlayerJoinListener(ICTUID plugin, UIDLogger uidLogger) {
        this.plugin = plugin;
        this.uidLogger = uidLogger;
    }

@EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
    UUID playerUUID = event.getPlayer().getUniqueId();
    String playerName = event.getPlayer().getName();
    String uid;

    try (Connection connection = plugin.getConnection();
         PreparedStatement selectStatement = connection.prepareStatement("SELECT uid FROM players WHERE uuid = ?")) {
        selectStatement.setString(1, playerUUID.toString());
        ResultSet resultSet = selectStatement.executeQuery();

        if (!resultSet.next()) {
            uid = generateUID();
            try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO players (uuid, uid) VALUES (?, ?)")) {
                insertStatement.setString(1, playerUUID.toString());
                insertStatement.setString(2, uid);
                insertStatement.executeUpdate();
            }
            uidLogger.writeUIDToFile(playerUUID, playerName, uid);
            plugin.getServer().broadcastMessage("Player " + playerName + " has been assigned UID: " + uid);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
}

    private int getPlayerCount(Connection connection) throws SQLException {
        try (PreparedStatement countStatement = connection.prepareStatement("SELECT COUNT(*) AS count FROM players")) {
            ResultSet resultSet = countStatement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getInt("count");
            }
        }
        return 0;
    }

private String generateUID() {
    boolean allowDuplicate = plugin.getConfig().getBoolean("uid_allocation.allow_duplicate");
    String customRule = plugin.getConfig().getString("uid_allocation.custom_rule", "[A-Za-z0-9]{4,10}");
    int uidLength = Integer.parseInt(customRule.replaceAll("[^0-9]", ""));
    String uid;
    int playerCount = 0;
    try (Connection connection = plugin.getConnection()) {
        playerCount = getPlayerCount(connection);
    } catch (SQLException e) {
        e.printStackTrace();
    }

    if (playerCount < 100) {
        do {
            uid = String.format("%0" + uidLength + "d", random.nextInt((int) Math.pow(10, uidLength)));
        } while (!allowDuplicate && uidExists(uid));
    } else {
        int uidCounter = plugin.getConfig().getInt("uid_counter");
        uid = String.format("%0" + uidLength + "d", uidCounter);
        plugin.getConfig().set("uid_counter", uidCounter + 1);
        plugin.saveConfig();
    }
    return uid;
}

    private boolean uidExists(String uid) {
    try (Connection connection = plugin.getConnection();
         PreparedStatement selectStatement = connection.prepareStatement("SELECT COUNT(*) FROM players WHERE uid = ?")) {
        selectStatement.setString(1, uid);
        ResultSet resultSet = selectStatement.executeQuery();
        if (resultSet.next()) {
            return resultSet.getInt(1) > 0;
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return false;
}




}