package com.urlcustomdiscs;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class SelfHostedManager {
    private final URLCustomDiscs plugin;
    private final URLCustomDiscs.OS os;

    public SelfHostedManager(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;
        this.os = os;
    }

    public void createCustomDisc(Player player, String mp3FileName, String discName, String audioType, JSONObject discInfo) {
        // Convert the MP3 file to Ogg Vorbis in the edit_resource_pack/temp_audio folder
        player.sendMessage(ChatColor.GRAY + "[2/3] Converting audio for Minecraft...");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            FFmpegManager ffmpegManager = new FFmpegManager(plugin, os);
            File oggFile = new File(plugin.getTempAudioFolder(), discName + ".ogg");
            boolean converted = ffmpegManager.convertAudioWithFFmpeg(mp3FileName, oggFile, audioType, player);

            if (!converted || !oggFile.exists()) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Failed to convert the MP3 audio to Ogg Vorbis using FFmpeg.")
                );
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () ->
                    player.sendMessage(ChatColor.GRAY + "[3/3] Adding it to the disc pack...")
            );

            // Delete the MP3 file in the temp_audio folder
            File mp3File = new File(plugin.getTempAudioFolder(), mp3FileName);
            if (mp3File.exists()) mp3File.delete();

            // Update the resource pack with the new custom disc
            ResourcePackService resourcePackService = new ResourcePackService(plugin);
            if (!resourcePackService.addDiscToResourcePack(player, oggFile, discName, discInfo)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error adding the custom disc to the resource pack.")
                );
                return;
            }

            Bukkit.getScheduler().runTask(plugin, () -> {
                {
                    // Generate the custom disc in memory and add it to the player's inventory
                    DiscFactory.giveCustomDiscToPlayer(player, discInfo);

                    // Browse all online players and send them the resource pack
                    String name = discInfo.getString("displayName");
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        if (onlinePlayer != player) { // fork: tell everyone why a pack prompt just appeared
                            onlinePlayer.sendMessage(ChatColor.GRAY + player.getName() + " added the disc " + ChatColor.GOLD + name
                                    + ChatColor.GRAY + ". Accept the resource pack update to hear it.");
                        }
                        plugin.sendPack(onlinePlayer);
                    }
                    player.sendMessage(ChatColor.GREEN + "Done! Sent the updated pack to " + Bukkit.getOnlinePlayers().size()
                            + " player(s). Accept the prompt, then put the disc in a jukebox.");
                }
            });
        });
    }

    public void deleteCustomDisc(Player player, String discName, JSONObject discInfo) {
        // Delete a custom disc from the resource pack
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            ResourcePackService resourcePackService = new ResourcePackService(plugin);
            if (!resourcePackService.removeDiscFromResourcePack(player, discName, discInfo)) {
                Bukkit.getScheduler().runTask(plugin, () ->
                        player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Error deleting the custom disc from the resource pack."));
                return;
            }

            try { // Remove the disc entry from the local disc JSON file
                DiscJsonManager discManager = new DiscJsonManager(plugin);
                discManager.deleteDisc(discName);
                Bukkit.getScheduler().runTask(plugin, () -> Bukkit.getOnlinePlayers().forEach(plugin::sendPack)); // fork: upstream never re-sent the pack on delete
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to clean up local disc entry: " + ex.getMessage());
            }
        });
    }
}
