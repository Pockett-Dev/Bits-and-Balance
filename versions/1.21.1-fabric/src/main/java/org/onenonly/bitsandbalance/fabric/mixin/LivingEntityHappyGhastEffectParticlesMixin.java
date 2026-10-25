package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.onenonly.bitsandbalance.common.entity.SpeedyHappyGhastSupport;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHappyGhastEffectParticlesMixin {

    @Redirect(
            method = "tickEffects",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"
            ),
            require = 0
    )
    private void bitsandbalance$moveHappyGhastEffectParticles(Level level, ParticleOptions particle, double x, double y, double z, double velocityX, double velocityY, double velocityZ) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (FabricTweaksConfig.enableSpeedyHappyGhasts && SpeedyHappyGhastSupport.shouldUseVisibleEffectParticles(self)) {
            SpeedyHappyGhastSupport.spawnVisibleEffectParticles(level, particle, self, velocityX, velocityY, velocityZ);
            return;
        }

        level.addParticle(particle, x, y, z, velocityX, velocityY, velocityZ);
    }
}