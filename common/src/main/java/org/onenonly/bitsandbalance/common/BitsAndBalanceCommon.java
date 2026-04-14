package org.onenonly.bitsandbalance.common;

import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.config.ColorSystemMode;

import java.util.Objects;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Predicate;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

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

    private static volatile ForkJoinPool parallelWorldgenPool;

    private static volatile int parallelWorldgenPoolSize = -1;

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
        parallelWorldgenThreadCount = clampParallelWorldgenThreadCount(threadCount);
    }

    public static int getParallelWorldgenThreadCount() {
        return parallelWorldgenThreadCount;
    }

    public static int getEffectiveParallelWorldgenThreadCount() {
        int configuredThreadCount = parallelWorldgenThreadCount;
        if (configuredThreadCount == 0) {
            return Math.max(1, Math.min(64, Runtime.getRuntime().availableProcessors() / 2));
        }
        return Math.max(1, Math.min(64, configuredThreadCount));
    }

    public static void runParallelWorldgenRange(int endExclusive, IntConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        if (endExclusive <= 0) {
            return;
        }

        int threadCount = getEffectiveParallelWorldgenThreadCount();
        if (!parallelWorldgenProcessingEnabled || threadCount <= 1 || endExclusive == 1) {
            for (int index = 0; index < endExclusive; index++) {
                consumer.accept(index);
            }
            return;
        }

        getOrCreateParallelWorldgenPool(threadCount)
                .submit(() -> IntStream.range(0, endExclusive).parallel().forEach(consumer))
                .join();
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

    private static int clampParallelWorldgenThreadCount(int threadCount) {
        return Math.max(0, Math.min(64, threadCount));
    }

    private static ForkJoinPool getOrCreateParallelWorldgenPool(int threadCount) {
        ForkJoinPool pool = parallelWorldgenPool;
        if (pool != null && parallelWorldgenPoolSize == threadCount) {
            return pool;
        }

        synchronized (BitsAndBalanceCommon.class) {
            pool = parallelWorldgenPool;
            if (pool != null && parallelWorldgenPoolSize == threadCount) {
                return pool;
            }

            ForkJoinPool replacement = new ForkJoinPool(threadCount);
            ForkJoinPool oldPool = parallelWorldgenPool;
            parallelWorldgenPool = replacement;
            parallelWorldgenPoolSize = threadCount;
            if (oldPool != null) {
                oldPool.shutdown();
            }
            return replacement;
        }
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
