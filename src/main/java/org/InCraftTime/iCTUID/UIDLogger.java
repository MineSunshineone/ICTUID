package org.InCraftTime.iCTUID;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

public class UIDLogger {

    private final ICTUID plugin;

    public UIDLogger(ICTUID plugin) {
        this.plugin = plugin;
    }

    public void writeUIDToFile(UUID playerUUID, String playerName, String uid) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("plugins/ICTUID/player_uids.txt", true))) {
            writer.write(playerUUID.toString() + " (" + playerName + ") - " + uid);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}