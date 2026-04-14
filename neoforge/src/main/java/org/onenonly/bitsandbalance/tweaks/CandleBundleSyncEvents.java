package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.CandleBlock;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.candle.CandleBundleAccess;
import org.onenonly.bitsandbalance.common.candle.CandleBundleColorUtil;
import org.onenonly.bitsandbalance.network.CandleBundleColorsBulkPayload;
import org.onenonly.bitsandbalance.network.CandleBundleColorsUpdatePayload;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
@SuppressWarnings("null")
public final class CandleBundleSyncEvents {
    private CandleBundleSyncEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getPlayer() != null && !event.getPlayer().getAbilities().instabuild) {
            return;
        }

        BlockPos pos = event.getPos();
        var state = level.getBlockState(pos);
        if (state.getBlock() instanceof CandleBlock && state.hasProperty(CandleBlock.CANDLES)) {
            long posLong = pos.asLong();
            var bundleData = CandleBundleAccess.get(level);
            if (bundleData.has(posLong)) {
                bundleData.remove(posLong);
                for (ServerPlayer player : level.players()) {
                    PacketDistributor.sendToPlayer(player, new CandleBundleColorsUpdatePayload(posLong, -1, -1, -1, -1));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        var bundles = CandleBundleAccess.get(level).snapshot();
        long[] positions = new long[bundles.size()];
        int[] color0 = new int[bundles.size()];
        int[] color1 = new int[bundles.size()];
        int[] color2 = new int[bundles.size()];
        int[] color3 = new int[bundles.size()];
        for (int index = 0; index < bundles.size(); index++) {
            var entry = bundles.get(index);
            positions[index] = entry.pos();
            int[] colors = CandleBundleColorUtil.computeBundleColors(entry.stacks());
            color0[index] = colors[0];
            color1[index] = colors[1];
            color2[index] = colors[2];
            color3[index] = colors[3];
        }
        PacketDistributor.sendToPlayer(player, new CandleBundleColorsBulkPayload(positions, color0, color1, color2, color3));
    }
}