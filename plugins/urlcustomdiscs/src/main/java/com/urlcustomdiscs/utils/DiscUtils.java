package com.urlcustomdiscs.utils;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.bukkit.Bukkit.getLogger;

public class DiscUtils {
    // fork: discs.json is written by the disc bot; cache it by mtime so jukebox clicks don't re-read and re-parse it every time
    private static JSONObject cached = new JSONObject();
    private static long cachedMtime = -1;

    // Load data from the discs.json file
    public static synchronized JSONObject loadDiscData(File discUuidFile) {
        try {
            if (discUuidFile.exists()) {
                long mtime = discUuidFile.lastModified();
                if (mtime != cachedMtime) {
                    cached = new JSONObject(Files.readString(discUuidFile.toPath()));
                    cachedMtime = mtime;
                }
                return cached;
            }
        } catch (IOException e) {
            getLogger().severe("Exception: " + e.getMessage());
            getLogger().severe("Failed to load disc data: " + discUuidFile);
        }
        return new JSONObject(); // Returns an empty object on error
    }

    // Find the disc name from the CustomModelData
    public static String getDiscNameFromCustomModelData(JSONObject discData, int customModelData) {
        for (String key : discData.keySet()) {
            JSONObject disc = discData.getJSONObject(key);
            if (disc.has("customModelData") && disc.getInt("customModelData") == customModelData) {
                return key;
            }
        }
        return null; // If no disc matches, returns null
    }
}