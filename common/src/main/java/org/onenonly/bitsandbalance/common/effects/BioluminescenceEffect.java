package org.onenonly.bitsandbalance.common.effects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.content.ModGlowGoo;
import org.onenonly.bitsandbalance.common.mechanics.BioluminescenceFadeHelper;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;

public final class BioluminescenceEffect extends MobEffect {
    private static final int PARTICLE_INTERVAL_TICKS = 4;
    private static final int PARTICLES_PER_INTERVAL = 2;

    public BioluminescenceEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x095656);
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!GlowGooRuntime.enabled || !GlowGooRuntime.bioluminescenceEnabled || GlowGooRuntime.bioluminescenceLightLevel <= 0) {
            return false;
        }

        bitsandbalance$spawnEffectParticles(level, entity);
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    private static int bitsandbalance$getRemainingDuration(LivingEntity entity) {
        MobEffectInstance bioluminescence = bitsandbalance$getBioluminescenceInstance(entity);
        return bioluminescence != null ? bioluminescence.getDuration() : 0;
    }

    private static MobEffectInstance bitsandbalance$getBioluminescenceInstance(LivingEntity entity) {
        return entity.getEffect(ModGlowGoo.bioluminescence());
    }

    private static void bitsandbalance$spawnEffectParticles(ServerLevel level, LivingEntity entity) {
        if ((entity.tickCount & (PARTICLE_INTERVAL_TICKS - 1)) != 0) {
            return;
        }

        int remainingDurationTicks = bitsandbalance$getRemainingDuration(entity);
        double fadeStrength = BioluminescenceFadeHelper.getFadeStrength(remainingDurationTicks);
        if (fadeStrength <= 0.0D) {
            return;
        }

        double radius = Math.max(0.38D, entity.getBbWidth() * 0.72D);
        double minY = entity.getY(0.1D);
        double height = Math.max(0.4D, entity.getBbHeight() * 0.85D);
        RandomSource random = entity.getRandom();
        double desiredParticleCount = PARTICLES_PER_INTERVAL * fadeStrength;
        int particleCount = (int) Math.floor(desiredParticleCount);
        if (random.nextDouble() < desiredParticleCount - particleCount) {
            particleCount++;
        }
        if (particleCount <= 0) {
            return;
        }

        for (int index = 0; index < particleCount; index++) {
            double angle = random.nextDouble() * (Math.PI * 2.0D);
            double distance = radius * (0.8D + random.nextDouble() * 0.2D);
            double x = entity.getX() + Math.cos(angle) * distance;
            double y = minY + random.nextDouble() * height;
            double z = entity.getZ() + Math.sin(angle) * distance;
            level.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}