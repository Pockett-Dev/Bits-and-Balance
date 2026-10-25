package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentAccess;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHangIntentClientCache;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;

public final class FabricEnhancedSlabHangIntentNetworking {
    private FabricEnhancedSlabHangIntentNetworking() {
    }

    public static void initClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(EnhancedSlabHangIntentUpdatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                EnhancedSlabHangIntentClientCache.apply(payload.pos(), payload.explicit());

                var level = Minecraft.getInstance().level;
                if (level != null) {
                    var pos = net.minecraft.core.BlockPos.of(payload.pos());
                    EnhancedSlabHelper.invalidateVisualYOffsetCacheAround(level, pos);
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                } else {
                    EnhancedSlabHelper.bumpVisualYOffsetCacheEpoch();
                }
            });
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(EnhancedSlabHangIntentBulkPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                EnhancedSlabHangIntentClientCache.clear();
                for (long posLong : payload.positions()) {
                    EnhancedSlabHangIntentClientCache.apply(posLong, true);
                }
                EnhancedSlabHelper.bumpVisualYOffsetCacheEpoch();
            });
        });
    }

    public static void sendUpdateToAll(ServerLevel level, long posLong, boolean explicit) {
        EnhancedSlabHangIntentUpdatePayload payload = new EnhancedSlabHangIntentUpdatePayload(posLong, explicit);
        for (ServerPlayer player : level.players()) {
            ServerPlayNetworking.send(player, payload);
        }
    }

    public static void sendBulkTo(ServerPlayer player, ServerLevel level) {
        long[] positions = EnhancedSlabHangIntentAccess.get(level).snapshot();
        ServerPlayNetworking.send(player, new EnhancedSlabHangIntentBulkPayload(positions));
    }
}
