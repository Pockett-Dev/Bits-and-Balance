package org.onenonly.bitsandbalance.fabric.mechanics;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.QuickHarvestingLogic;
import org.onenonly.bitsandbalance.fabric.config.FabricMechanicsConfig;

/**
 * Fabric port: Quick Harvesting
 * Right-click mature crops with hands/hoes/axes to harvest and replant.
 */
public final class FabricQuickHarvesting {
    private FabricQuickHarvesting() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!FabricMechanicsConfig.enableQuickHarvesting) return InteractionResult.PASS;
            if (level.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

            BlockPos clickedPos = hitResult.getBlockPos();
            BlockState clickedState = level.getBlockState(clickedPos);

            try {
                boolean handled = QuickHarvestingLogic.tryHandleInteraction(
                        sp,
                        level,
                        hand == null ? InteractionHand.MAIN_HAND : hand,
                        clickedPos,
                        clickedState,
                        FabricMechanicsConfig.enableQuickHarvestingHoes,
                        FabricMechanicsConfig.enableQuickHarvestingAxes,
                        FabricMechanicsConfig.quickHarvestingHomeDropsToUser
                );

                if (handled) {
                    return InteractionResult.SUCCESS;
                }
            } catch (Throwable ignored) {
            }

            return InteractionResult.PASS;
        });
    }
}
