package com.dungeonapi;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DungeonManager {

    private final Path dungeonsDirectory;
    private final SchematicManager schematicManager;
    private final MinecraftServer server;

    public DungeonManager(
            MinecraftServer server,
            SchematicManager schematicManager) {
        this.server = server;
        this.schematicManager = schematicManager;

        this.dungeonsDirectory = server.getServerDirectory()
                .toPath()
                .resolve("config")
                .resolve("dungeon-api")
                .resolve("dungeons");
    }

    public void createDefaultConfiguration() throws IOException {

        Files.createDirectories(dungeonsDirectory);

        Path dungeon01 = dungeonsDirectory
                .resolve("dungeon_01");

        Files.createDirectories(dungeon01);

        Path propertiesFile = dungeon01.resolve("dungeon.properties");

        if (!Files.exists(propertiesFile)) {

            Properties properties = new Properties();

            properties.setProperty(
                    "schematic",
                    "minialdea");

            properties.setProperty(
                    "world",
                    "overworld");

            properties.setProperty(
                    "x",
                    "0");

            properties.setProperty(
                    "y",
                    "64");

            properties.setProperty(
                    "z",
                    "0");

            try (var output = Files.newOutputStream(propertiesFile)) {

                properties.store(
                        output,
                        "Dungeon configuration");
            }

            DungeonAPI.LOGGER.info(
                    "Configuración de dungeon_01 creada.");
        }
    }

    public List<DungeonConfig> loadDungeons() {

        List<DungeonConfig> dungeons = new ArrayList<>();

        if (!Files.exists(dungeonsDirectory)) {
            return dungeons;
        }

        try {

            try (var directories = Files.list(dungeonsDirectory)) {

                directories
                        .filter(Files::isDirectory)
                        .forEach(directory -> {

                            try {

                                DungeonConfig config = loadDungeon(directory);

                                if (config != null) {
                                    dungeons.add(config);
                                }

                            } catch (Exception e) {

                                DungeonAPI.LOGGER.error(
                                        "Error cargando la dungeon '{}'.",
                                        directory.getFileName(),
                                        e);
                            }
                        });
            }

        } catch (IOException e) {

            DungeonAPI.LOGGER.error(
                    "No se pudieron leer las dungeons.",
                    e);
        }

        return dungeons;
    }

    private DungeonConfig loadDungeon(
            Path dungeonDirectory) throws IOException {

        Path propertiesFile = dungeonDirectory.resolve(
                "dungeon.properties");

        if (!Files.exists(propertiesFile)) {

            DungeonAPI.LOGGER.warn(
                    "La dungeon '{}' no tiene dungeon.properties.",
                    dungeonDirectory.getFileName());

            return null;
        }

        Properties properties = new Properties();

        try (FileInputStream input = new FileInputStream(
                propertiesFile.toFile())) {

            properties.load(input);
        }

        String id = dungeonDirectory
                .getFileName()
                .toString();

        String schematic = getRequired(
                properties,
                "schematic");

        String world = getRequired(
                properties,
                "world");

        int x = Integer.parseInt(
                getRequired(properties, "x"));

        int y = Integer.parseInt(
                getRequired(properties, "y"));

        int z = Integer.parseInt(
                getRequired(properties, "z"));

        return new DungeonConfig(
                id,
                schematic,
                world,
                x,
                y,
                z);
    }

    private String getRequired(
            Properties properties,
            String key) {

        String value = properties.getProperty(key);

        if (value == null ||
                value.isBlank()) {

            throw new IllegalArgumentException(
                    "Falta la propiedad '" + key + "'.");
        }

        return value.trim();
    }

    public boolean resetDungeon(
            String dungeonId) {

        List<DungeonConfig> dungeons = loadDungeons();

        for (DungeonConfig dungeon : dungeons) {

            if (dungeon.getId()
                    .equalsIgnoreCase(dungeonId)) {

                return pasteDungeon(dungeon);
            }
        }

        DungeonAPI.LOGGER.error(
                "No se encontró la dungeon '{}'.",
                dungeonId);

        return false;
    }

    public DungeonResetResult resetAllDungeons() {

        List<DungeonConfig> dungeons = loadDungeons();

        int successful = 0;
        int failed = 0;

        for (DungeonConfig dungeon : dungeons) {

            if (pasteDungeon(dungeon)) {
                successful++;
            } else {
                failed++;
            }
        }

        return new DungeonResetResult(
                dungeons.size(),
                successful,
                failed);
    }

    private boolean pasteDungeon(
            DungeonConfig dungeon) {

        try {

            ServerLevel world = getWorld(dungeon.getWorld());

            if (world == null) {

                DungeonAPI.LOGGER.error(
                        "No se encontró el mundo '{}' para la dungeon '{}'.",
                        dungeon.getWorld(),
                        dungeon.getId());

                return false;
            }

            schematicManager.paste(
                    world,
                    dungeon.getSchematic(),
                    dungeon.getX(),
                    dungeon.getY(),
                    dungeon.getZ());

            DungeonAPI.LOGGER.info(
                    "Dungeon '{}' regenerada correctamente.",
                    dungeon.getId());

            return true;

        } catch (Exception e) {

            DungeonAPI.LOGGER.error(
                    "Error regenerando la dungeon '{}'.",
                    dungeon.getId(),
                    e);

            return false;
        }
    }

    private ServerLevel getWorld(
            String worldName) {

        String location;

        if (worldName.contains(":")) {
            location = worldName;
        } else {
            location = "minecraft:" + worldName;
        }

        ResourceLocation resourceLocation = ResourceLocation.parse(location);

        ResourceKey<net.minecraft.world.level.Level> worldKey = ResourceKey.create(
                Registries.DIMENSION,
                resourceLocation);

        return server.getLevel(worldKey);
    }

    public Path getDungeonsDirectory() {
        return dungeonsDirectory;
    }

    public boolean createDungeon(
            String dungeonId,
            String schematic,
            String world,
            int x,
            int y,
            int z,
            boolean overwrite) {

        // Validar el identificador de la dungeon.
        if (dungeonId == null ||
                !dungeonId.matches("[a-zA-Z0-9_-]+")) {

            DungeonAPI.LOGGER.warn(
                    "Nombre de dungeon no válido: '{}'.",
                    dungeonId);

            return false;
        }

        // Validar el nombre del schematic.
        if (schematic == null ||
                !schematic.matches("[a-zA-Z0-9_-]+")) {

            DungeonAPI.LOGGER.warn(
                    "Nombre de schematic no válido: '{}'.",
                    schematic);

            return false;
        }

        // Comprobar que existe el schematic.
        Path schematicsDirectory = server.getServerDirectory()
                .toPath()
                .resolve("config")
                .resolve("worldedit")
                .resolve("schematics");

        Path schematicFile = schematicsDirectory
                .resolve(schematic + ".schem");

        if (!Files.isRegularFile(schematicFile)) {

            DungeonAPI.LOGGER.warn(
                    "No se encontró el schematic '{}.schem'.",
                    schematic);

            return false;
        }

        Path dungeonDirectory = dungeonsDirectory.resolve(dungeonId);

        boolean exists = Files.isDirectory(dungeonDirectory);

        // No sobrescribir sin confirmación.
        if (exists && !overwrite) {

            return false;
        }

        try {

            Files.createDirectories(dungeonDirectory);

            Properties properties = new Properties();

            properties.setProperty("world", world);
            properties.setProperty("x", String.valueOf(x));
            properties.setProperty("y", String.valueOf(y));
            properties.setProperty("z", String.valueOf(z));
            properties.setProperty("schematic", schematic);

            Path propertiesFile = dungeonDirectory.resolve("dungeon.properties");

            try (var output = Files.newOutputStream(propertiesFile)) {

                properties.store(
                        output,
                        "Dungeon configuration");
            }

            DungeonAPI.LOGGER.info(
                    "Dungeon '{}' guardada. Schematic: '{}', mundo: {}, posición: {}, {}, {}.",
                    dungeonId,
                    schematic,
                    world,
                    x,
                    y,
                    z);

            return true;

        } catch (IOException e) {

            DungeonAPI.LOGGER.error(
                    "No se pudo guardar la dungeon '{}'.",
                    dungeonId,
                    e);

            return false;
        }
    }

    public boolean dungeonExists(String dungeonId) {

        if (dungeonId == null ||
                !dungeonId.matches("[a-zA-Z0-9_-]+")) {
            return false;
        }

        return Files.isDirectory(
                dungeonsDirectory.resolve(dungeonId));
    }

    public boolean deleteDungeon(String dungeonId) {

        // Validar el identificador.
        if (dungeonId == null ||
                !dungeonId.matches("[a-zA-Z0-9_-]+")) {

            DungeonAPI.LOGGER.warn(
                    "Nombre de dungeon no válido: '{}'.",
                    dungeonId);

            return false;
        }

        Path dungeonDirectory = dungeonsDirectory.resolve(dungeonId);

        if (!Files.isDirectory(dungeonDirectory)) {

            DungeonAPI.LOGGER.warn(
                    "No se encontró la dungeon '{}'.",
                    dungeonId);

            return false;
        }

        try (var paths = Files.walk(dungeonDirectory)) {

            for (Path path : paths
                    .sorted(java.util.Comparator.reverseOrder())
                    .toList()) {

                Files.delete(path);
            }

            DungeonAPI.LOGGER.info(
                    "Dungeon '{}' eliminada correctamente.",
                    dungeonId);

            return true;

        } catch (IOException e) {

            DungeonAPI.LOGGER.error(
                    "No se pudo eliminar la dungeon '{}'.",
                    dungeonId,
                    e);

            return false;
        }
    }

}