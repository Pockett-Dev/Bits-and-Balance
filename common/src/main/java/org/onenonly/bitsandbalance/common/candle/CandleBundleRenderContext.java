package org.onenonly.bitsandbalance.common.candle;

/**
 * ThreadLocal context used by the candle bundle block entity renderer.
 *
 * Vanilla candle models use a fixed tint index (typically 0). When we render multiple
 * different candle blocks within one bundle block, we need BlockColors to be able to
 * select the per-slot color.
 */
public final class CandleBundleRenderContext {
    private static final ThreadLocal<Integer> FORCED_TINT_INDEX = ThreadLocal.withInitial(() -> -1);
    private static final ThreadLocal<Long> FORCED_POS_LONG = new ThreadLocal<>();

    private CandleBundleRenderContext() {
    }

    public static int forcedTintIndexOr(int fallback) {
        int v = FORCED_TINT_INDEX.get();
        return v >= 0 ? v : fallback;
    }

    public static void setForcedTintIndex(int tintIndex) {
        FORCED_TINT_INDEX.set(tintIndex);
    }

    /**
     * Some render paths (e.g., submit-based block model rendering) don't provide a BlockPos to BlockColors.
     * When set, BlockColors can treat this as the effective position for resolving per-bundle colors.
     */
    public static Long forcedPosLongOrNull() {
        return FORCED_POS_LONG.get();
    }

    public static void setForcedPosLong(long posLong) {
        FORCED_POS_LONG.set(posLong);
    }

    public static void clear() {
        FORCED_TINT_INDEX.set(-1);
        FORCED_POS_LONG.remove();
    }
}
