package org.onenonly.bitsandbalance.common.client;

/**
 * Thread-local context used to offset client particle spawn coordinates while running
 * a block's {@code animateTick(...)}.
 */
public final class EnhancedSlabParticleOffsetContext {
    private static final ThreadLocal<Double> Y_OFFSET_TL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ARMED_TL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private EnhancedSlabParticleOffsetContext() {
    }

    public static boolean isArmed() {
        return Boolean.TRUE.equals(ARMED_TL.get());
    }

    public static double getYOffset() {
        Double off = Y_OFFSET_TL.get();
        return off != null ? off : 0.0;
    }

    public static Double getRawOrNull() {
        return Y_OFFSET_TL.get();
    }

    public static void setYOffset(double yOffset) {
        if (yOffset == 0.0) {
            clear();
            return;
        }
        Y_OFFSET_TL.set(yOffset);
        ARMED_TL.set(Boolean.TRUE);
    }

    public static void restore(Double prevOffset, boolean prevArmed) {
        if (!prevArmed) {
            clear();
            return;
        }
        Y_OFFSET_TL.set(prevOffset);
        ARMED_TL.set(Boolean.TRUE);
    }

    public static void clear() {
        Y_OFFSET_TL.remove();
        ARMED_TL.remove();
    }
}
