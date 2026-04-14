package org.onenonly.bitsandbalance.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.client.SoulFireOverlayClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AbstractCandleBlock.class)
public abstract class AbstractCandleBlockSoulFireFlameMixin {

    private static final float BITSANDBALANCE_SMALL_FLAME_SCALE = 0.5F;

    @Redirect(
            method = "addParticlesAndSound",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V",
                    ordinal = 1
            ),
            require = 0
    )
    private static void bitsandbalance$swapSoulFireCandleFlame(Level level, ParticleOptions particle, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        if (Config.soulFireCandleFlamesEnabled && SoulFireOverlayClient.shouldUseSoulFireCandleFlames(level, new Vec3(x, y, z))) {
            if (bitsandbalance$spawnScaledSoulFireCandleFlame(level, x, y, z, velocityX, velocityY, velocityZ)) {
                return;
            }

            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, velocityX, velocityY, velocityZ);
            return;
        }

        level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
    }

    private static boolean bitsandbalance$spawnScaledSoulFireCandleFlame(Level level, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        if (!(level instanceof ClientLevel clientLevel)) {
            return false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.particleEngine == null) {
            return false;
        }

        Particle particle = minecraft.particleEngine.createParticle(
                ParticleTypes.SOUL_FIRE_FLAME,
                x,
                y,
                z,
                velocityX,
                velocityY,
                velocityZ
        );
        if (particle == null) {
            return false;
        }

        particle.scale(BITSANDBALANCE_SMALL_FLAME_SCALE);
        minecraft.particleEngine.add(particle);
        return true;
    }
}