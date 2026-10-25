package org.onenonly.bitsandbalance.mixin.sable;

import org.onenonly.bitsandbalance.common.compat.SableCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.plot.ServerLevelPlot", remap = false)
public abstract class SableServerLevelPlotEnhancedSlabMaterialCleanupMixin {
    @Inject(method = "destroyAllBlocks", at = @At("HEAD"), require = 0)
    private void bitsandbalance$clearEnhancedSlabMaterials(CallbackInfo ci) {
        SableCompat.clearPlotEnhancedSlabMaterials(this);
    }
}
