package org.onenonly.bitsandbalance.common.potions;

import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/** Shared range check for goal AI, brain sensors, and direct retaliation. */
public final class BlindnessMobVision {
    public static final double RANGE = 3.0;

    private BlindnessMobVision() {
    }

    public static boolean isActive(LivingEntity observer) {
        return observer instanceof Mob
                && PotionFeatureFlags.isBlindnessPotionEnabled()
                && observer.hasEffect(MobEffects.BLINDNESS);
    }

    public static boolean isOutOfRange(LivingEntity observer, Entity target) {
        return observer != null && target != null && isActive(observer)
                && observer.distanceToSqr(target) > RANGE * RANGE;
    }
}
