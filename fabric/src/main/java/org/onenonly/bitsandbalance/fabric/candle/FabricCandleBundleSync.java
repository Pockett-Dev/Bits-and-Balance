package org.onenonly.bitsandbalance.fabric.candle;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.CandleBlock;
import org.onenonly.bitsandbalance.common.candle.CandleBundleAccess;
import org.onenonly.bitsandbalance.fabric.network.FabricCandleBundleNetworking;

public final class FabricCandleBundleSync {
    private FabricCandleBundleSync() {
    }

    @SuppressWarnings({"unused", "null"})
    public static void init() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            if (!(handler.player.level() instanceof ServerLevel serverLevel)) return;
            FabricCandleBundleNetworking.sendBulkTo(handler.player, serverLevel);
        });

        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world == null || world.isClientSide()) return;
            if (!(world instanceof ServerLevel serverLevel)) return;
            if (player != null && !player.getAbilities().instabuild) return;
            long posLong = pos.asLong();

            if (state.getBlock() instanceof CandleBlock && state.hasProperty(CandleBlock.CANDLES)) {
                var bundleData = CandleBundleAccess.get(serverLevel);
                if (bundleData.has(posLong)) {
                    bundleData.remove(posLong);
                    FabricCandleBundleNetworking.sendUpdateToAll(serverLevel, posLong, -1, -1, -1, -1);
                }
            }
        });
    }
}