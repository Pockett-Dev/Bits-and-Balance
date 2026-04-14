package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelRenderer.class)
public abstract class BioluminescenceBrightnessGetterMixin {
    @Inject(
        method = "getLightColor(Lnet/minecraft/client/renderer/LevelRenderer$BrightnessGetter;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)I",
            at = @At("RETURN"),
            cancellable = true,
            require = 0
    )
    private static void bitsandbalance$applyBioluminescenceDynamicLight(
            LevelRenderer.BrightnessGetter brightnessGetter,
            BlockAndTintGetter level,
            BlockState state,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(BioluminescenceLightManager.getLightmapWithDynamicLight(pos, cir.getReturnValue()));
    }
}