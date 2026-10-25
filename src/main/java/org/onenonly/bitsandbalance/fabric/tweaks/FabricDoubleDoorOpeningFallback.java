package org.onenonly.bitsandbalance.fabric.tweaks;

import org.onenonly.bitsandbalance.common.config.DoubleDoorFeatureFlags;
import org.onenonly.bitsandbalance.common.mechanics.TrapDoorInteractionFallbackCore;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;

public final class FabricDoubleDoorOpeningFallback {
    private FabricDoubleDoorOpeningFallback() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled() || !DoubleDoorFeatureFlags.doubleDoorChainTrapdoorsEnabled()) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            BlockState state = level.getBlockState(hitResult.getBlockPos());
            if (!TrapDoorInteractionFallbackCore.shouldUseInteractionFallback(state)) {
                return InteractionResult.PASS;
            }

            TrapDoorInteractionFallbackCore.schedulePostInteractionTrapdoorChain(level, hitResult.getBlockPos(), player, state);
            return InteractionResult.PASS;
        });
    }
}
