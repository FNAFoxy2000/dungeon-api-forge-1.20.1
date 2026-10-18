package com.dungeonapi;

import com.mojang.brigadier.arguments.StringArgumentType;

import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ServerChatEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod(DungeonAPI.MOD_ID)
public class DungeonAPI {

        public static final String MOD_ID = "dungeonapi";

        public static String API_KEY;
        public static int PORT;

        public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

        private static MinecraftServer server;
        private static HttpServer httpServer;
        private static SchematicManager schematicManager;
        private static DungeonManager dungeonManager;

        private final Map<UUID, PendingDungeonSave> pendingDungeonSaves = new HashMap<>();

        public DungeonAPI() {

                LOGGER.info("========================================");
                LOGGER.info("Dungeon API: CONSTRUCTOR EJECUTADO");
                LOGGER.info("========================================");

                MinecraftForge.EVENT_BUS.register(this);
        }

        // =========================================================
        // CONFIGURACIÓN
        // =========================================================

        private static void loadConfiguration() {

                if (server == null) {
                        throw new IllegalStateException(
                                        "El servidor de Minecraft todavía no está disponible.");
                }

                File configFile = server.getServerDirectory()
                                .toPath()
                                .resolve("config")
                                .resolve("dungeonapi.properties")
                                .toFile();

                Properties properties = new Properties();

                try {

                        if (!configFile.exists()) {

                                configFile.getParentFile().mkdirs();

                                properties.setProperty(
                                                "port",
                                                "8080");

                                properties.setProperty(
                                                "api-key",
                                                "dungeon-test-123456");

                                try (FileOutputStream output = new FileOutputStream(configFile)) {

                                        properties.store(
                                                        output,
                                                        "Dungeon API Configuration");
                                }

                                LOGGER.info(
                                                "Se ha creado la configuración de Dungeon API en {}",
                                                configFile.getAbsolutePath());
                        }

                        try (FileInputStream input = new FileInputStream(configFile)) {

                                properties.load(input);
                        }

                        API_KEY = properties.getProperty("api-key");

                        if (API_KEY == null || API_KEY.isBlank()) {

                                throw new IllegalStateException(
                                                "La propiedad 'api-key' no está configurada.");
                        }

                        String portString = properties.getProperty("port");

                        try {

                                PORT = Integer.parseInt(portString);

                        } catch (NumberFormatException e) {

                                throw new IllegalStateException(
                                                "El puerto configurado no es válido: "
                                                                + portString,
                                                e);
                        }

                        LOGGER.info(
                                        "Configuración de Dungeon API cargada correctamente.");

                        LOGGER.info(
                                        "Configuración cargada: puerto={}, api-key configurada={}",
                                        PORT,
                                        API_KEY != null && !API_KEY.isBlank());

                } catch (IOException e) {

                        LOGGER.error(
                                        "No se pudo cargar dungeonapi.properties.",
                                        e);

                        throw new RuntimeException(e);
                }
        }

        // =========================================================
        // SERVIDOR INICIADO
        // =========================================================

        @SubscribeEvent
        public void onServerStarted(ServerStartedEvent event) {

                server = event.getServer();

                LOGGER.info(
                                "Servidor de Minecraft iniciado.");

                // -----------------------------------------------------
                // Cargar configuración
                // -----------------------------------------------------

                loadConfiguration();

                // -----------------------------------------------------
                // Schematic Manager
                // -----------------------------------------------------

                schematicManager = new SchematicManager(
                                server.getServerDirectory()
                                                .toPath()
                                                .resolve("config")
                                                .resolve("worldedit")
                                                .resolve("schematics")
                                                .toFile());

                // -----------------------------------------------------
                // Dungeon Manager
                // -----------------------------------------------------

                dungeonManager = new DungeonManager(
                                server,
                                schematicManager);

                try {

                        dungeonManager.createDefaultConfiguration();

                } catch (Exception e) {

                        LOGGER.error(
                                        "No se pudo crear la configuración de las dungeons.",
                                        e);
                }

                // -----------------------------------------------------
                // HTTP Server
                // -----------------------------------------------------

                httpServer = new HttpServer(
                                server,
                                schematicManager);

                try {

                        LOGGER.info(
                                        "Intentando iniciar Dungeon API en el puerto {}...",
                                        PORT);

                        httpServer.start();

                } catch (Exception e) {

                        LOGGER.error(
                                        "No se pudo iniciar la Dungeon API en el puerto {}.",
                                        PORT,
                                        e);
                }
        }

        // =========================================================
        // SERVIDOR DETENIÉNDOSE
        // =========================================================

        @SubscribeEvent
        public void onServerStopping(ServerStoppingEvent event) {

                if (httpServer != null) {

                        httpServer.stop();

                        pendingDungeonSaves.clear();
                        httpServer = null;
                }

                server = null;

                LOGGER.info(
                                "Dungeon API detenida.");
        }

        // =========================================================
        // REGISTRO DE COMANDOS
        // =========================================================

        @SubscribeEvent
        public void onRegisterCommands(RegisterCommandsEvent event) {

                event.getDispatcher().register(

                                Commands.literal("dungeon-api")

                                                .requires(source -> source.hasPermission(2))

                                                // -------------------------------------------------
                                                // /dungeon-api reset <dungeon>
                                                // -------------------------------------------------

                                                .then(
                                                                Commands.literal("reset")

                                                                                .then(
                                                                                                Commands.argument(
                                                                                                                "dungeon",
                                                                                                                StringArgumentType
                                                                                                                                .word())

                                                                                                                .executes(context -> {

                                                                                                                        String dungeonId = StringArgumentType
                                                                                                                                        .getString(
                                                                                                                                                        context,
                                                                                                                                                        "dungeon");

                                                                                                                        if (dungeonManager == null) {

                                                                                                                                LOGGER.error(
                                                                                                                                                "La Dungeon API todavía no está preparada.");

                                                                                                                                return 0;
                                                                                                                        }

                                                                                                                        boolean success = dungeonManager
                                                                                                                                        .resetDungeon(
                                                                                                                                                        dungeonId);

                                                                                                                        return success ? 1
                                                                                                                                        : 0;
                                                                                                                })))

                                                // -------------------------------------------------
                                                // /dungeon-api reset-all
                                                // -------------------------------------------------

                                                .then(
                                                                Commands.literal("reset-all")

                                                                                .executes(context -> {

                                                                                        if (dungeonManager == null) {

                                                                                                LOGGER.error(
                                                                                                                "La Dungeon API todavía no está preparada.");

                                                                                                return 0;
                                                                                        }

                                                                                        DungeonResetResult result = dungeonManager
                                                                                                        .resetAllDungeons();

                                                                                        String message = "Dungeons regeneradas: "
                                                                                                        + result.getSuccessful()
                                                                                                        + "/"
                                                                                                        + result.getTotal();

                                                                                        server.getPlayerList()
                                                                                                        .broadcastSystemMessage(
                                                                                                                        Component.literal(
                                                                                                                                        message),
                                                                                                                        false);

                                                                                        if (result.getFailed() > 0) {

                                                                                                String errorMessage = "Dungeons con error: "
                                                                                                                + result.getFailed();

                                                                                                server.getPlayerList()
                                                                                                                .broadcastSystemMessage(
                                                                                                                                Component.literal(
                                                                                                                                                errorMessage),
                                                                                                                                false);
                                                                                        }

                                                                                        return result.getSuccessful();
                                                                                }))

                                                // -------------------------------------------------
                                                // /dungeon-api list
                                                // -------------------------------------------------

                                                .then(
                                                                Commands.literal("list")
                                                                                .executes(context -> {

                                                                                        if (dungeonManager == null) {

                                                                                                context.getSource()
                                                                                                                .sendFailure(
                                                                                                                                Component.literal(
                                                                                                                                                "La Dungeon API todavía no está preparada."));

                                                                                                return 0;
                                                                                        }

                                                                                        var dungeons = dungeonManager
                                                                                                        .loadDungeons();

                                                                                        if (dungeons.isEmpty()) {

                                                                                                context.getSource()
                                                                                                                .sendSuccess(
                                                                                                                                () -> Component.literal(
                                                                                                                                                "No hay dungeons disponibles."),
                                                                                                                                false);

                                                                                                return 1;
                                                                                        }

                                                                                        context.getSource().sendSuccess(
                                                                                                        () -> Component.literal(
                                                                                                                        "Dungeons disponibles ("
                                                                                                                                        + dungeons.size()
                                                                                                                                        + "):"),
                                                                                                        false);

                                                                                        for (DungeonConfig dungeon : dungeons) {

                                                                                                context.getSource()
                                                                                                                .sendSuccess(
                                                                                                                                () -> Component.literal(
                                                                                                                                                "- " + dungeon.getId()),
                                                                                                                                false);
                                                                                        }

                                                                                        return dungeons.size();
                                                                                }))

                                                // -------------------------------------------------
                                                // /dungeon-api save <nombreDungeon> <schematic> <pos>
                                                // -------------------------------------------------

                                                .then(
                                                                Commands.literal("save")
                                                                                .then(
                                                                                                Commands.argument(
                                                                                                                "nombreDungeon",
                                                                                                                StringArgumentType
                                                                                                                                .word())
                                                                                                                .then(
                                                                                                                                Commands.argument(
                                                                                                                                                "schematic",
                                                                                                                                                StringArgumentType
                                                                                                                                                                .word())
                                                                                                                                                .then(
                                                                                                                                                                Commands.argument(
                                                                                                                                                                                "pos",
                                                                                                                                                                                Vec3Argument.vec3())
                                                                                                                                                                                .executes(context -> {

                                                                                                                                                                                        if (dungeonManager == null) {

                                                                                                                                                                                                context.getSource()
                                                                                                                                                                                                                .sendFailure(
                                                                                                                                                                                                                                Component.literal(
                                                                                                                                                                                                                                                "La Dungeon API todavía no está preparada."));

                                                                                                                                                                                                return 0;
                                                                                                                                                                                        }

                                                                                                                                                                                        ServerPlayer player;

                                                                                                                                                                                        try {
                                                                                                                                                                                                player = context.getSource()
                                                                                                                                                                                                                .getPlayerOrException();
                                                                                                                                                                                        } catch (Exception e) {
                                                                                                                                                                                                context.getSource()
                                                                                                                                                                                                                .sendFailure(
                                                                                                                                                                                                                                Component.literal(
                                                                                                                                                                                                                                                "Este comando debe ejecutarse desde un jugador."));
                                                                                                                                                                                                return 0;
                                                                                                                                                                                        }

                                                                                                                                                                                        String dungeonId = StringArgumentType
                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                        context,
                                                                                                                                                                                                                        "nombreDungeon");

                                                                                                                                                                                        String schematic = StringArgumentType
                                                                                                                                                                                                        .getString(
                                                                                                                                                                                                                        context,
                                                                                                                                                                                                                        "schematic");

                                                                                                                                                                                        Vec3 position = Vec3Argument
                                                                                                                                                                                                        .getVec3(
                                                                                                                                                                                                                        context,
                                                                                                                                                                                                                        "pos");

                                                                                                                                                                                        BlockPos blockPos = BlockPos
                                                                                                                                                                                                        .containing(position);

                                                                                                                                                                                        String world = context
                                                                                                                                                                                                        .getSource()
                                                                                                                                                                                                        .getLevel()
                                                                                                                                                                                                        .dimension()
                                                                                                                                                                                                        .location()
                                                                                                                                                                                                        .toString();

                                                                                                                                                                                        if (!dungeonId.matches(
                                                                                                                                                                                                        "[a-zA-Z0-9_-]+")) {

                                                                                                                                                                                                context.getSource()
                                                                                                                                                                                                                .sendFailure(
                                                                                                                                                                                                                                Component.literal(
                                                                                                                                                                                                                                                "Nombre de dungeon no válido. Usa letras, números, guiones o guiones bajos."));
                                                                                                                                                                                                return 0;
                                                                                                                                                                                        }

                                                                                                                                                                                        // Si
                                                                                                                                                                                        // ya
                                                                                                                                                                                        // existe,
                                                                                                                                                                                        // solicitar
                                                                                                                                                                                        // confirmación.
                                                                                                                                                                                        if (dungeonManager
                                                                                                                                                                                                        .dungeonExists(dungeonId)) {

                                                                                                                                                                                                pendingDungeonSaves
                                                                                                                                                                                                                .put(
                                                                                                                                                                                                                                player.getUUID(),
                                                                                                                                                                                                                                new PendingDungeonSave(
                                                                                                                                                                                                                                                dungeonId,
                                                                                                                                                                                                                                                schematic,
                                                                                                                                                                                                                                                world,
                                                                                                                                                                                                                                                blockPos.getX(),
                                                                                                                                                                                                                                                blockPos.getY(),
                                                                                                                                                                                                                                                blockPos.getZ()));

                                                                                                                                                                                                context.getSource()
                                                                                                                                                                                                                .sendSuccess(
                                                                                                                                                                                                                                () -> Component.literal(
                                                                                                                                                                                                                                                "La dungeon '" + dungeonId
                                                                                                                                                                                                                                                                + "' ya existe. Escribe 'y' en el chat para sobrescribirla. Cualquier otra respuesta cancelará el guardado."),
                                                                                                                                                                                                                                false);

                                                                                                                                                                                                return 1;
                                                                                                                                                                                        }

                                                                                                                                                                                        // Crear
                                                                                                                                                                                        // una
                                                                                                                                                                                        // dungeon
                                                                                                                                                                                        // nueva.
                                                                                                                                                                                        boolean success = dungeonManager
                                                                                                                                                                                                        .createDungeon(
                                                                                                                                                                                                                        dungeonId,
                                                                                                                                                                                                                        schematic,
                                                                                                                                                                                                                        world,
                                                                                                                                                                                                                        blockPos.getX(),
                                                                                                                                                                                                                        blockPos.getY(),
                                                                                                                                                                                                                        blockPos.getZ(),
                                                                                                                                                                                                                        false);

                                                                                                                                                                                        if (!success) {

                                                                                                                                                                                                context.getSource()
                                                                                                                                                                                                                .sendFailure(
                                                                                                                                                                                                                                Component.literal(
                                                                                                                                                                                                                                                "No se pudo crear la dungeon. Comprueba que el schematic existe y que la configuración es válida."));
                                                                                                                                                                                                return 0;
                                                                                                                                                                                        }

                                                                                                                                                                                        context.getSource()
                                                                                                                                                                                                        .sendSuccess(
                                                                                                                                                                                                                        () -> Component.literal(
                                                                                                                                                                                                                                        "Dungeon '" + dungeonId
                                                                                                                                                                                                                                                        + "' creada correctamente."),
                                                                                                                                                                                                                        false);

                                                                                                                                                                                        return 1;
                                                                                                                                                                                })))))

                );
        }

        private static class PendingDungeonSave {

                private final String dungeonId;
                private final String schematic;
                private final String world;
                private final int x;
                private final int y;
                private final int z;

                private PendingDungeonSave(
                                String dungeonId,
                                String schematic,
                                String world,
                                int x,
                                int y,
                                int z) {

                        this.dungeonId = dungeonId;
                        this.schematic = schematic;
                        this.world = world;
                        this.x = x;
                        this.y = y;
                        this.z = z;
                }
        }

        @SubscribeEvent
        public void onServerChat(ServerChatEvent event) {

                ServerPlayer player = event.getPlayer();

                PendingDungeonSave pending = pendingDungeonSaves.remove(player.getUUID());

                // Este jugador no tiene ninguna confirmación pendiente.
                if (pending == null) {
                        return;
                }

                // La respuesta se procesa como confirmación,
                // no como un mensaje público del chat.
                event.setCanceled(true);

                String answer = event.getMessage()
                                .getString()
                                .trim();

                if (!answer.equalsIgnoreCase("y")) {

                        player.sendSystemMessage(
                                        Component.literal(
                                                        "Guardado cancelado. La dungeon '"
                                                                        + pending.dungeonId
                                                                        + "' no se ha sobrescrito."));

                        return;
                }

                if (dungeonManager == null) {

                        player.sendSystemMessage(
                                        Component.literal(
                                                        "No se pudo guardar: DungeonAPI no está preparada."));

                        return;
                }

                boolean success = dungeonManager.createDungeon(
                                pending.dungeonId,
                                pending.schematic,
                                pending.world,
                                pending.x,
                                pending.y,
                                pending.z,
                                true);

                if (success) {

                        player.sendSystemMessage(
                                        Component.literal(
                                                        "Dungeon '"
                                                                        + pending.dungeonId
                                                                        + "' sobrescrita correctamente."));

                } else {

                        player.sendSystemMessage(
                                        Component.literal(
                                                        "No se pudo sobrescribir la dungeon. "
                                                                        + "Comprueba el schematic y los registros del servidor."));
                }
        }

        // =========================================================
        // GET SERVER
        // =========================================================

        public static MinecraftServer getServer() {

                return server;
        }
}