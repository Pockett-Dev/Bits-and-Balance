package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AoCalculator.class, remap = false)
public abstract class IndigoAoCalculatorBioluminescenceMixin {
    @Inject(method = "getLightmapCoordinates", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private static void bitsandbalance$applyBioluminescenceDynamicLight(
            BlockAndTintGetter world,
            BlockState state,
            BlockPos pos,
            CallbackInfoReturnable<Integer> cir
    ) {
        cir.setReturnValue(BioluminescenceLightManager.getLightmapWithDynamicLight(world, pos, cir.getReturnValue()));
    }
}
