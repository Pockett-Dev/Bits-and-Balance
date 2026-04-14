package org.onenonly.bitsandbalance.fabric.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.fabric.config.FabricClientConfig;

/**
 * Stores modifier masks for arbitrary keybindings (Amecs-like behavior).
 *
 * Vanilla Minecraft doesn't persist modifiers alongside keybindings, so we store a parallel map
 * keyed by {@link KeyMapping#getName()}.
 */
public final class KeybindModifierStore {
    private KeybindModifierStore() {
    }

    public static final int MOD_MASK = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL;

    public static void init() {
        KeyMapping itemShare = org.onenonly.bitsandbalance.fabric.client.FabricKeyBindings.ITEM_SHARE;
        if (itemShare != null && getRequiredMask(itemShare) == 0) {
            int legacyMask = 0;
            if (FabricClientConfig.itemShareRequireShift) {
                legacyMask |= GLFW.GLFW_MOD_SHIFT;
            }
            if (FabricClientConfig.itemShareRequireAlt) {
                legacyMask |= GLFW.GLFW_MOD_ALT;
            }
            if (FabricClientConfig.itemShareRequireCtrl) {
                legacyMask |= GLFW.GLFW_MOD_CONTROL;
            }
            if (legacyMask != 0) {
                FabricClientConfig.setKeybindModifierMask(itemShare.getName(), legacyMask);
            }
        }
    }

    public static int getRequiredMask(KeyMapping mapping) {
        if (mapping == null) {
            return 0;
        }
        return FabricClientConfig.getKeybindModifierMask(mapping.getName()) & MOD_MASK;
    }

    public static void setRequiredMask(KeyMapping mapping, int mask) {
        if (mapping == null) {
            return;
        }
        int normalized = mask & MOD_MASK;
        FabricClientConfig.setKeybindModifierMask(mapping.getName(), normalized);
        FabricClientConfig.save();
    }

    public static boolean areRequiredModifiersDown(int requiredMask) {
        int required = requiredMask & MOD_MASK;
        if (required == 0) {
            return true;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.getWindow() == null) {
            return false;
        }

        var window = mc.getWindow();
        boolean shiftDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT);
        boolean altDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
        boolean ctrlDown = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);

        if ((required & GLFW.GLFW_MOD_SHIFT) != 0 && !shiftDown) return false;
        if ((required & GLFW.GLFW_MOD_ALT) != 0 && !altDown) return false;
        if ((required & GLFW.GLFW_MOD_CONTROL) != 0 && !ctrlDown) return false;
        return true;
    }

    public static Component formatWithModifiers(Component baseKeyName, int requiredMask) {
        int required = requiredMask & MOD_MASK;
        if (required == 0) {
            return baseKeyName;
        }

        MutableComponent out = Component.empty();

        if ((required & GLFW.GLFW_MOD_CONTROL) != 0) {
            out.append(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_CONTROL).getDisplayName());
            out.append(Component.literal(" + "));
        }
        if ((required & GLFW.GLFW_MOD_ALT) != 0) {
            out.append(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_ALT).getDisplayName());
            out.append(Component.literal(" + "));
        }
        if ((required & GLFW.GLFW_MOD_SHIFT) != 0) {
            out.append(InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_SHIFT).getDisplayName());
            out.append(Component.literal(" + "));
        }

        out.append(baseKeyName);
        return out;
    }
}
