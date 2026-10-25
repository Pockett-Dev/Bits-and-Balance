package org.onenonly.bitsandbalance.fabric.mixin;

import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.BlockModelLighter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceLightManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = AoCalculator.class, remap = false)
public abstract class IndigoAoCalculatorBioluminescenceMixin {
    @Redirect(
            method = "computeFace(Lnet/fabricmc/fabric/impl/client/indigo/renderer/aocalc/AoFaceData;Lnet/minecraft/core/Direction;ZZ)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/block/BlockModelLighter$Cache;getLightCoords(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/block/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"
            ),
            require = 0,
            remap = false
    )
    private int bitsandbalance$applyBioluminescenceDynamicLight(
            BlockModelLighter.Cache cache,
            BlockState state,
            BlockAndTintGetter level,
            BlockPos pos
    ) {
        return BioluminescenceLightManager.getLightmapWithDynamicLight(level, pos, cache.getLightCoords(state, level, pos));
    }
}
