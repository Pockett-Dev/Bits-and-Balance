package org.onenonly.bitsandbalance.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedStepFluidWallGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class EnhancedStepFluidWallMixin {
    @ModifyReturnValue(
            method = "canPassThroughWall(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("RETURN")
    )
    private static boolean bitsandbalance$blockStackedStepWall(boolean original,
                                                               Direction direction,
                                                               BlockGetter level,
                                                               BlockPos fromPos,
                                                               BlockState fromState,
                                                               BlockPos toPos,
                                                               BlockState toState) {
        if (!original
                || !Config.enableEnhancedSlabs
                || !Config.enhancedSlabsSteps
                || direction == null
                || !direction.getAxis().isHorizontal()) {
            return original;
        }
        return !EnhancedStepFluidWallGeometry.blocksAxisFlow(level, fromPos, fromState, direction)
                && !EnhancedStepFluidWallGeometry.blocksAxisFlow(
                        level, toPos, toState, direction.getOpposite());
    }
}
