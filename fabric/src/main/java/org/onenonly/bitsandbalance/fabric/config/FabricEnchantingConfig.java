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
 * Minimal Fabric-side config for enchanting tweaks.
 *
 * Config file: config/Bits and Balance/bitsandbalance-enchanting.json
 */
public final class FabricEnchantingConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-enchanting-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-enchanting.json";

    // Aerodynamic (mirrors NeoForge defaults in EnchantingConfig)
    public static volatile boolean enableAerodynamic = true;
    public static volatile double aerodynamicBaseDragReduction = 0.5D;
    public static volatile int aerodynamicMinAltitude = 192;
    public static volatile int aerodynamicMaxAltitude = 320;
    public static volatile double aerodynamicCloudLevel = 192.0D;
    public static volatile double aerodynamicSpeedMultiplier = 1.0D;
    public static volatile double aerodynamicMaxFlightSpeed = 50.0D;

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
            }

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
        JsonObject aerodynamic = new JsonObject();

        FabricJsonComments.put(root, "aerodynamic", "Settings for the Aerodynamic enchantment.");
        FabricJsonComments.put(aerodynamic, "enabled", "Whether the Aerodynamic enchantment effects are enabled.");
        FabricJsonComments.put(aerodynamic, "baseDragReduction", "Base reduction applied to flight drag at/above the cloud level.");
        FabricJsonComments.put(aerodynamic, "minAltitude", "Minimum Y level where Aerodynamic begins applying.");
        FabricJsonComments.put(aerodynamic, "maxAltitude", "Maximum Y level for Aerodynamic scaling (values above behave as max).");
        FabricJsonComments.put(aerodynamic, "cloudLevel", "Cloud level used as the reference point for altitude scaling.");
        FabricJsonComments.put(aerodynamic, "speedMultiplier", "Multiplier applied to elytra flight speed when Aerodynamic is active.");
        FabricJsonComments.put(aerodynamic, "maxFlightSpeed", "Hard cap for Aerodynamic-modified flight speed.");

        aerodynamic.addProperty("enabled", enableAerodynamic);
        aerodynamic.addProperty("baseDragReduction", aerodynamicBaseDragReduction);
        aerodynamic.addProperty("minAltitude", aerodynamicMinAltitude);
        aerodynamic.addProperty("maxAltitude", aerodynamicMaxAltitude);
        aerodynamic.addProperty("cloudLevel", aerodynamicCloudLevel);
        aerodynamic.addProperty("speedMultiplier", aerodynamicSpeedMultiplier);
        aerodynamic.addProperty("maxFlightSpeed", aerodynamicMaxFlightSpeed);
        root.add("aerodynamic", aerodynamic);

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
