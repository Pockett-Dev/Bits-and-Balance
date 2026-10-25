package org.onenonly.bitsandbalance.fabric.mixin;

import org.onenonly.bitsandbalance.common.client.BioluminescenceLambDynamicLightsBridge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.lambdaurora.lambdynlights.LambDynLights", remap = false)
public class LambDynamicLightsBioluminescenceCompatMixin {
    @Inject(
            method = "initializeApi",
            at = @At("TAIL"),
            remap = false
    )
    private void bitsandbalance$connectBioluminescenceToLambDynamicLights(CallbackInfo ci) {
        BioluminescenceLambDynamicLightsBridge.initializeFromLambDynamicLights(this);
    }
}
