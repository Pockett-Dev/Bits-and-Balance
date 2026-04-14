package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimal Fabric-side config for balance features.
 *
 * Config file: config/Bits and Balance/bitsandbalance-balance.json
 */
public final class FabricBalanceConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-balance-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-gameplay.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-balance.json";

    // Ender Dragon XP
    public static volatile boolean enableFullEnderDragonXp = true;

    // Balanced Elytra
    public static volatile boolean enableBalancedElytra = true;
    public static volatile int balancedElytraBlocksPerDurability = 10;

    // Nerfed Mending
    public static volatile boolean enableNerfedMending = true;
    /**
     * How many times more XP Mending should require for the same repair.
     * Higher = worse (slower repairs). Intended range: 2-10.
     */
    public static volatile int nerfedMendingMultiplier = 4;

    // Food Always Edible
    public static volatile boolean enableFoodAlwaysEdible = true;

    // Source Dependent Invulnerability Frames
    public static volatile boolean enableSourceDependentIFrames = true;
    public static volatile java.util.Set<String> sourceIFrameBlacklist = java.util.Set.of();

    // Nerfed Discounts
    public static volatile boolean enableNerfedDiscounts = true;
    public static volatile boolean removeZombieCureDiscounts = false;
    public static volatile double maxVillagerDiscount = 0.5D;

    // Depth Scaling Enemies (mirrors NeoForge BalanceConfig defaults)
    public static volatile boolean dseEnabled = true;
    public static volatile boolean dseOverworldOnly = true;
    public static volatile boolean dseEnableSurfaceTier = false;
    public static volatile boolean dseEnableMidTier = true;
    public static volatile boolean dseEnableDeepTier = true;
    public static volatile int dseMidDepthY = 40;
    public static volatile int dseDeepDepthY = 0;
    public static volatile double dseSurfaceEquipChance = 0.05D;
    public static volatile double dseMidEquipChance = 0.15D;
    public static volatile double dseDeepEquipChance = 0.30D;
    public static volatile double dseSurfaceLeatherWeight = 0.80D;
    public static volatile double dseSurfaceChainWeight = 0.15D;
    public static volatile double dseSurfaceCopperWeight = 0.06D;
    public static volatile double dseSurfaceGoldWeight = 0.05D;
    public static volatile double dseSurfaceIronWeight = 0.00D;
    public static volatile double dseMidLeatherWeight = 0.50D;
    public static volatile double dseMidChainWeight = 0.25D;
    public static volatile double dseMidCopperWeight = 0.06D;
    public static volatile double dseMidGoldWeight = 0.05D;
    public static volatile double dseMidIronWeight = 0.20D;
    public static volatile double dseDeepLeatherWeight = 0.50D;
    public static volatile double dseDeepChainWeight = 0.45D;
    public static volatile double dseDeepCopperWeight = 0.16D;
    public static volatile double dseDeepGoldWeight = 0.15D;
    public static volatile double dseDeepIronWeight = 0.50D;
    public static volatile int dseDeepMaxIronPieces = 1;
    public static volatile double dseSurfaceZombieWeaponChance = 0.05D;
    public static volatile double dseMidZombieWeaponChance = 0.15D;
    public static volatile double dseDeepZombieWeaponChance = 0.30D;

    private FabricBalanceConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
        LOGGER.info("Loaded balance config: nerfedMending.enabled={}, nerfedMending.multiplier={}",
                enableNerfedMending,
                nerfedMendingMultiplier);
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        JsonObject obj = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject legacyObj = FabricJsonConfigFiles.readRoot(LEGACY_FILE_NAME, GSON);

        try {
            JsonObject balanced = getObject(obj, "balancedElytra");
            if (balanced != null) {
                enableBalancedElytra = getBoolean(balanced, "enabled", enableBalancedElytra);
                balancedElytraBlocksPerDurability = clampInt(getInt(balanced, "blocksPerDurability", balancedElytraBlocksPerDurability), 1, 1000);
            } else if ((balanced = getObject(legacyObj, "balancedElytra")) != null) {
                enableBalancedElytra = getBoolean(balanced, "enabled", enableBalancedElytra);
                balancedElytraBlocksPerDurability = clampInt(getInt(balanced, "blocksPerDurability", balancedElytraBlocksPerDurability), 1, 1000);
            } else {
                enableBalancedElytra = getBoolean(legacyObj, "enableBalancedElytra", enableBalancedElytra);
                balancedElytraBlocksPerDurability = clampInt(getInt(legacyObj, "balancedElytraBlocksPerDurability", balancedElytraBlocksPerDurability), 1, 1000);
            }

            JsonObject dragonXp = getObject(obj, "enderDragonXp");
            if (dragonXp != null) {
                enableFullEnderDragonXp = getBoolean(dragonXp, "enableFullEnderDragonXp", enableFullEnderDragonXp);
                // Also support a simpler 'enabled' key.
                enableFullEnderDragonXp = getBoolean(dragonXp, "enabled", enableFullEnderDragonXp);
            } else if ((dragonXp = getObject(legacyObj, "enderDragonXp")) != null) {
                enableFullEnderDragonXp = getBoolean(dragonXp, "enableFullEnderDragonXp", enableFullEnderDragonXp);
                enableFullEnderDragonXp = getBoolean(dragonXp, "enabled", enableFullEnderDragonXp);
            } else {
                enableFullEnderDragonXp = getBoolean(legacyObj, "enableFullEnderDragonXp", enableFullEnderDragonXp);
            }

            JsonObject foodAlwaysEdible = getObject(obj, "foodAlwaysEdible");
            if (foodAlwaysEdible != null) {
                enableFoodAlwaysEdible = getBoolean(foodAlwaysEdible, "enabled", enableFoodAlwaysEdible);
            } else if ((foodAlwaysEdible = getObject(legacyObj, "foodAlwaysEdible")) != null) {
                enableFoodAlwaysEdible = getBoolean(foodAlwaysEdible, "enabled", enableFoodAlwaysEdible);
            } else {
                enableFoodAlwaysEdible = getBoolean(legacyObj, "enableFoodAlwaysEdible", enableFoodAlwaysEdible);
            }

            JsonObject nerfedMending = getObject(obj, "nerfedMending");
            if (nerfedMending != null) {
                enableNerfedMending = getBoolean(nerfedMending, "enabled", enableNerfedMending);
                nerfedMendingMultiplier = clampInt(getInt(nerfedMending, "multiplier", nerfedMendingMultiplier), 2, 10);
            } else if ((nerfedMending = getObject(legacyObj, "nerfedMending")) != null) {
                enableNerfedMending = getBoolean(nerfedMending, "enabled", enableNerfedMending);
                nerfedMendingMultiplier = clampInt(getInt(nerfedMending, "multiplier", nerfedMendingMultiplier), 2, 10);
            } else {
                enableNerfedMending = getBoolean(legacyObj, "enableNerfedMending", enableNerfedMending);
                nerfedMendingMultiplier = clampInt(getInt(legacyObj, "nerfedMendingMultiplier", nerfedMendingMultiplier), 2, 10);
            }

            JsonObject iframes = getObject(obj, "sourceDependentIFrames");
            if (iframes != null) {
                enableSourceDependentIFrames = getBoolean(iframes, "enabled", enableSourceDependentIFrames);
                sourceIFrameBlacklist = getStringSet(iframes, "blacklist", sourceIFrameBlacklist);
            } else if ((iframes = getObject(legacyObj, "sourceDependentIFrames")) != null) {
                enableSourceDependentIFrames = getBoolean(iframes, "enabled", enableSourceDependentIFrames);
                sourceIFrameBlacklist = getStringSet(iframes, "blacklist", sourceIFrameBlacklist);
            } else {
                enableSourceDependentIFrames = getBoolean(legacyObj, "enableSourceDependentIFrames", enableSourceDependentIFrames);
                sourceIFrameBlacklist = getStringSet(legacyObj, "sourceIFrameBlacklist", sourceIFrameBlacklist);
            }

            JsonObject nerfedDiscounts = getObject(obj, "nerfedDiscounts");
            if (nerfedDiscounts != null) {
                enableNerfedDiscounts = getBoolean(nerfedDiscounts, "enabled", enableNerfedDiscounts);
                removeZombieCureDiscounts = getBoolean(nerfedDiscounts, "removeZombieCureDiscounts", removeZombieCureDiscounts);
                maxVillagerDiscount = getDouble(nerfedDiscounts, "maxDiscount", maxVillagerDiscount);
            } else if ((nerfedDiscounts = getObject(legacyObj, "nerfedDiscounts")) != null) {
                enableNerfedDiscounts = getBoolean(nerfedDiscounts, "enabled", enableNerfedDiscounts);
                removeZombieCureDiscounts = getBoolean(nerfedDiscounts, "removeZombieCureDiscounts", removeZombieCureDiscounts);
                maxVillagerDiscount = getDouble(nerfedDiscounts, "maxDiscount", maxVillagerDiscount);
            } else {
                enableNerfedDiscounts = getBoolean(legacyObj, "enableNerfedDiscounts", enableNerfedDiscounts);
                removeZombieCureDiscounts = getBoolean(legacyObj, "removeZombieCureDiscounts", removeZombieCureDiscounts);
                maxVillagerDiscount = getDouble(legacyObj, "maxVillagerDiscount", maxVillagerDiscount);
            }

            JsonObject dse = getObject(obj, "depthScalingEnemies");
            if (dse == null) {
                dse = getObject(legacyObj, "depthScalingEnemies");
            }
            if (dse != null) {
                dseEnabled = getBoolean(dse, "enabled", dseEnabled);
                dseOverworldOnly = getBoolean(dse, "overworldOnly", dseOverworldOnly);
                dseEnableSurfaceTier = getBoolean(dse, "enableSurfaceTier", dseEnableSurfaceTier);
                dseEnableMidTier = getBoolean(dse, "enableMidTier", dseEnableMidTier);
                dseEnableDeepTier = getBoolean(dse, "enableDeepTier", dseEnableDeepTier);
                dseMidDepthY = getInt(dse, "midDepthY", dseMidDepthY);
                dseDeepDepthY = getInt(dse, "deepDepthY", dseDeepDepthY);

                dseSurfaceEquipChance = getDouble(dse, "surfaceEquipChance", dseSurfaceEquipChance);
                dseMidEquipChance = getDouble(dse, "midEquipChance", dseMidEquipChance);
                dseDeepEquipChance = getDouble(dse, "deepEquipChance", dseDeepEquipChance);

                dseSurfaceLeatherWeight = getDouble(dse, "surfaceLeatherWeight", dseSurfaceLeatherWeight);
                dseSurfaceChainWeight = getDouble(dse, "surfaceChainWeight", dseSurfaceChainWeight);
                dseSurfaceCopperWeight = getDouble(dse, "surfaceCopperWeight", dseSurfaceCopperWeight);
                dseSurfaceGoldWeight = getDouble(dse, "surfaceGoldWeight", dseSurfaceGoldWeight);
                dseSurfaceIronWeight = getDouble(dse, "surfaceIronWeight", dseSurfaceIronWeight);

                dseMidLeatherWeight = getDouble(dse, "midLeatherWeight", dseMidLeatherWeight);
                dseMidChainWeight = getDouble(dse, "midChainWeight", dseMidChainWeight);
                dseMidCopperWeight = getDouble(dse, "midCopperWeight", dseMidCopperWeight);
                dseMidGoldWeight = getDouble(dse, "midGoldWeight", dseMidGoldWeight);
                dseMidIronWeight = getDouble(dse, "midIronWeight", dseMidIronWeight);

                dseDeepLeatherWeight = getDouble(dse, "deepLeatherWeight", dseDeepLeatherWeight);
                dseDeepChainWeight = getDouble(dse, "deepChainWeight", dseDeepChainWeight);
                dseDeepCopperWeight = getDouble(dse, "deepCopperWeight", dseDeepCopperWeight);
                dseDeepGoldWeight = getDouble(dse, "deepGoldWeight", dseDeepGoldWeight);
                dseDeepIronWeight = getDouble(dse, "deepIronWeight", dseDeepIronWeight);
                dseDeepMaxIronPieces = getInt(dse, "deepMaxIronPieces", dseDeepMaxIronPieces);

                dseSurfaceZombieWeaponChance = getDouble(dse, "surfaceZombieWeaponChance", dseSurfaceZombieWeaponChance);
                dseMidZombieWeaponChance = getDouble(dse, "midZombieWeaponChance", dseMidZombieWeaponChance);
                dseDeepZombieWeaponChance = getDouble(dse, "deepZombieWeaponChance", dseDeepZombieWeaponChance);
            }

            maxVillagerDiscount = clamp01(maxVillagerDiscount);

            dseSurfaceEquipChance = clamp01(dseSurfaceEquipChance);
            dseMidEquipChance = clamp01(dseMidEquipChance);
            dseDeepEquipChance = clamp01(dseDeepEquipChance);

            dseSurfaceZombieWeaponChance = clamp01(dseSurfaceZombieWeaponChance);
            dseMidZombieWeaponChance = clamp01(dseMidZombieWeaponChance);
            dseDeepZombieWeaponChance = clamp01(dseDeepZombieWeaponChance);

            dseSurfaceLeatherWeight = clampWeight(dseSurfaceLeatherWeight);
            dseSurfaceChainWeight = clampWeight(dseSurfaceChainWeight);
            dseSurfaceCopperWeight = clampWeight(dseSurfaceCopperWeight);
            dseSurfaceGoldWeight = clampWeight(dseSurfaceGoldWeight);
            dseSurfaceIronWeight = clampWeight(dseSurfaceIronWeight);

            dseMidLeatherWeight = clampWeight(dseMidLeatherWeight);
            dseMidChainWeight = clampWeight(dseMidChainWeight);
            dseMidCopperWeight = clampWeight(dseMidCopperWeight);
            dseMidGoldWeight = clampWeight(dseMidGoldWeight);
            dseMidIronWeight = clampWeight(dseMidIronWeight);

            dseDeepLeatherWeight = clampWeight(dseDeepLeatherWeight);
            dseDeepChainWeight = clampWeight(dseDeepChainWeight);
            dseDeepCopperWeight = clampWeight(dseDeepCopperWeight);
            dseDeepGoldWeight = clampWeight(dseDeepGoldWeight);
            dseDeepIronWeight = clampWeight(dseDeepIronWeight);

            if (dseDeepMaxIronPieces < 0) dseDeepMaxIronPieces = 0;
            if (dseDeepMaxIronPieces > 4) dseDeepMaxIronPieces = 4;

            // Rewrite config to include any newly added keys with current values.
            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing migrated {} (continuing)", configPath, e);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed reading {} (continuing with in-memory defaults)", configPath, e);
        }
    }

    private static void writeDefaults(Path configPath) throws IOException {
        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);
        FabricJsonComments.put(root, "_file", "Bits and Balance (Fabric) balance configuration.");

        JsonObject dragonXp = new JsonObject();
        FabricJsonComments.put(dragonXp, "enableFullEnderDragonXp", "If true, the Ender Dragon drops its full XP value.");
        dragonXp.addProperty("enableFullEnderDragonXp", enableFullEnderDragonXp);
        root.add("enderDragonXp", dragonXp);
        JsonObject balanced = new JsonObject();
        FabricJsonComments.put(balanced, "enabled", "If true, enables Balanced Elytra: elytras lose durability based on distance flown (10 blocks = 1 durability) instead of time.");
        FabricJsonComments.put(balanced, "blocksPerDurability", "How many horizontal blocks flown equals 1 durability point (default: 10). Lower = faster wear.");
        balanced.addProperty("enabled", enableBalancedElytra);
        balanced.addProperty("blocksPerDurability", clampInt(balancedElytraBlocksPerDurability, 1, 1000));
        root.add("balancedElytra", balanced);

        JsonObject foodAlwaysEdible = new JsonObject();
        FabricJsonComments.put(foodAlwaysEdible, "enabled", "If true, allows eating food even when not hungry.");
        foodAlwaysEdible.addProperty("enabled", enableFoodAlwaysEdible);
        root.add("foodAlwaysEdible", foodAlwaysEdible);

        JsonObject nerfedMending = new JsonObject();
        FabricJsonComments.put(nerfedMending, "enabled", "If true, nerfs Mending by increasing XP required per durability repaired.");
        FabricJsonComments.put(nerfedMending, "multiplier", "XP multiplier for Mending repairs (higher = slower repairs). Intended range: 2-10.");
        nerfedMending.addProperty("enabled", enableNerfedMending);
        nerfedMending.addProperty("multiplier", clampInt(nerfedMendingMultiplier, 2, 10));
        root.add("nerfedMending", nerfedMending);

        JsonObject iframes = new JsonObject();
        FabricJsonComments.put(iframes, "enabled", "If true, gives different damage sources independent victim i-frame timers instead of fully disabling cooldowns.");
        FabricJsonComments.put(iframes, "blacklist", "Damage sources (resource locations) to exclude from source-dependent I-frames.");
        iframes.addProperty("enabled", enableSourceDependentIFrames);
        com.google.gson.JsonArray blacklist = new com.google.gson.JsonArray();
        if (sourceIFrameBlacklist != null) {
            for (String s : sourceIFrameBlacklist) {
                if (s != null && !s.isBlank()) {
                    blacklist.add(s);
                }
            }
        }
        iframes.add("blacklist", blacklist);
        root.add("sourceDependentIFrames", iframes);

        JsonObject nerfedDiscounts = new JsonObject();
        FabricJsonComments.put(nerfedDiscounts, "enabled", "If true, nerfs villager discounts.");
        FabricJsonComments.put(nerfedDiscounts, "removeZombieCureDiscounts", "If true, removes villager discounts from curing zombie villagers.");
        FabricJsonComments.put(nerfedDiscounts, "maxDiscount", "Maximum total villager discount (0.0-1.0). 0.5 = 50% off.");
        nerfedDiscounts.addProperty("enabled", enableNerfedDiscounts);
        nerfedDiscounts.addProperty("removeZombieCureDiscounts", removeZombieCureDiscounts);
        nerfedDiscounts.addProperty("maxDiscount", clamp01(maxVillagerDiscount));
        root.add("nerfedDiscounts", nerfedDiscounts);

        JsonObject dse = new JsonObject();
        FabricJsonComments.put(dse, "enabled", "If true, enables Depth Scaling Enemies.");
        FabricJsonComments.put(dse, "overworldOnly", "If true, applies Depth Scaling Enemies only in the Overworld.");
        FabricJsonComments.put(dse, "enableSurfaceTier", "If false, the surface tier is disabled (no depth-scaling changes for surface-band spawns). Useful to keep surface mobs vanilla.");
        FabricJsonComments.put(dse, "enableMidTier", "If false, the mid tier is disabled (no depth-scaling changes for mid-depth spawns).");
        FabricJsonComments.put(dse, "enableDeepTier", "If false, the deep tier is disabled (no depth-scaling changes for deep spawns).");
        FabricJsonComments.put(dse, "midDepthY", "Y level considered 'mid depth' for scaling calculations.");
        FabricJsonComments.put(dse, "deepDepthY", "Y level considered 'deep depth' for scaling calculations.");
        FabricJsonComments.put(dse, "surfaceEquipChance", "Chance (0.0-1.0) for mobs to spawn with equipment near the surface.");
        FabricJsonComments.put(dse, "midEquipChance", "Chance (0.0-1.0) for mobs to spawn with equipment at mid depth.");
        FabricJsonComments.put(dse, "deepEquipChance", "Chance (0.0-1.0) for mobs to spawn with equipment at deep depth.");
        FabricJsonComments.put(dse, "surfaceLeatherWeight", "Relative weight for leather gear near the surface.");
        FabricJsonComments.put(dse, "surfaceChainWeight", "Relative weight for chain gear near the surface.");
        FabricJsonComments.put(dse, "surfaceCopperWeight", "Relative weight for copper gear near the surface.");
        FabricJsonComments.put(dse, "surfaceGoldWeight", "Relative weight for gold gear near the surface.");
        FabricJsonComments.put(dse, "surfaceIronWeight", "Relative weight for iron gear near the surface.");
        FabricJsonComments.put(dse, "midLeatherWeight", "Relative weight for leather gear at mid depth.");
        FabricJsonComments.put(dse, "midChainWeight", "Relative weight for chain gear at mid depth.");
        FabricJsonComments.put(dse, "midCopperWeight", "Relative weight for copper gear at mid depth.");
        FabricJsonComments.put(dse, "midGoldWeight", "Relative weight for gold gear at mid depth.");
        FabricJsonComments.put(dse, "midIronWeight", "Relative weight for iron gear at mid depth.");
        FabricJsonComments.put(dse, "deepLeatherWeight", "Relative weight for leather gear at deep depth.");
        FabricJsonComments.put(dse, "deepChainWeight", "Relative weight for chain gear at deep depth.");
        FabricJsonComments.put(dse, "deepCopperWeight", "Relative weight for copper gear at deep depth.");
        FabricJsonComments.put(dse, "deepGoldWeight", "Relative weight for gold gear at deep depth.");
        FabricJsonComments.put(dse, "deepIronWeight", "Relative weight for iron gear at deep depth.");
        FabricJsonComments.put(dse, "deepMaxIronPieces", "Maximum number of iron armor pieces a deep mob can spawn with.");
        FabricJsonComments.put(dse, "surfaceZombieWeaponChance", "Chance (0.0-1.0) for zombies to spawn with weapons near the surface.");
        FabricJsonComments.put(dse, "midZombieWeaponChance", "Chance (0.0-1.0) for zombies to spawn with weapons at mid depth.");
        FabricJsonComments.put(dse, "deepZombieWeaponChance", "Chance (0.0-1.0) for zombies to spawn with weapons at deep depth.");
        dse.addProperty("enabled", dseEnabled);
        dse.addProperty("overworldOnly", dseOverworldOnly);
        dse.addProperty("enableSurfaceTier", dseEnableSurfaceTier);
        dse.addProperty("enableMidTier", dseEnableMidTier);
        dse.addProperty("enableDeepTier", dseEnableDeepTier);
        dse.addProperty("midDepthY", dseMidDepthY);
        dse.addProperty("deepDepthY", dseDeepDepthY);

        dse.addProperty("surfaceEquipChance", clamp01(dseSurfaceEquipChance));
        dse.addProperty("midEquipChance", clamp01(dseMidEquipChance));
        dse.addProperty("deepEquipChance", clamp01(dseDeepEquipChance));

        dse.addProperty("surfaceLeatherWeight", clampWeight(dseSurfaceLeatherWeight));
        dse.addProperty("surfaceChainWeight", clampWeight(dseSurfaceChainWeight));
        dse.addProperty("surfaceCopperWeight", clampWeight(dseSurfaceCopperWeight));
        dse.addProperty("surfaceGoldWeight", clampWeight(dseSurfaceGoldWeight));
        dse.addProperty("surfaceIronWeight", clampWeight(dseSurfaceIronWeight));

        dse.addProperty("midLeatherWeight", clampWeight(dseMidLeatherWeight));
        dse.addProperty("midChainWeight", clampWeight(dseMidChainWeight));
        dse.addProperty("midCopperWeight", clampWeight(dseMidCopperWeight));
        dse.addProperty("midGoldWeight", clampWeight(dseMidGoldWeight));
        dse.addProperty("midIronWeight", clampWeight(dseMidIronWeight));

        dse.addProperty("deepLeatherWeight", clampWeight(dseDeepLeatherWeight));
        dse.addProperty("deepChainWeight", clampWeight(dseDeepChainWeight));
        dse.addProperty("deepCopperWeight", clampWeight(dseDeepCopperWeight));
        dse.addProperty("deepGoldWeight", clampWeight(dseDeepGoldWeight));
        dse.addProperty("deepIronWeight", clampWeight(dseDeepIronWeight));
        dse.addProperty("deepMaxIronPieces", Math.max(0, Math.min(4, dseDeepMaxIronPieces)));

        dse.addProperty("surfaceZombieWeaponChance", clamp01(dseSurfaceZombieWeaponChance));
        dse.addProperty("midZombieWeaponChance", clamp01(dseMidZombieWeaponChance));
        dse.addProperty("deepZombieWeaponChance", clamp01(dseDeepZombieWeaponChance));
        root.add("depthScalingEnemies", dse);

        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
        if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
            FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
        }
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        return el instanceof JsonObject o ? o : null;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isBoolean() ? el.getAsBoolean() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsInt() : def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isNumber() ? el.getAsDouble() : def;
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v)) return 0.0D;
        if (v < 0.0D) return 0.0D;
        if (v > 1.0D) return 1.0D;
        return v;
    }

    private static int clampInt(int v, int min, int max) {
        if (v < min) return min;
        if (v > max) return max;
        return v;
    }

    private static double clampWeight(double v) {
        if (Double.isNaN(v)) return 0.0D;
        if (v < 0.0D) return 0.0D;
        if (v > 100.0D) return 100.0D;
        return v;
    }

    private static java.util.Set<String> getStringSet(JsonObject obj, String key, java.util.Set<String> def) {
        JsonElement el = obj.get(key);
        if (el == null) return def;

        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        try {
            if (el.isJsonArray()) {
                for (JsonElement e : el.getAsJsonArray()) {
                    if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                        String s = e.getAsString();
                        if (s != null && !s.isBlank()) {
                            out.add(s);
                        }
                    }
                }
                return java.util.Collections.unmodifiableSet(out);
            }
        } catch (Throwable ignored) {
        }

        return def;
    }
}
