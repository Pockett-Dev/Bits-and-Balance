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
 * Minimal Fabric-side config for mob-related mechanics.
 *
 * Config file: config/Bits and Balance/bitsandbalance-mobs.json
 */
public final class FabricMobsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-mobs-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-gameplay.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-mobs.json";

    // Villagers
    public static volatile boolean disableNitwits = true;

    // Iron Golems vs Creepers
    public static volatile boolean enableIronGolemsKillCreepers = true;

    // Improved Phantoms
    public static volatile boolean enableImprovedPhantoms = true;

    // Phantom pickup
    public static volatile boolean enablePhantomPickup = true;
    public static volatile double phantomPickupChance = 0.3D;
    public static volatile int phantomPickupMinDelay = 1;
    public static volatile double phantomPickupSpeedMultiplier = 0.5D;

    // Phantom slowness stacking
    public static volatile boolean enablePhantomSlownessStacking = true;
    public static volatile int phantomSlownessDuration = 10;
    public static volatile int phantomSlownessMaxLevel = 4;

    // Double damage while carried
    public static volatile boolean enablePhantomDoubleDamage = true;

    // Automatic drop after being carried
    public static volatile boolean enablePhantomAutoDrop = true;
    public static volatile int phantomAutoDropSeconds = 3;

    // Drop-on-hit and hit-triggered slow falling
    public static volatile boolean enablePhantomDropOnHit = true;
    public static volatile boolean enablePhantomSlowFallingOnHitDismount = true;

    // Spawn swap: cave spiders in caves
    public static volatile boolean enableCaveSpidersInCaves = true;
    public static volatile double caveSpiderReplacementChance = 0.35D;

    private FabricMobsConfig() {
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
            JsonObject mobs = getObject(obj, "mobs");
            if (mobs == null) {
                mobs = getObject(legacyObj, "mobs");
            }
            if (mobs != null) {
                disableNitwits = getBoolean(mobs, "disableNitwits", disableNitwits);
                enableIronGolemsKillCreepers = getBoolean(mobs, "ironGolemsKillCreepers", enableIronGolemsKillCreepers);

                JsonObject spiders = getObject(mobs, "spiders");
                if (spiders != null) {
                    enableCaveSpidersInCaves = getBoolean(spiders, "caveSpidersInCaves", enableCaveSpidersInCaves);
                    caveSpiderReplacementChance = getDouble(spiders, "caveSpiderReplacementChance", caveSpiderReplacementChance);
                }

                JsonObject phantoms = getObject(mobs, "phantoms");
                if (phantoms != null) {
                    enableImprovedPhantoms = getBoolean(phantoms, "enabled", enableImprovedPhantoms);
                    enablePhantomDoubleDamage = getBoolean(phantoms, "doubleDamage", enablePhantomDoubleDamage);
                    enablePhantomAutoDrop = getBoolean(phantoms, "autoDrop",
                            getBoolean(phantoms, "slowFallingLanding",
                                    getBoolean(phantoms, "halveFallDamage", enablePhantomAutoDrop)));
                    phantomAutoDropSeconds = getInt(phantoms, "autoDropSeconds", phantomAutoDropSeconds);
                    enablePhantomDropOnHit = getBoolean(phantoms, "dropOnHit", enablePhantomDropOnHit);
                        enablePhantomSlowFallingOnHitDismount = getBoolean(phantoms, "slowFallingOnHitDismount",
                            getBoolean(phantoms, "slowFallingOnNonHitDismount", enablePhantomSlowFallingOnHitDismount));

                    JsonObject pickup = getObject(phantoms, "pickup");
                    if (pickup != null) {
                        enablePhantomPickup = getBoolean(pickup, "enabled", enablePhantomPickup);
                        phantomPickupChance = getDouble(pickup, "chance", phantomPickupChance);
                        phantomPickupMinDelay = getInt(pickup, "minDelaySeconds", phantomPickupMinDelay);
                        phantomPickupSpeedMultiplier = getDouble(pickup, "speedMultiplier", phantomPickupSpeedMultiplier);
                    }

                    JsonObject slowness = getObject(phantoms, "slowness");
                    if (slowness != null) {
                        enablePhantomSlownessStacking = getBoolean(slowness, "enabled", enablePhantomSlownessStacking);
                        phantomSlownessDuration = getInt(slowness, "durationSeconds", phantomSlownessDuration);
                        phantomSlownessMaxLevel = getInt(slowness, "maxLevel", phantomSlownessMaxLevel);
                    }
                }
            } else {
                disableNitwits = getBoolean(legacyObj, "disableNitwits", disableNitwits);
                enableIronGolemsKillCreepers = getBoolean(legacyObj, "enableIronGolemsKillCreepers", enableIronGolemsKillCreepers);

                // allow flat key fallback for phantoms
                enableImprovedPhantoms = getBoolean(legacyObj, "enableImprovedPhantoms", enableImprovedPhantoms);
                enablePhantomPickup = getBoolean(legacyObj, "enablePhantomPickup", enablePhantomPickup);
                phantomPickupChance = getDouble(legacyObj, "phantomPickupChance", phantomPickupChance);
                phantomPickupMinDelay = getInt(legacyObj, "phantomPickupMinDelay", phantomPickupMinDelay);
                phantomPickupSpeedMultiplier = getDouble(legacyObj, "phantomPickupSpeedMultiplier", phantomPickupSpeedMultiplier);
                enablePhantomSlownessStacking = getBoolean(legacyObj, "enablePhantomSlownessStacking", enablePhantomSlownessStacking);
                phantomSlownessDuration = getInt(legacyObj, "phantomSlownessDuration", phantomSlownessDuration);
                phantomSlownessMaxLevel = getInt(legacyObj, "phantomSlownessMaxLevel", phantomSlownessMaxLevel);
                enablePhantomDoubleDamage = getBoolean(legacyObj, "enablePhantomDoubleDamage", enablePhantomDoubleDamage);
                enablePhantomAutoDrop = getBoolean(legacyObj, "enablePhantomAutoDrop",
                    getBoolean(legacyObj, "enablePhantomSlowFallingLanding",
                        getBoolean(legacyObj, "enablePhantomHalvedFallDamage", enablePhantomAutoDrop)));
                phantomAutoDropSeconds = getInt(legacyObj, "phantomAutoDropSeconds", phantomAutoDropSeconds);
                enablePhantomDropOnHit = getBoolean(legacyObj, "enablePhantomDropOnHit", enablePhantomDropOnHit);
                enablePhantomSlowFallingOnHitDismount = getBoolean(legacyObj, "enablePhantomSlowFallingOnHitDismount",
                    getBoolean(legacyObj, "enablePhantomSlowFallingOnNonHitDismount", enablePhantomSlowFallingOnHitDismount));

                enableCaveSpidersInCaves = getBoolean(legacyObj, "enableCaveSpidersInCaves", enableCaveSpidersInCaves);
                caveSpiderReplacementChance = getDouble(legacyObj, "caveSpiderReplacementChance", caveSpiderReplacementChance);

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
        FabricJsonComments.put(root, "_file", "Bits and Balance (Fabric) mobs configuration.");
        JsonObject mobs = new JsonObject();

        FabricJsonComments.put(mobs, "disableNitwits", "If true, prevents Nitwit villagers from spawning naturally.");
        FabricJsonComments.put(mobs, "ironGolemsKillCreepers", "If true, Iron Golems will actively target and kill Creepers.");
        FabricJsonComments.put(mobs, "phantoms", "Phantom-related settings.");
        FabricJsonComments.put(mobs, "spiders", "Spider-related settings.");
        mobs.addProperty("disableNitwits", disableNitwits);
        mobs.addProperty("ironGolemsKillCreepers", enableIronGolemsKillCreepers);

        JsonObject phantoms = new JsonObject();
        FabricJsonComments.put(phantoms, "enabled", "If true, enables the Improved Phantoms feature.");
        FabricJsonComments.put(phantoms, "doubleDamage", "If true, hitting a phantom while being carried deals double damage.");
        FabricJsonComments.put(phantoms, "autoDrop", "If true, phantoms automatically drop carried players after the configured carry time.");
        FabricJsonComments.put(phantoms, "autoDropSeconds", "How many seconds a phantom carries a player before automatically dropping them.");
        FabricJsonComments.put(phantoms, "dropOnHit", "If true, hitting the phantom that is carrying you makes it drop you immediately.");
        FabricJsonComments.put(phantoms, "slowFallingOnHitDismount", "If true, ending a phantom carry after hitting the phantom during that carry grants 2 seconds of Slow Falling I.");
        FabricJsonComments.put(phantoms, "pickup", "Phantom pickup settings (carrying players).");
        FabricJsonComments.put(phantoms, "slowness", "Slowness stacking settings when hit by phantoms.");
        phantoms.addProperty("enabled", enableImprovedPhantoms);
        phantoms.addProperty("doubleDamage", enablePhantomDoubleDamage);
        phantoms.addProperty("autoDrop", enablePhantomAutoDrop);
        phantoms.addProperty("autoDropSeconds", Math.max(1, phantomAutoDropSeconds));
        phantoms.addProperty("dropOnHit", enablePhantomDropOnHit);
        phantoms.addProperty("slowFallingOnHitDismount", enablePhantomSlowFallingOnHitDismount);

        JsonObject pickup = new JsonObject();
        FabricJsonComments.put(pickup, "enabled", "If true, allows phantoms to pick up and carry players.");
        FabricJsonComments.put(pickup, "chance", "Chance (0.0-1.0) for a phantom to attempt picking up a player when attacking.");
        FabricJsonComments.put(pickup, "minDelaySeconds", "Minimum delay (seconds) before the player can dismount.");
        FabricJsonComments.put(pickup, "speedMultiplier", "Speed multiplier while carrying a player (1.0 = normal speed).");
        pickup.addProperty("enabled", enablePhantomPickup);
        pickup.addProperty("chance", phantomPickupChance);
        pickup.addProperty("minDelaySeconds", Math.max(0, phantomPickupMinDelay));
        pickup.addProperty("speedMultiplier", phantomPickupSpeedMultiplier);
        phantoms.add("pickup", pickup);

        JsonObject slowness = new JsonObject();
        FabricJsonComments.put(slowness, "enabled", "If true, enables stacking slowness effect when hit by phantoms.");
        FabricJsonComments.put(slowness, "durationSeconds", "Duration (seconds) of slowness effect applied by phantoms.");
        FabricJsonComments.put(slowness, "maxLevel", "Maximum slowness level that can be stacked.");
        slowness.addProperty("enabled", enablePhantomSlownessStacking);
        slowness.addProperty("durationSeconds", Math.max(1, phantomSlownessDuration));
        slowness.addProperty("maxLevel", Math.max(1, phantomSlownessMaxLevel));
        phantoms.add("slowness", slowness);

        mobs.add("phantoms", phantoms);

        JsonObject spiders = new JsonObject();
        FabricJsonComments.put(spiders, "caveSpidersInCaves", "If true, allows Cave Spiders to spawn in place of Spiders in caves.");
        FabricJsonComments.put(spiders, "caveSpiderReplacementChance", "Chance (0.0-1.0) for a cave Spider to be replaced by a Cave Spider.");
        spiders.addProperty("caveSpidersInCaves", enableCaveSpidersInCaves);
        spiders.addProperty("caveSpiderReplacementChance", caveSpiderReplacementChance);
        mobs.add("spiders", spiders);

        root.add("mobs", mobs);

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
