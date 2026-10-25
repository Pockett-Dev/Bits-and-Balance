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
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal Fabric-side config for enchanting tweaks.
 *
 * Config file: config/Bits and Balance/bitsandbalance-enchanting.json
 */
public final class FabricEnchantingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-enchanting-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-enchanting.json";

    public static volatile boolean enableFortuneBrush = true;
    public static volatile List<String> fortuneBrushBlacklist = List.of();

    // Aerodynamic (mirrors NeoForge defaults in EnchantingConfig)
    public static volatile boolean enableAerodynamic = true;
    public static volatile double aerodynamicBaseDragReduction = 0.5D;
    public static volatile int aerodynamicMinAltitude = 192;
    public static volatile int aerodynamicMaxAltitude = 320;
    public static volatile double aerodynamicCloudLevel = 192.0D;
    public static volatile double aerodynamicSpeedMultiplier = 1.0D;
    public static volatile double aerodynamicMaxFlightSpeed = 50.0D;
    public static volatile double aerodynamicLowAltitudeStrengthFloor = 0.5D;
    public static volatile double aerodynamicGlidePitchRange = 60.0D;
    public static volatile double aerodynamicUpwardPitchStart = 5.0D;
    public static volatile double aerodynamicUpwardPitchRange = 40.0D;
    public static volatile double aerodynamicUpwardMotionFullSpeed = 0.25D;
    public static volatile double aerodynamicClimbAssistScale = 0.2D;
    public static volatile double aerodynamicSinkReductionScale = 0.6D;
    public static volatile double aerodynamicMaxSinkReduction = 0.75D;
    public static volatile double aerodynamicSinkReductionMinHorizontalSpeed = 0.5D;
    public static volatile double aerodynamicSinkReductionFullHorizontalSpeed = 1.5D;
    public static volatile double aerodynamicClimbRestoreCap = 0.15D;
    public static volatile double aerodynamicClimbRestoreScale = 0.3D;
    public static volatile double aerodynamicTurnResponse = 0.5D;
    public static volatile double aerodynamicTurnRestoreBonus = 0.25D;
    public static volatile boolean enableEnchancementElytraDurabilityCompat = true;

    // Cascading: maximum total ore blocks in one sequence, including the first hand-mined block.
    public static volatile boolean enableCascading = true;
    public static volatile int cascadingMaxActiveLevel = 1;
    public static volatile int cascadingLevelOneMaxOres = 8;
    public static volatile int cascadingLevelTwoMaxOres = 16;
    public static volatile int cascadingLevelThreeMaxOres = 24;

    private FabricEnchantingConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        JsonObject obj = FabricJsonConfigFiles.readRoot(configPath, GSON);
        JsonObject legacyObj = FabricJsonConfigFiles.readRoot(LEGACY_FILE_NAME, GSON);

        try {
            JsonObject fortuneBrush = getObject(obj, "fortuneBrush");
            if (fortuneBrush != null) {
                enableFortuneBrush = getBoolean(fortuneBrush, "enabled", enableFortuneBrush);
                JsonElement blacklist = fortuneBrush.get("blacklist");
                if (blacklist != null && blacklist.isJsonArray()) {
                    List<String> items = new ArrayList<>();
                    for (JsonElement entry : blacklist.getAsJsonArray()) {
                        if (entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) items.add(entry.getAsString());
                    }
                    fortuneBrushBlacklist = List.copyOf(items);
                }
            }
            org.onenonly.bitsandbalance.common.loot.BrushFortune.enabled = enableFortuneBrush;
            org.onenonly.bitsandbalance.common.loot.BrushFortune.setItemBlacklist(fortuneBrushBlacklist);
            JsonObject aerodynamic = getObject(obj, "aerodynamic");
            if (aerodynamic == null) {
                aerodynamic = getObject(legacyObj, "aerodynamic");
            }
            if (aerodynamic != null) {
                enableAerodynamic = getBoolean(aerodynamic, "enabled", enableAerodynamic);
                aerodynamicBaseDragReduction = getDouble(aerodynamic, "baseDragReduction", aerodynamicBaseDragReduction);
                aerodynamicMinAltitude = getInt(aerodynamic, "minAltitude", aerodynamicMinAltitude);
                aerodynamicMaxAltitude = getInt(aerodynamic, "maxAltitude", aerodynamicMaxAltitude);
                aerodynamicCloudLevel = getDouble(aerodynamic, "cloudLevel", aerodynamicCloudLevel);
                aerodynamicSpeedMultiplier = getDouble(aerodynamic, "speedMultiplier", aerodynamicSpeedMultiplier);
                aerodynamicMaxFlightSpeed = getDouble(aerodynamic, "maxFlightSpeed", aerodynamicMaxFlightSpeed);
                aerodynamicLowAltitudeStrengthFloor = getDouble(aerodynamic, "lowAltitudeStrengthFloor", aerodynamicLowAltitudeStrengthFloor);
                aerodynamicGlidePitchRange = getDouble(aerodynamic, "glidePitchRange", aerodynamicGlidePitchRange);
                aerodynamicUpwardPitchStart = getDouble(aerodynamic, "upwardPitchStart", aerodynamicUpwardPitchStart);
                aerodynamicUpwardPitchRange = getDouble(aerodynamic, "upwardPitchRange", aerodynamicUpwardPitchRange);
                aerodynamicUpwardMotionFullSpeed = getDouble(aerodynamic, "upwardMotionFullSpeed", aerodynamicUpwardMotionFullSpeed);
                aerodynamicClimbAssistScale = getDouble(aerodynamic, "climbAssistScale", aerodynamicClimbAssistScale);
                aerodynamicSinkReductionScale = getDouble(aerodynamic, "sinkReductionScale", aerodynamicSinkReductionScale);
                aerodynamicMaxSinkReduction = getDouble(aerodynamic, "maxSinkReduction", aerodynamicMaxSinkReduction);
                aerodynamicSinkReductionMinHorizontalSpeed = getDouble(aerodynamic, "sinkReductionMinHorizontalSpeed", aerodynamicSinkReductionMinHorizontalSpeed);
                aerodynamicSinkReductionFullHorizontalSpeed = getDouble(aerodynamic, "sinkReductionFullHorizontalSpeed", aerodynamicSinkReductionFullHorizontalSpeed);
                aerodynamicClimbRestoreCap = getDouble(aerodynamic, "climbRestoreCap", aerodynamicClimbRestoreCap);
                aerodynamicClimbRestoreScale = getDouble(aerodynamic, "climbRestoreScale", aerodynamicClimbRestoreScale);
                aerodynamicTurnResponse = getDouble(aerodynamic, "turnResponse", aerodynamicTurnResponse);
                aerodynamicTurnRestoreBonus = getDouble(aerodynamic, "turnRestoreBonus", aerodynamicTurnRestoreBonus);
                enableEnchancementElytraDurabilityCompat = getBoolean(aerodynamic, "enchancementElytraDurabilityCompat", enableEnchancementElytraDurabilityCompat);
            }

            JsonObject cascading = getObject(obj, "cascading");
            if (cascading != null) {
                enableCascading = getBoolean(cascading, "enabled", enableCascading);
                cascadingMaxActiveLevel = Math.max(1, Math.min(3,
                        getInt(cascading, "maxActiveLevel", cascadingMaxActiveLevel)));
                cascadingLevelOneMaxOres = Math.max(1, Math.min(64,
                        getInt(cascading, "levelOneMaxOres", cascadingLevelOneMaxOres)));
                cascadingLevelTwoMaxOres = Math.max(1, Math.min(64,
                        getInt(cascading, "levelTwoMaxOres", cascadingLevelTwoMaxOres)));
                cascadingLevelThreeMaxOres = Math.max(1, Math.min(64,
                        getInt(cascading, "levelThreeMaxOres", cascadingLevelThreeMaxOres)));
            }
            org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining.configuredMaxLevel = cascadingMaxActiveLevel;
            org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining.enabled = enableCascading;

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
        org.onenonly.bitsandbalance.common.loot.BrushFortune.enabled = enableFortuneBrush;
        org.onenonly.bitsandbalance.common.loot.BrushFortune.setItemBlacklist(fortuneBrushBlacklist);
        JsonObject fortuneBrush = new JsonObject();
        FabricJsonComments.put(root, "fortuneBrush", "Fortune on brushes: +50% brushing XP per level; common rewards reach triple quantity at level III and rare finds cap at two. Rewards remain datapack controlled.");
        FabricJsonComments.put(fortuneBrush, "enabled", "Enable Fortune brushing drop and XP bonuses. Fortune can still be applied through an anvil when bonuses are disabled.");
        fortuneBrush.addProperty("enabled", enableFortuneBrush);
        FabricJsonComments.put(fortuneBrush, "blacklist", "Item IDs excluded from Fortune brushing quantity bonuses, e.g. minecraft:diamond. Keeps the normal reward and XP bonus. Applies when loot is first revealed; already revealed rewards keep their saved count.");
        fortuneBrush.add("blacklist", GSON.toJsonTree(fortuneBrushBlacklist));
        root.add("fortuneBrush", fortuneBrush);
        JsonObject aerodynamic = new JsonObject();

        FabricJsonComments.put(root, "aerodynamic", "Settings for the Aerodynamic enchantment.");
        FabricJsonComments.put(aerodynamic, "enabled", "Whether the Aerodynamic enchantment effects are enabled.");
        FabricJsonComments.put(aerodynamic, "baseDragReduction", "Base reduction applied to flight drag at/above the cloud level.");
        FabricJsonComments.put(aerodynamic, "minAltitude", "Y level where Aerodynamic reaches full strength; below this it still applies at reduced strength.");
        FabricJsonComments.put(aerodynamic, "maxAltitude", "Legacy ramp reference. The distance from minAltitude controls how gradually Aerodynamic fades below full strength.");
        FabricJsonComments.put(aerodynamic, "cloudLevel", "Legacy setting kept for config compatibility. Full strength is controlled by minAltitude.");
        FabricJsonComments.put(aerodynamic, "speedMultiplier", "Multiplier applied to elytra flight speed when Aerodynamic is active.");
        FabricJsonComments.put(aerodynamic, "maxFlightSpeed", "Hard cap for Aerodynamic-modified flight speed at full altitude strength. Scales down below full-strength altitude.");
        FabricJsonComments.put(aerodynamic, "lowAltitudeStrengthFloor", "Minimum Aerodynamic strength below the altitude ramp. 0.5 = at least half strength.");
        FabricJsonComments.put(aerodynamic, "glidePitchRange", "Pitch angle in degrees where level-glide efficiency fades out. Larger values make the glide bonus apply over steeper pitch.");
        FabricJsonComments.put(aerodynamic, "upwardPitchStart", "Upward pitch angle in degrees where climb efficiency starts.");
        FabricJsonComments.put(aerodynamic, "upwardPitchRange", "Additional upward pitch range in degrees needed to reach full climb pitch efficiency.");
        FabricJsonComments.put(aerodynamic, "upwardMotionFullSpeed", "Upward velocity in blocks per tick that counts as full climb motion efficiency.");
        FabricJsonComments.put(aerodynamic, "climbAssistScale", "How much climb efficiency keeps drag reduction active while pitched upward.");
        FabricJsonComments.put(aerodynamic, "sinkReductionScale", "How strongly Aerodynamic reduces downward sink from its current drag-reduction strength.");
        FabricJsonComments.put(aerodynamic, "maxSinkReduction", "Maximum fraction of downward sink Aerodynamic can remove. 0.75 = up to 75% less sink.");
        FabricJsonComments.put(aerodynamic, "sinkReductionMinHorizontalSpeed", "Horizontal speed in blocks per tick where sink reduction starts ramping in.");
        FabricJsonComments.put(aerodynamic, "sinkReductionFullHorizontalSpeed", "Horizontal speed in blocks per tick where sink reduction reaches full configured strength.");
        FabricJsonComments.put(aerodynamic, "climbRestoreCap", "Maximum fraction of upward speed loss that Aerodynamic can restore per tick.");
        FabricJsonComments.put(aerodynamic, "climbRestoreScale", "How strongly climb efficiency restores upward speed loss.");
        FabricJsonComments.put(aerodynamic, "turnResponse", "Direction-change amount needed to reach full turn speed preservation bonus. Lower values make turns count sooner.");
        FabricJsonComments.put(aerodynamic, "turnRestoreBonus", "Extra multiplier applied to lost horizontal speed restoration while turning.");
        FabricJsonComments.put(aerodynamic, "enchancementElytraDurabilityCompat", "When Enchancement is installed, keep vanilla Elytra durability loss even if Enchancement's Disable Durability setting is enabled.");

        aerodynamic.addProperty("enabled", enableAerodynamic);
        aerodynamic.addProperty("baseDragReduction", aerodynamicBaseDragReduction);
        aerodynamic.addProperty("minAltitude", aerodynamicMinAltitude);
        aerodynamic.addProperty("maxAltitude", aerodynamicMaxAltitude);
        aerodynamic.addProperty("cloudLevel", aerodynamicCloudLevel);
        aerodynamic.addProperty("speedMultiplier", aerodynamicSpeedMultiplier);
        aerodynamic.addProperty("maxFlightSpeed", aerodynamicMaxFlightSpeed);
        aerodynamic.addProperty("lowAltitudeStrengthFloor", aerodynamicLowAltitudeStrengthFloor);
        aerodynamic.addProperty("glidePitchRange", aerodynamicGlidePitchRange);
        aerodynamic.addProperty("upwardPitchStart", aerodynamicUpwardPitchStart);
        aerodynamic.addProperty("upwardPitchRange", aerodynamicUpwardPitchRange);
        aerodynamic.addProperty("upwardMotionFullSpeed", aerodynamicUpwardMotionFullSpeed);
        aerodynamic.addProperty("climbAssistScale", aerodynamicClimbAssistScale);
        aerodynamic.addProperty("sinkReductionScale", aerodynamicSinkReductionScale);
        aerodynamic.addProperty("maxSinkReduction", aerodynamicMaxSinkReduction);
        aerodynamic.addProperty("sinkReductionMinHorizontalSpeed", aerodynamicSinkReductionMinHorizontalSpeed);
        aerodynamic.addProperty("sinkReductionFullHorizontalSpeed", aerodynamicSinkReductionFullHorizontalSpeed);
        aerodynamic.addProperty("climbRestoreCap", aerodynamicClimbRestoreCap);
        aerodynamic.addProperty("climbRestoreScale", aerodynamicClimbRestoreScale);
        aerodynamic.addProperty("turnResponse", aerodynamicTurnResponse);
        aerodynamic.addProperty("turnRestoreBonus", aerodynamicTurnRestoreBonus);
        aerodynamic.addProperty("enchancementElytraDurabilityCompat", enableEnchancementElytraDurabilityCompat);
        root.add("aerodynamic", aerodynamic);

        JsonObject cascading = new JsonObject();
        FabricJsonComments.put(root, "cascading", "Experimental Cascading ore mining enchantment settings.");
        FabricJsonComments.put(cascading, "enabled", "Enable Cascading when an enchanted tool mines ore. Crouching always disables the effect.");
        FabricJsonComments.put(cascading, "maxActiveLevel", "Highest Cascading level that can trigger, from 1 to 3.");
        FabricJsonComments.put(cascading, "levelOneMaxOres", "Maximum total ores at Cascading I, including the first ore (1-64).");
        FabricJsonComments.put(cascading, "levelTwoMaxOres", "Maximum total ores at Cascading II, including the first ore (1-64).");
        FabricJsonComments.put(cascading, "levelThreeMaxOres", "Maximum total ores at Cascading III, including the first ore (1-64).");
        cascading.addProperty("enabled", enableCascading);
        cascading.addProperty("maxActiveLevel", cascadingMaxActiveLevel);
        cascading.addProperty("levelOneMaxOres", cascadingLevelOneMaxOres);
        cascading.addProperty("levelTwoMaxOres", cascadingLevelTwoMaxOres);
        cascading.addProperty("levelThreeMaxOres", cascadingLevelThreeMaxOres);
        root.add("cascading", cascading);

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
}
