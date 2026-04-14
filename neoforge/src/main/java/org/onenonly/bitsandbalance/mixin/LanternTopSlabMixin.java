package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LanternBlock.class)
public abstract class LanternTopSlabMixin {

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$allowLanternsUnderEnhancedSlabs(BlockState state, LevelReader level,
                                                                BlockPos pos,
                                                                CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsHangBelow) return;
        if (!state.getValue(BlockStateProperties.HANGING)) return;

        BlockPos supportPos = pos.above();
        BlockState supportState = level.getBlockState(supportPos);
        if (EnhancedSlabHelper.hasFullBottomSurface(level, supportPos, supportState)) {
            cir.setReturnValue(true);
        }
    }
}