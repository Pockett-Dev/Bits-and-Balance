package org.onenonly.bitsandbalance.fabric.client;

/**
 * Thread-local modifier context for the current input event.
 *
 * Vanilla {@code KeyMapping.click/set} don't receive modifiers, so we capture the current
 * {@code KeyEvent.modifiers()} in {@code KeyboardHandler.keyPress} and use it to filter
 * which keybindings get updated.
 */
public final class KeybindModifierContext {
    private KeybindModifierContext() {
    }

    private static final ThreadLocal<Integer> CURRENT_MODIFIERS = ThreadLocal.withInitial(() -> 0);

    public static void set(int modifiers) {
        CURRENT_MODIFIERS.set(modifiers);
    }

    public static int get() {
        return CURRENT_MODIFIERS.get();
    }

    public static void clear() {
        CURRENT_MODIFIERS.remove();
    }
}
