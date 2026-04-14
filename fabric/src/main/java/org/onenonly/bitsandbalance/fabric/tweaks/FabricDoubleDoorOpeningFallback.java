package org.onenonly.bitsandbalance.fabric.tweaks;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;

public final class FabricDoubleDoorOpeningFallback {
    private FabricDoubleDoorOpeningFallback() {
    }

    public static void init() {
        UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
            if (!FabricTweaksConfig.enableDoubleDoorOpening || !FabricTweaksConfig.doubleDoorChainTrapdoors) {
                return InteractionResult.PASS;
            }
            if (level.isClientSide()) {
                return InteractionResult.PASS;
            }

            BlockState state = level.getBlockState(hitResult.getBlockPos());
            if (!FabricTrapDoorInteractionFallbackHelper.shouldUseInteractionFallback(state)) {
                return InteractionResult.PASS;
            }

            return FabricTrapDoorInteractionFallbackHelper.handlePlayerTrapdoorInteraction(level, hitResult.getBlockPos(), player)
                    ? InteractionResult.CONSUME
                    : InteractionResult.PASS;
        });
    }
}