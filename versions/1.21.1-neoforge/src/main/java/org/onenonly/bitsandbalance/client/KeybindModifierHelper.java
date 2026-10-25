package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

/**
 * Utility class for checking keybind presses with configurable modifier requirements.
 * 
 * This allows keybinds to optionally require Shift, Alt, and/or Ctrl modifiers,
 * making the system flexible and extensible for all mod keybinds.
 * 
 * Example usage:
 * <pre>
 * if (KeybindModifierHelper.matchesWithModifiers(
 *     MyModKeyBindings.ITEM_SHARE, 
 *     keyCode, 
 *     scanCode, 
 *     modifiers,
 *     ClientConfig.itemShareRequireShift,
 *     ClientConfig.itemShareRequireAlt,
 *     ClientConfig.itemShareRequireCtrl
 * )) {
 *     // Handle the keybind press
 * }
 * </pre>
 */
public class KeybindModifierHelper {
    
    /**
     * Checks if a keybind matches AND all required modifiers are pressed.
     * 
     * @param keyMapping The keybind to check
     * @param keyCode The key code from the input event
     * @param scanCode The scan code from the input event
     * @param modifiers The modifier bitfield from the input event (GLFW modifiers)
     * @param requireShift Whether Shift must be held
     * @param requireAlt Whether Alt must be held
     * @param requireCtrl Whether Ctrl must be held
     * @return true if the keybind matches AND all required modifiers are pressed
     */
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

        boolean baseMatch = keyMapping.matches(keyCode, scanCode);

        if (!baseMatch) {
            return false;
        }
        
        // Then check if all required modifiers are pressed
        return checkModifiers(modifiers, requireShift, requireAlt, requireCtrl);
    }

    /**
     * Checks if the required modifiers are currently pressed.
     * 
     * @param modifiers The modifier bitfield from the input event (GLFW modifiers)
     * @param requireShift Whether Shift must be held
     * @param requireAlt Whether Alt must be held
     * @param requireCtrl Whether Ctrl must be held
     * @return true if all required modifiers are pressed
     */
    public static boolean checkModifiers(
            int modifiers,
            boolean requireShift,
            boolean requireAlt,
            boolean requireCtrl
    ) {
        boolean shiftPressed;
        boolean altPressed;
        boolean ctrlPressed;

        try {
            Minecraft mc = Minecraft.getInstance();
            long window = mc.getWindow().getWindow();
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
        
        // Check each required modifier
        if (requireShift && !shiftPressed) {
            return false;
        }
        if (requireAlt && !altPressed) {
            return false;
        }
        if (requireCtrl && !ctrlPressed) {
            return false;
        }
        
        return true;
    }
    
    public static int effectiveModifierMask(
            KeyMapping keyMapping,
            int additionalMask,
            boolean requireShift,
            boolean requireAlt,
            boolean requireCtrl
    ) {
        int mask = additionalMask & (GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL);
        if (keyMapping != null) {
            mask |= modifierToMask(keyMapping.getKeyModifier());
        }
        if (requireShift) mask |= GLFW.GLFW_MOD_SHIFT;
        if (requireAlt) mask |= GLFW.GLFW_MOD_ALT;
        if (requireCtrl) mask |= GLFW.GLFW_MOD_CONTROL;
        return mask;
    }

    public static Component formatWithModifiers(Component baseKeyName, int requiredMask) {
        MutableComponent out = Component.empty();

        if ((requiredMask & GLFW.GLFW_MOD_CONTROL) != 0) {
            out.append(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL).getDisplayName());
            out.append(Component.literal(" + "));
        }
        if ((requiredMask & GLFW.GLFW_MOD_ALT) != 0) {
            out.append(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT).getDisplayName());
            out.append(Component.literal(" + "));
        }
        if ((requiredMask & GLFW.GLFW_MOD_SHIFT) != 0) {
            out.append(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT).getDisplayName());
            out.append(Component.literal(" + "));
        }

        out.append(baseKeyName);
        return out;
    }

    private static int modifierToMask(KeyModifier modifier) {
        if (modifier == KeyModifier.SHIFT) return GLFW.GLFW_MOD_SHIFT;
        if (modifier == KeyModifier.ALT) return GLFW.GLFW_MOD_ALT;
        if (modifier == KeyModifier.CONTROL) return GLFW.GLFW_MOD_CONTROL;
        return 0;
    }

    /**
     * Returns a human-readable string describing the required modifiers.
     * Useful for tooltips or help text.
     * 
     * @param requireShift Whether Shift is required
     * @param requireAlt Whether Alt is required
     * @param requireCtrl Whether Ctrl is required
     * @return A string like "Shift + Alt" or "None" if no modifiers required
     */
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

