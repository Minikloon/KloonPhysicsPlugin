package com.minikloon.physicsplugin;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public final class CustomNativeLibraryLoader {
    public static void extractAndLoad(JavaPlugin plugin, String jarFilepath) throws Exception {
        InputStream stream = CustomNativeLibraryLoader.class.getResourceAsStream(jarFilepath);
        if (stream == null) {
            throw new RuntimeException("Couldn't find " + jarFilepath + " in jar ");
        }

        String fileName = Paths.get(jarFilepath).getFileName().toString();
        Path outputPath = plugin.getDataFolder().toPath().resolve("native/" + System.currentTimeMillis() + "_" + fileName);

        if (Bukkit.getCurrentTick() == 0) {
            Files.list(outputPath.getParent()).forEach(file -> {
                if (file.getFileName().toString().contains(fileName)) {
                    try {
                        Files.deleteIfExists(file);
                    } catch (Throwable t) {
                        Bukkit.getLogger().log(Level.SEVERE, "Error cleaning up pre-existing native library " + file, t);
                    }
                }
            });
        }
        Files.createDirectories(outputPath.getParent());

        Files.copy(stream, outputPath);

        System.load(outputPath.toAbsolutePath().toString());
    }
}
