package org.onenonly.bitsandbalance.common.potions;

/**
 * Shared loader-agnostic snapshot of potion feature toggles.
 *
 * Loader-specific config bootstraps should mirror their live potion settings into
 * this class before shared brewing code is used.
 */
public final class PotionFeatureFlags {
    private static volatile boolean customBrewingEnabled = true;
    private static volatile boolean witheringPotionEnabled = true;
    private static volatile boolean resurfacingPotionEnabled = true;
    private static volatile boolean displacementPotionEnabled = true;
    private static volatile boolean returningPotionEnabled = true;
    private static volatile boolean levitationPotionEnabled = true;
    private static volatile boolean hastePotionEnabled = true;
    private static volatile boolean blindnessPotionEnabled = true;
    private static volatile boolean glowingPotionEnabled = true;
    private static volatile boolean bioluminescencePotionEnabled = true;

    private PotionFeatureFlags() {
    }

    public static void syncRuntimeFlags(
        boolean customBrewingEnabled,
        boolean witheringPotionEnabled,
        boolean resurfacingPotionEnabled,
        boolean displacementPotionEnabled,
        boolean returningPotionEnabled,
        boolean levitationPotionEnabled,
        boolean hastePotionEnabled,
        boolean blindnessPotionEnabled,
        boolean glowingPotionEnabled,
        boolean bioluminescencePotionEnabled
    ) {
        PotionFeatureFlags.customBrewingEnabled = customBrewingEnabled;
        PotionFeatureFlags.witheringPotionEnabled = witheringPotionEnabled;
        PotionFeatureFlags.resurfacingPotionEnabled = resurfacingPotionEnabled;
        PotionFeatureFlags.displacementPotionEnabled = displacementPotionEnabled;
        PotionFeatureFlags.returningPotionEnabled = returningPotionEnabled;
        PotionFeatureFlags.levitationPotionEnabled = levitationPotionEnabled;
        PotionFeatureFlags.hastePotionEnabled = hastePotionEnabled;
        PotionFeatureFlags.blindnessPotionEnabled = blindnessPotionEnabled;
        PotionFeatureFlags.glowingPotionEnabled = glowingPotionEnabled;
        PotionFeatureFlags.bioluminescencePotionEnabled = bioluminescencePotionEnabled;
    }

    public static void setPotionBrewingFlags(
        boolean witheringPotionEnabled,
        boolean displacementPotionEnabled,
        boolean returningPotionEnabled,
        boolean levitationPotionEnabled,
        boolean hastePotionEnabled,
        boolean blindnessPotionEnabled,
        boolean glowingPotionEnabled,
        boolean bioluminescencePotionEnabled
    ) {
        syncRuntimeFlags(
            witheringPotionEnabled
                || displacementPotionEnabled
                || returningPotionEnabled
                || levitationPotionEnabled
                || hastePotionEnabled
                || blindnessPotionEnabled
                || glowingPotionEnabled
                || bioluminescencePotionEnabled,
            witheringPotionEnabled,
            PotionFeatureFlags.resurfacingPotionEnabled,
            displacementPotionEnabled,
            returningPotionEnabled,
            levitationPotionEnabled,
            hastePotionEnabled,
            blindnessPotionEnabled,
            glowingPotionEnabled,
            bioluminescencePotionEnabled
        );
    }

    public static boolean isCustomBrewingEnabled() {
        return customBrewingEnabled;
    }

    public static boolean isWitheringPotionEnabled() {
        return witheringPotionEnabled;
    }

    public static boolean isResurfacingPotionEnabled() {
        return resurfacingPotionEnabled;
    }

    public static void setResurfacingPotionEnabled(boolean enabled) {
        resurfacingPotionEnabled = enabled;
    }

    public static boolean isDisplacementPotionEnabled() {
        return displacementPotionEnabled;
    }

    public static boolean isReturningPotionEnabled() {
        return returningPotionEnabled;
    }

    public static boolean isLevitationPotionEnabled() {
        return levitationPotionEnabled;
    }

    public static boolean isHastePotionEnabled() {
        return hastePotionEnabled;
    }

    public static boolean isBlindnessPotionEnabled() {
        return blindnessPotionEnabled;
    }

    public static void setBlindnessPotionEnabled(boolean enabled) {
        blindnessPotionEnabled = enabled;
    }

    public static boolean isGlowingPotionEnabled() {
        return glowingPotionEnabled;
    }

    public static boolean isBioluminescencePotionEnabled() {
        return bioluminescencePotionEnabled;
    }
}
