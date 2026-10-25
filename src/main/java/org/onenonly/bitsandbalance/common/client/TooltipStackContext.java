package org.onenonly.bitsandbalance.common.client;

import net.minecraft.world.item.ItemStack;

/**
 * Thread-local context used to identify which {@link ItemStack} an inventory tooltip is being rendered for.
 *
 * <p>Needed because some mods append/modify tooltip lines late (at render-time), after the initial tooltip
 * lines are constructed.
 */
public final class TooltipStackContext {
    private static final ThreadLocal<ItemStack> STACK_TL = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> ARMED_TL = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private TooltipStackContext() {
    }

    public static void set(ItemStack stack) {
        STACK_TL.set(stack);
        ARMED_TL.set(Boolean.TRUE);
    }

    public static ItemStack get() {
        return STACK_TL.get();
    }

    public static boolean isArmed() {
        return Boolean.TRUE.equals(ARMED_TL.get());
    }

    public static void clear() {
        STACK_TL.remove();
        ARMED_TL.remove();
    }
}
