package com.urlcustomdiscs;
import com.urlcustomdiscs.utils.DiscUtils;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.io.*;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.*;

public class CommandURLCustomDiscs implements CommandExecutor {

    private final URLCustomDiscs plugin;
    private final URLCustomDiscs.OS os;
    private final SelfHostedManager selfHostedManager;
    private final File discUuidFile;

    public CommandURLCustomDiscs(URLCustomDiscs plugin, URLCustomDiscs.OS os, SelfHostedManager selfHostedManager) {
        this.plugin = plugin;
        this.os = os;
        this.selfHostedManager = selfHostedManager;
        this.discUuidFile = new File(plugin.getDataFolder(), "discs.json");
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return false;
        }

        if (!sender.hasPermission("customdisc.admin")) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Command reserved for administrators.");
            return true;
        }

        // Help command
        // fork: compact help; each command is clickable and fills the chat box
        if (args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help"))) {
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Custom Discs" + ChatColor.GRAY + "  (click a command to use it)");
            helpLine(player, "/disc create <YouTube link> <name>", "/disc create ", "Download a song and make it a disc");
            helpLine(player, "/disc create <file.mp3>", "/disc create ", "Make a disc from an MP3 in the drop folder (e.g. sent via the Discord bot)");
            player.sendMessage(ChatColor.DARK_GRAY + "   add " + ChatColor.GRAY + "mono" + ChatColor.DARK_GRAY + " at the end for jukebox-style sound that fades with distance");
            helpLine(player, "/disc list", "/disc list", "All discs (click one to get it)");
            helpLine(player, "/disc give <name>", "/disc give ", "Get a copy of a disc");
            helpLine(player, "/disc delete <name>", "/disc delete ", "Remove a disc for everyone");
            helpLine(player, "/disc info", "/disc info", "Details of the disc in your hand");
            helpLine(player, "/disc updatedep", "/disc updatedep", "Update yt-dlp if YouTube downloads start failing");
            player.sendMessage(ChatColor.DARK_GRAY + "Tracks over " + plugin.getMaxSeconds() / 60 + " min are trimmed. Names: letters, numbers, _ and -.");
            player.sendMessage("");
            return true;
        }

        // Command to create a custom disc
        // fork: /disc create <URL|file.mp3> [disc_name] [mono|stereo]; name defaults to the file name, type to stereo
        if (args.length >= 2 && args.length <= 4 && args[0].equalsIgnoreCase("create")) {
            String input = args[1];
            boolean isUrl = input.toLowerCase().startsWith("http");
            String last = args[args.length - 1].toLowerCase();
            boolean hasType = args.length > 2 && (last.equals("mono") || last.equals("stereo"));
            String audioType = hasType ? last : "stereo";
            String nameArg = args.length > (hasType ? 3 : 2) ? args[2] : (isUrl ? null : input.replaceFirst("(?i)\\.mp3$", ""));
            if (nameArg == null) {
                player.sendMessage(ChatColor.RED + "Give the disc a name: /disc create <URL> <disc_name>");
                return true;
            }
            String rawDiscName = nameArg.replaceAll("[^a-zA-Z0-9_-]", "_");

            player.sendMessage(ChatColor.GRAY + (isUrl ? "[1/3] Downloading audio (progress above your hotbar)..." : "[1/3] Using " + input + "..."));

            try { // For URL

                // Checks if input is a valid URL
                new URL(input);

                {
                    // Download the MP3 file from the URL into edit_resource_pack/temp_audio with the local yt-dlp
                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                        YtDlpManager ytDlpManager = new YtDlpManager(plugin, os);
                        File mp3File = new File(plugin.getAudioFolder(), rawDiscName + ".mp3");
                        boolean downloaded = ytDlpManager.downloadAudioWithYtDlp(input, mp3File, player);

                        Bukkit.getScheduler().runTask(plugin, () -> {
                            if (!downloaded || !mp3File.exists()) {
                                player.sendMessage(ChatColor.RED + "Download failed: " + ChatColor.GRAY + ytDlpManager.getLastError());
                                player.sendMessage(ChatColor.GRAY + "Updating yt-dlp and retrying once...");

                                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                    // Update deno
                                    new DenoSetup(plugin, os).setup();

                                    // Update yt-dlp
                                    new YtDlpSetup(plugin, os).setup();

                                    // Retry downloading the MP3 file from the URL using yt-dlp after updating deno and yt-dlp
                                    Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                                        boolean retried = ytDlpManager.downloadAudioWithYtDlp(input, mp3File, player);

                                        Bukkit.getScheduler().runTask(plugin, () -> {
                                            if (!retried || !mp3File.exists()) {
                                                player.sendMessage(ChatColor.RED + "Download failed again: " + ChatColor.GRAY + ytDlpManager.getLastError());
                                                player.sendMessage(ChatColor.GRAY + "You can still make a disc from an MP3 (/disc help).");
                                                return;
                                            }
                                            player.sendMessage(ChatColor.GREEN + "Audio downloaded after updating yt-dlp.");

                                            // Pass the downloaded MP3 file name
                                            continueDiscCreation(player, mp3File.getName(), rawDiscName, audioType);
                                        });
                                    });
                                });
                                return; // fork: upstream fell through here and also created a disc from the missing file
                            }
                            continueDiscCreation(player, mp3File.getName(), rawDiscName, audioType);
                        });
                    });

                }
                return true;

            } catch (MalformedURLException e) { // For MP3 file
                // If it is not a URL, check if it is an MP3 file in the audio_to_send folder
                File localMp3 = new File(plugin.getAudioToSendFolder(), input);
                if (localMp3.exists() && localMp3.isFile() && input.toLowerCase().endsWith(".mp3")) {

                    {
                        File destFile = new File(plugin.getTempAudioFolder(), input);
                        try { // Move the MP3 file from the audio_to_send folder to the edit_resource_pack/temp_audio folder
                            Files.move(localMp3.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e1) {
                            plugin.getLogger().severe("Exception: " + e.getMessage());
                            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to move the MP3 file to temp_audio.");
                            return true;
                        }
                    }

                    continueDiscCreation(player, input, rawDiscName, audioType);
                } else {
                    // fork: say why; the file is moved out of audio_to_send once a disc is made from it
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No file '" + input + "' in audio_to_send (each MP3 is used up once its disc is made). Use /disc give <name> for an existing disc.");
                }
                return true;
            }
        }

        // Command to give yourself a custom disc
        if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            String discName = args[1].toLowerCase().replaceAll(" ", "_");
            giveCustomMusicDisc(player, discName);
            return true;
        }

        // Command to show the list of custom discs
        if (args.length == 1 && args[0].equalsIgnoreCase("list")) {
            JSONObject discData = DiscUtils.loadDiscData(discUuidFile);

            if (discData.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found. Create a disc first (/disc help).");
                return true;
            }
            player.sendMessage(ChatColor.WHITE + "" + ChatColor.BOLD + "List of custom music discs:");

            // Sort disc names alphabetically
            List<String> discNames = new ArrayList<>(discData.keySet());
            Collections.sort(discNames);
            for (String discName : discNames) {
                // Retrieve the object corresponding to the disc
                JSONObject discInfo = discData.getJSONObject(discName);
                // Retrieve the displayName of each disc
                String displayName = discInfo.getString("displayName");
                // Create the TextComponent using the displayName
                TextComponent discText = createDiscTextComponent(displayName);
                player.spigot().sendMessage(discText);
            }
            return true;
        }

        // Command to delete a custom disc
        if (args.length == 2 && args[0].equalsIgnoreCase("delete")) {
            JSONObject discData = DiscUtils.loadDiscData(discUuidFile);
            if (discData.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom disc found. Create a custom disc first (/disc help).");
                return true;
            }

            String discName = args[1].toLowerCase();

            DiscJsonManager discManager = new DiscJsonManager(plugin);
            JSONObject discInfo = null;
            try {
                discInfo = discManager.getDisc(discName);
            } catch (IOException e) {
                plugin.getLogger().severe("Exception: " + e.getMessage());
            }

            // Check if the custom disc exists
            if (discInfo == null || discInfo.isEmpty()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Custom disc '" + discName + "' does not exist.");
                return true;
            }

            final JSONObject discInfoFinal = discInfo;

            selfHostedManager.deleteCustomDisc(player, discName, discInfoFinal);
            return true;
        }

        // Command to get information about the disc in hand
        if (args.length == 1 && args[0].equalsIgnoreCase("info")) {
            ItemStack itemInHand = player.getInventory().getItemInMainHand();

            if (itemInHand.hasItemMeta()) {
                ItemMeta meta = itemInHand.getItemMeta();
                if (meta != null && meta.hasCustomModelData()) {
                    int customModelData = meta.getCustomModelData();

                    // Load disc data from JSON file
                    JSONObject discData = DiscUtils.loadDiscData(discUuidFile);
                    // Find the disc name from the CustomModelData
                    String discName = DiscUtils.getDiscNameFromCustomModelData(discData, customModelData);

                    if (discName != null) {
                        JSONObject discInfo = discData.getJSONObject(discName);
                        String displayName = discInfo.getString("displayName");
                        String discUUID = discInfo.getString("uuid");
                        String soundKey = "customdisc." + discName.toLowerCase().replaceAll(" ", "_");

                        // Send information to the player
                        player.sendMessage(ChatColor.GRAY + "Disc played: " + ChatColor.GOLD + discName);
                        player.sendMessage(ChatColor.GRAY + "Display name: " + ChatColor.GOLD + displayName);
                        player.sendMessage(ChatColor.GRAY + "UUID: " + ChatColor.GOLD + discUUID);
                        player.sendMessage(ChatColor.GRAY + "CustomModelData: " + ChatColor.GOLD + customModelData);
                        player.sendMessage(ChatColor.GRAY + "SoundKey: " + ChatColor.GOLD + soundKey);

                    } else {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found with this CustomModelData.");
                    }
                } else {
                    player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be holding a custom music disc.");
                }
            } else {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "You must be holding a custom music disc.");
            }
            return true;
        }

        // Command to update Deno and yt-dlp dependencies
        if (args.length == 1 && args[0].equalsIgnoreCase("updatedep")) {
            {
                player.sendMessage(ChatColor.GRAY + "Checking for Deno and yt-dlp updates...");
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    DenoSetup denoSetup = new DenoSetup(plugin, os);
                    try {
                        denoSetup.setup();
                        player.sendMessage(ChatColor.GREEN + "Deno update check finished. See console for details.");
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to update Deno: " + e.getMessage());
                    }

                    YtDlpSetup ytDlpSetup = new YtDlpSetup(plugin, os);
                    try {
                        ytDlpSetup.setup();
                        player.sendMessage(ChatColor.GREEN + "yt-dlp update check finished. See console for details.");
                    } catch (Exception e) {
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to update yt-dlp: " + e.getMessage());
                    }
                });
                return true;
            }
        }

        player.sendMessage(ChatColor.GRAY + "Unknown subcommand. Try " + ChatColor.YELLOW + "/disc help");
        return true;
    }

    private void helpLine(Player player, String usage, String suggest, String what) {
        TextComponent line = new TextComponent(ChatColor.YELLOW + " " + usage + ChatColor.GRAY + "  " + what);
        line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, suggest));
        line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text("Click to type " + suggest)));
        player.spigot().sendMessage(line);
    }

    private void continueDiscCreation(Player player, String finalAudioIdentifier, String displayName, String audioType) {
        final String discName = displayName.toLowerCase();

        DiscJsonManager discManager = new DiscJsonManager(plugin);
        JSONObject discInfo = null;
        try {
            discInfo = discManager.getOrCreateDisc(discName, displayName);
        } catch (IOException e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error creating disc information.");
        }
        selfHostedManager.createCustomDisc(player, finalAudioIdentifier, discName, audioType, discInfo);
    }

    private void giveCustomMusicDisc(Player player, String discName) {
        try {
            if (!discUuidFile.exists()) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "No custom music disc found. Create a disc first (/disc help).");
                return;
            }

            // Read JSON file from discs
            String content = Files.readString(discUuidFile.toPath());
            JSONObject discData = new JSONObject(content);

            // Check if the disk exists
            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "The disc '" + discName + "' doesn't exist.");
                return;
            }

            JSONObject discInfo = discData.getJSONObject(discName);
            int customModelData = discInfo.getInt("customModelData");
            String displayName = discInfo.getString("displayName");

            // Create the disc with the same properties as when it was created
            ItemStack disc = new ItemStack(Material.MUSIC_DISC_13);
            ItemMeta meta = disc.getItemMeta();

            if (meta != null) {
                meta.setDisplayName(ChatColor.GOLD + displayName);
                List<String> lore = new ArrayList<>();
                lore.add(ChatColor.GRAY + "Custom music disc: " + displayName);
                meta.setLore(lore);
                meta.setCustomModelData(customModelData);
                // Hide "C418 - 13" => cant (or rather lazy to create a new json just for this)
                disc.setItemMeta(meta);
            }

            //Add the disc to the player's inventory
            player.getInventory().addItem(disc);
            player.sendMessage(ChatColor.GRAY + "Custom disc " + ChatColor.GOLD + displayName + ChatColor.GRAY + " added to your inventory.");
        } catch (IOException e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error recovering the custom disc.");
        }
    }

    private TextComponent createDiscTextComponent(String displayName) {
        TextComponent discText = new TextComponent(displayName);
        discText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        discText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/disc give " + displayName));
        discText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to get this disc!")));
        return discText;
    }
}