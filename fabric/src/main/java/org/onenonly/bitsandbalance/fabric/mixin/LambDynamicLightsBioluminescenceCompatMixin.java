package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.lambdaurora.lambdynlights.LambDynLights", remap = false)
public class LambDynamicLightsBioluminescenceCompatMixin {
    @Inject(
            method = "getDynamicLightLevel(Lnet/minecraft/core/BlockPos;)D",
            at = @At("RETURN"),
            cancellable = true,
            require = 0,
            remap = false
    )
    private void bitsandbalance$extendBioluminescenceRange(BlockPos pos, CallbackInfoReturnable<Double> cir) {
        double dynamicLightLevel = Math.max(cir.getReturnValue(), BioluminescenceLightManager.getDynamicLightLevel(pos));
        if (dynamicLightLevel != cir.getReturnValue()) {
            cir.setReturnValue(dynamicLightLevel);
        }
    }
}