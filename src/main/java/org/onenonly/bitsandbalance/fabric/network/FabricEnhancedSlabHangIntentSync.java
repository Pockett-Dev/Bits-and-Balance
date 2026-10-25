package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;

public final class FabricEnhancedSlabHangIntentSync {
    private FabricEnhancedSlabHangIntentSync() {
    }

    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (handler.player.level() instanceof ServerLevel serverLevel) {
                FabricEnhancedSlabHangIntentNetworking.sendBulkTo(handler.player, serverLevel);
            }
        });
    }
}