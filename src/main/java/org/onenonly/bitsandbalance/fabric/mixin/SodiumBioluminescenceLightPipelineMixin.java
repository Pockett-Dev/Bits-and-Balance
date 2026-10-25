package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.onenonly.bitsandbalance.common.client.BioluminescenceSodiumLightPipelineHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(
        targets = {
                "net.caffeinemc.mods.sodium.client.model.light.smooth.SmoothLightPipeline",
                "net.caffeinemc.mods.sodium.client.model.light.flat.FlatLightPipeline"
        },
        remap = false
)
public abstract class SodiumBioluminescenceLightPipelineMixin {
    @Inject(method = "calculate", at = @At("RETURN"), require = 0, remap = false)
    private void bitsandbalance$applyBioluminescenceDynamicLight(
            @Coerce Object quad,
            BlockPos pos,
            @Coerce Object lightData,
            Direction cullFace,
            Direction lightFace,
            boolean shade,
            boolean enhanced,
            CallbackInfo ci
    ) {
        BioluminescenceSodiumLightPipelineHelper.apply(this, quad, pos, lightData, cullFace, lightFace);
    }
}
