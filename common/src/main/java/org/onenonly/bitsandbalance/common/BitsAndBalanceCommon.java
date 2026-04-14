package org.onenonly.bitsandbalance.common;

import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.config.ColorSystemMode;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * Shared, loader-agnostic entrypoint.
 *
 * This module is intentionally kept free of direct Minecraft/loader classes for now;
 * we will expand it as the multiloader port progresses.
 */
public final class BitsAndBalanceCommon {
    public static final String MOD_ID = "bitsandbalance";

    private static volatile Predicate<Identifier> worldgenEnabled = key -> true;

    private static volatile boolean oreVariantsEnabled = true;

    private static volatile boolean parallelWorldgenProcessingEnabled = false;

    private static volatile int parallelWorldgenThreadCount = 0;

    private static volatile boolean veinsDebugEnabled = false;

    private static volatile boolean disableNetherLavaSprings = false;

    private static volatile boolean keepExposedNetherLavaSprings = true;

    // Worldgen > Veins
    // If false, veins that would place bitsandbalance:raw_quartz_block will never place it.
    private static volatile boolean enableRawQuartzBlockInQuartzVeins = true;

    private BitsAndBalanceCommon() {
    }

    public static void init() {
        // Intentionally empty for now.
        // Fabric/NeoForge entrypoints should call this so shared state can be added incrementally.
    }

    public static void setWorldgenEnabledPredicate(Predicate<Identifier> predicate) {
        worldgenEnabled = Objects.requireNonNull(predicate, "predicate");
    }

    public static boolean isWorldgenEnabled(Identifier key) {
        return worldgenEnabled.test(key);
    }

    public static void setOreVariantsEnabled(boolean enabled) {
        oreVariantsEnabled = enabled;
    }

    public static boolean isOreVariantsEnabled() {
        return oreVariantsEnabled;
    }

    public static void setParallelWorldgenProcessingEnabled(boolean enabled) {
        parallelWorldgenProcessingEnabled = enabled;
    }

    public static boolean isParallelWorldgenProcessingEnabled() {
        return parallelWorldgenProcessingEnabled;
    }

    public static void setParallelWorldgenThreadCount(int threadCount) {
        parallelWorldgenThreadCount = Math.max(0, threadCount);
    }

    public static int getParallelWorldgenThreadCount() {
        return parallelWorldgenThreadCount;
    }

    public static void setVeinsDebugEnabled(boolean enabled) {
        veinsDebugEnabled = enabled;
    }

    public static boolean isVeinsDebugEnabled() {
        return veinsDebugEnabled;
    }

    public static void setDisableNetherLavaSprings(boolean disabled) {
        disableNetherLavaSprings = disabled;
    }

    public static boolean isDisableNetherLavaSprings() {
        return disableNetherLavaSprings;
    }

    public static void setKeepExposedNetherLavaSprings(boolean keepExposed) {
        keepExposedNetherLavaSprings = keepExposed;
    }

    public static boolean isKeepExposedNetherLavaSprings() {
        return keepExposedNetherLavaSprings;
    }

    public static void setEnableRawQuartzBlockInQuartzVeins(boolean enabled) {
        enableRawQuartzBlockInQuartzVeins = enabled;
    }

    public static boolean isEnableRawQuartzBlockInQuartzVeins() {
        return enableRawQuartzBlockInQuartzVeins;
    }

    // ── Dye System: Master Toggle ───────────────────────────────────────

    // ── Colour System Mode ───────────────────────────────────────────────

    public static void setColorSystemMode(ColorSystemMode mode) {
        // Block registration is always active.
    }

    public static ColorSystemMode getColorSystemMode() {
        return ColorSystemMode.BLOCK_REGISTRATION;
    }

    /** {@code true} when the active mode is {@link ColorSystemMode#BLOCK_REGISTRATION}. */
    public static boolean isBlockRegistrationMode() {
        return true;
    }

    /** {@code true} when the active mode is {@link ColorSystemMode#BLOCK_ENTITY}. */
    public static boolean isBlockEntityMode() {
        return false;
    }
}
