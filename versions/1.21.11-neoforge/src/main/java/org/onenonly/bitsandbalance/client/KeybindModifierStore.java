package org.onenonly.bitsandbalance.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.neoforged.neoforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class KeybindModifierStore {
    public static final int MOD_MASK = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL;
    private static final String ITEM_SHARE_MAPPING = "key.bitsandbalance.item_share";

    private static final Map<String, Integer> EXTRA_MODIFIER_MASKS = new ConcurrentHashMap<>();
    private static final Map<Object, CapturedBinding> CAPTURED_BINDINGS = new WeakHashMap<>();

    private KeybindModifierStore() {
    }

    public static int getStoredMask(KeyMapping mapping) {
        if (mapping == null) {
            return 0;
        }
        return EXTRA_MODIFIER_MASKS.getOrDefault(mapping.getName(), 0) & MOD_MASK;
    }

    public static boolean isItemShareMapping(KeyMapping mapping) {
        return mapping != null && ITEM_SHARE_MAPPING.equals(mapping.getName());
    }

    public static int getEffectiveModifierMask(KeyMapping mapping) {
        boolean itemShare = isItemShareMapping(mapping);
        return KeybindModifierHelper.effectiveModifierMask(
                mapping,
                getStoredMask(mapping),
                itemShare && org.onenonly.bitsandbalance.ClientConfig.itemShareRequireShift,
                itemShare && org.onenonly.bitsandbalance.ClientConfig.itemShareRequireAlt,
                itemShare && org.onenonly.bitsandbalance.ClientConfig.itemShareRequireCtrl
        );
    }

    public static void clearStoredMask(KeyMapping mapping) {
        if (mapping != null) {
            EXTRA_MODIFIER_MASKS.remove(mapping.getName());
        }
    }

    public static void loadStoredMask(KeyMapping mapping, int mask) {
        setStoredMask(mapping, mask, false);
    }

    public static void captureKeyPress(Object screen, KeyMapping mapping, InputConstants.Key pressedKey, int glfwModifiers) {
        if (screen == null || mapping == null || pressedKey == null) {
            return;
        }

        int mask = maskFromGlfwModifiers(glfwModifiers) | currentModifierMask();
        boolean modifierKey = isModifierKey(pressedKey);
        if (modifierKey) {
            mask |= maskFromModifierKey(pressedKey);
        }

        synchronized (CAPTURED_BINDINGS) {
            CapturedBinding captured = CAPTURED_BINDINGS.computeIfAbsent(screen, ignored -> new CapturedBinding(mapping));
            if (captured.mapping != mapping) {
                captured.mapping = mapping;
                captured.mask = 0;
                captured.sawNonModifierKey = false;
            }
            captured.mask |= mask & MOD_MASK;
            if (!modifierKey) {
                captured.sawNonModifierKey = true;
            }
        }
    }

    public static void captureCurrentModifiers(Object screen, KeyMapping mapping) {
        if (screen == null || mapping == null) {
            return;
        }
        synchronized (CAPTURED_BINDINGS) {
            CapturedBinding captured = CAPTURED_BINDINGS.computeIfAbsent(screen, ignored -> new CapturedBinding(mapping));
            if (captured.mapping != mapping) {
                captured.mapping = mapping;
                captured.mask = 0;
            }
            captured.mask |= currentModifierMask();
            captured.sawNonModifierKey = true;
        }
    }

    public static void clearCapturedBinding(Object screen) {
        if (screen == null) {
            return;
        }
        synchronized (CAPTURED_BINDINGS) {
            CAPTURED_BINDINGS.remove(screen);
        }
    }

    public static void applyCapturedBinding(Object screen, KeyMapping mapping) {
        if (screen == null || mapping == null) {
            return;
        }

        CapturedBinding captured;
        synchronized (CAPTURED_BINDINGS) {
            captured = CAPTURED_BINDINGS.remove(screen);
        }

        int mask = modifierToMask(mapping.getKeyModifier());
        if (captured != null && captured.mapping == mapping) {
            mask |= captured.mask;
        }

        setStoredMask(mapping, mask, true);
    }

    public static boolean areStoredModifiersDown(KeyMapping mapping) {
        int storedMask = getStoredMask(mapping);
        return storedMask == 0 || areModifiersDown(storedMask);
    }

    public static Component formatStoredModifiers(KeyMapping mapping, Component baseKeyName) {
        int effectiveMask = getEffectiveModifierMask(mapping);
        if (effectiveMask == 0) {
            return baseKeyName;
        }
        return formatWithModifiers(baseKeyName, effectiveMask);
    }

    public static String appendSavedModifiers(KeyMapping mapping) {
        int storedMask = getStoredMask(mapping);
        if (storedMask == 0) {
            int nativeMask = modifierToMask(mapping.getKeyModifier());
            if (nativeMask == 0) {
                return mapping.saveString();
            }
            return mapping.saveString() + ":" + modifiersToOptionSuffix(nativeMask);
        }
        return mapping.saveString() + ":" + modifiersToOptionSuffix(storedMask);
    }

    public static int parseSavedModifierMask(String savedValue) {
        int firstColon = savedValue.indexOf(':');
        if (firstColon < 0 || firstColon == savedValue.length() - 1) {
            return 0;
        }

        String[] parts = savedValue.substring(firstColon + 1).split(":");
        int mask = 0;
        for (String part : parts) {
            mask |= modifierNameToMask(part);
        }
        return Integer.bitCount(mask & MOD_MASK) > 1 ? mask & MOD_MASK : 0;
    }

    public static int maskFromGlfwModifiers(int glfwModifiers) {
        return glfwModifiers & MOD_MASK;
    }

    public static boolean isModifierKey(InputConstants.Key key) {
        return KeyModifier.isKeyCodeModifier(key);
    }

    public static KeyModifier primaryModifierForCapturedBinding(Object screen, KeyMapping mapping, InputConstants.Key pressedKey) {
        int mask = maskFromGlfwModifiers(0) | currentModifierMask();
        synchronized (CAPTURED_BINDINGS) {
            CapturedBinding captured = CAPTURED_BINDINGS.computeIfAbsent(screen, ignored -> new CapturedBinding(mapping));
            if (captured.mapping != mapping) {
                captured.mapping = mapping;
                captured.mask = 0;
                captured.sawNonModifierKey = false;
            }
            captured.mask |= mask & MOD_MASK;
            if (!isModifierKey(pressedKey)) {
                captured.sawNonModifierKey = true;
            }
        }

        int effectiveMask = mask | maskFromModifierKey(pressedKey);
        if ((effectiveMask & GLFW.GLFW_MOD_CONTROL) != 0) {
            return KeyModifier.CONTROL;
        }
        if ((effectiveMask & GLFW.GLFW_MOD_ALT) != 0) {
            return KeyModifier.ALT;
        }
        if ((effectiveMask & GLFW.GLFW_MOD_SHIFT) != 0) {
            return KeyModifier.SHIFT;
        }
        return KeyModifier.NONE;
    }

    private static void setStoredMask(KeyMapping mapping, int mask, boolean saveOptions) {
        if (mapping == null) {
            return;
        }

        int normalized = mask & MOD_MASK;
        if (Integer.bitCount(normalized) <= 1) {
            EXTRA_MODIFIER_MASKS.remove(mapping.getName());
        } else {
            EXTRA_MODIFIER_MASKS.put(mapping.getName(), normalized);
        }

        if (saveOptions) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.options != null) {
                minecraft.options.save();
            }
        }
    }

    private static boolean areModifiersDown(int requiredMask) {
        int required = requiredMask & MOD_MASK;
        if (required == 0) {
            return true;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return false;
        }

        var window = minecraft.getWindow();
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

    private static int currentModifierMask() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getWindow() == null) {
            return 0;
        }

        var window = minecraft.getWindow();
        int mask = 0;
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_SHIFT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            mask |= GLFW.GLFW_MOD_SHIFT;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT)) {
            mask |= GLFW.GLFW_MOD_ALT;
        }
        if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            mask |= GLFW.GLFW_MOD_CONTROL;
        }
        return mask;
    }

    private static int maskFromModifierKey(InputConstants.Key key) {
        return modifierToMask(KeyModifier.getKeyModifier(key));
    }

    private static int modifierToMask(KeyModifier modifier) {
        if (modifier == KeyModifier.SHIFT) {
            return GLFW.GLFW_MOD_SHIFT;
        }
        if (modifier == KeyModifier.ALT) {
            return GLFW.GLFW_MOD_ALT;
        }
        if (modifier == KeyModifier.CONTROL) {
            return GLFW.GLFW_MOD_CONTROL;
        }
        return 0;
    }

    private static int modifierNameToMask(String name) {
        KeyModifier modifier = KeyModifier.valueFromString(name);
        return modifierToMask(modifier);
    }

    private static String modifiersToOptionSuffix(int mask) {
        StringBuilder builder = new StringBuilder();
        appendModifierName(builder, mask, GLFW.GLFW_MOD_CONTROL, "CONTROL");
        appendModifierName(builder, mask, GLFW.GLFW_MOD_ALT, "ALT");
        appendModifierName(builder, mask, GLFW.GLFW_MOD_SHIFT, "SHIFT");
        return builder.toString();
    }

    private static void appendModifierName(StringBuilder builder, int mask, int bit, String name) {
        if ((mask & bit) == 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(':');
        }
        builder.append(name);
    }

    private static Component formatWithModifiers(Component baseKeyName, int requiredMask) {
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

    private static final class CapturedBinding {
        private KeyMapping mapping;
        private int mask;
        private boolean sawNonModifierKey;

        private CapturedBinding(KeyMapping mapping) {
            this.mapping = mapping;
        }
    }
}
