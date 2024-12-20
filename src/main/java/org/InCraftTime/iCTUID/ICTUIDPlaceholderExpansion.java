package org.InCraftTime.iCTUID;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class ICTUIDPlaceholderExpansion extends PlaceholderExpansion {

    private final ICTUID plugin;

    public ICTUIDPlaceholderExpansion(ICTUID plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "ictuid";
    }

    @Override
    public @NotNull String getAuthor() {
        return plugin.getDescription().getAuthors().toString();
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String identifier) {
        if (player == null) {
            return "";
        }

        if (identifier.equals("uid")) {
            return getPlayerUID(player.getUniqueId());
        }

        return null;
    }

    private String getPlayerUID(UUID playerUUID) {
        try (Connection connection = plugin.getConnection();
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
}