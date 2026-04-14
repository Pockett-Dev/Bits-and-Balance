package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractCandleBlock.class)
public abstract class AbstractCandleBlockSoulFlameMixin {
    private static final float BITSANDBALANCE$SOUL_CANDLE_PARTICLE_SCALE = 0.5F;

    @Redirect(
            method = "addParticlesAndSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    ordinal = 1
            )
    )
    private static void bitsandbalance$useSoulFireFlamesOnSoulBase(Level level,
                                                                    ParticleOptions particle,
                                                                    double x,
                                                                    double y,
                                                                    double z,
                                                                    double velocityX,
                                                                    double velocityY,
                                                                    double velocityZ) {
        ParticleOptions selectedParticle = particle;
        if (FabricClientConfig.soulFireCandleFlamesEnabled) {
            BlockPos candlePos = BlockPos.containing(x, y, z);
            if (level.getBlockState(candlePos.below()).is(BlockTags.SOUL_FIRE_BASE_BLOCKS)) {
                selectedParticle = ParticleTypes.SOUL_FIRE_FLAME;
            }
        }

        if (selectedParticle == ParticleTypes.SOUL_FIRE_FLAME && level instanceof ClientLevel) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.particleEngine != null) {
                var soulCandleParticle = minecraft.particleEngine.createParticle(selectedParticle, x, y, z, velocityX, velocityY, velocityZ);
                if (soulCandleParticle != null) {
                    soulCandleParticle.scale(BITSANDBALANCE$SOUL_CANDLE_PARTICLE_SCALE);
                    return;
                }
            }
        }

        level.addParticle(selectedParticle, x, y, z, velocityX, velocityY, velocityZ);
    }
}