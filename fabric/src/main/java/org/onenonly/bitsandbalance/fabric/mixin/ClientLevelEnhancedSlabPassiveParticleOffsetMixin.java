package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.client.EnhancedSlabParticleOffsetContext;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabHelper;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientLevel.class)
public abstract class ClientLevelEnhancedSlabPassiveParticleOffsetMixin {

    @Redirect(
            method = "doAnimateTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;animateTick(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/util/RandomSource;)V"
            ),
            require = 0
    )
    private void bitsandbalance$wrapAnimateTickForParticleYOffset(Block block, BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) {
            block.animateTick(state, level, pos, random);
            return;
        }

        Double prevOffset = EnhancedSlabParticleOffsetContext.getRawOrNull();
        boolean prevArmed = EnhancedSlabParticleOffsetContext.isArmed();
        try {
            double yOff;
            try {
                yOff = EnhancedSlabHelper.getVisualYOffset(level, pos, state);
            } catch (Throwable ignored) {
                yOff = 0.0;
            }
            EnhancedSlabParticleOffsetContext.setYOffset(yOff);
            block.animateTick(state, level, pos, random);
        } finally {
            EnhancedSlabParticleOffsetContext.restore(prevOffset, prevArmed);
        }
    }

    @ModifyArgs(
            method = "doAddParticle",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/particle/ParticleEngine;createParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)Lnet/minecraft/client/particle/Particle;"
            ),
            require = 0
    )
    private void bitsandbalance$offsetPassiveParticleY(Args args) {
        if (!FabricTweaksConfig.enableEnhancedSlabs) return;
        if (!EnhancedSlabParticleOffsetContext.isArmed()) return;

        double yOff = EnhancedSlabParticleOffsetContext.getYOffset();
        if (yOff == 0.0) return;

        double y = args.get(2);
        args.set(2, y + yOff);
    }
}
