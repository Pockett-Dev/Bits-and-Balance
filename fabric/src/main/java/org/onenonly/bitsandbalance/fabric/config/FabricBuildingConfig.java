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

        JsonObject description = getOrCreateObject(root, "description");
        description.addProperty("building", "Building-related feature toggles.");

        JsonObject building = getOrCreateObject(root, "building");
        JsonObject buildingDescription = getOrCreateObject(building, "description");
        buildingDescription.addProperty("verticalSlabs", "Enable or disable dynamic Vertical Slabs.");
        buildingDescription.addProperty("stepsAndVerticalSteps", "Enable or disable quarter-block Steps and Vertical Steps.");

        JsonObject verticalSlabs = getOrCreateObject(building, "verticalSlabs");
        JsonObject verticalSlabsDescription = getOrCreateObject(verticalSlabs, "description");
        verticalSlabsDescription.addProperty("enabled", "Enable Vertical Slabs. Disabling this stops dynamic vertical slab generation and generated recipes, and turns off related placement and presentation.");

        JsonObject stepsAndVerticalSteps = getOrCreateObject(building, "stepsAndVerticalSteps");
        JsonObject stepsDescription = getOrCreateObject(stepsAndVerticalSteps, "description");
        stepsDescription.addProperty("enabled", "Enable Steps and Vertical Steps. Disabling this stops dynamic step/vertical-step generation and generated recipes, and turns off related placement and presentation.");

        boolean verticalSlabsDefault = readLegacyTweaksBoolean("enhancedSlabsVerticalSlabs", enhancedSlabsVerticalSlabs);
        boolean stepsDefault = readLegacyTweaksBoolean("enhancedSlabsSteps", enhancedSlabsSteps);
        JsonObject legacyEnhancedSlabs = getObject(building, "enhancedSlabs");
        if (legacyEnhancedSlabs == null) {
            JsonObject legacyBuilding = getObject(legacyRoot, "building");
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
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()
                ? element.getAsBoolean()
                : def;
    }
}