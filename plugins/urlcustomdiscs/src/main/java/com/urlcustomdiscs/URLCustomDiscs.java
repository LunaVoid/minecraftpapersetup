package com.urlcustomdiscs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HexFormat;
import java.util.Objects;

// fork: the in-game half only. Downloading, converting and pack building happen in the disc bot (ADR 0006);
// this plugin reads the files it writes (discs.json, URLCustomDiscsPack.zip + .sha1) and does no heavy work.
public class URLCustomDiscs extends JavaPlugin implements Listener {
    private String minecraftServerVersion;
    private File audioToSendFolder;
    private File packFile;
    private String downloadResourcePackURL;
    private BotClient bot;

    @Override
    public void onEnable() {
        minecraftServerVersion = getServer().getBukkitVersion().split("-")[0]; // "26.3.build.227"
        getLogger().info("Detected Minecraft server version: " + minecraftServerVersion);

        loadConfig();
        audioToSendFolder = new File(getDataFolder(), "audio_to_send");
        audioToSendFolder.mkdirs();
        packFile = new File(getDataFolder(), "URLCustomDiscsPack.zip");

        Objects.requireNonNull(getCommand("customdisc")).setExecutor(new CommandURLCustomDiscs(this));

        // Detect the ProtocolLib plugin to enable custom Now Playing toasts
        boolean protocolLibEnabled = getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        getServer().getPluginManager().registerEvents(new JukeboxListener(this, protocolLibEnabled), this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        getDataFolder().mkdirs();
        if (!configFile.exists()) {
            try {
                Files.writeString(configFile.toPath(),
                        "# URLCustomDiscs (fork): the disc bot (discord-bot/ in our repo) downloads, converts and builds the pack.\n" +
                        "# This plugin only runs the /disc commands and the jukebox. Nothing is sent to the original author's API.\n" +
                        "\n" +
                        "# Where the disc bot answers, and its DISC_API_SECRET. Needed for /disc create, delete and updatedep.\n" +
                        "botURL: \"\"\n" +
                        "botSecret: \"\"\n" +
                        "\n" +
                        "# Public URL players download the disc pack from (the disc bot serves it at https://<host>/<PACK_TOKEN>/discs.zip).\n" +
                        "# Sent with its SHA-1 on join and after every change, so leave server.properties resource-pack= empty.\n" +
                        "downloadResourcePackURL: \"\"\n");
            } catch (IOException e) {
                getLogger().severe("Couldn't write config.yml: " + e.getMessage());
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        downloadResourcePackURL = config.getString("downloadResourcePackURL", "");
        bot = new BotClient(config.getString("botURL", ""), config.getString("botSecret", ""));
        if (config.contains("pluginUsageMode") || config.contains("zipFileAbsolutePath")) {
            getLogger().warning("config.yml is from an older version and is ignored where it differs. Delete it to regenerate.");
        }
        if (!bot.isConfigured()) getLogger().warning("config.yml: botURL/botSecret not set, so /disc create and delete won't work.");
        if (downloadResourcePackURL.isEmpty()) getLogger().warning("config.yml: downloadResourcePackURL is empty, so players won't hear discs.");
    }

    /** Push the disc pack with the SHA-1 the bot wrote next to it, so clients only re-download when it changed. */
    public void sendPack(Player player) {
        if (downloadResourcePackURL.isEmpty() || !packFile.exists()) return;
        try {
            String hex = Files.readString(new File(packFile.getPath() + ".sha1").toPath()).trim();
            player.setResourcePack(downloadResourcePackURL, HexFormat.of().parseHex(hex));
        } catch (Exception e) {
            player.setResourcePack(downloadResourcePackURL); // no/odd .sha1: still works, just without caching
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sendPack(event.getPlayer());
    }

    public String getMinecraftServerVersion() { return minecraftServerVersion; }
    public File getAudioToSendFolder() { return audioToSendFolder; }
    public BotClient getBot() { return bot; }
}
