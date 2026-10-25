package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.onenonly.bitsandbalance.common.mechanics.OreSmeltingXpRuntime;
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
    public static volatile boolean enableEnchantingTableDiamondBlocksRecipe = false;

    public static volatile boolean enableRawIronSmeltingRecipe = true;
    public static volatile boolean enableRawGoldSmeltingRecipe = true;
    public static volatile boolean enableRawCopperSmeltingRecipe = true;

    public static volatile boolean enableRawIronBlastingRecipe = true;
    public static volatile boolean enableRawGoldBlastingRecipe = true;
    public static volatile boolean enableRawCopperBlastingRecipe = true;

    public static volatile boolean enableOreSmeltingXp = true;
    public static volatile double coalOreSmeltingXp = 0.5D;
    public static volatile double lapisOreSmeltingXp = 1.0D;
    public static volatile double netherQuartzOreSmeltingXp = 1.0D;
    public static volatile double ironOreSmeltingXp = 3.5D;
    public static volatile double copperOreSmeltingXp = 3.5D;
    public static volatile double redstoneOreSmeltingXp = 3.5D;
    public static volatile double goldOreSmeltingXp = 5.0D;
    public static volatile double netherGoldOreSmeltingXp = 5.0D;
    public static volatile double diamondOreSmeltingXp = 5.0D;
    public static volatile double emeraldOreSmeltingXp = 5.0D;
    public static volatile double ancientDebrisSmeltingXp = 10.0D;

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
                || enableEnchantingTableDiamondBlocksRecipe
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
                enableEnchantingTableDiamondBlocksRecipe = getBoolean(alternativeCrafting, "enchantingTableDiamondBlocks", enableEnchantingTableDiamondBlocksRecipe);
            } else if ((alternativeCrafting = getObject(legacyObj, "alternativeCrafting")) != null) {
                enableAlternativeRepeaterRecipe = getBoolean(alternativeCrafting, "repeater", enableAlternativeRepeaterRecipe);
                enableChestFromLogsRecipe = getBoolean(alternativeCrafting, "chestFromLogs", enableChestFromLogsRecipe);
                enableMapInkSacRecipe = getBoolean(alternativeCrafting, "mapInkSac", enableMapInkSacRecipe);
                enableRecoveryCompassRecipe = getBoolean(alternativeCrafting, "recoveryCompass", enableRecoveryCompassRecipe);
                enableEnderEyeRecipe = getBoolean(alternativeCrafting, "enderEye", enableEnderEyeRecipe);
                enableEnchantingTableDiamondBlocksRecipe = getBoolean(alternativeCrafting, "enchantingTableDiamondBlocks", enableEnchantingTableDiamondBlocksRecipe);
            } else {
                enableAlternativeRepeaterRecipe = getBoolean(legacySource, "enableAlternativeRepeaterRecipe", enableAlternativeRepeaterRecipe);
                enableChestFromLogsRecipe = getBoolean(legacySource, "enableChestFromLogsRecipe", enableChestFromLogsRecipe);
                enableMapInkSacRecipe = getBoolean(legacySource, "enableMapInkSacRecipe", enableMapInkSacRecipe);
                enableRecoveryCompassRecipe = getBoolean(legacySource, "enableRecoveryCompassRecipe", enableRecoveryCompassRecipe);
                enableEnderEyeRecipe = getBoolean(legacySource, "enableEnderEyeRecipe", enableEnderEyeRecipe);
                enableEnchantingTableDiamondBlocksRecipe = getBoolean(legacySource, "enableEnchantingTableDiamondBlocksRecipe", enableEnchantingTableDiamondBlocksRecipe);
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

            JsonObject oreSmeltingXp = getObject(obj, "oreSmeltingXp");
            if (oreSmeltingXp == null) {
                oreSmeltingXp = getObject(legacyObj, "oreSmeltingXp");
            }
            if (oreSmeltingXp != null) {
                enableOreSmeltingXp = getBoolean(oreSmeltingXp, "enabled", enableOreSmeltingXp);
                coalOreSmeltingXp = getXp(oreSmeltingXp, "coal", coalOreSmeltingXp);
                lapisOreSmeltingXp = getXp(oreSmeltingXp, "lapis", lapisOreSmeltingXp);
                netherQuartzOreSmeltingXp = getXp(oreSmeltingXp, "netherQuartz", netherQuartzOreSmeltingXp);
                ironOreSmeltingXp = getXp(oreSmeltingXp, "iron", ironOreSmeltingXp);
                copperOreSmeltingXp = getXp(oreSmeltingXp, "copper", copperOreSmeltingXp);
                redstoneOreSmeltingXp = getXp(oreSmeltingXp, "redstone", redstoneOreSmeltingXp);
                goldOreSmeltingXp = getXp(oreSmeltingXp, "gold", goldOreSmeltingXp);
                netherGoldOreSmeltingXp = getXp(oreSmeltingXp, "netherGold", netherGoldOreSmeltingXp);
                diamondOreSmeltingXp = getXp(oreSmeltingXp, "diamond", diamondOreSmeltingXp);
                emeraldOreSmeltingXp = getXp(oreSmeltingXp, "emerald", emeraldOreSmeltingXp);
                ancientDebrisSmeltingXp = getXp(oreSmeltingXp, "ancientDebris", ancientDebrisSmeltingXp);
            }

            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing migrated {} (continuing)", configPath, e);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed reading {} (continuing with in-memory defaults): {}", configPath, e.toString());
        }
        syncOreSmeltingXpRuntime();
    }

    private static void writeDefaults(Path configPath) throws IOException {
        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);

        JsonObject alternativeCrafting = new JsonObject();
        FabricJsonComments.put(root, "alternativeCrafting", "Redstone Repeater, Chest from Logs, Map, Recovery Compass, and Ender Eye recipe toggles.");
        FabricJsonComments.put(alternativeCrafting, "repeater", "Enable the alternate Redstone Repeater recipe using sticks and stone.");
        FabricJsonComments.put(alternativeCrafting, "chestFromLogs", "Enable crafting 4 chests directly from logs.");
        FabricJsonComments.put(alternativeCrafting, "mapInkSac", "Enable the alternate Map recipe using an ink sac instead of a compass.");
        FabricJsonComments.put(alternativeCrafting, "recoveryCompass", "Enable the Recovery Compass recipe using echo shards and a compass.");
        FabricJsonComments.put(alternativeCrafting, "enderEye", "Enable the Ender Eye recipe using an echo shard, wind charge, blaze powder, and ender pearl.");
        FabricJsonComments.put(alternativeCrafting, "enchantingTableDiamondBlocks", "Replace the Enchanting Table recipe diamonds with diamond blocks.");
        alternativeCrafting.addProperty("repeater", enableAlternativeRepeaterRecipe);
        alternativeCrafting.addProperty("chestFromLogs", enableChestFromLogsRecipe);
        alternativeCrafting.addProperty("mapInkSac", enableMapInkSacRecipe);
        alternativeCrafting.addProperty("recoveryCompass", enableRecoveryCompassRecipe);
        alternativeCrafting.addProperty("enderEye", enableEnderEyeRecipe);
        alternativeCrafting.addProperty("enchantingTableDiamondBlocks", enableEnchantingTableDiamondBlocksRecipe);
        root.add("alternativeCrafting", alternativeCrafting);

        JsonObject rawBlockSmelting = new JsonObject();
        FabricJsonComments.put(root, "rawBlockSmelting", "Raw Iron, Raw Gold, and Raw Copper Block smelting-to-storage-block recipe toggles.");
        FabricJsonComments.put(rawBlockSmelting, "iron", "Enable smelting Raw Iron Blocks into Iron Blocks.");
        FabricJsonComments.put(rawBlockSmelting, "gold", "Enable smelting Raw Gold Blocks into Gold Blocks.");
        FabricJsonComments.put(rawBlockSmelting, "copper", "Enable smelting Raw Copper Blocks into Copper Blocks.");
        rawBlockSmelting.addProperty("iron", enableRawIronSmeltingRecipe);
        rawBlockSmelting.addProperty("gold", enableRawGoldSmeltingRecipe);
        rawBlockSmelting.addProperty("copper", enableRawCopperSmeltingRecipe);
        root.add("rawBlockSmelting", rawBlockSmelting);

        JsonObject rawBlockBlasting = new JsonObject();
        FabricJsonComments.put(root, "rawBlockBlasting", "Raw Iron, Raw Gold, and Raw Copper Block blasting-to-storage-block recipe toggles.");
        FabricJsonComments.put(rawBlockBlasting, "iron", "Enable blasting Raw Iron Blocks into Iron Blocks.");
        FabricJsonComments.put(rawBlockBlasting, "gold", "Enable blasting Raw Gold Blocks into Gold Blocks.");
        FabricJsonComments.put(rawBlockBlasting, "copper", "Enable blasting Raw Copper Blocks into Copper Blocks.");
        rawBlockBlasting.addProperty("iron", enableRawIronBlastingRecipe);
        rawBlockBlasting.addProperty("gold", enableRawGoldBlastingRecipe);
        rawBlockBlasting.addProperty("copper", enableRawCopperBlastingRecipe);
        root.add("rawBlockBlasting", rawBlockBlasting);

        JsonObject oreSmeltingXp = new JsonObject();
        FabricJsonComments.put(root, "oreSmeltingXp", "Experience awarded when vanilla ore blocks are smelted or blasted. Deepslate ores use the matching normal ore value.");
        FabricJsonComments.put(oreSmeltingXp, "enabled", "Enable configurable smelting and blasting XP for vanilla ore blocks.");
        FabricJsonComments.put(oreSmeltingXp, "coal", "XP for Coal Ore and Deepslate Coal Ore.");
        FabricJsonComments.put(oreSmeltingXp, "lapis", "XP for Lapis Ore and Deepslate Lapis Ore.");
        FabricJsonComments.put(oreSmeltingXp, "netherQuartz", "XP for Nether Quartz Ore.");
        FabricJsonComments.put(oreSmeltingXp, "iron", "XP for Iron Ore and Deepslate Iron Ore.");
        FabricJsonComments.put(oreSmeltingXp, "copper", "XP for Copper Ore and Deepslate Copper Ore.");
        FabricJsonComments.put(oreSmeltingXp, "redstone", "XP for Redstone Ore and Deepslate Redstone Ore.");
        FabricJsonComments.put(oreSmeltingXp, "gold", "XP for Gold Ore and Deepslate Gold Ore.");
        FabricJsonComments.put(oreSmeltingXp, "netherGold", "XP for Nether Gold Ore.");
        FabricJsonComments.put(oreSmeltingXp, "diamond", "XP for Diamond Ore and Deepslate Diamond Ore.");
        FabricJsonComments.put(oreSmeltingXp, "emerald", "XP for Emerald Ore and Deepslate Emerald Ore.");
        FabricJsonComments.put(oreSmeltingXp, "ancientDebris", "XP for Ancient Debris.");
        oreSmeltingXp.addProperty("enabled", enableOreSmeltingXp);
        oreSmeltingXp.addProperty("coal", coalOreSmeltingXp);
        oreSmeltingXp.addProperty("lapis", lapisOreSmeltingXp);
        oreSmeltingXp.addProperty("netherQuartz", netherQuartzOreSmeltingXp);
        oreSmeltingXp.addProperty("iron", ironOreSmeltingXp);
        oreSmeltingXp.addProperty("copper", copperOreSmeltingXp);
        oreSmeltingXp.addProperty("redstone", redstoneOreSmeltingXp);
        oreSmeltingXp.addProperty("gold", goldOreSmeltingXp);
        oreSmeltingXp.addProperty("netherGold", netherGoldOreSmeltingXp);
        oreSmeltingXp.addProperty("diamond", diamondOreSmeltingXp);
        oreSmeltingXp.addProperty("emerald", emeraldOreSmeltingXp);
        oreSmeltingXp.addProperty("ancientDebris", ancientDebrisSmeltingXp);
        root.add("oreSmeltingXp", oreSmeltingXp);

        JsonObject stairRecipes = new JsonObject();
        FabricJsonComments.put(root, "stairRecipes", "Stair output override and compact 2x2 stair crafting recipe toggles.");
        FabricJsonComments.put(stairRecipes, "overrideVanillaAndModded", "Override all stair recipes to output 8 stairs instead of 4.");
        FabricJsonComments.put(stairRecipes, "compact2x2", "Enable compact 2x2 stair recipes for all stair types.");
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

    private static double getXp(JsonObject obj, String key, double fallback) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            return fallback;
        }
        return Math.max(0.0D, Math.min(1000.0D, el.getAsDouble()));
    }

    private static void syncOreSmeltingXpRuntime() {
        OreSmeltingXpRuntime.enabled = enableOreSmeltingXp;
        OreSmeltingXpRuntime.coal = coalOreSmeltingXp;
        OreSmeltingXpRuntime.lapis = lapisOreSmeltingXp;
        OreSmeltingXpRuntime.netherQuartz = netherQuartzOreSmeltingXp;
        OreSmeltingXpRuntime.iron = ironOreSmeltingXp;
        OreSmeltingXpRuntime.copper = copperOreSmeltingXp;
        OreSmeltingXpRuntime.redstone = redstoneOreSmeltingXp;
        OreSmeltingXpRuntime.gold = goldOreSmeltingXp;
        OreSmeltingXpRuntime.netherGold = netherGoldOreSmeltingXp;
        OreSmeltingXpRuntime.diamond = diamondOreSmeltingXp;
        OreSmeltingXpRuntime.emerald = emeraldOreSmeltingXp;
        OreSmeltingXpRuntime.ancientDebris = ancientDebrisSmeltingXp;
    }
}
