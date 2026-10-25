package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;

public final class FabricBioluminescenceSyncClient {
    private FabricBioluminescenceSyncClient() {
    }

    public static void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(BioluminescenceSyncPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> applyClientState(payload.entityId(), payload.remainingTicks(), payload.amplifier()));
        });
    }

    private static void applyClientState(int entityId, int remainingTicks, int amplifier) {
        if (Minecraft.getInstance().level == null) {
            return;
        }

        BioluminescenceLightManager.syncClientBioluminescence(entityId, remainingTicks, amplifier);
    }
}
