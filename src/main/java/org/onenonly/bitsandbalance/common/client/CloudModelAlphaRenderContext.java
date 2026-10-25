package org.onenonly.bitsandbalance.common.client;

public final class CloudModelAlphaRenderContext {
    private static final ThreadLocal<Integer> ALPHA = new ThreadLocal<>();

    private CloudModelAlphaRenderContext() {
    }

    public static void push(int alpha) {
        ALPHA.set(Math.max(0, Math.min(255, alpha)));
    }

    public static void pop() {
        ALPHA.remove();
    }

    public static int currentAlpha() {
        Integer alpha = ALPHA.get();
        return alpha != null ? alpha : 255;
    }

    public static boolean isActive() {
        return ALPHA.get() != null;
    }
}