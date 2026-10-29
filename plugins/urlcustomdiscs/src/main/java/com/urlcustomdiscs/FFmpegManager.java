package com.urlcustomdiscs;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class FFmpegManager {

    private final URLCustomDiscs plugin;
    private final File ffmpegFile;

    public FFmpegManager(URLCustomDiscs plugin, URLCustomDiscs.OS os) {
        this.plugin = plugin;

        File binDir = plugin.getBinFolder();
        File ffmpegDir = new File(binDir, "FFmpeg");
        String ffmpegName = switch (os) {
            case WINDOWS_X64, WINDOWS_ARM64 -> "ffmpeg.exe";
            case LINUX_X64, LINUX_ARM64, LINUX_ARMV7, LINUX_MUSL_X64, LINUX_MUSL_ARM64 -> "ffmpeg";
            default -> null;
        };

        try {
            this.ffmpegFile = findFile(ffmpegDir, ffmpegName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean convertAudioWithFFmpeg(String inputFileMP3, File outputFileOggVorbis, String audioType, org.bukkit.entity.Player player) {
        File mp3File = new File(plugin.getTempAudioFolder(), inputFileMP3);
        try {
            List<String> command = new ArrayList<>();
            command.add(ffmpegFile.getAbsolutePath());
            command.add("-y"); // Overwrite existing files
            command.add("-i"); // Input file
            command.add(String.valueOf(mp3File)); // Input file
            command.add("-vn"); // No video
            int maxSeconds = plugin.getMaxSeconds();
            if (maxSeconds > 0) { // fork: trim long tracks instead of refusing them
                command.add("-t");
                command.add(String.valueOf(maxSeconds));
            }
            if ("mono".equalsIgnoreCase(audioType)) {
                command.add("-ac"); // Channels
                command.add("1"); // Mono
            }
            command.add("-c:a"); // Audio codec
            command.add("libvorbis"); // Ogg Vorbis codec for Minecraft compatibility
            command.add(outputFileOggVorbis.getAbsolutePath()); // Output file

            ProcessBuilder pb = new ProcessBuilder(command);

            pb.redirectErrorStream(true); // Merge stderr in stdout
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    plugin.getLogger().info("[FFmpeg] " + line);
                    // fork: "  Duration: 00:23:41.05, start: ..." -> warn the player if we're trimming
                    java.util.regex.Matcher m = java.util.regex.Pattern.compile("Duration: (\\d+):(\\d+):(\\d+)").matcher(line);
                    if (player != null && maxSeconds > 0 && m.find()) {
                        int secs = Integer.parseInt(m.group(1)) * 3600 + Integer.parseInt(m.group(2)) * 60 + Integer.parseInt(m.group(3));
                        if (secs > maxSeconds) {
                            player.sendMessage(org.bukkit.ChatColor.YELLOW + "  Track is " + secs / 60 + ":" + String.format("%02d", secs % 60)
                                    + " long, trimming to " + maxSeconds / 60 + ":" + String.format("%02d", maxSeconds % 60) + ".");
                        }
                    }
                }
            }

            int exitCode = process.waitFor();
            return exitCode == 0 && outputFileOggVorbis.exists();

        } catch (Exception e) {
            plugin.getLogger().severe("Exception: " + e.getMessage());
            plugin.getLogger().severe("[FFmpeg] Failed to run FFmpeg: " + e.getMessage());
            return false;
        }
    }

    private File findFile(File dir, String name) throws IOException {
        try (Stream<Path> files = Files.walk(dir.toPath())) {
            return files
                    .filter(p -> p.toFile().isFile()) // Only look at files
                    .filter(p -> p.getFileName().toString().equalsIgnoreCase(name))
                    .map(Path::toFile)
                    .findFirst()
                    .orElse(null);
        }
    }
}