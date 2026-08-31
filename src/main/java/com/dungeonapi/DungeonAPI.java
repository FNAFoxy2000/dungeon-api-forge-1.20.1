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
                                                                                })));
        }

        // =========================================================
        // GET SERVER
        // =========================================================

        public static MinecraftServer getServer() {

                return server;
        }
}