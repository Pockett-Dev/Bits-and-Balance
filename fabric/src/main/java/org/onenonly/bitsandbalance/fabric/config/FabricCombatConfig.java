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
import java.util.Set;

/**
 * Minimal Fabric-side config for combat features.
 *
 * Config file: config/Bits and Balance/bitsandbalance-combat.json
 */
public final class FabricCombatConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-combat-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-gameplay.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-combat.json";

    // Maintain Experience
    public static volatile boolean enableMaintainExperience = true;
    public static volatile int maintainExperienceLossPercent = 50;

    // Second Chance
    public static volatile boolean enableSecondChance = true;
    public static volatile boolean secondChanceExcludeFallDamage = true;
    public static volatile int secondChanceCooldownDays = 7;
    public static volatile boolean secondChanceResetOnRespawn = true;

    public static volatile boolean secondChanceResistanceEnabled = true;
    public static volatile int secondChanceResistanceLevel = 1;

    public static volatile boolean secondChanceNauseaEnabled = true;
    public static volatile int secondChanceNauseaLevel = 2;

        // Snowball Rework
        public static volatile boolean enableSnowballRework = true;
        public static volatile double snowballBaseDamage = 0.5D;
        public static volatile double snowballNetherDamage = 1.0D;
        public static volatile int snowballMinFreezeSeconds = 5;
        public static volatile int snowballMaxFreezeSeconds = 8;
        public static volatile Set<String> snowballNetherEntities = Set.of(
            "minecraft:blaze", "minecraft:ghast", "minecraft:magma_cube", "minecraft:strider",
            "minecraft:wither_skeleton", "minecraft:piglin", "minecraft:piglin_brute",
            "minecraft:zombified_piglin", "minecraft:hoglin", "minecraft:zoglin"
        );

    // Compatibility
    public static volatile boolean enableBetterCombatIntegration = true;

    private FabricCombatConfig() {
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
            JsonObject secondChance = getObject(obj, "secondChance");
            if (secondChance == null) {
                secondChance = getObject(legacyObj, "secondChance");
            }
            if (secondChance != null) {
                enableSecondChance = getBoolean(secondChance, "enabled", enableSecondChance);
                secondChanceExcludeFallDamage = getBoolean(secondChance, "excludeFallDamage", secondChanceExcludeFallDamage);
                secondChanceCooldownDays = getInt(secondChance, "cooldownDays", secondChanceCooldownDays);
                secondChanceResetOnRespawn = getBoolean(secondChance, "resetOnRespawn", secondChanceResetOnRespawn);

                JsonObject res = getObject(secondChance, "applyResistance");
                if (res != null) {
                    secondChanceResistanceEnabled = getBoolean(res, "enabled", secondChanceResistanceEnabled);
                    secondChanceResistanceLevel = getInt(res, "level", secondChanceResistanceLevel);
                }

                JsonObject nau = getObject(secondChance, "applyNausea");
                if (nau != null) {
                    secondChanceNauseaEnabled = getBoolean(nau, "enabled", secondChanceNauseaEnabled);
                    secondChanceNauseaLevel = getInt(nau, "level", secondChanceNauseaLevel);
                }
            } else {
                // allow flat key fallback
                enableSecondChance = getBoolean(legacyObj, "enableSecondChance", enableSecondChance);
            }

            JsonObject maintainExperience = getObject(obj, "maintainExperience");
            if (maintainExperience == null) {
                maintainExperience = getObject(legacyObj, "maintainExperience");
            }
            if (maintainExperience != null) {
                enableMaintainExperience = getBoolean(maintainExperience, "enabled", enableMaintainExperience);
                maintainExperienceLossPercent = getInt(maintainExperience, "lossPercent", maintainExperienceLossPercent);
            } else {
                enableMaintainExperience = getBoolean(legacyObj, "enableMaintainExperience", enableMaintainExperience);
                maintainExperienceLossPercent = getInt(legacyObj, "maintainExperienceLossPercent", maintainExperienceLossPercent);
            }

            JsonObject snowball = getObject(obj, "snowball");
            if (snowball == null) {
                snowball = getObject(legacyObj, "snowball");
            }
            if (snowball != null) {
                enableSnowballRework = getBoolean(snowball, "enabled", enableSnowballRework);
                snowballBaseDamage = getDouble(snowball, "baseDamage", snowballBaseDamage);
                snowballNetherDamage = getDouble(snowball, "netherDamage", snowballNetherDamage);
                snowballMinFreezeSeconds = getInt(snowball, "minFreezeSeconds", snowballMinFreezeSeconds);
                snowballMaxFreezeSeconds = getInt(snowball, "maxFreezeSeconds", snowballMaxFreezeSeconds);
                snowballNetherEntities = getStringSet(snowball, "netherEntities", snowballNetherEntities);
            } else {
                enableSnowballRework = getBoolean(legacyObj, "enableSnowballRework", enableSnowballRework);
            }

            JsonObject compat = getObject(obj, "compat");
            if (compat == null) {
                compat = getObject(legacyObj, "compat");
            }
            if (compat != null) {
                enableBetterCombatIntegration = getBoolean(compat, "betterCombatIntegration", enableBetterCombatIntegration);
            } else {
                enableBetterCombatIntegration = getBoolean(legacyObj, "enableBetterCombatIntegration", enableBetterCombatIntegration);
            }

            maintainExperienceLossPercent = clampPercent(maintainExperienceLossPercent);

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
        FabricJsonComments.put(root, "_file", "Bits and Balance (Fabric) combat configuration.");

        JsonObject maintainExperience = new JsonObject();
        FabricJsonComments.put(maintainExperience, "enabled", "If true, death uses the configured total-XP loss instead of vanilla XP loss.");
        FabricJsonComments.put(maintainExperience, "lossPercent", "Percent of total XP points to lose on death (0-100). 50 = lose half of your total XP, not half of your displayed levels.");
        maintainExperience.addProperty("enabled", enableMaintainExperience);
        maintainExperience.addProperty("lossPercent", clampPercent(maintainExperienceLossPercent));
        root.add("maintainExperience", maintainExperience);

        JsonObject secondChance = new JsonObject();
        FabricJsonComments.put(secondChance, "enabled", "If true, enables 'Second Chance' instead of dying under certain conditions.");
        FabricJsonComments.put(secondChance, "excludeFallDamage", "If true, fall damage cannot trigger Second Chance.");
        FabricJsonComments.put(secondChance, "cooldownDays", "Cooldown between Second Chance triggers (in in-game days).");
        FabricJsonComments.put(secondChance, "resetOnRespawn", "If true, resets the cooldown when you respawn.");
        secondChance.addProperty("enabled", enableSecondChance);
        secondChance.addProperty("excludeFallDamage", secondChanceExcludeFallDamage);
        secondChance.addProperty("cooldownDays", Math.max(0, secondChanceCooldownDays));
        secondChance.addProperty("resetOnRespawn", secondChanceResetOnRespawn);

        JsonObject res = new JsonObject();
        FabricJsonComments.put(res, "enabled", "If true, applies Resistance after Second Chance triggers.");
        FabricJsonComments.put(res, "level", "Resistance level to apply (1 = Resistance I).");
        res.addProperty("enabled", secondChanceResistanceEnabled);
        res.addProperty("level", Math.max(1, secondChanceResistanceLevel));
        secondChance.add("applyResistance", res);

        JsonObject nau = new JsonObject();
        FabricJsonComments.put(nau, "enabled", "If true, applies Nausea after Second Chance triggers.");
        FabricJsonComments.put(nau, "level", "Nausea amplifier (1 = Nausea II)." );
        nau.addProperty("enabled", secondChanceNauseaEnabled);
        nau.addProperty("level", Math.max(1, secondChanceNauseaLevel));
        secondChance.add("applyNausea", nau);

        root.add("secondChance", secondChance);

        JsonObject snowball = new JsonObject();
        FabricJsonComments.put(snowball, "enabled", "If true, enables the snowball rework (damage + freezing).");
        FabricJsonComments.put(snowball, "baseDamage", "Base snowball damage outside the Nether.");
        FabricJsonComments.put(snowball, "netherDamage", "Snowball damage in the Nether.");
        FabricJsonComments.put(snowball, "minFreezeSeconds", "Minimum freeze duration (seconds).");
        FabricJsonComments.put(snowball, "maxFreezeSeconds", "Maximum freeze duration (seconds).");
        FabricJsonComments.put(snowball, "netherEntities", "Entity IDs that should take Nether snowball damage/freezing behavior.");
        snowball.addProperty("enabled", enableSnowballRework);
        snowball.addProperty("baseDamage", snowballBaseDamage);
        snowball.addProperty("netherDamage", snowballNetherDamage);
        snowball.addProperty("minFreezeSeconds", Math.max(0, snowballMinFreezeSeconds));
        snowball.addProperty("maxFreezeSeconds", Math.max(0, snowballMaxFreezeSeconds));
        snowball.add("netherEntities", GSON.toJsonTree(snowballNetherEntities.stream().sorted().toList()));
        root.add("snowball", snowball);

        JsonObject compat = new JsonObject();
        FabricJsonComments.put(compat, "betterCombatIntegration", "If true, enables Better Combat integration (only has effect if the mod is installed).");
        compat.addProperty("betterCombatIntegration", enableBetterCombatIntegration);
        root.add("compat", compat);

        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
        if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
            FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
        }
    }

    private static int clampPercent(int value) {
        if (value < 0) return 0;
        if (value > 100) return 100;
        return value;
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

    private static Set<String> getStringSet(JsonObject obj, String key, Set<String> def) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonArray()) {
            return def;
        }
        var out = new java.util.ArrayList<String>();
        try {
            for (JsonElement child : el.getAsJsonArray()) {
                if (child != null && child.isJsonPrimitive() && child.getAsJsonPrimitive().isString()) {
                    out.add(child.getAsString());
                }
            }
        } catch (Exception ignored) {
            return def;
        }

        return out.isEmpty() ? Set.of() : Set.copyOf(out);
    }
}
