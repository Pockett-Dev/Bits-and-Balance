package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabFaceCullingHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.block.ModelBlockRenderer")
public abstract class EnhancedSlabFaceCullingMixin {
    @Inject(
            method = "shouldRenderFace(Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void bitsandbalance$renderFacesNextToOffsetBlocks(BlockAndTintGetter level,
                                                              BlockState state,
                                                              Direction direction,
                                                              BlockPos adjacentPos,
                                                              CallbackInfoReturnable<Boolean> cir) {
        if (!FabricTweaksConfig.enableEnhancedSlabs || direction == null || adjacentPos == null) {
            return;
        }

        BlockPos pos = adjacentPos.relative(direction.getOpposite());
        if (EnhancedSlabFaceCullingHelper.shouldForceRenderFace(level, pos, state, direction, adjacentPos)) {
            cir.setReturnValue(true);
        }
    }
}
