package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.client.YearnEchoClient;

public final class FabricYearnEchoSync {
    private FabricYearnEchoSync() {
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(YearnEchoPayload.TYPE, (payload, context) ->
                context.client().execute(() -> YearnEchoClient.start(payload.x(), payload.y(), payload.z())));
    }

    public static void send(ServerPlayer player) {
        if (ServerPlayNetworking.canSend(player, YearnEchoPayload.TYPE)) {
            ServerPlayNetworking.send(player, new YearnEchoPayload(
                    player.getX(), player.getEyeY(), player.getZ()));
        }
    }
}
