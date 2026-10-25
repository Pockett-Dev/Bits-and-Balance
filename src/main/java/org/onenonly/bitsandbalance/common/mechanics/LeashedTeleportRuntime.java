package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.entity.EntityType;

import java.util.Set;

/**
 * Loader-agnostic runtime settings for leashed teleport behavior.
 */
public final class LeashedTeleportRuntime {
    public static volatile boolean enabled = true;
    public static volatile int maxFollowers = 12;
    public static volatile double scanRadius = 48.0D;
    public static volatile int safePlacementTries = 16;
    public static volatile double baseRadius = 2.5D;
    public static volatile double maxRadius = 5.0D;
    public static volatile boolean postTeleportLeash = true;

    private static volatile Set<EntityType<?>> blacklistTypes = Set.of();

    private LeashedTeleportRuntime() {
    }

    public static void applyConfig(
        boolean enabledValue,
        int maxFollowersValue,
        double scanRadiusValue,
        int safePlacementTriesValue,
        double baseRadiusValue,
        double maxRadiusValue,
        boolean postTeleportLeashValue,
        Set<EntityType<?>> blacklistTypesValue
    ) {
        enabled = enabledValue;
        maxFollowers = Math.max(1, maxFollowersValue);
        scanRadius = Math.max(0.0D, scanRadiusValue);
        safePlacementTries = Math.max(1, safePlacementTriesValue);
        baseRadius = Math.max(0.0D, baseRadiusValue);
        maxRadius = Math.max(baseRadius, maxRadiusValue);
        postTeleportLeash = postTeleportLeashValue;
        blacklistTypes = blacklistTypesValue == null ? Set.of() : Set.copyOf(blacklistTypesValue);
    }

    public static boolean isBlacklisted(EntityType<?> entityType) {
        return blacklistTypes.contains(entityType);
    }
}