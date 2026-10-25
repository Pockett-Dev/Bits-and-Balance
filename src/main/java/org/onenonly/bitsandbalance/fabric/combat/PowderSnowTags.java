package org.onenonly.bitsandbalance.fabric.combat;

import net.minecraft.world.entity.LivingEntity;
import org.onenonly.bitsandbalance.common.combat.PowderSnowMarks;

public final class PowderSnowTags {
    private PowderSnowTags() {
    }

    public static long getUntil(LivingEntity entity) {
        return PowderSnowMarks.getUntil(entity);
    }

    public static void setUntil(LivingEntity entity, long until) {
        PowderSnowMarks.setUntil(entity, until);
    }

    public static long getNextDamage(LivingEntity entity) {
        return PowderSnowMarks.getNextDamage(entity);
    }

    public static void setNextDamage(LivingEntity entity, long nextTick) {
        PowderSnowMarks.setNextDamage(entity, nextTick);
    }

    public static void clear(LivingEntity entity) {
        PowderSnowMarks.clear(entity);
    }

    public static void extendUntil(LivingEntity entity, long proposedUntil) {
        PowderSnowMarks.extendUntil(entity, proposedUntil);
    }
}
