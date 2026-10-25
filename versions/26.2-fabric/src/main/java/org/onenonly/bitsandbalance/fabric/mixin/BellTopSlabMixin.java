package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BellBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BellAttachType;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BellBlock.class)
public abstract class BellTopSlabMixin {

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$allowCeilingBellsUnderEnhancedSlabs(BlockState state, LevelReader level,
                                                                    BlockPos pos,
                                                                    CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        if (!FabricTweaksConfig.enableEnhancedSlabs || !FabricTweaksConfig.enhancedSlabsHangBelow) return;
        if (state.getValue(BellBlock.ATTACHMENT) != BellAttachType.CEILING) return;

        BlockPos supportPos = pos.above();
        BlockState supportState = level.getBlockState(supportPos);
        if (EnhancedSlabHelper.hasFullBottomSurface(level, supportPos, supportState)) {
            cir.setReturnValue(true);
        }
    }
}