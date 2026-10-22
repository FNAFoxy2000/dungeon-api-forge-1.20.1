package com.dungeonapi;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "dungeonapi", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BonfireInteractionHandler {

    @SubscribeEvent
    // Maneja la interacción del jugador con una hoguera.
    public static void onBonfireInteract(
            PlayerInteractEvent.RightClickBlock event) {

        // Evitar procesar la interacción dos veces.
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }

        // Solo ejecutar la lógica en el servidor.
        if (event.getLevel().isClientSide()) {
            return;
        }

        // Comprobar que el jugador interactúa con una
        // hoguera normal de Minecraft.
        if (!event.getLevel().getBlockState(event.getPos())
                .is(Blocks.CAMPFIRE)) {
            return;
        }

        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        onBonfireActivated(player, event.getPos());
    }

    // Lógica de activación de la hoguera.
    private static void onBonfireActivated(ServerPlayer player, BlockPos campfirePos) {

        // 1. Establecer el punto de reaparición.
        BlockPos position = player.blockPosition();

        player.setRespawnPosition(
                player.serverLevel().dimension(),
                position,
                player.getYRot(),
                true,
                true);

        // 2. Consultar la dungeon asignada a esta hoguera.
        BonfireManager bonfireManager = DungeonAPI.getBonfireManager();

        if (bonfireManager == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "[DungeonAPI] El gestor de hogueras no está disponible."));
            return;
        }

        String dungeonId = bonfireManager.getDungeon(
                player.serverLevel().dimension(),
                campfirePos);

        // 3. Si no tiene dungeon, no se reinicia ninguna.
        if (dungeonId == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "[DungeonAPI] Punto de reaparición actualizado."));
            return;
        }

        // 4. Reiniciar exclusivamente la dungeon asignada.
        DungeonManager dungeonManager = DungeonAPI.getDungeonManager();

        if (dungeonManager == null) {
            player.sendSystemMessage(
                    Component.literal(
                            "[DungeonAPI] El gestor de dungeons no está disponible."));
            return;
        }

        boolean success = dungeonManager.resetDungeon(dungeonId);

        if (success) {
            player.sendSystemMessage(
                    Component.literal(
                            "[DungeonAPI] Hoguera activada. Dungeon '" +
                                    dungeonId + "' regenerada."));
        } else {
            player.sendSystemMessage(
                    Component.literal(
                            "[DungeonAPI] Hoguera activada, pero no se pudo regenerar '" +
                                    dungeonId + "'. Revisa el registro del servidor."));
        }
    }

    // Determina la posición de la hoguera con la que el jugador está interactuando.
    private static BlockPos positionOfCampfire(ServerPlayer player) {
        var hit = player.pick(6.0D, 0.0F, false);

        if (hit instanceof net.minecraft.world.phys.BlockHitResult blockHit
                && hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK
                && player.level().getBlockState(blockHit.getBlockPos())
                        .is(Blocks.CAMPFIRE)) {
            return blockHit.getBlockPos();
        }

        return player.blockPosition();
    }

}