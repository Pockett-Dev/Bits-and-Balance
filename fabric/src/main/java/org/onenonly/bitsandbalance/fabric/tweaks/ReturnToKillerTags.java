package org.onenonly.bitsandbalance.fabric.tweaks;

import net.minecraft.world.entity.Entity;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Set;

/**
 * Vanilla-only storage for Return-To-Killer using persistent entity tags.
 */
public final class ReturnToKillerTags {
    private static final String UUID_PREFIX = "bitsandbalance:rtk_uuid:";
    private static final String EXP_PREFIX = "bitsandbalance:rtk_exp:";

    private ReturnToKillerTags() {
    }

    public static boolean hasKiller(Entity entity) {
        return getKillerUuidString(entity) != null;
    }

    public static String getKillerUuidString(Entity entity) {
        Set<String> tags = bitsandbalance$getTags(entity);
        for (String tag : tags) {
            if (tag.startsWith(UUID_PREFIX)) {
                String uuid = tag.substring(UUID_PREFIX.length());
                return uuid.isEmpty() ? null : uuid;
            }
        }
        return null;
    }

    public static long getExpiresAt(Entity entity) {
        Set<String> tags = bitsandbalance$getTags(entity);
        for (String tag : tags) {
            if (tag.startsWith(EXP_PREFIX)) {
                String raw = tag.substring(EXP_PREFIX.length());
                try {
                    return Long.parseLong(raw);
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }
        return 0L;
    }

    public static void set(Entity entity, String killerUuidString, long expiresAt) {
        if (killerUuidString == null || killerUuidString.isEmpty()) return;
        if (hasKiller(entity)) return;
        entity.addTag(UUID_PREFIX + killerUuidString);
        entity.addTag(EXP_PREFIX + expiresAt);
    }

    public static void clear(Entity entity) {
        List<String> toRemove = new ArrayList<>();
        for (String tag : bitsandbalance$getTags(entity)) {
            if (tag.startsWith(UUID_PREFIX) || tag.startsWith(EXP_PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            entity.removeTag(tag);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> bitsandbalance$getTags(Entity entity) {
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
