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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandURLCustomDiscs implements CommandExecutor {

    private final URLCustomDiscs plugin;
    private final File discUuidFile;

    public CommandURLCustomDiscs(URLCustomDiscs plugin) {
        this.plugin = plugin;
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
            helpLine(player, "/disc get <name>", "/disc get ", "Get a copy of a disc (also /disc give)");
            helpLine(player, "/disc delete <name>", "/disc delete ", "Remove a disc for everyone");
            helpLine(player, "/disc info", "/disc info", "Details of the disc in your hand");
            helpLine(player, "/disc updatedep", "/disc updatedep", "Update yt-dlp in the disc bot if YouTube downloads fail");
            player.sendMessage(ChatColor.DARK_GRAY + "Long tracks are trimmed (10 min by default). Names: letters, numbers, _ and -.");
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
            String discName = nameArg.toLowerCase().replaceAll("[^a-z0-9_-]", "_");
            if (!isUrl && (input.contains("/") || input.contains("\\"))) { // fork: a bare file name from the drop folder, never a path
                player.sendMessage(ChatColor.RED + "Use just the file name from the drop folder, e.g. /disc create song.mp3");
                return true;
            }
            JSONObject request = new JSONObject().put("source", input).put("name", discName).put("mono", audioType.equals("mono"));
            player.sendMessage(ChatColor.GRAY + "Making disc " + ChatColor.GOLD + discName + ChatColor.GRAY + "...");
            callBot(player, "/api/create", request, event -> {
                switch (event.optString("stage")) {
                    case "download" -> {
                        if (event.has("progress")) {
                            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                                    new TextComponent(ChatColor.GRAY + "Downloading: " + ChatColor.YELLOW + event.getString("progress")));
                        } else {
                            player.sendMessage(ChatColor.GRAY + "[1/3] " + event.optString("message") + " (progress above your hotbar)");
                        }
                    }
                    case "convert" -> player.sendMessage((event.optString("message").startsWith("Track is") ? ChatColor.YELLOW + "  " : ChatColor.GRAY + "[2/3] ") + event.optString("message"));
                    case "pack" -> player.sendMessage(ChatColor.GRAY + "[3/3] " + event.optString("message"));
                    case "queued", "update" -> player.sendMessage(ChatColor.YELLOW + event.optString("message"));
                    case "error" -> player.sendMessage(ChatColor.RED + event.optString("message"));
                    case "done" -> Bukkit.getScheduler().runTask(plugin, () -> discReady(player, discName, event.getJSONObject("disc")));
                    default -> { }
                }
            });
            return true;
        }

        // Command to give yourself a custom disc
        if (args.length == 2 && (args[0].equalsIgnoreCase("give") || args[0].equalsIgnoreCase("get"))) {
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
                TextComponent discText = createDiscTextComponent(displayName, discInfo.optString("source", ""));
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
            if (!discData.has(discName)) {
                player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Custom disc '" + discName + "' does not exist.");
                return true;
            }
            callBot(player, "/api/delete", new JSONObject().put("name", discName), reply -> {
                if (!reply.optBoolean("ok")) {
                    player.sendMessage(ChatColor.RED + reply.optString("message", "Delete failed."));
                    return;
                }
                Bukkit.getScheduler().runTask(plugin, () -> {
                    player.sendMessage(ChatColor.GREEN + "Disc " + ChatColor.GOLD + discName + ChatColor.GREEN + " deleted.");
                    Bukkit.getOnlinePlayers().forEach(plugin::sendPack);
                });
            });
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

        // Command to update yt-dlp (in the disc bot)
        if (args.length == 1 && args[0].equalsIgnoreCase("updatedep")) {
            player.sendMessage(ChatColor.GRAY + "Asking the disc bot to update yt-dlp...");
            callBot(player, "/api/update", new JSONObject(), reply ->
                    player.sendMessage(ChatColor.GREEN + "yt-dlp is now " + reply.optString("version", "?") + "."));
            return true;
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

    // fork: run a disc API call off the game thread; replies arrive as JSON objects (streamed for create)
    private void callBot(Player player, String path, JSONObject request, java.util.function.Consumer<JSONObject> onEvent) {
        if (!plugin.getBot().isConfigured()) {
            player.sendMessage(ChatColor.RED + "The disc bot isn't set up (botURL/botSecret in config.yml).");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                plugin.getBot().post(path, request, onEvent);
            } catch (IOException e) {
                plugin.getLogger().warning("Disc bot call " + path + " failed: " + e.getMessage());
                player.sendMessage(ChatColor.RED + "Couldn't reach the disc bot: " + ChatColor.GRAY + e.getMessage());
            }
        });
    }

    private void discReady(Player player, String discName, JSONObject discInfo) {
        DiscFactory.giveCustomDiscToPlayer(player, discInfo);
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online != player) { // tell everyone why a pack prompt just appeared
                online.sendMessage(ChatColor.GRAY + player.getName() + " added the disc " + ChatColor.GOLD + discName
                        + ChatColor.GRAY + ". Accept the resource pack update to hear it.");
            }
            plugin.sendPack(online);
        }
        player.sendMessage(ChatColor.GREEN + "Done! Sent the updated pack to " + Bukkit.getOnlinePlayers().size()
                + " player(s). Accept the prompt, then put the disc in a jukebox.");
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

    private TextComponent createDiscTextComponent(String displayName, String source) {
        TextComponent discText = new TextComponent(displayName);
        discText.setColor(net.md_5.bungee.api.ChatColor.GOLD);
        discText.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/disc give " + displayName));
        discText.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(ChatColor.YELLOW + "Click to get this disc!" + (source.isEmpty() ? "" : "\n" + ChatColor.GRAY + source))));
        return discText;
    }
}