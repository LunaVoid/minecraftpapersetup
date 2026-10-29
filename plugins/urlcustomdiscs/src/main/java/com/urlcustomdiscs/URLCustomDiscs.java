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
    private String botURL;
    private String botSecret;
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
                        "# The disc bot's public address, e.g. https://packs.example.org. The only setting you need.\n" +
                        "botURL: \"\"\n" +
                        "\n" +
                        "# Leave these empty: they're read from .disc-secrets.json, which the disc bot writes into this folder.\n" +
                        "# botSecret = the bot's DISC_API_SECRET; downloadResourcePackURL defaults to <botURL>/<PACK_TOKEN>/discs.zip.\n" +
                        "# The pack is sent with its SHA-1 on join and after every change, so leave server.properties resource-pack= empty.\n" +
                        "botSecret: \"\"\n" +
                        "downloadResourcePackURL: \"\"\n");
            } catch (IOException e) {
                getLogger().severe("Couldn't write config.yml: " + e.getMessage());
            }
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        botURL = config.getString("botURL", "").replaceAll("/+$", "");
        botSecret = config.getString("botSecret", "");
        downloadResourcePackURL = config.getString("downloadResourcePackURL", "");
        if (config.contains("pluginUsageMode") || config.contains("zipFileAbsolutePath")) {
            getLogger().warning("config.yml is from an older version and is ignored where it differs. Delete it to regenerate.");
        }
        if (botURL.isEmpty()) getLogger().warning("config.yml: botURL is not set, so discs can't be made or heard.");
    }

    /** Push the disc pack with the SHA-1 the bot wrote next to it, so clients only re-download when it changed. */
    public void sendPack(Player player) {
        String url = packUrl();
        if (url.isEmpty() || !packFile.exists()) return;
        try {
            String hex = Files.readString(new File(packFile.getPath() + ".sha1").toPath()).trim();
            player.setResourcePack(url, HexFormat.of().parseHex(hex));
        } catch (Exception e) {
            player.setResourcePack(url); // no/odd .sha1: still works, just without caching
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        sendPack(event.getPlayer());
    }

    public String getMinecraftServerVersion() { return minecraftServerVersion; }
    public File getAudioToSendFolder() { return audioToSendFolder; }
    // Secrets the disc bot generated and saved next to us; read lazily so start order doesn't matter.
    private org.json.JSONObject botSecrets() {
        try {
            return new org.json.JSONObject(Files.readString(new File(getDataFolder(), ".disc-secrets.json").toPath()));
        } catch (Exception e) {
            return new org.json.JSONObject();
        }
    }

    private String packUrl() {
        if (!downloadResourcePackURL.isEmpty()) return downloadResourcePackURL;
        String token = botSecrets().optString("pack_token");
        return botURL.isEmpty() || token.isEmpty() ? "" : botURL + "/" + token + "/discs.zip";
    }

    public BotClient getBot() {
        if (bot == null || !bot.isConfigured()) {
            bot = new BotClient(botURL, botSecret.isEmpty() ? botSecrets().optString("api_secret") : botSecret);
        }
        return bot;
    }
}
