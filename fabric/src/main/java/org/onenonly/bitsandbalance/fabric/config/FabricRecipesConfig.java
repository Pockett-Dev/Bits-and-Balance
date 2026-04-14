package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Fabric-side config for dynamic datapack-managed recipe tweaks.
 *
 * Config file: config/Bits and Balance/bitsandbalance-recipes.json
 */
public final class FabricRecipesConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-recipes-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-recipes.json";

    public static volatile boolean enableAlternativeRepeaterRecipe = true;
    public static volatile boolean enableChestFromLogsRecipe = true;
    public static volatile boolean enableMapInkSacRecipe = true;
    public static volatile boolean enableRecoveryCompassRecipe = true;
    public static volatile boolean enableEnderEyeRecipe = true;

    public static volatile boolean enableRawIronSmeltingRecipe = true;
    public static volatile boolean enableRawGoldSmeltingRecipe = true;
    public static volatile boolean enableRawCopperSmeltingRecipe = true;

    public static volatile boolean enableRawIronBlastingRecipe = true;
    public static volatile boolean enableRawGoldBlastingRecipe = true;
    public static volatile boolean enableRawCopperBlastingRecipe = true;

    public static volatile boolean enableStairRecipeOverride = true;
    public static volatile boolean enableCompactStairRecipe = true;

    private FabricRecipesConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
    }

    public static boolean anyEnabled() {
        return enableAlternativeRepeaterRecipe
                || enableChestFromLogsRecipe
                || enableMapInkSacRecipe
                || enableRecoveryCompassRecipe
                || enableEnderEyeRecipe
                || enableRawIronSmeltingRecipe
                || enableRawGoldSmeltingRecipe
                || enableRawCopperSmeltingRecipe
                || enableRawIronBlastingRecipe
                || enableRawGoldBlastingRecipe
                || enableRawCopperBlastingRecipe
                || enableStairRecipeOverride
                || enableCompactStairRecipe;
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        JsonObject obj = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject legacyObj = FabricJsonConfigFiles.readRoot(LEGACY_FILE_NAME, GSON);

        try {
            JsonObject legacyRecipes = getObject(legacyObj, "recipes");
            JsonObject legacySource = legacyRecipes != null ? legacyRecipes : obj;
            if (legacyRecipes == null && obj.entrySet().isEmpty()) {
                legacySource = legacyObj;
            }

            JsonObject alternativeCrafting = getObject(obj, "alternativeCrafting");
            if (alternativeCrafting != null) {
                enableAlternativeRepeaterRecipe = getBoolean(alternativeCrafting, "repeater", enableAlternativeRepeaterRecipe);
                enableChestFromLogsRecipe = getBoolean(alternativeCrafting, "chestFromLogs", enableChestFromLogsRecipe);
                enableMapInkSacRecipe = getBoolean(alternativeCrafting, "mapInkSac", enableMapInkSacRecipe);
                enableRecoveryCompassRecipe = getBoolean(alternativeCrafting, "recoveryCompass", enableRecoveryCompassRecipe);
                enableEnderEyeRecipe = getBoolean(alternativeCrafting, "enderEye", enableEnderEyeRecipe);
            } else if ((alternativeCrafting = getObject(legacyObj, "alternativeCrafting")) != null) {
                enableAlternativeRepeaterRecipe = getBoolean(alternativeCrafting, "repeater", enableAlternativeRepeaterRecipe);
                enableChestFromLogsRecipe = getBoolean(alternativeCrafting, "chestFromLogs", enableChestFromLogsRecipe);
                enableMapInkSacRecipe = getBoolean(alternativeCrafting, "mapInkSac", enableMapInkSacRecipe);
                enableRecoveryCompassRecipe = getBoolean(alternativeCrafting, "recoveryCompass", enableRecoveryCompassRecipe);
                enableEnderEyeRecipe = getBoolean(alternativeCrafting, "enderEye", enableEnderEyeRecipe);
            } else {
                enableAlternativeRepeaterRecipe = getBoolean(legacySource, "enableAlternativeRepeaterRecipe", enableAlternativeRepeaterRecipe);
                enableChestFromLogsRecipe = getBoolean(legacySource, "enableChestFromLogsRecipe", enableChestFromLogsRecipe);
                enableMapInkSacRecipe = getBoolean(legacySource, "enableMapInkSacRecipe", enableMapInkSacRecipe);
                enableRecoveryCompassRecipe = getBoolean(legacySource, "enableRecoveryCompassRecipe", enableRecoveryCompassRecipe);
                enableEnderEyeRecipe = getBoolean(legacySource, "enableEnderEyeRecipe", enableEnderEyeRecipe);
            }

            JsonObject rawBlockSmelting = getObject(obj, "rawBlockSmelting");
            if (rawBlockSmelting != null) {
                enableRawIronSmeltingRecipe = getBoolean(rawBlockSmelting, "iron", enableRawIronSmeltingRecipe);
                enableRawGoldSmeltingRecipe = getBoolean(rawBlockSmelting, "gold", enableRawGoldSmeltingRecipe);
                enableRawCopperSmeltingRecipe = getBoolean(rawBlockSmelting, "copper", enableRawCopperSmeltingRecipe);
            } else if ((rawBlockSmelting = getObject(legacyObj, "rawBlockSmelting")) != null) {
                enableRawIronSmeltingRecipe = getBoolean(rawBlockSmelting, "iron", enableRawIronSmeltingRecipe);
                enableRawGoldSmeltingRecipe = getBoolean(rawBlockSmelting, "gold", enableRawGoldSmeltingRecipe);
                enableRawCopperSmeltingRecipe = getBoolean(rawBlockSmelting, "copper", enableRawCopperSmeltingRecipe);
            } else {
                enableRawIronSmeltingRecipe = getBoolean(legacySource, "enableRawIronSmeltingRecipe", enableRawIronSmeltingRecipe);
                enableRawGoldSmeltingRecipe = getBoolean(legacySource, "enableRawGoldSmeltingRecipe", enableRawGoldSmeltingRecipe);
                enableRawCopperSmeltingRecipe = getBoolean(legacySource, "enableRawCopperSmeltingRecipe", enableRawCopperSmeltingRecipe);
            }

            JsonObject rawBlockBlasting = getObject(obj, "rawBlockBlasting");
            if (rawBlockBlasting != null) {
                enableRawIronBlastingRecipe = getBoolean(rawBlockBlasting, "iron", enableRawIronBlastingRecipe);
                enableRawGoldBlastingRecipe = getBoolean(rawBlockBlasting, "gold", enableRawGoldBlastingRecipe);
                enableRawCopperBlastingRecipe = getBoolean(rawBlockBlasting, "copper", enableRawCopperBlastingRecipe);
            } else if ((rawBlockBlasting = getObject(legacyObj, "rawBlockBlasting")) != null) {
                enableRawIronBlastingRecipe = getBoolean(rawBlockBlasting, "iron", enableRawIronBlastingRecipe);
                enableRawGoldBlastingRecipe = getBoolean(rawBlockBlasting, "gold", enableRawGoldBlastingRecipe);
                enableRawCopperBlastingRecipe = getBoolean(rawBlockBlasting, "copper", enableRawCopperBlastingRecipe);
            } else {
                enableRawIronBlastingRecipe = getBoolean(legacySource, "enableRawIronBlastingRecipe", enableRawIronBlastingRecipe);
                enableRawGoldBlastingRecipe = getBoolean(legacySource, "enableRawGoldBlastingRecipe", enableRawGoldBlastingRecipe);
                enableRawCopperBlastingRecipe = getBoolean(legacySource, "enableRawCopperBlastingRecipe", enableRawCopperBlastingRecipe);
            }

            JsonObject stairRecipes = getObject(obj, "stairRecipes");
            if (stairRecipes != null) {
                enableStairRecipeOverride = getBoolean(stairRecipes, "overrideVanillaAndModded", enableStairRecipeOverride);
                enableCompactStairRecipe = getBoolean(stairRecipes, "compact2x2", enableCompactStairRecipe);
            } else if ((stairRecipes = getObject(legacyObj, "stairRecipes")) != null) {
                enableStairRecipeOverride = getBoolean(stairRecipes, "overrideVanillaAndModded", enableStairRecipeOverride);
                enableCompactStairRecipe = getBoolean(stairRecipes, "compact2x2", enableCompactStairRecipe);
            } else {
                enableStairRecipeOverride = getBoolean(legacySource, "enableStairRecipeOverride", enableStairRecipeOverride);
                enableCompactStairRecipe = getBoolean(legacySource, "enableCompactStairRecipe", enableCompactStairRecipe);
            }

            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing migrated {} (continuing)", configPath, e);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed reading {} (continuing with in-memory defaults): {}", configPath, e.toString());
        }
    }

    private static void writeDefaults(Path configPath) throws IOException {
        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);

        JsonObject alternativeCrafting = new JsonObject();
        FabricJsonComments.put(root, "alternativeCrafting", "Alternative crafting recipe toggles.");
        FabricJsonComments.put(alternativeCrafting, "repeater", "Enable an alternate cheaper repeater recipe.");
        FabricJsonComments.put(alternativeCrafting, "chestFromLogs", "Enable crafting chests directly from logs.");
        FabricJsonComments.put(alternativeCrafting, "mapInkSac", "Enable an alternate map recipe using ink sac.");
        FabricJsonComments.put(alternativeCrafting, "recoveryCompass", "Enable a custom recovery compass recipe.");
        FabricJsonComments.put(alternativeCrafting, "enderEye", "Enable a custom Ender Eye recipe.");
        alternativeCrafting.addProperty("repeater", enableAlternativeRepeaterRecipe);
        alternativeCrafting.addProperty("chestFromLogs", enableChestFromLogsRecipe);
        alternativeCrafting.addProperty("mapInkSac", enableMapInkSacRecipe);
        alternativeCrafting.addProperty("recoveryCompass", enableRecoveryCompassRecipe);
        alternativeCrafting.addProperty("enderEye", enableEnderEyeRecipe);
        root.add("alternativeCrafting", alternativeCrafting);

        JsonObject rawBlockSmelting = new JsonObject();
        FabricJsonComments.put(root, "rawBlockSmelting", "Raw metal block smelting recipe toggles.");
        FabricJsonComments.put(rawBlockSmelting, "iron", "Enable smelting raw iron blocks into iron blocks.");
        FabricJsonComments.put(rawBlockSmelting, "gold", "Enable smelting raw gold blocks into gold blocks.");
        FabricJsonComments.put(rawBlockSmelting, "copper", "Enable smelting raw copper blocks into copper blocks.");
        rawBlockSmelting.addProperty("iron", enableRawIronSmeltingRecipe);
        rawBlockSmelting.addProperty("gold", enableRawGoldSmeltingRecipe);
        rawBlockSmelting.addProperty("copper", enableRawCopperSmeltingRecipe);
        root.add("rawBlockSmelting", rawBlockSmelting);

        JsonObject rawBlockBlasting = new JsonObject();
        FabricJsonComments.put(root, "rawBlockBlasting", "Raw metal block blasting recipe toggles.");
        FabricJsonComments.put(rawBlockBlasting, "iron", "Enable blasting raw iron blocks into iron blocks.");
        FabricJsonComments.put(rawBlockBlasting, "gold", "Enable blasting raw gold blocks into gold blocks.");
        FabricJsonComments.put(rawBlockBlasting, "copper", "Enable blasting raw copper blocks into copper blocks.");
        rawBlockBlasting.addProperty("iron", enableRawIronBlastingRecipe);
        rawBlockBlasting.addProperty("gold", enableRawGoldBlastingRecipe);
        rawBlockBlasting.addProperty("copper", enableRawCopperBlastingRecipe);
        root.add("rawBlockBlasting", rawBlockBlasting);

        JsonObject stairRecipes = new JsonObject();
        FabricJsonComments.put(root, "stairRecipes", "Overrides and compact crafting for stair recipes.");
        FabricJsonComments.put(stairRecipes, "overrideVanillaAndModded", "Override all stair recipes to output 8 stairs instead of 4.");
        FabricJsonComments.put(stairRecipes, "compact2x2", "Enable compact 2x2 stair recipes when stair overrides are enabled.");
        stairRecipes.addProperty("overrideVanillaAndModded", enableStairRecipeOverride);
        stairRecipes.addProperty("compact2x2", enableCompactStairRecipe);
        root.add("stairRecipes", stairRecipes);

        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
        if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
            FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
        }
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el instanceof JsonObject o ? o : null;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean fallback) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() ? el.getAsBoolean() : fallback;
    }
}