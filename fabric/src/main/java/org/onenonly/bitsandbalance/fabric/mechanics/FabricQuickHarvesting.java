package org.onenonly.bitsandbalance.fabric.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import org.onenonly.bitsandbalance.common.mechanics.QuickHarvestingLogic;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;
import org.onenonly.bitsandbalance.fabric.network.QuickHarvestPayload;

/**
 * Fabric port: Quick Harvesting
 * Right-click mature crops with hands/hoes/axes to harvest and replant.
 */
public final class FabricQuickHarvesting {
    private FabricQuickHarvesting() {
    }

    public static void init() {
        ServerPlayNetworking.registerGlobalReceiver(QuickHarvestPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                if (!FabricMechanicsConfig.enableQuickHarvesting) return;
                if (player.isSpectator()) return;
                if (!(player.level() instanceof ServerLevel level)) return;

                BlockPos clickedPos = payload.pos();
                if (!level.isLoaded(clickedPos)) return;

                BlockState clickedState = level.getBlockState(clickedPos);
                if (!player.blockPosition().closerThan(clickedPos, 8.0D)) return;

                tryHandleInteraction(player, level, payload.hand(), clickedPos, clickedState);
            });
        });
    }

    public static boolean tryHandleInteraction(ServerPlayer player, net.minecraft.world.level.Level level, InteractionHand hand, BlockPos clickedPos, BlockState clickedState) {
        if (QuickHarvestingLogic.tryHandleInteraction(player, level, hand, clickedPos, clickedState, FabricMechanicsConfig.enableQuickHarvestingHoes, FabricMechanicsConfig.enableQuickHarvestingAxes, FabricMechanicsConfig.quickHarvestingHomeDropsToUser)) {
            player.swing(hand, true);
            return true;
        }
        return false;
    }

    public static boolean shouldHandleInteraction(ItemStack stack, BlockState state) {
        return QuickHarvestingLogic.shouldHandleInteraction(stack, state, FabricMechanicsConfig.enableQuickHarvestingHoes, FabricMechanicsConfig.enableQuickHarvestingAxes);
    }
}
