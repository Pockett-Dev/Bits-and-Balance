package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class EnhancedSlabInteractionShapeMixin {

    @Inject(
        method = "getInteractionShape(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/shapes/VoxelShape;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void bitsandbalance$offsetInteractionShape(
            BlockGetter level, BlockPos pos,
            CallbackInfoReturnable<VoxelShape> cir) {

        if (!Config.enableEnhancedSlabs) return;

        BlockState state = (BlockState) (Object) this;
        double yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
        if (yOff != 0.0) {
            cir.setReturnValue(cir.getReturnValue().move(0.0, yOff, 0.0));
        }
    }
}