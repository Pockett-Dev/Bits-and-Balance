package org.onenonly.bitsandbalance.fabric.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;
import org.onenonly.bitsandbalance.fabric.config.FabricConfigPaths;
import org.onenonly.bitsandbalance.fabric.config.FabricJsonConfigFiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Stores modifier masks for arbitrary keybindings (Amecs-like behavior).
 *
 * Vanilla Minecraft doesn't persist modifiers alongside keybindings, so we store a parallel map
 * keyed by {@link KeyMapping#getName()}.
 */
public final class KeybindModifierStore {
    private KeybindModifierStore() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-keybind-modifiers");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "bitsandbalance-client.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-keybind-modifiers.json";

    public static final int MOD_MASK = GLFW.GLFW_MOD_SHIFT | GLFW.GLFW_MOD_ALT | GLFW.GLFW_MOD_CONTROL;

    private static final Map<String, Integer> REQUIRED_BY_NAME = new HashMap<>();

    public static void init() {
        load();
    }

    public static int getRequiredMask(KeyMapping mapping) {
        if (mapping == null) {
            return 0;
        }
        return REQUIRED_BY_NAME.getOrDefault(mapping.getName(), 0) & MOD_MASK;
    }

    public static void setRequiredMask(KeyMapping mapping, int mask) {
        if (mapping == null) {
            return;
        }
        int normalized = mask & MOD_MASK;
        if (normalized == 0) {
            REQUIRED_BY_NAME.remove(mapping.getName());
        } else {
            REQUIRED_BY_NAME.put(mapping.getName(), normalized);
        }
        save();
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

    private static Path configPath() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        return FabricConfigPaths.resolve(FILE_NAME);
    }

    private static void load() {
        Path path = configPath();
        REQUIRED_BY_NAME.clear();

        if (!Files.exists(path)) {
            return;
        }

        try {
            String json = Files.readString(path);
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                return;
            }

            JsonObject keybindModifiers = null;
            JsonElement keybindModifiersElement = root.get("keybindModifiers");
            if (keybindModifiersElement != null && keybindModifiersElement.isJsonObject()) {
                keybindModifiers = keybindModifiersElement.getAsJsonObject();
            }

            JsonObject modifiers = null;
            JsonElement el = keybindModifiers != null ? keybindModifiers.get("modifiers") : root.get("modifiers");
            if (el != null && el.isJsonObject()) {
                modifiers = el.getAsJsonObject();
            }

            if (modifiers == null) {
                return;
            }

            for (Map.Entry<String, JsonElement> entry : modifiers.entrySet()) {
                if (!entry.getValue().isJsonPrimitive()) {
                    continue;
                }
                try {
                    int mask = entry.getValue().getAsInt() & MOD_MASK;
                    if (mask != 0) {
                        REQUIRED_BY_NAME.put(entry.getKey(), mask);
                    }
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to load keybind modifiers {}", path, e);
        }
    }

    private static void save() {
        Path path = configPath();

        try {
            JsonObject root = FabricJsonConfigFiles.readRoot(path, GSON);
            JsonObject keybindModifiers = root.getAsJsonObject("keybindModifiers");
            if (keybindModifiers == null) {
                keybindModifiers = new JsonObject();
                root.add("keybindModifiers", keybindModifiers);
            }

            JsonObject modifiers = new JsonObject();
            for (Map.Entry<String, Integer> entry : REQUIRED_BY_NAME.entrySet()) {
                modifiers.addProperty(entry.getKey(), entry.getValue());
            }
            keybindModifiers.add("modifiers", modifiers);

            FabricJsonConfigFiles.writeRoot(path, GSON, root);
            if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
                FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
            }
        } catch (Throwable e) {
            LOGGER.warn("Failed to save keybind modifiers {}", path, e);
        }
    }
}
