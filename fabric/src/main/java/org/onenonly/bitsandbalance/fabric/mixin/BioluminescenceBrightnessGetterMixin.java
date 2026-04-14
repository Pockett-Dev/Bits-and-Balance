package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LevelRenderer.BrightnessGetter.class, priority = 900)
public interface BioluminescenceBrightnessGetterMixin {
    @Inject(
            method = "lambda$static$0",
            at = @At("TAIL"),
            cancellable = true,
            remap = false,
            allow = 1,
            require = 0
    )
    private static void bitsandbalance$applyBioluminescenceDynamicLight(
            BlockAndTintGetter level,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(BioluminescenceLightManager.getLightmapWithDynamicLight(pos, cir.getReturnValue()));
    }
}