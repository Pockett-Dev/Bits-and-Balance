package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

public final class FabricBuildingConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-building.json";
    private static final String LEGACY_TWEAKS_FILE_NAME = "bitsandbalance-tweaks.json";
    private static final String LEGACY_GAMEPLAY_FILE_NAME = "bitsandbalance-gameplay.json";

    public static volatile boolean enableEnhancedSlabs = true;
    public static volatile boolean enhancedSlabsPlaceOnTop = true;
    public static volatile boolean enhancedSlabsHangBelow = true;
    public static volatile boolean enhancedSlabsKneeSlabMining = true;
    public static volatile boolean enhancedSlabsMixedDoubleSlabs = true;
    public static volatile boolean enhancedSlabsVerticalSlabs = true;
    public static volatile boolean enhancedSlabsSteps = true;

    private FabricBuildingConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject legacyRoot = FabricJsonConfigFiles.readRoot(LEGACY_FILE_NAME, GSON);
        JsonObject legacyGameplayRoot = FabricJsonConfigFiles.readRoot(FabricConfigPaths.resolve(LEGACY_GAMEPLAY_FILE_NAME), GSON);

        JsonObject description = getOrCreateObject(root, "description");
        description.remove("building");
        description.addProperty("enhancedSlabs", "Enhanced slab-family interaction and mining behavior.");
        description.addProperty("verticalSlabs", "Enable or disable dynamic Vertical Slabs.");
        description.addProperty("stepsAndVerticalSteps", "Enable or disable quarter-block Steps and Vertical Steps.");

        JsonObject building = getObject(root, "building");
        JsonObject legacyBuilding = getObject(legacyRoot, "building");
        JsonObject legacyGameplayEnhancedSlabs = getObject(legacyGameplayRoot, "enhancedSlabs");
        JsonObject enhancedSlabs = getOrCreateObject(root, "enhancedSlabs");
        JsonObject enhancedSlabsDescription = getOrCreateObject(enhancedSlabs, "description");
        enhancedSlabsDescription.addProperty("enabled", "Master toggle for enhanced slab behavior.");
        enhancedSlabsDescription.addProperty("placeOnTop", "Legacy toggle for top-placement behavior; kept for compatibility.");
        enhancedSlabsDescription.addProperty("hangBelow", "Allow hanging lanterns, chains, and signs below supported slabs and steps.");
        enhancedSlabsDescription.addProperty("kneeSlabMining", "Allow targeted half-mining for supported slab-family blocks.");
        enhancedSlabsDescription.addProperty("mixedDoubleSlabs", "Allow different slab types to combine into mixed double slabs.");

        JsonObject verticalSlabs = getOrCreateObject(root, "verticalSlabs");
        JsonObject verticalSlabsDescription = getOrCreateObject(verticalSlabs, "description");
        verticalSlabsDescription.addProperty("enabled", "Enable Vertical Slabs. Disabling this stops dynamic vertical slab generation and generated recipes, and turns off related placement and presentation.");

        JsonObject stepsAndVerticalSteps = getOrCreateObject(root, "stepsAndVerticalSteps");
        JsonObject stepsDescription = getOrCreateObject(stepsAndVerticalSteps, "description");
        stepsDescription.addProperty("enabled", "Enable Steps and Vertical Steps. Disabling this stops dynamic step/vertical-step generation and generated recipes, and turns off related placement and presentation.");

        enableEnhancedSlabs = getBoolean(legacyGameplayEnhancedSlabs, "enabled", enableEnhancedSlabs);
        enhancedSlabsPlaceOnTop = getBoolean(legacyGameplayEnhancedSlabs, "placeOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabsPlaceOnTop = getBoolean(legacyGameplayEnhancedSlabs, "allowPlaceOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabsHangBelow = getBoolean(legacyGameplayEnhancedSlabs, "hangBelow", enhancedSlabsHangBelow);
        enhancedSlabsHangBelow = getBoolean(legacyGameplayEnhancedSlabs, "allowHangBelow", enhancedSlabsHangBelow);
        enhancedSlabsKneeSlabMining = getBoolean(legacyGameplayEnhancedSlabs, "kneeSlabMining", enhancedSlabsKneeSlabMining);
        enhancedSlabsMixedDoubleSlabs = getBoolean(legacyGameplayEnhancedSlabs, "mixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
        enableEnhancedSlabs = readLegacyTweaksBoolean("enableEnhancedSlabs", enableEnhancedSlabs);
        enhancedSlabsPlaceOnTop = readLegacyTweaksBoolean("enhancedSlabsPlaceOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabsHangBelow = readLegacyTweaksBoolean("enhancedSlabsHangBelow", enhancedSlabsHangBelow);
        enhancedSlabsKneeSlabMining = readLegacyTweaksBoolean("enhancedSlabsKneeSlabMining", enhancedSlabsKneeSlabMining);
        enhancedSlabsMixedDoubleSlabs = readLegacyTweaksBoolean("enhancedSlabsMixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
        enableEnhancedSlabs = getBoolean(enhancedSlabs, "enabled", enableEnhancedSlabs);
        enhancedSlabsPlaceOnTop = getBoolean(enhancedSlabs, "placeOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabsPlaceOnTop = getBoolean(enhancedSlabs, "allowPlaceOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabsHangBelow = getBoolean(enhancedSlabs, "hangBelow", enhancedSlabsHangBelow);
        enhancedSlabsHangBelow = getBoolean(enhancedSlabs, "allowHangBelow", enhancedSlabsHangBelow);
        enhancedSlabsKneeSlabMining = getBoolean(enhancedSlabs, "kneeSlabMining", enhancedSlabsKneeSlabMining);
        enhancedSlabsMixedDoubleSlabs = getBoolean(enhancedSlabs, "mixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
        if (!enhancedSlabsPlaceOnTop
                && !enhancedSlabsHangBelow
                && !enhancedSlabsKneeSlabMining
                && !enhancedSlabsMixedDoubleSlabs) {
            enableEnhancedSlabs = false;
        }
        enhancedSlabs.addProperty("enabled", enableEnhancedSlabs);
        enhancedSlabs.addProperty("placeOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabs.addProperty("hangBelow", enhancedSlabsHangBelow);
        enhancedSlabs.addProperty("kneeSlabMining", enhancedSlabsKneeSlabMining);
        enhancedSlabs.addProperty("mixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
        enhancedSlabs.remove("allowPlaceOnTop");
        enhancedSlabs.remove("allowHangBelow");

        boolean verticalSlabsDefault = readLegacyTweaksBoolean("enhancedSlabsVerticalSlabs", enhancedSlabsVerticalSlabs);
        boolean stepsDefault = readLegacyTweaksBoolean("enhancedSlabsSteps", enhancedSlabsSteps);
        verticalSlabsDefault = getBoolean(getObject(building, "verticalSlabs"), "enabled", verticalSlabsDefault);
        stepsDefault = getBoolean(getObject(building, "stepsAndVerticalSteps"), "enabled", stepsDefault);
        verticalSlabsDefault = getBoolean(getObject(legacyBuilding, "verticalSlabs"), "enabled", verticalSlabsDefault);
        stepsDefault = getBoolean(getObject(legacyBuilding, "stepsAndVerticalSteps"), "enabled", stepsDefault);
        JsonObject legacyEnhancedSlabs = getObject(building, "enhancedSlabs");
        if (legacyEnhancedSlabs == null) {
            if (legacyBuilding != null) {
                legacyEnhancedSlabs = getObject(legacyBuilding, "enhancedSlabs");
            }
        }
        if (legacyEnhancedSlabs != null) {
            verticalSlabsDefault = getBoolean(legacyEnhancedSlabs, "verticalSlabs", verticalSlabsDefault);

            JsonObject legacyDescription = getObject(legacyEnhancedSlabs, "description");
            if (legacyDescription != null) {
                legacyDescription.remove("verticalSlabs");
            }
            legacyEnhancedSlabs.remove("verticalSlabs");

            stepsDefault = getBoolean(legacyEnhancedSlabs, "steps", stepsDefault);
        }

        enhancedSlabsVerticalSlabs = getBoolean(verticalSlabs, "enabled", verticalSlabsDefault);
        verticalSlabs.addProperty("enabled", enhancedSlabsVerticalSlabs);

        enhancedSlabsSteps = getBoolean(stepsAndVerticalSteps, "enabled", stepsDefault);
        stepsAndVerticalSteps.addProperty("enabled", enhancedSlabsSteps);
        root.remove("building");
        syncRuntimeFlags();

        try {
            FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
            if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
                FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
            }
        } catch (IOException ignored) {
            // Keep the loaded value even if the normalized write fails.
        }

        cleanupDisabledGeneratedDatapacks();
    }

    public static void syncRuntimeFlags() {
        FabricTweaksConfig.enableEnhancedSlabs = enableEnhancedSlabs;
        FabricTweaksConfig.enhancedSlabsPlaceOnTop = enhancedSlabsPlaceOnTop;
        FabricTweaksConfig.enhancedSlabsHangBelow = enhancedSlabsHangBelow;
        FabricTweaksConfig.enhancedSlabsKneeSlabMining = enhancedSlabsKneeSlabMining;
        FabricTweaksConfig.enhancedSlabsMixedDoubleSlabs = enhancedSlabsMixedDoubleSlabs;
        FabricTweaksConfig.enhancedSlabsVerticalSlabs = enhancedSlabsVerticalSlabs;
        FabricTweaksConfig.enhancedSlabsSteps = enhancedSlabsSteps;
    }

    private static void cleanupDisabledGeneratedDatapacks() {
        Path savesRoot = FabricLoader.getInstance().getGameDir().resolve("saves");
        if (!Files.isDirectory(savesRoot)) {
            return;
        }

        try (var saves = Files.list(savesRoot)) {
            for (Path worldPath : saves.toList()) {
                if (!Files.isDirectory(worldPath)) {
                    continue;
                }

                Path datapacksDir = worldPath.resolve("datapacks");
                if (!Files.isDirectory(datapacksDir)) {
                    continue;
                }

                if (!enhancedSlabsVerticalSlabs) {
                    removeGeneratedDatapack(datapacksDir.resolve("bitsandbalance_vertical_slabs"));
                }
                if (!enhancedSlabsSteps) {
                    removeGeneratedDatapack(datapacksDir.resolve("bitsandbalance_steps"));
                }
            }
        } catch (IOException ignored) {
            // Best-effort cleanup only; world-load fallback still handles current world after startup.
        }
    }

    private static void removeGeneratedDatapack(Path datapackPath) {
        if (!Files.exists(datapackPath)) {
            return;
        }

        try (var paths = Files.walk(datapackPath)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // Best-effort cleanup only.
                }
            });
        } catch (IOException ignored) {
            // Best-effort cleanup only; world-load fallback still handles current world after startup.
        }
    }

    private static boolean readLegacyTweaksBoolean(String key, boolean fallback) {
        Path legacyTweaksPath = FabricConfigPaths.resolve(LEGACY_TWEAKS_FILE_NAME);
        if (!Files.exists(legacyTweaksPath)) {
            return fallback;
        }

        try (Reader reader = Files.newBufferedReader(legacyTweaksPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
            if (!(parsed instanceof JsonObject obj)) {
                return fallback;
            }

            JsonObject tweaks = getObject(obj, "tweaks");
            if (tweaks != null) {
                return getBoolean(tweaks, key, fallback);
            }

            return getBoolean(obj, key, fallback);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        if (obj == null) {
            return null;
        }
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

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        if (obj == null) {
            return def;
        }
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean()
                : def;
    }
}
