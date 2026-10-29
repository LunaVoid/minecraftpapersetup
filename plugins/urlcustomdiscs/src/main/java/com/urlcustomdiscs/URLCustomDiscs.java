package com.urlcustomdiscs;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Objects;

public class URLCustomDiscs extends JavaPlugin implements org.bukkit.event.Listener {
    private final String os = System.getProperty("os.name").toLowerCase();
    private String minecraftServerVersion;

    private File audioToSendFolder;
    private File binFolder;
    private File editResourcePackFolder;
    private File tempAudioFolder;
    private File tempUnpackedFolder;

    private String downloadResourcePackURL;
    private String zipFileAbsolutePath;
    private int maxSeconds;

    @Override
    public void onEnable() {
        getLogger().info("URLCustomDiscs enabled !");
        getLogger().info("Running on OS: " + getOperatingSystem());

        // Retrieve the Minecraft server version
        String fullVersion = getServer().getBukkitVersion(); // "1.21.4-R0.1-SNAPSHOT"
        minecraftServerVersion = fullVersion.split("-")[0];  // "1.21.4"
        getLogger().info("Detected Minecraft server version: " + minecraftServerVersion);

        if (minecraftServerVersion == null) {
            minecraftServerVersion = "1.21.4";
            getLogger().warning("Failed to parse Minecraft version from Bukkit version. Defaulting to: " + minecraftServerVersion);
        }

        loadConfig(); // Load or create the configuration file
        loadFiles(); // Load or create folders

        OS os = getOperatingSystemEnum();
        new DenoSetup(this, os).setup();
        new YtDlpSetup(this, os).setup();
        new FFmpegSetup(this, os).setup();

        installDefaultPack();
        SelfHostedManager selfHostedManager = new SelfHostedManager(this, os);

        Objects.requireNonNull(this.getCommand("customdisc")).setExecutor(new CommandURLCustomDiscs(this, os, selfHostedManager));

        // Detect the ProtocolLib plugin to enable custom Now Playing toasts
        boolean protocolLibEnabled = getServer().getPluginManager().isPluginEnabled("ProtocolLib");
        if (protocolLibEnabled) {
            getLogger().info("ProtocolLib detected! Custom Now Playing toasts will be available.");
            getServer().getPluginManager().registerEvents(new JukeboxListener(this, true), this);
        } else {
            getLogger().warning("ProtocolLib is not installed! The plugin will work normally, but custom Now Playing toasts will not be available.");
            getServer().getPluginManager().registerEvents(new JukeboxListener(this, false), this);
        }
        getServer().getPluginManager().registerEvents(this, this); // fork: pack on join (ADR 0005)
    }

    @Override
    public void onDisable() {
        getLogger().info("URLCustomDiscs disabled!");
    }

    public enum OS {
        WINDOWS_X64,
        WINDOWS_ARM64,
        LINUX_X64,
        LINUX_ARM64,
        LINUX_ARMV7,
        LINUX_MUSL_X64,
        LINUX_MUSL_ARM64,
        OTHER
    }
    public OS getOperatingSystemEnum() {
        String name = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

        boolean isArm = arch.contains("aarch64") || arch.contains("arm");
        boolean isArmV7 = arch.contains("armv7");
        boolean isMusl = new File("/lib/ld-musl-x86_64.so.1").exists() || new File("/lib/ld-musl-aarch64.so.1").exists();

        if (name.contains("win")) {
            if (isArm) return OS.WINDOWS_ARM64;
            return OS.WINDOWS_X64;
        }

        if (name.contains("nux") || name.contains("nix")) {
            if (isMusl) {
                if (isArm) return OS.LINUX_MUSL_ARM64;
                return OS.LINUX_MUSL_X64;
            } else if (isArmV7) {
                return OS.LINUX_ARMV7;
            } else if (isArm) {
                return OS.LINUX_ARM64;
            } else {
                return OS.LINUX_X64;
            }
        }

        return OS.OTHER;
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");

        // Create the plugins/URLCustomDiscs folder
        getDataFolder().mkdirs();

        // Create the plugins/URLCustomDiscs/config.yml file
        if (!configFile.exists()) {
            try {
                configFile.createNewFile();
                // fork: self-hosted only. The plugin edits the disc pack on this server's disk; players download it from downloadResourcePackURL.
                String configContent =
                        "# URLCustomDiscs (fork): everything runs on this server. Nothing is sent to the original author's API.\n" +
                                "\n" +
                                "# Where the disc pack lives on this server's disk. Created empty on first start if missing.\n" +
                                "zipFileAbsolutePath: \"" + new File(getDataFolder(), "URLCustomDiscsPack.zip").getAbsolutePath() + "\"\n" +
                                "\n" +
                                "# Public URL players download that same file from (the disc bot serves it at https://<host>/<PACK_TOKEN>/discs.zip).\n" +
                                "# The plugin sends it with its SHA-1 on join and after every change, so leave server.properties resource-pack= empty.\n" +
                                "downloadResourcePackURL: \"\"\n" +
                                "\n" +
                                "# Longer tracks are trimmed to this many seconds (0 = no limit). Every player downloads every disc, so keep it modest.\n" +
                                "maxSeconds: 600\n"
                        ;
                try (FileWriter writer = new FileWriter(configFile)) {
                    writer.write(configContent);
                }
            } catch (IOException e) {
                this.getLogger().severe("Exception: " + e.getMessage());
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        downloadResourcePackURL = config.getString("downloadResourcePackURL", "");
        zipFileAbsolutePath = config.getString("zipFileAbsolutePath", new File(getDataFolder(), "URLCustomDiscsPack.zip").getAbsolutePath());
        maxSeconds = config.getInt("maxSeconds", 600);
        if (config.contains("pluginUsageMode")) {
            getLogger().warning("config.yml is from upstream URLCustomDiscs; this fork ignores pluginUsageMode/api settings. Delete config.yml to regenerate it.");
        }
        if (downloadResourcePackURL.isEmpty()) {
            getLogger().warning("config.yml: downloadResourcePackURL is empty, so players won't receive new discs until it's set.");
        }
    }

    public String getOperatingSystem() { return os; }

    // fork: push the disc pack with its SHA-1 so clients cache it and only re-download when it changes (ADR 0005)
    public void sendPack(org.bukkit.entity.Player player) {
        if (downloadResourcePackURL.isEmpty()) return;
        try {
            // ponytail: hashes the zip on every push; fine at tens of MB, cache by mtime if packs get huge
            byte[] sha1 = java.security.MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(new File(zipFileAbsolutePath).toPath()));
            player.setResourcePack(downloadResourcePackURL, sha1);
        } catch (Exception e) {
            getLogger().warning("Couldn't hash the disc pack, sending without hash: " + e.getMessage());
            player.setResourcePack(downloadResourcePackURL);
        }
    }

    @org.bukkit.event.EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        if (new File(zipFileAbsolutePath).exists()) sendPack(event.getPlayer());
    }

    // fork: seed an empty disc pack so a fresh install works without downloading one by hand
    private void installDefaultPack() {
        if (zipFileAbsolutePath.isEmpty()) return;
        File zip = new File(zipFileAbsolutePath);
        if (zip.exists()) return;
        try (var in = getResource("URLCustomDiscsPack.zip")) {
            if (zip.getParentFile() != null) zip.getParentFile().mkdirs();
            Files.copy(Objects.requireNonNull(in), zip.toPath());
            getLogger().info("Created empty disc pack at " + zip.getAbsolutePath());
        } catch (Exception e) {
            getLogger().severe("Failed to create disc pack at " + zip.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    private void loadFiles() {
        File discUuidFile = new File(getDataFolder(), "discs.json"); // Assign to class variable
        if (!discUuidFile.exists()) {
            try {
                discUuidFile.createNewFile();
                Files.writeString(discUuidFile.toPath(), "{}"); // Initialize an empty JSON
            } catch (IOException e) {
                this.getLogger().severe("Exception: " + e.getMessage());
            }
        }

        audioToSendFolder = new File(getDataFolder(), "audio_to_send");
        if (!audioToSendFolder.exists()) audioToSendFolder.mkdir();

        binFolder = new File(getDataFolder(), "bin");
        if (!binFolder.exists()) binFolder.mkdir();

        {
            editResourcePackFolder = new File(getDataFolder(), "edit_resource_pack");
            if (!editResourcePackFolder.exists()) editResourcePackFolder.mkdirs();

            tempAudioFolder = new File(editResourcePackFolder, "temp_audio");
            if (!tempAudioFolder.exists()) tempAudioFolder.mkdirs();

            tempUnpackedFolder = new File(editResourcePackFolder, "temp_unpacked");
            if (!tempUnpackedFolder.exists()) tempUnpackedFolder.mkdirs();
        }
    }

    // Getters / Setters
    public String getMinecraftServerVersion() { return minecraftServerVersion; }
    public File getAudioToSendFolder() { return audioToSendFolder; }
    public File getBinFolder() { return binFolder; }
    public File getEditResourcePackFolder() { return editResourcePackFolder; }
    public File getTempAudioFolder() { return tempAudioFolder; }
    public File getTempUnpackedFolder() { return tempUnpackedFolder; }
    public File getAudioFolder() { return tempAudioFolder; }
    public String getDownloadResourcePackURL() { return downloadResourcePackURL; }
    public int getMaxSeconds() { return maxSeconds; }
    public String getZipFileAbsolutePath() { return zipFileAbsolutePath; }

    // Edit-Only mode
}