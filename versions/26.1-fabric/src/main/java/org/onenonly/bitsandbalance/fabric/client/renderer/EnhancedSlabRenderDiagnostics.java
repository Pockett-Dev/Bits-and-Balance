package org.onenonly.bitsandbalance.fabric.client.renderer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public final class EnhancedSlabRenderDiagnostics {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-enhanced-slab-render");
    private static final boolean ENABLED = Boolean.getBoolean("bitsandbalance.enhancedSlabRenderDiagnostics");
    private static final long LOG_INTERVAL_NANOS = 5_000_000_000L;

    private static final AtomicLong FIXED_SECTION = new AtomicLong();
    private static final AtomicLong MIXED_SECTION = new AtomicLong();
    private static final AtomicLong VERTICAL_SLAB_SECTION = new AtomicLong();
    private static final AtomicLong STEP_SECTION = new AtomicLong();
    private static final AtomicLong VERTICAL_STEP_SECTION = new AtomicLong();
    private static final AtomicLong QUAD_STEP_SECTION = new AtomicLong();
    private static final AtomicLong QUAD_VERTICAL_STEP_SECTION = new AtomicLong();
    private static final AtomicLong FIXED_FALLBACK_SKIPPED = new AtomicLong();
    private static final AtomicLong DYNAMIC_FALLBACK_SKIPPED = new AtomicLong();
    private static final AtomicLong MIXED_FALLBACK = new AtomicLong();
    private static final AtomicLong VERTICAL_SLAB_FALLBACK = new AtomicLong();
    private static final AtomicLong STEP_FALLBACK = new AtomicLong();
    private static final AtomicLong VERTICAL_STEP_FALLBACK = new AtomicLong();
    private static final AtomicLong PISTON_CUSTOM = new AtomicLong();
    private static final AtomicLong PISTON_SKIPPED_DISTANCE = new AtomicLong();
    private static final AtomicLong LAST_LOG_NANOS = new AtomicLong();

    private EnhancedSlabRenderDiagnostics() {
    }

    public static void fixedSection() { increment(FIXED_SECTION); }
    public static void mixedSection() { increment(MIXED_SECTION); }
    public static void verticalSlabSection() { increment(VERTICAL_SLAB_SECTION); }
    public static void stepSection() { increment(STEP_SECTION); }
    public static void verticalStepSection() { increment(VERTICAL_STEP_SECTION); }
    public static void quadStepSection() { increment(QUAD_STEP_SECTION); }
    public static void quadVerticalStepSection() { increment(QUAD_VERTICAL_STEP_SECTION); }
    public static void fixedFallbackSkipped() { increment(FIXED_FALLBACK_SKIPPED); }
    public static void dynamicFallbackSkipped() { increment(DYNAMIC_FALLBACK_SKIPPED); }
    public static void mixedFallback() { increment(MIXED_FALLBACK); }
    public static void verticalSlabFallback() { increment(VERTICAL_SLAB_FALLBACK); }
    public static void stepFallback() { increment(STEP_FALLBACK); }
    public static void verticalStepFallback() { increment(VERTICAL_STEP_FALLBACK); }
    public static void pistonCustom() { increment(PISTON_CUSTOM); }
    public static void pistonSkippedDistance() { increment(PISTON_SKIPPED_DISTANCE); }

    private static void increment(AtomicLong counter) {
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
                "Enhanced slab render counters: section fixed={} mixed={} verticalSlab={} step={} verticalStep={} quadStep={} quadVerticalStep={} skipped fixed={} dynamic={} fallback mixed={} verticalSlab={} step={} verticalStep={} piston={} pistonDistanceSkipped={}",
                FIXED_SECTION.get(), MIXED_SECTION.get(), VERTICAL_SLAB_SECTION.get(), STEP_SECTION.get(),
                VERTICAL_STEP_SECTION.get(), QUAD_STEP_SECTION.get(), QUAD_VERTICAL_STEP_SECTION.get(),
                FIXED_FALLBACK_SKIPPED.get(), DYNAMIC_FALLBACK_SKIPPED.get(), MIXED_FALLBACK.get(),
                VERTICAL_SLAB_FALLBACK.get(), STEP_FALLBACK.get(), VERTICAL_STEP_FALLBACK.get(),
                PISTON_CUSTOM.get(), PISTON_SKIPPED_DISTANCE.get()
        );
    }
}