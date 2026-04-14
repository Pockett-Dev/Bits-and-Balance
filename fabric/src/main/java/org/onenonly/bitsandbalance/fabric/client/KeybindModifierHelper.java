package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;

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
            int scanCode,
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
            baseMatch = keyMapping.matches(new KeyEvent(keyCode, scanCode, effectiveModifiers))
                    || keyMapping.matches(new KeyEvent(keyCode, scanCode, 0));
        } catch (Throwable t) {
            baseMatch = keyMapping.matches(new KeyEvent(keyCode, scanCode, 0));
        }

        if (!baseMatch) {
            return false;
        }
        return checkModifiers(modifiers, requireShift, requireAlt, requireCtrl);
    }

    public static boolean matchesWithRequiredMask(KeyMapping keyMapping, int keyCode, int scanCode, int modifiers, int requiredMask) {
        return matchesWithModifiers(
                keyMapping,
                keyCode,
                scanCode,
                modifiers,
                (requiredMask & GLFW.GLFW_MOD_SHIFT) != 0,
                (requiredMask & GLFW.GLFW_MOD_ALT) != 0,
                (requiredMask & GLFW.GLFW_MOD_CONTROL) != 0
        );
    }

    private static int deriveModifierMaskFromKeyboardState() {
        try {
            Minecraft mc = Minecraft.getInstance();
            var window = mc.getWindow();

            boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            boolean altDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
            boolean ctrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

            int mask = 0;
            if (shiftDown) mask |= GLFW.GLFW_MOD_SHIFT;
            if (altDown) mask |= GLFW.GLFW_MOD_ALT;
            if (ctrlDown) mask |= GLFW.GLFW_MOD_CONTROL;
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
            Minecraft mc = Minecraft.getInstance();
            var window = mc.getWindow();
            shiftPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
            altPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
            ctrlPressed = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                    || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        } catch (Throwable ignored) {
            shiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
            altPressed = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
            ctrlPressed = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
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
