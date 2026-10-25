package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.compat.ThatchLighting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ThatchBlockStateLightingMixin {
    @Inject(method = {"isSolidRender", "isViewBlocking", "isSuffocating"}, at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$thatchOcclusion(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (ThatchLighting.usesThatchLighting((BlockState) (Object) this, level, pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$thatchOcclusionShape(BlockGetter level, BlockPos pos, CallbackInfoReturnable<VoxelShape> cir) {
        if (ThatchLighting.usesThatchLighting((BlockState) (Object) this, level, pos)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "getFaceOcclusionShape", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$thatchFaceOcclusion(BlockGetter level, BlockPos pos, Direction face,
                                                  CallbackInfoReturnable<VoxelShape> cir) {
        if (ThatchLighting.usesThatchLighting((BlockState) (Object) this, level, pos)) {
            cir.setReturnValue(Shapes.empty());
        }
    }

    @Inject(method = "getShadeBrightness", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$thatchShade(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (ThatchLighting.usesThatchLighting((BlockState) (Object) this, level, pos)) {
            cir.setReturnValue(1.0F);
        }
    }

    @Inject(method = "propagatesSkylightDown", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$thatchSkylight(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (ThatchLighting.usesThatchLighting((BlockState) (Object) this, level, pos)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getLightBlock", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$thatchLightBlock(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (ThatchLighting.usesThatchLighting((BlockState) (Object) this, level, pos)) {
            cir.setReturnValue(0);
        }
    }
}
