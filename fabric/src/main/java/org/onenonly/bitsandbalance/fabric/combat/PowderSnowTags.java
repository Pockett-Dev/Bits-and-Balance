package org.onenonly.bitsandbalance.fabric.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;

public final class PowderSnowTags {
    private PowderSnowTags() {
    }

    private static final String UNTIL_PREFIX = "bitsandbalance_powdersnow_mark_until:";
    private static final String NEXT_DMG_PREFIX = "bitsandbalance_powdersnow_next_dmg:";

    public static long getUntil(LivingEntity entity) {
        return getLong(entity, UNTIL_PREFIX);
    }

    public static void setUntil(LivingEntity entity, long until) {
        setLong(entity, UNTIL_PREFIX, until);
    }

    public static long getNextDamage(LivingEntity entity) {
        return getLong(entity, NEXT_DMG_PREFIX);
    }

    public static void setNextDamage(LivingEntity entity, long nextTick) {
        setLong(entity, NEXT_DMG_PREFIX, nextTick);
    }

    private static long getLong(LivingEntity entity, String prefix) {
        for (String tag : entity.getTags()) {
            if (tag != null && tag.startsWith(prefix)) {
                try {
                    return Long.parseLong(tag.substring(prefix.length()));
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    private static void setLong(LivingEntity entity, String prefix, long value) {
        clear(entity, prefix);
        if (value > 0L) {
            entity.addTag(prefix + value);
        }
    }

    private static void clear(LivingEntity entity, String prefix) {
        var toRemove = new ArrayList<String>();
        for (String tag : entity.getTags()) {
            if (tag != null && tag.startsWith(prefix)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            entity.removeTag(tag);
        }
    }

    public static void extendUntil(LivingEntity entity, long proposedUntil) {
        long existing = getUntil(entity);
        setUntil(entity, Math.max(existing, proposedUntil));
    }
}
