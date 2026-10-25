package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFaceCullingHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class EnhancedSlabBlockFaceCullingMixin {
    @Inject(
            method = "shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private static void bitsandbalance$renderFacesNextToPotentiallyOffsetBlocks(BlockState state,
                                                                                BlockState adjacentState,
                                                                                Direction direction,
                                                                                CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            return;
        }
        if (EnhancedSlabFaceCullingHelper.shouldForceRenderFaceWithoutLevel(state, adjacentState)) {
            cir.setReturnValue(true);
        }
    }
}
