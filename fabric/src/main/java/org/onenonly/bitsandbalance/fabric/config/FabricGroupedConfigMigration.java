package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class FabricGroupedConfigMigration {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private FabricGroupedConfigMigration() {
    }

    public static void migrateIfNeeded() {
        mergeIntoGroupedFile(
                "bitsandbalance-gameplay.json",
                List.of(
                        "bitsandbalance-balance.json",
                        "bitsandbalance-combat.json",
                        "bitsandbalance-mechanics.json",
                        "bitsandbalance-mobs.json",
                        "bitsandbalance-tweaks.json"
                )
        );
        mergeIntoGroupedFile(
                "bitsandbalance-content.json",
                List.of(
                        "bitsandbalance-building.json",
                        "bitsandbalance-enchanting.json",
                        "bitsandbalance-loot.json",
                        "bitsandbalance-potions.json",
                        "bitsandbalance-recipes.json"
                )
        );
        migrateDogMusicDiscToContent();
        migrateGlowGooToContent();
        migrateClientOnlyTweaksToClient();
        mergeClientModifierStore();
        migrateItemShareModifiersToClientStore();
    }

    private static void migrateDogMusicDiscToContent() {
        Path contentPath = FabricConfigPaths.resolve("bitsandbalance-content.json");
        Path worldgenPath = FabricConfigPaths.resolve("bitsandbalance-worldgen.json");

        JsonObject contentRoot = FabricJsonConfigFiles.readRoot(contentPath, GSON);
        JsonObject worldgenRoot = FabricJsonConfigFiles.readRoot(worldgenPath, GSON);

        JsonObject worldgenDogMusicDisc = getObject(worldgenRoot, "dogMusicDisc");
        JsonElement legacyEnabled = worldgenRoot.get("enableDogMusicDisc");
        boolean contentHasDogMusicDisc = getObject(contentRoot, "dogMusicDisc") != null
                || (contentRoot.has("enableDogMusicDisc")
                && contentRoot.get("enableDogMusicDisc").isJsonPrimitive()
                && contentRoot.get("enableDogMusicDisc").getAsJsonPrimitive().isBoolean());

        boolean writeContent = false;
        boolean writeWorldgen = false;

        if (!contentHasDogMusicDisc) {
            if (worldgenDogMusicDisc != null) {
                contentRoot.add("dogMusicDisc", worldgenDogMusicDisc.deepCopy());
                writeContent = true;
            } else if (legacyEnabled != null && legacyEnabled.isJsonPrimitive() && legacyEnabled.getAsJsonPrimitive().isBoolean()) {
                JsonObject migratedDogMusicDisc = new JsonObject();
                migratedDogMusicDisc.addProperty("enabled", legacyEnabled.getAsBoolean());
                contentRoot.add("dogMusicDisc", migratedDogMusicDisc);
                writeContent = true;
            }
        }

        if (worldgenRoot.remove("dogMusicDisc") != null) {
            writeWorldgen = true;
        }
        if (worldgenRoot.remove("enableDogMusicDisc") != null) {
            writeWorldgen = true;
        }

        try {
            if (writeContent) {
                FabricJsonConfigFiles.writeRoot(contentPath, GSON, contentRoot);
            }
            if (writeWorldgen) {
                FabricJsonConfigFiles.writeRoot(worldgenPath, GSON, worldgenRoot);
            }
        } catch (IOException ignored) {
        }
    }

    private static void migrateGlowGooToContent() {
        Path contentPath = FabricConfigPaths.resolve("bitsandbalance-content.json");
        Path gameplayPath = FabricConfigPaths.resolve("bitsandbalance-gameplay.json");

        JsonObject contentRoot = FabricJsonConfigFiles.readRoot(contentPath, GSON);
        JsonObject gameplayRoot = FabricJsonConfigFiles.readRoot(gameplayPath, GSON);

        JsonObject gameplayGlowGoo = getObject(gameplayRoot, "glowGoo");
        JsonElement legacyEnabled = gameplayRoot.get("enableGlowGoo");
        boolean contentHasGlowGoo = getObject(contentRoot, "glowGoo") != null
                || (contentRoot.has("enableGlowGoo")
                && contentRoot.get("enableGlowGoo").isJsonPrimitive()
                && contentRoot.get("enableGlowGoo").getAsJsonPrimitive().isBoolean());

        boolean writeContent = false;
        boolean writeGameplay = false;

        if (!contentHasGlowGoo) {
            if (gameplayGlowGoo != null) {
                contentRoot.add("glowGoo", gameplayGlowGoo.deepCopy());
                writeContent = true;
            } else if (legacyEnabled != null && legacyEnabled.isJsonPrimitive() && legacyEnabled.getAsJsonPrimitive().isBoolean()) {
                JsonObject migratedGlowGoo = new JsonObject();
                migratedGlowGoo.addProperty("enabled", legacyEnabled.getAsBoolean());
                contentRoot.add("glowGoo", migratedGlowGoo);
                writeContent = true;
            }
        }

        if (gameplayRoot.remove("glowGoo") != null) {
            writeGameplay = true;
        }
        if (gameplayRoot.remove("enableGlowGoo") != null) {
            writeGameplay = true;
        }

        try {
            if (writeContent) {
                FabricJsonConfigFiles.writeRoot(contentPath, GSON, contentRoot);
            }
            if (writeGameplay) {
                FabricJsonConfigFiles.writeRoot(gameplayPath, GSON, gameplayRoot);
            }
        } catch (IOException ignored) {
        }
    }

    private static void migrateClientOnlyTweaksToClient() {
        Path clientPath = FabricConfigPaths.resolve("bitsandbalance-client.json");
        Path gameplayPath = FabricConfigPaths.resolve("bitsandbalance-gameplay.json");

        JsonObject clientRoot = FabricJsonConfigFiles.readRoot(clientPath, GSON);
        JsonObject gameplayRoot = FabricJsonConfigFiles.readRoot(gameplayPath, GSON);

        boolean writeClient = false;
        boolean writeGameplay = false;

        writeClient |= migrateSectionIfMissing(gameplayRoot, clientRoot, "nightVision", "nightVision");
        writeClient |= migrateSectionIfMissing(gameplayRoot, clientRoot, "guardian", "guardian");
        writeClient |= migrateSectionIfMissing(gameplayRoot, clientRoot, "experience", "experience");
        writeClient |= migrateSectionIfMissing(gameplayRoot, clientRoot, "seeHeldItem", "seeHeldItem");
        writeClient |= migrateSectionIfMissing(gameplayRoot, clientRoot, "worldWarnings", "worldWarnings");
        writeClient |= migrateSectionIfMissing(gameplayRoot, clientRoot, "customSplashTexts", "splashText");
        writeClient |= migrateChatHeadsIfMissing(gameplayRoot, clientRoot);

        writeGameplay |= removeKey(gameplayRoot, "nightVision");
        writeGameplay |= removeKey(gameplayRoot, "guardian");
        writeGameplay |= removeKey(gameplayRoot, "experience");
        writeGameplay |= removeKey(gameplayRoot, "seeHeldItem");
        writeGameplay |= removeKey(gameplayRoot, "worldWarnings");
        writeGameplay |= removeKey(gameplayRoot, "customSplashTexts");
        writeGameplay |= removeKey(gameplayRoot, "chatHeads");

        writeGameplay |= removeKey(gameplayRoot, "enableNightVisionFade");
        writeGameplay |= removeKey(gameplayRoot, "nightVisionFadeSeconds");
        writeGameplay |= removeKey(gameplayRoot, "disableGuardianJumpscare");
        writeGameplay |= removeKey(gameplayRoot, "enableLevel30OldSound");
        writeGameplay |= removeKey(gameplayRoot, "enableShowHeldItemWhenRiding");
        writeGameplay |= removeKey(gameplayRoot, "suppressExperimentalSettingsWarning");
        writeGameplay |= removeKey(gameplayRoot, "enableCustomSplashTexts");
        writeGameplay |= removeKey(gameplayRoot, "customSplashMultiplier");
        writeGameplay |= removeKey(gameplayRoot, "enableChatHeads");
        writeGameplay |= removeKey(gameplayRoot, "chatHeadSize");
        writeGameplay |= removeKey(gameplayRoot, "chatHeadOffset");

        JsonObject gameplayDescription = getObject(gameplayRoot, "description");
        if (gameplayDescription != null) {
            writeGameplay |= removeKey(gameplayDescription, "nightVision");
            writeGameplay |= removeKey(gameplayDescription, "guardian");
            writeGameplay |= removeKey(gameplayDescription, "experience");
            writeGameplay |= removeKey(gameplayDescription, "seeHeldItem");
            writeGameplay |= removeKey(gameplayDescription, "worldWarnings");
            writeGameplay |= removeKey(gameplayDescription, "customSplashTexts");
            writeGameplay |= removeKey(gameplayDescription, "chatHeads");
        }

        try {
            if (writeClient) {
                FabricJsonConfigFiles.writeRoot(clientPath, GSON, clientRoot);
            }
            if (writeGameplay) {
                FabricJsonConfigFiles.writeRoot(gameplayPath, GSON, gameplayRoot);
            }
        } catch (IOException ignored) {
        }
    }

    private static void mergeIntoGroupedFile(String targetFileName, List<String> legacyFileNames) {
        Path targetPath = FabricConfigPaths.resolve(targetFileName);
        if (Files.exists(targetPath)) {
            return;
        }

        JsonObject merged = new JsonObject();
        boolean foundLegacy = false;
        for (String legacyFileName : legacyFileNames) {
            Path legacyPath = FabricConfigPaths.resolve(legacyFileName);
            JsonObject legacyRoot = FabricJsonConfigFiles.readRoot(legacyPath, GSON);
            if (legacyRoot.isEmpty()) {
                continue;
            }
            foundLegacy = true;
            mergeObjects(merged, legacyRoot);
        }

        if (!foundLegacy) {
            return;
        }

        try {
            FabricJsonConfigFiles.writeRoot(targetPath, GSON, merged);
            for (String legacyFileName : legacyFileNames) {
                FabricJsonConfigFiles.deleteIfExists(legacyFileName);
            }
        } catch (IOException ignored) {
        }
    }

    private static void mergeClientModifierStore() {
        Path clientPath = FabricConfigPaths.resolve("bitsandbalance-client.json");
        Path legacyPath = FabricConfigPaths.resolve("bitsandbalance-keybind-modifiers.json");
        if (!Files.exists(legacyPath)) {
            return;
        }

        JsonObject clientRoot = FabricJsonConfigFiles.readRoot(clientPath, GSON);
        JsonObject legacyRoot = FabricJsonConfigFiles.readRoot(legacyPath, GSON);
        JsonObject legacyModifiers = getObject(legacyRoot, "modifiers");
        if (legacyModifiers == null) {
            FabricJsonConfigFiles.deleteIfExists("bitsandbalance-keybind-modifiers.json");
            return;
        }

        JsonObject keybindModifiers = getOrCreateObject(clientRoot, "keybindModifiers");
        if (!keybindModifiers.has("modifiers") || !keybindModifiers.get("modifiers").isJsonObject()) {
            keybindModifiers.add("modifiers", legacyModifiers.deepCopy());
        }

        try {
            FabricJsonConfigFiles.writeRoot(clientPath, GSON, clientRoot);
            FabricJsonConfigFiles.deleteIfExists("bitsandbalance-keybind-modifiers.json");
        } catch (IOException ignored) {
        }
    }

    private static void migrateItemShareModifiersToClientStore() {
        Path clientPath = FabricConfigPaths.resolve("bitsandbalance-client.json");
        Path gameplayPath = FabricConfigPaths.resolve("bitsandbalance-gameplay.json");

        JsonObject clientRoot = FabricJsonConfigFiles.readRoot(clientPath, GSON);
        JsonObject gameplayRoot = FabricJsonConfigFiles.readRoot(gameplayPath, GSON);

        JsonObject keybindModifiers = getOrCreateObject(clientRoot, "keybindModifiers");
        JsonObject modifiers = getOrCreateObject(keybindModifiers, "modifiers");
        JsonObject clientDescription = getObject(clientRoot, "description");
        JsonObject gameplayDescription = getObject(gameplayRoot, "description");
        JsonObject clientKeybindDescription = getObject(keybindModifiers, "description");
        JsonObject gameplayItemSharing = getObject(gameplayRoot, "itemSharing");
        JsonObject gameplayItemSharingDescription = gameplayItemSharing != null ? getObject(gameplayItemSharing, "description") : null;

        boolean writeClient = false;
        boolean writeGameplay = false;

        String itemShareKeyName = "key.bitsandbalance.item_share";
        if (!modifiers.has(itemShareKeyName)) {
            int legacyMask = 0;

            JsonElement clientShift = keybindModifiers.get("itemShareRequireShift");
            JsonElement clientAlt = keybindModifiers.get("itemShareRequireAlt");
            JsonElement clientCtrl = keybindModifiers.get("itemShareRequireCtrl");

            JsonElement gameplayShift = gameplayItemSharing != null ? gameplayItemSharing.get("requireShift") : null;
            JsonElement gameplayAlt = gameplayItemSharing != null ? gameplayItemSharing.get("requireAlt") : null;
            JsonElement gameplayCtrl = gameplayItemSharing != null ? gameplayItemSharing.get("requireCtrl") : null;

            JsonElement legacyShift = gameplayRoot.get("itemShareRequireShift");
            JsonElement legacyAlt = gameplayRoot.get("itemShareRequireAlt");
            JsonElement legacyCtrl = gameplayRoot.get("itemShareRequireCtrl");

            if (getBoolean(clientShift, false) || getBoolean(gameplayShift, false) || getBoolean(legacyShift, false)) {
                legacyMask |= org.lwjgl.glfw.GLFW.GLFW_MOD_SHIFT;
            }
            if (getBoolean(clientAlt, false) || getBoolean(gameplayAlt, false) || getBoolean(legacyAlt, false)) {
                legacyMask |= org.lwjgl.glfw.GLFW.GLFW_MOD_ALT;
            }
            if (getBoolean(clientCtrl, false) || getBoolean(gameplayCtrl, false) || getBoolean(legacyCtrl, false)) {
                legacyMask |= org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;
            }

            if (legacyMask != 0) {
                modifiers.addProperty(itemShareKeyName, legacyMask);
                writeClient = true;
            }
        }

        writeClient |= removeKey(keybindModifiers, "itemShareRequireShift");
        writeClient |= removeKey(keybindModifiers, "itemShareRequireAlt");
        writeClient |= removeKey(keybindModifiers, "itemShareRequireCtrl");
        writeClient |= removeKey(clientKeybindDescription, "itemShareRequireShift");
        writeClient |= removeKey(clientKeybindDescription, "itemShareRequireAlt");
        writeClient |= removeKey(clientKeybindDescription, "itemShareRequireCtrl");
        writeClient |= removeKey(clientDescription, "keybindModifiers");

        writeGameplay |= removeKey(gameplayRoot, "itemShareRequireShift");
        writeGameplay |= removeKey(gameplayRoot, "itemShareRequireAlt");
        writeGameplay |= removeKey(gameplayRoot, "itemShareRequireCtrl");
        if (gameplayDescription != null && gameplayDescription.has("itemSharing")) {
            gameplayDescription.addProperty("itemSharing", "Item share gameplay settings.");
            writeGameplay = true;
        }
        if (gameplayItemSharing != null) {
            writeGameplay |= removeKey(gameplayItemSharing, "requireShift");
            writeGameplay |= removeKey(gameplayItemSharing, "requireAlt");
            writeGameplay |= removeKey(gameplayItemSharing, "requireCtrl");
            writeGameplay |= removeKey(gameplayItemSharingDescription, "requireShift");
            writeGameplay |= removeKey(gameplayItemSharingDescription, "requireAlt");
            writeGameplay |= removeKey(gameplayItemSharingDescription, "requireCtrl");
        }

        try {
            if (writeClient) {
                FabricJsonConfigFiles.writeRoot(clientPath, GSON, clientRoot);
            }
            if (writeGameplay) {
                FabricJsonConfigFiles.writeRoot(gameplayPath, GSON, gameplayRoot);
            }
        } catch (IOException ignored) {
        }
    }

    private static void mergeObjects(JsonObject target, JsonObject source) {
        for (String key : source.keySet()) {
            if (key.startsWith("_")) {
                continue;
            }

            JsonElement sourceValue = source.get(key);
            if (sourceValue == null) {
                continue;
            }

            JsonElement targetValue = target.get(key);
            if (sourceValue.isJsonObject() && targetValue != null && targetValue.isJsonObject()) {
                mergeObjects(targetValue.getAsJsonObject(), sourceValue.getAsJsonObject());
                continue;
            }

            if (!target.has(key)) {
                target.add(key, sourceValue.deepCopy());
            }
        }
    }

    private static boolean migrateSectionIfMissing(JsonObject sourceRoot, JsonObject targetRoot, String sourceKey, String targetKey) {
        JsonObject sourceSection = getObject(sourceRoot, sourceKey);
        if (sourceSection == null || getObject(targetRoot, targetKey) != null) {
            return false;
        }

        targetRoot.add(targetKey, sourceSection.deepCopy());
        return true;
    }

    private static boolean migrateChatHeadsIfMissing(JsonObject gameplayRoot, JsonObject clientRoot) {
        if (getObject(clientRoot, "chatHeads") != null) {
            return false;
        }

        JsonObject gameplayChatHeads = getObject(gameplayRoot, "chatHeads");
        JsonElement legacyEnabled = gameplayRoot.get("enableChatHeads");
        JsonElement legacySize = gameplayRoot.get("chatHeadSize");
        JsonElement legacyOffset = gameplayRoot.get("chatHeadOffset");
        if (gameplayChatHeads == null && legacyEnabled == null && legacySize == null && legacyOffset == null) {
            return false;
        }

        JsonObject migratedChatHeads = new JsonObject();
        if (gameplayChatHeads != null) {
            JsonElement enabled = gameplayChatHeads.get("enabled");
            JsonElement size = gameplayChatHeads.get("size");
            JsonElement offset = gameplayChatHeads.get("offset");
            if (enabled != null) {
                migratedChatHeads.add("enableChatHeads", enabled.deepCopy());
            }
            if (size != null) {
                migratedChatHeads.add("chatHeadSize", size.deepCopy());
            }
            if (offset != null) {
                migratedChatHeads.add("chatHeadOffset", offset.deepCopy());
            }
        }
        if (!migratedChatHeads.has("enableChatHeads") && legacyEnabled != null) {
            migratedChatHeads.add("enableChatHeads", legacyEnabled.deepCopy());
        }
        if (!migratedChatHeads.has("chatHeadSize") && legacySize != null) {
            migratedChatHeads.add("chatHeadSize", legacySize.deepCopy());
        }
        if (!migratedChatHeads.has("chatHeadOffset") && legacyOffset != null) {
            migratedChatHeads.add("chatHeadOffset", legacyOffset.deepCopy());
        }

        if (migratedChatHeads.size() == 0) {
            return false;
        }

        clientRoot.add("chatHeads", migratedChatHeads);
        return true;
    }

    private static boolean removeKey(JsonObject obj, String key) {
        return obj != null && obj.remove(key) != null;
    }

    private static boolean getBoolean(JsonElement element, boolean fallback) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement element = obj.get(key);
        return element instanceof JsonObject child ? child : null;
    }

    private static JsonObject getOrCreateObject(JsonObject obj, String key) {
        JsonObject child = getObject(obj, key);
        if (child != null) {
            return child;
        }

        child = new JsonObject();
        obj.add(key, child);
        return child;
    }
}
