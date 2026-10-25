package org.onenonly.bitsandbalance.fabric.client.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public final class EnhancedSlabRenderDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-enhanced-slab-render");
    private static final boolean ENABLED = Boolean.getBoolean("bitsandbalance.enhancedSlabRenderDiagnostics");
    private static final long LOG_INTERVAL_NANOS = 5_000_000_000L;

    private static final AtomicLong MIXED_TERRAIN = new AtomicLong();
    private static final AtomicLong VERTICAL_SLAB_TERRAIN = new AtomicLong();
    private static final AtomicLong STEP_TERRAIN = new AtomicLong();
    private static final AtomicLong VERTICAL_STEP_TERRAIN = new AtomicLong();
    private static final AtomicLong QUAD_STEP_TERRAIN = new AtomicLong();
    private static final AtomicLong QUAD_VERTICAL_STEP_TERRAIN = new AtomicLong();
    private static final AtomicLong MIXED_FALLBACK = new AtomicLong();
    private static final AtomicLong VERTICAL_SLAB_FALLBACK = new AtomicLong();
    private static final AtomicLong STEP_FALLBACK = new AtomicLong();
    private static final AtomicLong VERTICAL_STEP_FALLBACK = new AtomicLong();
    private static final AtomicLong PISTON_CUSTOM = new AtomicLong();
    private static final AtomicLong PISTON_SKIPPED_DISTANCE = new AtomicLong();
    private static final AtomicLong LAST_LOG_NANOS = new AtomicLong();

    private EnhancedSlabRenderDiagnostics() {
    }

    public static void mixedTerrain() { increment(MIXED_TERRAIN); }
    public static void verticalSlabTerrain() { increment(VERTICAL_SLAB_TERRAIN); }
    public static void stepTerrain() { increment(STEP_TERRAIN); }
    public static void verticalStepTerrain() { increment(VERTICAL_STEP_TERRAIN); }
    public static void quadStepTerrain() { increment(QUAD_STEP_TERRAIN); }
    public static void quadVerticalStepTerrain() { increment(QUAD_VERTICAL_STEP_TERRAIN); }
    public static void mixedFallback() { increment(MIXED_FALLBACK); }
    public static void verticalSlabFallback() { increment(VERTICAL_SLAB_FALLBACK); }
    public static void stepFallback() { increment(STEP_FALLBACK); }
    public static void verticalStepFallback() { increment(VERTICAL_STEP_FALLBACK); }
    public static void pistonCustom() { increment(PISTON_CUSTOM); }
    public static void pistonSkippedDistance() { increment(PISTON_SKIPPED_DISTANCE); }

    private static void increment(AtomicLong counter) {
        if (!ENABLED) {
            return;
        }
        counter.incrementAndGet();
        maybeLog();
    }

    private static void maybeLog() {
        if (!ENABLED) {
            return;
        }

        long now = System.nanoTime();
        long previous = LAST_LOG_NANOS.get();
        if (now - previous < LOG_INTERVAL_NANOS || !LAST_LOG_NANOS.compareAndSet(previous, now)) {
            return;
        }

        LOGGER.info(
                "Enhanced slab render counters: terrain mixed={} verticalSlab={} step={} verticalStep={} quadStep={} quadVerticalStep={} fallback mixed={} verticalSlab={} step={} verticalStep={} piston={} pistonDistanceSkipped={}",
                MIXED_TERRAIN.get(), VERTICAL_SLAB_TERRAIN.get(), STEP_TERRAIN.get(), VERTICAL_STEP_TERRAIN.get(),
                QUAD_STEP_TERRAIN.get(), QUAD_VERTICAL_STEP_TERRAIN.get(), MIXED_FALLBACK.get(),
                VERTICAL_SLAB_FALLBACK.get(), STEP_FALLBACK.get(), VERTICAL_STEP_FALLBACK.get(),
                PISTON_CUSTOM.get(), PISTON_SKIPPED_DISTANCE.get()
        );
    }
}
