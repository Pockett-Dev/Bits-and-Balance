package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FaceAttachedHorizontalDirectionalBlock.class)
public abstract class FaceAttachedHorizontalDirectionalEnhancedSlabMixin {

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$allowCeilingAttachmentsUnderEnhancedSlabs(BlockState state, LevelReader level,
                                                                          BlockPos pos,
                                                                          CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow) return;

        FaceAttachedHorizontalDirectionalBlock self = (FaceAttachedHorizontalDirectionalBlock) (Object) this;
        if (!(self instanceof LeverBlock) && !(self instanceof ButtonBlock)) return;
        if (state.getValue(FaceAttachedHorizontalDirectionalBlock.FACE) != AttachFace.CEILING) return;

        BlockPos supportPos = pos.above();
        BlockState supportState = level.getBlockState(supportPos);
        if (EnhancedSlabHelper.hasFullBottomSurface(level, supportPos, supportState)) {
            cir.setReturnValue(true);
        }
    }
}