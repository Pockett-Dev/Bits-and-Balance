package org.onenonly.bitsandbalance.fabric.combat;

import net.minecraft.world.entity.LivingEntity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

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
        for (String tag : bitsandbalance$getTags(entity)) {
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
        for (String tag : bitsandbalance$getTags(entity)) {
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

    @SuppressWarnings("unchecked")
    private static Set<String> bitsandbalance$getTags(LivingEntity entity) {
        if (entity == null) {
            return Collections.emptySet();
        }

        try {
            Method entityTagsMethod = entity.getClass().getMethod("entityTags");
            Object tags = entityTagsMethod.invoke(entity);
            if (tags instanceof Set<?> typedSet) {
                return (Set<String>) typedSet;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Method getTagsMethod = entity.getClass().getMethod("getTags");
            Object tags = getTagsMethod.invoke(entity);
            if (tags instanceof Set<?> typedSet) {
                return (Set<String>) typedSet;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        return Collections.emptySet();
    }
}
