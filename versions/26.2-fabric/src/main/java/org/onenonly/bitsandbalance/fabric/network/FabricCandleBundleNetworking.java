package org.onenonly.bitsandbalance.fabric.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.onenonly.bitsandbalance.common.candle.CandleBundleAccess;
import org.onenonly.bitsandbalance.common.candle.CandleBundleColorClientCache;
import org.onenonly.bitsandbalance.common.candle.CandleBundleColorUtil;

public final class FabricCandleBundleNetworking {
    private FabricCandleBundleNetworking() {
    }

    public static void initClient() {
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CandleBundleColorsUpdatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                CandleBundleColorClientCache.apply(payload.pos(), payload.c0(), payload.c1(), payload.c2(), payload.c3());
                var level = Minecraft.getInstance().level;
                if (level != null) {
                    var pos = net.minecraft.core.BlockPos.of(payload.pos());
                    level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                }
            });
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CandleBundleColorsBulkPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                CandleBundleColorClientCache.clear();
                long[] pos = payload.positions();
                int[] c0 = payload.c0();
                int[] c1 = payload.c1();
                int[] c2 = payload.c2();
                int[] c3 = payload.c3();
                int n = pos.length;
                n = Math.min(n, c0.length);
                n = Math.min(n, c1.length);
                n = Math.min(n, c2.length);
                n = Math.min(n, c3.length);
                for (int i = 0; i < n; i++) {
                    CandleBundleColorClientCache.apply(pos[i], c0[i], c1[i], c2[i], c3[i]);
                }
            });
        });

        net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(CandleBundleContentsUpdatePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                var level = Minecraft.getInstance().level;
                if (level == null) return;

                org.onenonly.bitsandbalance.common.candle.CandleBundleContentsClientCache.apply(
                    payload.pos(),
                    payload.s0(),
                    payload.s1(),
                    payload.s2(),
                    payload.s3()
                );

                var pos = net.minecraft.core.BlockPos.of(payload.pos());
                var be = level.getBlockEntity(pos);
                if (be instanceof org.onenonly.bitsandbalance.common.blockentity.CandleBundleBlockEntity bundleBe) {
                    bundleBe.setCandles(java.util.List.of(payload.s0(), payload.s1(), payload.s2(), payload.s3()));
                }
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
            });
        });
    }

    public static void sendUpdateToAll(ServerLevel level, long posLong, int c0, int c1, int c2, int c3) {
        for (ServerPlayer sp : level.players()) {
            ServerPlayNetworking.send(sp, new CandleBundleColorsUpdatePayload(posLong, c0, c1, c2, c3));
        }
    }

    public static void sendContentsUpdateToAll(ServerLevel level, long posLong, java.util.List<net.minecraft.world.item.ItemStack> stacks) {
        net.minecraft.world.item.ItemStack s0 = (stacks != null && stacks.size() > 0) ? stacks.get(0).copyWithCount(1) : net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack s1 = (stacks != null && stacks.size() > 1) ? stacks.get(1).copyWithCount(1) : net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack s2 = (stacks != null && stacks.size() > 2) ? stacks.get(2).copyWithCount(1) : net.minecraft.world.item.ItemStack.EMPTY;
        net.minecraft.world.item.ItemStack s3 = (stacks != null && stacks.size() > 3) ? stacks.get(3).copyWithCount(1) : net.minecraft.world.item.ItemStack.EMPTY;

        for (ServerPlayer sp : level.players()) {
            ServerPlayNetworking.send(sp, new CandleBundleContentsUpdatePayload(posLong, s0, s1, s2, s3));
        }
    }

    public static void sendBulkTo(ServerPlayer player, ServerLevel level) {
        var bundleData = CandleBundleAccess.get(level);
        var entries = bundleData.snapshot();

        long[] positions = new long[entries.size()];
        int[] c0 = new int[entries.size()];
        int[] c1 = new int[entries.size()];
        int[] c2 = new int[entries.size()];
        int[] c3 = new int[entries.size()];

        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            positions[i] = e.pos();
            int[] colors = CandleBundleColorUtil.computeBundleColors(e.stacks());
            c0[i] = colors[0];
            c1[i] = colors[1];
            c2[i] = colors[2];
            c3[i] = colors[3];
        }

        ServerPlayNetworking.send(player, new CandleBundleColorsBulkPayload(positions, c0, c1, c2, c3));
    }
}