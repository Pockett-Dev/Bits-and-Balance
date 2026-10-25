package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabParticleOffsetContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LeverBlock.class)
public abstract class LeverBlockEnhancedSlabParticleOffsetMixin {
    @Redirect(
            method = "makeParticle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/LevelAccessor;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            ),
            require = 0
    )
    private static void bitsandbalance$offsetEnhancedSlabLeverParticle(LevelAccessor level, ParticleOptions particle,
                                                                       double x, double y, double z,
                                                                       double velocityX, double velocityY, double velocityZ,
                                                                       BlockState state, LevelAccessor originalLevel,
                                                                       BlockPos pos, float alpha) {
        double yOff = 0.0;
        if (Config.enableEnhancedSlabs && level instanceof BlockGetter blockGetter) {
            try {
                yOff = EnhancedSlabHelper.getVisualYOffset(blockGetter, pos, state);
            } catch (Throwable ignored) {
                yOff = 0.0;
            }
        }

        if (yOff != 0.0) {
            y += yOff;
            if (EnhancedSlabParticleOffsetContext.isArmed()) {
                EnhancedSlabParticleOffsetContext.suppressNextParticleOffset();
            }
        }

        level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
    }
}