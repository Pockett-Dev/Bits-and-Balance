package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.client.BiomeTitlesClient;
import org.onenonly.bitsandbalance.common.mechanics.BiomeTitlesStructureTracker;

public final class FabricBiomeTitlesStructureSync {
    private FabricBiomeTitlesStructureSync() {
    }

    public static void initServer() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            BiomeTitlesStructureTracker.forgetMissingPlayers(server.getPlayerList().getPlayers());
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                BiomeTitlesStructureTracker.tick(player, FabricBiomeTitlesStructureSync::send);
            }
        });
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(BiomeTitlesStructurePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> BiomeTitlesClient.setInsideStructure(payload.insideStructure()));
        });
    }

    private static void send(ServerPlayer player, boolean insideStructure) {
        if (!ServerPlayNetworking.canSend(player, BiomeTitlesStructurePayload.TYPE)) {
            return;
        }
        ServerPlayNetworking.send(player, new BiomeTitlesStructurePayload(insideStructure));
    }
}
