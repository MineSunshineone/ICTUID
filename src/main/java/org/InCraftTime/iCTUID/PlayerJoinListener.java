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
                int playerCount = getPlayerCount(connection);
                if (playerCount < 100) {
                    uid = String.valueOf(random.nextInt(100) + 1);
                } else {
                    int uidCounter = plugin.getConfig().getInt("uid_counter");
                    uid = String.valueOf(uidCounter);
                    plugin.getConfig().set("uid_counter", uidCounter + 1);
                    plugin.saveConfig();
                }

                try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO players (uuid, uid) VALUES (?, ?)")) {
                    insertStatement.setString(1, playerUUID.toString());
                    insertStatement.setString(2, uid);
                    insertStatement.executeUpdate();
                }

                uidLogger.writeUIDToFile(playerUUID, playerName, uid);
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
}