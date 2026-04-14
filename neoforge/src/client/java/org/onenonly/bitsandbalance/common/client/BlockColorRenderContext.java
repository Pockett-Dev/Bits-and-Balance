package org.onenonly.bitsandbalance.common.client;

public final class BlockColorRenderContext {
    private static final ThreadLocal<Long> FORCED_POS_LONG = new ThreadLocal<>();

    private BlockColorRenderContext() {
    }

    public static Long forcedPosLongOrNull() {
        return FORCED_POS_LONG.get();
    }

    public static void setForcedPosLong(long posLong) {
        FORCED_POS_LONG.set(posLong);
    }

    public static void clear() {
        FORCED_POS_LONG.remove();
    }
}