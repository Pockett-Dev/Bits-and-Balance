package org.onenonly.bitsandbalance.fabric.tweaks;

import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;
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
        Set<String> tags = entity.getTags();
        for (String tag : tags) {
            if (tag.startsWith(UUID_PREFIX)) {
                String uuid = tag.substring(UUID_PREFIX.length());
                return uuid.isEmpty() ? null : uuid;
            }
        }
        return null;
    }

    public static long getExpiresAt(Entity entity) {
        Set<String> tags = entity.getTags();
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
        for (String tag : entity.getTags()) {
            if (tag.startsWith(UUID_PREFIX) || tag.startsWith(EXP_PREFIX)) {
                toRemove.add(tag);
            }
        }
        for (String tag : toRemove) {
            entity.removeTag(tag);
        }
    }
}
