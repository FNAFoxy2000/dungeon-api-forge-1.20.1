package com.dungeonapi;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class BonfireManager {

    private final Path configFile;
    private final Properties assignments = new Properties();

    public BonfireManager(Path configDirectory) throws IOException {
        this.configFile = configDirectory.resolve("bonfires.properties");

        Files.createDirectories(configDirectory);

        if (Files.exists(configFile)) {
            try (InputStream input = Files.newInputStream(configFile)) {
                assignments.load(input);
            }
        } else {
            save();
        }
    }

    private String getKey(ResourceKey<Level> dimension, BlockPos pos) {
        return dimension.location() + "|" +
                pos.getX() + "," +
                pos.getY() + "," +
                pos.getZ();
    }

    public synchronized boolean assign(
            ResourceKey<Level> dimension,
            BlockPos pos,
            String dungeonId) {

        String key = getKey(dimension, pos);
        String previous = assignments.getProperty(key);

        assignments.setProperty(key, dungeonId);

        try {
            save();
            return true;
        } catch (IOException e) {
            if (previous == null) {
                assignments.remove(key);
            } else {
                assignments.setProperty(key, previous);
            }

            DungeonAPI.LOGGER.error(
                    "No se pudo guardar la asignación de la hoguera.",
                    e);

            return false;
        }
    }

    public synchronized boolean remove(
            ResourceKey<Level> dimension,
            BlockPos pos) {

        String key = getKey(dimension, pos);
        String previous = assignments.getProperty(key);

        if (previous == null) {
            return false;
        }

        assignments.remove(key);

        try {
            save();
            return true;
        } catch (IOException e) {
            assignments.setProperty(key, previous);

            DungeonAPI.LOGGER.error(
                    "No se pudo eliminar la asignación de la hoguera.",
                    e);

            return false;
        }
    }

    public synchronized String getDungeon(
            ResourceKey<Level> dimension,
            BlockPos pos) {

        return assignments.getProperty(getKey(dimension, pos));
    }

    private void save() throws IOException {
        try (OutputStream output = Files.newOutputStream(configFile)) {
            assignments.store(output, "DungeonAPI bonfire assignments");
        }
    }
}
