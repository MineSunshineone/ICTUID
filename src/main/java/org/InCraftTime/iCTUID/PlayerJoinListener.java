package org.InCraftTime.iCTUID;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Firework;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.meta.FireworkMeta;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class PlayerJoinListener implements Listener {

    private final ICTUID plugin;
    private final UIDLogger uidLogger;
    private final UIDCache uidCache;
    private final Random random = new Random();

    public PlayerJoinListener(ICTUID plugin, UIDLogger uidLogger, UIDCache uidCache) {
        this.plugin = plugin;
        this.uidLogger = uidLogger;
        this.uidCache = uidCache;
    }

@EventHandler
public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID playerUUID = player.getUniqueId();
    String playerName = player.getName();
    String uid = uidCache.get(playerUUID);

    if (uid == null) {
        AtomicReference<String> uidRef = new AtomicReference<>();
        plugin.getDatabaseManager().getConnectionAsync().thenAccept(connection -> {
            try (PreparedStatement selectStatement = connection.prepareStatement("SELECT uid FROM players WHERE uuid = ?")) {
                selectStatement.setString(1, playerUUID.toString());
                ResultSet resultSet = selectStatement.executeQuery();

                if (!resultSet.next()) {
                    String newUid = generateUID();
                    try (PreparedStatement insertStatement = connection.prepareStatement("INSERT INTO players (uuid, uid) VALUES (?, ?)")) {
                        insertStatement.setString(1, playerUUID.toString());
                        insertStatement.setString(2, newUid);
                        insertStatement.executeUpdate();
                    }
                    uidLogger.writeUIDToFile(playerUUID, playerName, newUid);
                    plugin.getServer().broadcastMessage("Player " + playerName + " has been assigned UID: " + newUid);
                    uidRef.set(newUid);
                } else {
                    uidRef.set(resultSet.getString("uid"));
                }
                uidCache.put(playerUUID, uidRef.get());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }).thenRun(() -> {
            // 发送可定制消息
            String customMessage = ChatColor.GREEN + "欢迎加入ICT " + playerName + "! 这个是你在这个服务器独一无二的UID: " + uidRef.get();
            player.sendMessage(customMessage);

            // 播放声音
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            // 显示烟花效果
            spawnFireworks(player.getLocation(), 1);
        });
    } else {
        // 发送可定制消息
        String customMessage = ChatColor.GREEN + "欢迎加入ICT  " + playerName + "! 这个是你在这个服务器独一无二的UID:" + uid;
        player.sendMessage(customMessage);

        // 播放声音
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        // 显示烟花效果
        spawnFireworks(player.getLocation(), 1);
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
        int counter = plugin.getConfig().getInt("uid_counter");
        int uidLength = Integer.parseInt(customRule.replaceAll("[^0-9]", ""));
        String uid;
        int playerCount = 0;
        try (Connection connection = plugin.getDatabaseManager().getConnection()) {
            playerCount = getPlayerCount(connection);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (playerCount < counter) {
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
        try (Connection connection = plugin.getDatabaseManager().getConnection();
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

    private void spawnFireworks(Location location, int amount) {
        for (int i = 0; i < amount; i++) {
            Firework firework = location.getWorld().spawn(location, Firework.class);
            FireworkMeta meta = firework.getFireworkMeta();
            meta.addEffect(FireworkEffect.builder()
                    .withColor(org.bukkit.Color.RED)
                    .withColor(org.bukkit.Color.GREEN)
                    .withColor(org.bukkit.Color.BLUE)
                    .with(FireworkEffect.Type.BALL)
                    .withFlicker()
                    .withTrail()
                    .build());
            meta.setPower(1);
            firework.setFireworkMeta(meta);
        }
    }
}