package org.onenonly.bitsandbalance.fabric.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

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
        if (keyMapping == null) {
            return false;
        }

        boolean baseMatch = keyMapping.matches(keyCode, scanCode);
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

    public static boolean checkModifiers(int modifiers, boolean requireShift, boolean requireAlt, boolean requireCtrl) {
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

    public static String getModifierString(boolean requireShift, boolean requireAlt, boolean requireCtrl) {
        if (!requireShift && !requireAlt && !requireCtrl) {
            return "None";
        }

        StringBuilder builder = new StringBuilder();
        if (requireCtrl) {
            builder.append("Ctrl");
        }
        if (requireAlt) {
            if (builder.length() > 0) {
                builder.append(" + ");
            }
            builder.append("Alt");
        }
        if (requireShift) {
            if (builder.length() > 0) {
                builder.append(" + ");
            }
            builder.append("Shift");
        }
        return builder.toString();
    }
}
