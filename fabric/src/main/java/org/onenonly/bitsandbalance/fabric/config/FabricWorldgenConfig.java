package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.resources.Identifier;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Minimal Fabric-side worldgen gating for remaining {@code bitsandbalance:config_enabled} placements.
 *
 * Config file: config/Bits and Balance/bitsandbalance-worldgen.json
 * Format:
 * {
 *   "disabled": ["bitsandbalance:nether_gold_vein"]
 * }
 */
public final class FabricWorldgenConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-worldgen-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-worldgen.json";

    private static volatile Set<Identifier> disabledKeys = Set.of();

    private static volatile boolean enableNetherGoldVeins = true;
    private static volatile boolean enableNetherQuartzVeins = true;
    private static volatile boolean enableRawQuartzBlockInQuartzVeins = true;
    private static volatile boolean enableRawQuartzBlock = true;

    // Mojang-style coal veins (implemented via OreVeinifier mixin)
    // Split into two independently-toggleable variants.
    private static volatile boolean enableCoalAndesiteVeins = true;
    private static volatile boolean enableCoalTuffVeins = true;
    private static volatile boolean debugVeins = false;
    private static volatile boolean disableNetherLavaSprings = false;
    private static volatile boolean keepExposedNetherLavaSprings = true;

    private static volatile boolean enableOreVariants = true;
    private static volatile boolean enableParallelProcessing = false;
    private static volatile int parallelProcessingThreadCount = 0;
    private static volatile boolean enableAzaleaWoodGeneration = true;
    private static volatile boolean enableAzaleaWoodset = true;

    private FabricWorldgenConfig() {
    }

    public static void init() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);
        load(configPath);

        LOGGER.info("Loaded worldgen config: {} (enableCoalAndesiteVeins={}, enableCoalTuffVeins={}, debugVeins={}, enableParallelProcessing={}, threadCount={})",
                configPath,
            enableCoalAndesiteVeins,
            enableCoalTuffVeins,
            debugVeins,
            enableParallelProcessing,
            parallelProcessingThreadCount);

        BitsAndBalanceCommon.setOreVariantsEnabled(enableOreVariants);
        BitsAndBalanceCommon.setParallelWorldgenProcessingEnabled(enableParallelProcessing);
        BitsAndBalanceCommon.setParallelWorldgenThreadCount(parallelProcessingThreadCount);
        BitsAndBalanceCommon.setVeinsDebugEnabled(debugVeins);
        BitsAndBalanceCommon.setDisableNetherLavaSprings(disableNetherLavaSprings);
        BitsAndBalanceCommon.setKeepExposedNetherLavaSprings(keepExposedNetherLavaSprings);
        if (!enableRawQuartzBlock) enableRawQuartzBlockInQuartzVeins = false;
        BitsAndBalanceCommon.setEnableRawQuartzBlockInQuartzVeins(enableRawQuartzBlockInQuartzVeins);
        BitsAndBalanceCommon.setWorldgenEnabledPredicate(FabricWorldgenConfig::isEnabled);
    }

    public static boolean isEnabled(Identifier key) {
        if (disabledKeys.contains(key)) {
            return false;
        }

        if (!BitsAndBalanceCommon.MOD_ID.equals(key.getNamespace())) {
            return true;
        }

        // Coal veins are no longer exposed as datapack-gated placed features. Their toggles are
        // consumed directly by the OreVeinifier mixin, so only the remaining placed-feature keys
        // stay in this shared predicate.
        return switch (key.getPath()) {
            case "nether_gold_vein" -> enableNetherGoldVeins;
            case "nether_quartz_vein" -> enableNetherQuartzVeins;
            // Spring suppression is handled by the bitsandbalance:air_exposure_filter modifier so we can
            // selectively keep air-exposed springs when disableNetherLavaSprings=true.
            case "nether_lava_springs" -> true;
            default -> true;
        };
    }

    public static boolean isOreVariantsEnabled() {
        return enableOreVariants;
    }

    public static boolean isParallelProcessingEnabled() {
        return enableParallelProcessing;
    }

    public static int getParallelProcessingThreadCount() {
        return parallelProcessingThreadCount;
    }

    public static boolean isAzaleaWoodGenerationEnabled() {
        return enableAzaleaWoodGeneration;
    }

    public static boolean isAzaleaWoodsetEnabled() {
        return enableAzaleaWoodset;
    }

    public static boolean isDogMusicDiscEnabled() {
        return FabricLootConfig.enableDogMusicDisc;
    }

    public static boolean isCoalAndesiteVeinsEnabled() {
        return enableCoalAndesiteVeins;
    }

    public static boolean isCoalTuffVeinsEnabled() {
        return enableCoalTuffVeins;
    }

    public static boolean isAnyMojangStyleCoalVeinsEnabled() {
        return enableCoalAndesiteVeins || enableCoalTuffVeins;
    }

    public static boolean isVeinsDebugEnabled() {
        return debugVeins;
    }

    private static void load(Path configPath) {

        if (!Files.exists(configPath)) {
            disabledKeys = Set.of();
            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing default {} (continuing with in-memory defaults)", configPath, e);
            }
            return;
        }

        try {
            String raw = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonElement element = GSON.fromJson(raw, JsonElement.class);
            if (!(element instanceof JsonObject obj)) {
                throw new JsonParseException("Expected JSON object");
            }

            // Optional simple booleans for parity with NeoForge's world.toml flags.
            // Defaults are chosen to match NeoForge defaults.
            JsonObject veins = getObject(obj, "veins");
            if (veins != null) {
                enableNetherGoldVeins = getBoolean(veins, "enableNetherGoldVeins", enableNetherGoldVeins);
                enableNetherQuartzVeins = getBoolean(veins, "enableNetherQuartzVeins", enableNetherQuartzVeins);
                enableRawQuartzBlockInQuartzVeins = getBoolean(veins, "enableRawQuartzBlockInQuartzVeins", enableRawQuartzBlockInQuartzVeins);
                enableCoalAndesiteVeins = getBoolean(veins, "enableCoalAndesiteVeins", enableCoalAndesiteVeins);
                enableCoalTuffVeins = getBoolean(veins, "enableCoalTuffVeins", enableCoalTuffVeins);
                debugVeins = getBoolean(veins, "debugVeins", debugVeins);

                // Back-compat
                enableCoalAndesiteVeins = getBoolean(veins, "enableCoalAndesiteVein", enableCoalAndesiteVeins);
            } else {
                enableNetherGoldVeins = getBoolean(obj, "enableNetherGoldVeins", enableNetherGoldVeins);
                enableNetherQuartzVeins = getBoolean(obj, "enableNetherQuartzVeins", enableNetherQuartzVeins);
                enableRawQuartzBlockInQuartzVeins = getBoolean(obj, "enableRawQuartzBlockInQuartzVeins", enableRawQuartzBlockInQuartzVeins);
                enableCoalAndesiteVeins = getBoolean(obj, "enableCoalAndesiteVeins", enableCoalAndesiteVeins);
                enableCoalTuffVeins = getBoolean(obj, "enableCoalTuffVeins", enableCoalTuffVeins);
                debugVeins = getBoolean(obj, "debugVeins", debugVeins);

                // Back-compat
                enableCoalAndesiteVeins = getBoolean(obj, "enableCoalAndesiteVein", enableCoalAndesiteVeins);
            }

            JsonObject rawQuartzBlockObj = getObject(obj, "rawQuartzBlock");
            if (rawQuartzBlockObj != null) {
                enableRawQuartzBlock = getBoolean(rawQuartzBlockObj, "enabled", enableRawQuartzBlock);
            } else {
                enableRawQuartzBlock = getBoolean(obj, "enableRawQuartzBlock", enableRawQuartzBlock);
            }

            JsonObject nether = getObject(obj, "nether");
            if (nether != null) {
                disableNetherLavaSprings = getBoolean(nether, "disableNetherLavaSprings", disableNetherLavaSprings);
                keepExposedNetherLavaSprings = getBoolean(nether, "keepExposedNetherLavaSprings", keepExposedNetherLavaSprings);
            } else {
                disableNetherLavaSprings = getBoolean(obj, "disableNetherLavaSprings", disableNetherLavaSprings);
                keepExposedNetherLavaSprings = getBoolean(obj, "keepExposedNetherLavaSprings", keepExposedNetherLavaSprings);
            }

            JsonObject oreVariants = getObject(obj, "oreVariants");
            if (oreVariants != null) {
                enableOreVariants = getBoolean(oreVariants, "enabled", enableOreVariants);
            } else {
                enableOreVariants = getBoolean(obj, "enableOreVariants", enableOreVariants);
            }

            JsonObject processing = getObject(obj, "processing");
            if (processing != null) {
                enableParallelProcessing = getBoolean(processing, "enableParallelProcessing", enableParallelProcessing);
                parallelProcessingThreadCount = getInt(processing, "threadCount", parallelProcessingThreadCount, 0, 64);
            } else {
                enableParallelProcessing = getBoolean(obj, "enableParallelProcessing", enableParallelProcessing);
                parallelProcessingThreadCount = getInt(obj, "threadCount", parallelProcessingThreadCount, 0, 64);
            }

            JsonObject azaleaWood = getObject(obj, "azaleaWood");
            if (azaleaWood != null) {
                enableAzaleaWoodGeneration = getBoolean(azaleaWood, "enableGeneration", enableAzaleaWoodGeneration);
                enableAzaleaWoodset = getBoolean(azaleaWood, "enableWoodset", enableAzaleaWoodset);
            } else {
                enableAzaleaWoodGeneration = getBoolean(obj, "enableAzaleaWoodGeneration", enableAzaleaWoodGeneration);
                enableAzaleaWoodset = getBoolean(obj, "enableAzaleaWoodset", enableAzaleaWoodset);
            }

            // Back-compat: keep reading the old dev/test key if present.
            // Mojang-style coal is now controlled by enableCoalAndesiteVeins/enableCoalTuffVeins.
            JsonObject dev = getObject(obj, "dev");
            if (dev != null) {
                // Back-compat: old single toggle enabled both variants.
                boolean legacyMojangCoal = getBoolean(dev, "enableTestMojangStyleCoalVeins", false);
                if (legacyMojangCoal) {
                    enableCoalAndesiteVeins = true;
                    enableCoalTuffVeins = true;
                }
            }
            // Back-compat: old root key enabled both variants.
            boolean legacyMojangCoal = getBoolean(obj, "enableMojangStyleCoalVeins", false);
            if (legacyMojangCoal) {
                enableCoalAndesiteVeins = true;
                enableCoalTuffVeins = true;
            }

            if (!enableAzaleaWoodset) {
                enableAzaleaWoodGeneration = false;
            }

            // Keep shared state in sync for common worldgen features.
            BitsAndBalanceCommon.setOreVariantsEnabled(enableOreVariants);
            BitsAndBalanceCommon.setParallelWorldgenProcessingEnabled(enableParallelProcessing);
            BitsAndBalanceCommon.setParallelWorldgenThreadCount(parallelProcessingThreadCount);

            Set<Identifier> disabled = new HashSet<>();
            JsonElement disabledEl = obj.get("disabled");
            if (disabledEl != null && disabledEl.isJsonArray()) {
                for (JsonElement entry : disabledEl.getAsJsonArray()) {
                    if (entry != null && entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                        disabled.add(Identifier.parse(entry.getAsString()));
                    }
                }
            }

            disabledKeys = Set.copyOf(disabled);
            if (!disabledKeys.isEmpty()) {
                LOGGER.info("Worldgen disabled keys: {}", disabledKeys);
            }

            // Rewrite config to include any newly added keys with current values.
            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing migrated {} (continuing)", configPath, e);
            }
        } catch (IOException | RuntimeException e) {
            disabledKeys = Set.of();
            BitsAndBalanceCommon.setOreVariantsEnabled(true);
            LOGGER.warn("Failed reading {} (defaulting to all-enabled)", configPath, e);
        }
    }

    private static void writeDefaults(Path configPath) throws IOException {
        Files.createDirectories(configPath.getParent());

        JsonObject root = new JsonObject();

        FabricJsonComments.put(root, "veins", "Ore vein / structure worldgen toggles.");
        FabricJsonComments.put(root, "nether", "Nether-specific worldgen toggles.");
        FabricJsonComments.put(root, "oreVariants", "Common ore-variant placement toggles.");
        FabricJsonComments.put(root, "processing", "Worldgen processing behavior toggles.");
        FabricJsonComments.put(root, "azaleaWood", "Azalea wood worldgen and woodset toggles.");
        FabricJsonComments.put(root, "disabled", "List of resource keys to forcibly disable (takes precedence over other toggles). Example: \"bitsandbalance:nether_gold_vein\".");

        JsonObject veins = new JsonObject();

        FabricJsonComments.put(veins, "enableNetherGoldVeins", "Enable nether gold vein worldgen.");
        FabricJsonComments.put(veins, "enableNetherQuartzVeins", "Enable nether quartz vein worldgen.");
        FabricJsonComments.put(veins, "enableRawQuartzBlockInQuartzVeins", "If false, nether quartz veins will never place bitsandbalance:raw_quartz_block.");
        FabricJsonComments.put(veins, "enableCoalAndesiteVeins", "Enable Mojang-style shallow coal+andesite veins (OreVeinifier-style). New chunks only.");
        FabricJsonComments.put(veins, "enableCoalTuffVeins", "Enable Mojang-style deep coal veins with a deepslate-biased 65/35 mix of deepslate and tuff filler, mixed tuff/deepslate coal ore, and occasional coal blocks (OreVeinifier-style). New chunks only.");
        FabricJsonComments.put(veins, "debugVeins", "Enable verbose debug logging for vein generation.");

        veins.addProperty("enableNetherGoldVeins", enableNetherGoldVeins);
        veins.addProperty("enableNetherQuartzVeins", enableNetherQuartzVeins);
        veins.addProperty("enableRawQuartzBlockInQuartzVeins", enableRawQuartzBlockInQuartzVeins);
        veins.addProperty("enableCoalAndesiteVeins", enableCoalAndesiteVeins);
        veins.addProperty("enableCoalTuffVeins", enableCoalTuffVeins);
        veins.addProperty("debugVeins", debugVeins);
        root.add("veins", veins);

        JsonObject rawQuartzBlock = new JsonObject();
        FabricJsonComments.put(rawQuartzBlock, "enabled", "Master toggle for Block of Raw Quartz worldgen. When false, raw quartz block will never be placed anywhere, overriding veins.enableRawQuartzBlockInQuartzVeins.");
        rawQuartzBlock.addProperty("enabled", enableRawQuartzBlock);
        root.add("rawQuartzBlock", rawQuartzBlock);

        JsonObject nether = new JsonObject();

        FabricJsonComments.put(nether, "disableNetherLavaSprings", "Disable nether lava springs worldgen (new chunks only). If keepExposedNetherLavaSprings=true, only enclosed/hidden springs are suppressed.");
        FabricJsonComments.put(nether, "keepExposedNetherLavaSprings", "When disableNetherLavaSprings=true, keep lava springs that are exposed to air on at least one side.");

        nether.addProperty("disableNetherLavaSprings", disableNetherLavaSprings);
        nether.addProperty("keepExposedNetherLavaSprings", keepExposedNetherLavaSprings);
        root.add("nether", nether);

        JsonObject oreVariants = new JsonObject();

        FabricJsonComments.put(oreVariants, "enabled", "Enable Bits and Balance ore variants placement.");

        oreVariants.addProperty("enabled", enableOreVariants);
        root.add("oreVariants", oreVariants);

        JsonObject processing = new JsonObject();

        FabricJsonComments.put(processing, "enableParallelProcessing", "Enable parallel processing of ore variants for better performance. When enabled, ore variant processing can use multiple CPU cores. When disabled, processing stays on a single thread.");
        FabricJsonComments.put(processing, "threadCount", "Number of threads to use for parallel processing. Leave at 0 to automatically use half the available CPU cores. Set to 1 to force single-threaded processing. Range: 0-64.");

        processing.addProperty("enableParallelProcessing", enableParallelProcessing);
        processing.addProperty("threadCount", parallelProcessingThreadCount);
        root.add("processing", processing);

        JsonObject azaleaWood = new JsonObject();

        FabricJsonComments.put(azaleaWood, "enableGeneration", "Enable azalea wood worldgen.");
        FabricJsonComments.put(azaleaWood, "enableWoodset", "Enable the azalea woodset (if disabled, generation is also forced off).");

        azaleaWood.addProperty("enableGeneration", enableAzaleaWoodGeneration);
        azaleaWood.addProperty("enableWoodset", enableAzaleaWoodset);
        root.add("azaleaWood", azaleaWood);

        root.add("disabled", GSON.toJsonTree(disabledKeys.stream().map(Identifier::toString).sorted().toList()));

        Files.writeString(configPath, GSON.toJson(root), StandardCharsets.UTF_8);
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) {
            return def;
        }
        return el.getAsBoolean();
    }

    private static int getInt(JsonObject obj, String key, int def, int min, int max) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
            return def;
        }

        int value = el.getAsInt();
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }
}
