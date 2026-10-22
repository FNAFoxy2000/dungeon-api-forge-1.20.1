
package com.dungeonapi;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.fml.ModList;

import java.lang.reflect.Constructor;

public final class AscendScreenOpener {

    private static final String ASCEND_MOD_ID = "ascend";

    private static final String STATS_SCREEN_CLASS =
            "net.fretux.ascend.client.screen.StatsScreen";

    private AscendScreenOpener() {
    }

    public static void openIfPresent() {

        // Ascend es opcional: si no está instalado, no hacemos nada.
        if (!ModList.get().isLoaded(ASCEND_MOD_ID)) {
            return;
        }

        try {
            Class<?> screenClass = Class.forName(STATS_SCREEN_CLASS);

            Constructor<?> constructor =
                    screenClass.getDeclaredConstructor();

            Object instance = constructor.newInstance();

            if (instance instanceof Screen screen) {
                Minecraft.getInstance().setScreen(screen);
            } else {
                DungeonAPI.LOGGER.warn(
                        "La pantalla de Ascend encontrada no es una pantalla de Minecraft."
                );
            }

        } catch (ReflectiveOperationException | LinkageError e) {
            DungeonAPI.LOGGER.warn(
                    "No se pudo abrir la pantalla de estadísticas de Ascend. "
                            + "Comprueba si la versión instalada es compatible.",
                    e
            );
        }
    }
}
