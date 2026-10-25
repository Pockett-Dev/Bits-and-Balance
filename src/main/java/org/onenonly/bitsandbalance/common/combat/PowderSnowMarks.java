package org.onenonly.bitsandbalance.common.combat;

import net.minecraft.world.entity.LivingEntity;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class PowderSnowMarks {
    private static final Map<LivingEntity, Mark> MARKS = Collections.synchronizedMap(new WeakHashMap<>());

    private PowderSnowMarks() {
    }

    public static long getUntil(LivingEntity entity) {
        Mark mark = get(entity);
        return mark == null ? 0L : mark.until;
    }

    public static void setUntil(LivingEntity entity, long until) {
        if (entity == null) {
            return;
        }
        if (until <= 0L) {
            clear(entity);
            return;
        }
        getOrCreate(entity).until = until;
    }

    public static void extendUntil(LivingEntity entity, long proposedUntil) {
        long existing = getUntil(entity);
        setUntil(entity, Math.max(existing, proposedUntil));
    }

    public static long getNextDamage(LivingEntity entity) {
        Mark mark = get(entity);
        return mark == null ? 0L : mark.nextDamage;
    }

    public static void setNextDamage(LivingEntity entity, long nextTick) {
        if (entity == null) {
            return;
        }
        if (nextTick <= 0L) {
            Mark mark = get(entity);
            if (mark != null) {
                mark.nextDamage = 0L;
            }
            return;
        }
        getOrCreate(entity).nextDamage = nextTick;
    }

    public static void clear(LivingEntity entity) {
        MARKS.remove(entity);
    }

    private static Mark get(LivingEntity entity) {
        return entity == null ? null : MARKS.get(entity);
    }

    private static Mark getOrCreate(LivingEntity entity) {
        Mark mark = MARKS.get(entity);
        if (mark == null) {
            mark = new Mark();
            MARKS.put(entity, mark);
        }
        return mark;
    }

    private static final class Mark {
        private long until;
        private long nextDamage;
    }
}
