package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.client.PonderLightAuditClient;

public final class FabricPonderLightAuditSync {
    private FabricPonderLightAuditSync() {
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(PonderLightAuditPayload.TYPE, (payload, context) ->
                context.client().execute(() -> PonderLightAuditClient.start(
                        payload.x(), payload.y(), payload.z(), payload.emission())));
    }

    public static void send(ServerPlayer player, int emission) {
        if (ServerPlayNetworking.canSend(player, PonderLightAuditPayload.TYPE)) {
            ServerPlayNetworking.send(player, new PonderLightAuditPayload(
                    player.getX(), player.getEyeY(), player.getZ(), emission));
        }
    }
}
