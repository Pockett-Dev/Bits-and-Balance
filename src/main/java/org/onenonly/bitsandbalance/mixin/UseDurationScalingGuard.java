package org.onenonly.bitsandbalance.mixin;

/**
 * Thread-local guard to ensure food use duration scaling is applied only once
 * per getUseDuration call chain (Item -> ItemStack).
 *
 * Item.getUseDuration mixin sets the flag when it scales the value.
 * ItemStack.getUseDuration mixin checks and consumes the flag to avoid
 * double-scaling, which could cause visible pauses/desyncs.
 */
public final class UseDurationScalingGuard {
    private UseDurationScalingGuard() {}

    private static final ThreadLocal<Boolean> SCALED_FLAG = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /** Mark that scaling has been applied in the current thread call chain. */
    public static void markScaled() {
        SCALED_FLAG.set(Boolean.TRUE);
    }

    /**
     * Returns true if scaling was already applied and clears the flag.
     * This should be called by the outer-level hook (ItemStack mixin).
     */
    public static boolean consumeIfMarked() {
        Boolean b = SCALED_FLAG.get();
        if (b != null && b) {
            SCALED_FLAG.set(Boolean.FALSE);
            return true;
        }
        return false;
    }
}
