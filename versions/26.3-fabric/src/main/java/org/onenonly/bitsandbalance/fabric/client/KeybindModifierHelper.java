package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;

/**
 * Utility class for checking keybind presses with configurable modifier requirements.
 *
 * Mirrors the NeoForge-side helper so Fabric can share the same behavior.
 */
public final class KeybindModifierHelper {
    private KeybindModifierHelper() {
    }

    public static boolean matchesWithModifiers(
            KeyMapping keyMapping,
            int keyCode,
            int keycode,
            int modifiers,
            boolean requireShift,
            boolean requireAlt,
            boolean requireCtrl
    ) {
        // Match the base keybind.
        // - First try with the actual modifier bitfield so modifier-bound keybinds (e.g. Shift+T) can match.
        // - Then fall back to ignoring modifier bits so plain keybinds still match even if a modifier is held.
        if (keyMapping == null) {
            return false;
        }

        int effectiveModifiers = modifiers;
        if (effectiveModifiers == 0) {
            // Some paths (notably screen handling) can lose the modifier bitfield.
            // Derive it from real-time key state, similar to how Amecs preserves modifiers.
            effectiveModifiers = deriveModifierMaskFromKeyboardState();
        }

        boolean baseMatch;
        try {
            baseMatch = keyMapping.matches(new KeyEvent(keyCode, keycode, effectiveModifiers))
                    || keyMapping.matches(new KeyEvent(keyCode, keycode, 0));
        } catch (Throwable t) {
            baseMatch = keyMapping.matches(new KeyEvent(keyCode, keycode, 0));
        }

        if (!baseMatch) {
            return false;
        }
        return checkModifiers(modifiers, requireShift, requireAlt, requireCtrl);
    }

    private static int deriveModifierMaskFromKeyboardState() {
        try {
            boolean shiftDown = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                    || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
            boolean altDown = InputConstants.isKeyDown(InputConstants.KEY_LALT)
                    || InputConstants.isKeyDown(InputConstants.KEY_RALT);
            boolean ctrlDown = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                    || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);

            int mask = 0;
            if (shiftDown) mask |= InputConstants.MOD_SHIFT;
            if (altDown) mask |= InputConstants.MOD_ALT;
            if (ctrlDown) mask |= InputConstants.MOD_CONTROL;
            return mask;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    public static boolean checkModifiers(int modifiers, boolean requireShift, boolean requireAlt, boolean requireCtrl) {
        boolean shiftPressed;
        boolean altPressed;
        boolean ctrlPressed;

        // Prefer real-time keyboard state (more reliable than event modifier bitfields).
        try {
            shiftPressed = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT)
                    || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT);
            altPressed = InputConstants.isKeyDown(InputConstants.KEY_LALT)
                    || InputConstants.isKeyDown(InputConstants.KEY_RALT);
            ctrlPressed = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                    || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
        } catch (Throwable ignored) {
            shiftPressed = (modifiers & InputConstants.MOD_SHIFT) != 0;
            altPressed = (modifiers & InputConstants.MOD_ALT) != 0;
            ctrlPressed = (modifiers & InputConstants.MOD_CONTROL) != 0;
        }

        if (requireShift && !shiftPressed) return false;
        if (requireAlt && !altPressed) return false;
        if (requireCtrl && !ctrlPressed) return false;

        return true;
    }

    public static String getModifierString(boolean requireShift, boolean requireAlt, boolean requireCtrl) {
        if (!requireShift && !requireAlt && !requireCtrl) {
            return "None";
        }

        StringBuilder sb = new StringBuilder();
        if (requireCtrl) {
            sb.append("Ctrl");
        }
        if (requireAlt) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append("Alt");
        }
        if (requireShift) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append("Shift");
        }

        return sb.toString();
    }
}
