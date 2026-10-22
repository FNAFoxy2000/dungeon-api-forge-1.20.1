
package com.dungeonapi;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

import java.util.Optional;
import java.util.function.Supplier;

public class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private static final SimpleChannel CHANNEL =
            NetworkRegistry.newSimpleChannel(
                    new ResourceLocation(DungeonAPI.MOD_ID, "main"),
                    () -> PROTOCOL_VERSION,
                    PROTOCOL_VERSION::equals,
                    PROTOCOL_VERSION::equals
            );

    private static boolean registered = false;

    private NetworkHandler() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        CHANNEL.registerMessage(
                0,
                OpenAscendScreenPacket.class,
                OpenAscendScreenPacket::encode,
                OpenAscendScreenPacket::decode,
                OpenAscendScreenPacket::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT)
        );

        registered = true;

        DungeonAPI.LOGGER.info(
                "Canal de red de DungeonAPI registrado."
        );
    }

    public static void openAscendScreen(ServerPlayer player) {
        CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new OpenAscendScreenPacket()
        );
    }

    public static class OpenAscendScreenPacket {

        public OpenAscendScreenPacket() {
        }

        public static void encode(
                OpenAscendScreenPacket packet,
                FriendlyByteBuf buffer
        ) {
            // No necesita datos: solo solicita abrir la pantalla.
        }

        public static OpenAscendScreenPacket decode(
                FriendlyByteBuf buffer
        ) {
            return new OpenAscendScreenPacket();
        }

        public static void handle(
                OpenAscendScreenPacket packet,
                Supplier<NetworkEvent.Context> contextSupplier
        ) {
            NetworkEvent.Context context = contextSupplier.get();

            context.enqueueWork(() ->
                    DistExecutor.unsafeRunWhenOn(
                            Dist.CLIENT,
                            () -> () -> AscendScreenOpener.openIfPresent()
                    )
            );

            context.setPacketHandled(true);
        }
    }
}
