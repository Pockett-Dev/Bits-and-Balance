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
 * Minimal Fabric-side config gating for loot injections.
 *
 * Config file: config/Bits and Balance/bitsandbalance-loot.json
 *
 * This mirrors the defaults/shape from NeoForge's {@code LootTablesConfig}.
 */
public final class FabricLootConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-loot-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-content.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-loot.json";

    public static volatile boolean enableCustomLootTables = true;
    public static volatile boolean enableDogMusicDisc = true;

    // Logging
    // When false, missing potion/enchantment lookups are silently skipped.
    public static volatile boolean logMissingIds = false;

    // Mineshaft
    public static volatile boolean enableMineshaftLoot = true;
    public static volatile boolean enableMineshaftResurfacingPotion = true;
    public static volatile int mineshaftResurfacingWeight = 5;
    public static volatile int mineshaftResurfacingMinCount = 1;
    public static volatile int mineshaftResurfacingMaxCount = 1;
    public static volatile boolean enableMineshaftReturningPotion = true;
    public static volatile int mineshaftReturningWeight = 1;
    public static volatile int mineshaftReturningMinCount = 1;
    public static volatile int mineshaftReturningMaxCount = 1;
    public static volatile boolean enableMineshaftHastePotion = true;
    public static volatile int mineshaftHasteWeight = 2;
    public static volatile int mineshaftHasteMinCount = 1;
    public static volatile int mineshaftHasteMaxCount = 1;
    public static volatile boolean enableMineshaftStrongHastePotion = true;
    public static volatile int mineshaftStrongHasteWeight = 2;
    public static volatile int mineshaftStrongHasteMinCount = 1;
    public static volatile int mineshaftStrongHasteMaxCount = 1;

    // Stronghold
    public static volatile boolean enableStrongholdLoot = true;
    public static volatile boolean enableStrongholdResurfacingPotion = true;
    public static volatile int strongholdResurfacingWeight = 4;
    public static volatile int strongholdResurfacingMinCount = 1;
    public static volatile int strongholdResurfacingMaxCount = 1;
    public static volatile boolean enableStrongholdReturningPotion = true;
    public static volatile int strongholdReturningWeight = 3;
    public static volatile int strongholdReturningMinCount = 1;
    public static volatile int strongholdReturningMaxCount = 1;

    // Ancient city
    public static volatile boolean enableAncientCityLoot = true;
    public static volatile boolean enableAncientCityResurfacingPotion = true;
    public static volatile int ancientCityResurfacingWeight = 1;
    public static volatile int ancientCityResurfacingMinCount = 1;
    public static volatile int ancientCityResurfacingMaxCount = 1;
    public static volatile boolean enableAncientCityReturningPotion = true;
    public static volatile int ancientCityReturningWeight = 3;
    public static volatile int ancientCityReturningMinCount = 1;
    public static volatile int ancientCityReturningMaxCount = 1;

    // End city
    public static volatile boolean enableEndCityLoot = true;
    public static volatile boolean enableEndCityReturningPotion = true;
    public static volatile int endCityReturningWeight = 5;
    public static volatile int endCityReturningMinCount = 1;
    public static volatile int endCityReturningMaxCount = 1;

    // Aerodynamic book (NeoForge hardcoded 1%): allow disabling on Fabric.
    public static volatile boolean enableEndCityAerodynamicBook = true;
    public static volatile double endCityAerodynamicBookChance = 0.02D;

    // Simple dungeon
    public static volatile boolean enableSimpleDungeonLoot = true;
    public static volatile boolean enableSimpleDungeonResurfacingPotion = true;
    public static volatile int simpleDungeonResurfacingWeight = 3;
    public static volatile int simpleDungeonResurfacingMinCount = 1;
    public static volatile int simpleDungeonResurfacingMaxCount = 1;
    public static volatile boolean enableSimpleDungeonReturningPotion = true;
    public static volatile int simpleDungeonReturningWeight = 6;
    public static volatile int simpleDungeonReturningMinCount = 1;
    public static volatile int simpleDungeonReturningMaxCount = 1;

    // Trial chambers
    public static volatile boolean enableTrialChamberLoot = true;
    public static volatile boolean enableTrialCommonResurfacingPotion = true;
    public static volatile int trialCommonResurfacingWeight = 2;
    public static volatile int trialCommonResurfacingMinCount = 1;
    public static volatile int trialCommonResurfacingMaxCount = 1;
    public static volatile boolean enableTrialRareResurfacingPotion = true;
    public static volatile int trialRareResurfacingWeight = 5;
    public static volatile int trialRareResurfacingMinCount = 1;
    public static volatile int trialRareResurfacingMaxCount = 1;
    public static volatile boolean enableTrialRareReturningPotion = true;
    public static volatile int trialRareReturningWeight = 2;
    public static volatile int trialRareReturningMinCount = 1;
    public static volatile int trialRareReturningMaxCount = 1;
    public static volatile boolean enableTrialUniqueReturningPotion = true;
    public static volatile int trialUniqueReturningWeight = 5;
    public static volatile int trialUniqueReturningMinCount = 1;
    public static volatile int trialUniqueReturningMaxCount = 1;

    private FabricLootConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
    }

    private static void loadOrCreateDefaults() {
        FabricConfigPaths.migrateLegacyIfPresent(FILE_NAME);
        Path configPath = FabricConfigPaths.resolve(FILE_NAME);

        if (!Files.exists(configPath)) {
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

            enableCustomLootTables = getBoolean(obj, "enableCustomLootTables", enableCustomLootTables);
            JsonObject dogMusicDisc = getObject(obj, "dogMusicDisc");
            if (dogMusicDisc != null) {
                enableDogMusicDisc = getBoolean(dogMusicDisc, "enabled", enableDogMusicDisc);
            } else {
                enableDogMusicDisc = getBoolean(obj, "enableDogMusicDisc", enableDogMusicDisc);
            }
            logMissingIds = getBoolean(obj, "logMissingIds", logMissingIds);

            JsonObject mineshaft = getObject(obj, "mineshaftLoot");
            if (mineshaft != null) {
                enableMineshaftLoot = getBoolean(mineshaft, "enableMineshaftLoot", enableMineshaftLoot);
                JsonObject resurfacing = getObject(mineshaft, "resurfacingPotion");
                if (resurfacing != null) {
                    enableMineshaftResurfacingPotion = getBoolean(resurfacing, "enabled", enableMineshaftResurfacingPotion);
                    mineshaftResurfacingWeight = getInt(resurfacing, "weight", mineshaftResurfacingWeight);
                    mineshaftResurfacingMinCount = getInt(resurfacing, "minCount", mineshaftResurfacingMinCount);
                    mineshaftResurfacingMaxCount = getInt(resurfacing, "maxCount", mineshaftResurfacingMaxCount);
                }
                JsonObject returning = getObject(mineshaft, "returningPotion");
                if (returning != null) {
                    enableMineshaftReturningPotion = getBoolean(returning, "enabled", enableMineshaftReturningPotion);
                    mineshaftReturningWeight = getInt(returning, "weight", mineshaftReturningWeight);
                    mineshaftReturningMinCount = getInt(returning, "minCount", mineshaftReturningMinCount);
                    mineshaftReturningMaxCount = getInt(returning, "maxCount", mineshaftReturningMaxCount);
                }
                JsonObject haste = getObject(mineshaft, "hastePotion");
                if (haste != null) {
                    enableMineshaftHastePotion = getBoolean(haste, "enabled", enableMineshaftHastePotion);
                    mineshaftHasteWeight = getInt(haste, "weight", mineshaftHasteWeight);
                    mineshaftHasteMinCount = getInt(haste, "minCount", mineshaftHasteMinCount);
                    mineshaftHasteMaxCount = getInt(haste, "maxCount", mineshaftHasteMaxCount);
                }
                JsonObject strongHaste = getObject(mineshaft, "strongHastePotion");
                if (strongHaste != null) {
                    enableMineshaftStrongHastePotion = getBoolean(strongHaste, "enabled", enableMineshaftStrongHastePotion);
                    mineshaftStrongHasteWeight = getInt(strongHaste, "weight", mineshaftStrongHasteWeight);
                    mineshaftStrongHasteMinCount = getInt(strongHaste, "minCount", mineshaftStrongHasteMinCount);
                    mineshaftStrongHasteMaxCount = getInt(strongHaste, "maxCount", mineshaftStrongHasteMaxCount);
                }
            }

            JsonObject stronghold = getObject(obj, "strongholdLoot");
            if (stronghold != null) {
                enableStrongholdLoot = getBoolean(stronghold, "enableStrongholdLoot", enableStrongholdLoot);
                JsonObject resurfacing = getObject(stronghold, "resurfacingPotion");
                if (resurfacing != null) {
                    enableStrongholdResurfacingPotion = getBoolean(resurfacing, "enabled", enableStrongholdResurfacingPotion);
                    strongholdResurfacingWeight = getInt(resurfacing, "weight", strongholdResurfacingWeight);
                    strongholdResurfacingMinCount = getInt(resurfacing, "minCount", strongholdResurfacingMinCount);
                    strongholdResurfacingMaxCount = getInt(resurfacing, "maxCount", strongholdResurfacingMaxCount);
                }
                JsonObject returning = getObject(stronghold, "returningPotion");
                if (returning != null) {
                    enableStrongholdReturningPotion = getBoolean(returning, "enabled", enableStrongholdReturningPotion);
                    strongholdReturningWeight = getInt(returning, "weight", strongholdReturningWeight);
                    strongholdReturningMinCount = getInt(returning, "minCount", strongholdReturningMinCount);
                    strongholdReturningMaxCount = getInt(returning, "maxCount", strongholdReturningMaxCount);
                }
            }

            JsonObject ancient = getObject(obj, "ancientCityLoot");
            if (ancient != null) {
                enableAncientCityLoot = getBoolean(ancient, "enableAncientCityLoot", enableAncientCityLoot);
                JsonObject resurfacing = getObject(ancient, "resurfacingPotion");
                if (resurfacing != null) {
                    enableAncientCityResurfacingPotion = getBoolean(resurfacing, "enabled", enableAncientCityResurfacingPotion);
                    ancientCityResurfacingWeight = getInt(resurfacing, "weight", ancientCityResurfacingWeight);
                    ancientCityResurfacingMinCount = getInt(resurfacing, "minCount", ancientCityResurfacingMinCount);
                    ancientCityResurfacingMaxCount = getInt(resurfacing, "maxCount", ancientCityResurfacingMaxCount);
                }
                JsonObject returning = getObject(ancient, "returningPotion");
                if (returning != null) {
                    enableAncientCityReturningPotion = getBoolean(returning, "enabled", enableAncientCityReturningPotion);
                    ancientCityReturningWeight = getInt(returning, "weight", ancientCityReturningWeight);
                    ancientCityReturningMinCount = getInt(returning, "minCount", ancientCityReturningMinCount);
                    ancientCityReturningMaxCount = getInt(returning, "maxCount", ancientCityReturningMaxCount);
                }
            }

            JsonObject end = getObject(obj, "endCityLoot");
            if (end != null) {
                enableEndCityLoot = getBoolean(end, "enableEndCityLoot", enableEndCityLoot);
                JsonObject returning = getObject(end, "returningPotion");
                if (returning != null) {
                    enableEndCityReturningPotion = getBoolean(returning, "enabled", enableEndCityReturningPotion);
                    endCityReturningWeight = getInt(returning, "weight", endCityReturningWeight);
                    endCityReturningMinCount = getInt(returning, "minCount", endCityReturningMinCount);
                    endCityReturningMaxCount = getInt(returning, "maxCount", endCityReturningMaxCount);
                }
                JsonObject book = getObject(end, "aerodynamicBook");
                if (book != null) {
                    enableEndCityAerodynamicBook = getBoolean(book, "enabled", enableEndCityAerodynamicBook);
                    endCityAerodynamicBookChance = getDouble(book, "chance", endCityAerodynamicBookChance);
                }
            }

            JsonObject dungeon = getObject(obj, "simpleDungeonLoot");
            if (dungeon != null) {
                enableSimpleDungeonLoot = getBoolean(dungeon, "enableSimpleDungeonLoot", enableSimpleDungeonLoot);
                JsonObject resurfacing = getObject(dungeon, "resurfacingPotion");
                if (resurfacing != null) {
                    enableSimpleDungeonResurfacingPotion = getBoolean(resurfacing, "enabled", enableSimpleDungeonResurfacingPotion);
                    simpleDungeonResurfacingWeight = getInt(resurfacing, "weight", simpleDungeonResurfacingWeight);
                    simpleDungeonResurfacingMinCount = getInt(resurfacing, "minCount", simpleDungeonResurfacingMinCount);
                    simpleDungeonResurfacingMaxCount = getInt(resurfacing, "maxCount", simpleDungeonResurfacingMaxCount);
                }
                JsonObject returning = getObject(dungeon, "returningPotion");
                if (returning != null) {
                    enableSimpleDungeonReturningPotion = getBoolean(returning, "enabled", enableSimpleDungeonReturningPotion);
                    simpleDungeonReturningWeight = getInt(returning, "weight", simpleDungeonReturningWeight);
                    simpleDungeonReturningMinCount = getInt(returning, "minCount", simpleDungeonReturningMinCount);
                    simpleDungeonReturningMaxCount = getInt(returning, "maxCount", simpleDungeonReturningMaxCount);
                }
            }

            JsonObject trial = getObject(obj, "trialChamberLoot");
            if (trial != null) {
                enableTrialChamberLoot = getBoolean(trial, "enableTrialChamberLoot", enableTrialChamberLoot);

                JsonObject common = getObject(trial, "common");
                if (common != null) {
                    JsonObject resurfacing = getObject(common, "resurfacingPotion");
                    if (resurfacing != null) {
                        enableTrialCommonResurfacingPotion = getBoolean(resurfacing, "enabled", enableTrialCommonResurfacingPotion);
                        trialCommonResurfacingWeight = getInt(resurfacing, "weight", trialCommonResurfacingWeight);
                        trialCommonResurfacingMinCount = getInt(resurfacing, "minCount", trialCommonResurfacingMinCount);
                        trialCommonResurfacingMaxCount = getInt(resurfacing, "maxCount", trialCommonResurfacingMaxCount);
                    }
                }

                JsonObject rare = getObject(trial, "rare");
                if (rare != null) {
                    JsonObject resurfacing = getObject(rare, "resurfacingPotion");
                    if (resurfacing != null) {
                        enableTrialRareResurfacingPotion = getBoolean(resurfacing, "enabled", enableTrialRareResurfacingPotion);
                        trialRareResurfacingWeight = getInt(resurfacing, "weight", trialRareResurfacingWeight);
                        trialRareResurfacingMinCount = getInt(resurfacing, "minCount", trialRareResurfacingMinCount);
                        trialRareResurfacingMaxCount = getInt(resurfacing, "maxCount", trialRareResurfacingMaxCount);
                    }

                    JsonObject returning = getObject(rare, "returningPotion");
                    if (returning != null) {
                        enableTrialRareReturningPotion = getBoolean(returning, "enabled", enableTrialRareReturningPotion);
                        trialRareReturningWeight = getInt(returning, "weight", trialRareReturningWeight);
                        trialRareReturningMinCount = getInt(returning, "minCount", trialRareReturningMinCount);
                        trialRareReturningMaxCount = getInt(returning, "maxCount", trialRareReturningMaxCount);
                    }
                }

                JsonObject unique = getObject(trial, "unique");
                if (unique != null) {
                    JsonObject returning = getObject(unique, "returningPotion");
                    if (returning != null) {
                        enableTrialUniqueReturningPotion = getBoolean(returning, "enabled", enableTrialUniqueReturningPotion);
                        trialUniqueReturningWeight = getInt(returning, "weight", trialUniqueReturningWeight);
                        trialUniqueReturningMinCount = getInt(returning, "minCount", trialUniqueReturningMinCount);
                        trialUniqueReturningMaxCount = getInt(returning, "maxCount", trialUniqueReturningMaxCount);
                    }
                }
            }

            clampCounts();
            clampChance();

            // Rewrite config to include any newly added keys with current values.
            try {
                writeDefaults(configPath);
            } catch (IOException e) {
                LOGGER.warn("Failed writing migrated {} (continuing)", configPath, e);
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed reading {} (continuing with defaults)", configPath, e);
        }
    }

    private static void writeDefaults(Path configPath) throws IOException {
        JsonObject root = FabricJsonConfigFiles.readRoot(configPath, GSON);

        FabricJsonComments.put(root, "enableCustomLootTables", "Master toggle for Bits and Balance loot injections.");
        FabricJsonComments.put(root, "dogMusicDisc", "Dog music disc feature toggle.");
        FabricJsonComments.put(root, "logMissingIds", "When true, log missing potion/enchantment ID lookups during loot generation.");
        FabricJsonComments.put(root, "mineshaftLoot", "Loot settings for mineshafts.");
        FabricJsonComments.put(root, "strongholdLoot", "Loot settings for strongholds.");
        FabricJsonComments.put(root, "ancientCityLoot", "Loot settings for ancient cities.");
        FabricJsonComments.put(root, "endCityLoot", "Loot settings for end cities.");
        FabricJsonComments.put(root, "simpleDungeonLoot", "Loot settings for simple dungeons.");
        FabricJsonComments.put(root, "trialChamberLoot", "Loot settings for trial chambers.");

        root.addProperty("enableCustomLootTables", enableCustomLootTables);
        root.add("dogMusicDisc", buildDogMusicDisc());
        root.addProperty("logMissingIds", logMissingIds);

        root.add("mineshaftLoot", buildMineshaft());
        root.add("strongholdLoot", buildStronghold());
        root.add("ancientCityLoot", buildAncient());
        root.add("endCityLoot", buildEndCity());
        root.add("simpleDungeonLoot", buildDungeon());
        root.add("trialChamberLoot", buildTrial());

        FabricJsonConfigFiles.writeRoot(configPath, GSON, root);
        if (!FILE_NAME.equals(LEGACY_FILE_NAME)) {
            FabricJsonConfigFiles.deleteIfExists(LEGACY_FILE_NAME);
        }
        LOGGER.info("Wrote default loot config: {}", configPath);
    }

    private static JsonObject buildDogMusicDisc() {
        JsonObject obj = new JsonObject();
        FabricJsonComments.put(obj, "enabled", "Enable the Dog music disc feature. When false, the disc is hidden from creative tabs and removed from Bits and Balance loot/drop injections.");
        obj.addProperty("enabled", enableDogMusicDisc);
        return obj;
    }

    private static JsonObject buildMineshaft() {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enableMineshaftLoot", "Enable mineshaft loot injections.");
        FabricJsonComments.put(obj, "resurfacingPotion", "Resurfacing potion loot entry.");
        FabricJsonComments.put(obj, "returningPotion", "Returning potion loot entry.");
        FabricJsonComments.put(obj, "hastePotion", "Haste potion loot entry.");
        FabricJsonComments.put(obj, "strongHastePotion", "Strong Haste potion loot entry.");

        obj.addProperty("enableMineshaftLoot", enableMineshaftLoot);
        obj.add("resurfacingPotion", potion(enableMineshaftResurfacingPotion, mineshaftResurfacingWeight, mineshaftResurfacingMinCount, mineshaftResurfacingMaxCount));
        obj.add("returningPotion", potion(enableMineshaftReturningPotion, mineshaftReturningWeight, mineshaftReturningMinCount, mineshaftReturningMaxCount));
        obj.add("hastePotion", potion(enableMineshaftHastePotion, mineshaftHasteWeight, mineshaftHasteMinCount, mineshaftHasteMaxCount));
        obj.add("strongHastePotion", potion(enableMineshaftStrongHastePotion, mineshaftStrongHasteWeight, mineshaftStrongHasteMinCount, mineshaftStrongHasteMaxCount));
        return obj;
    }

    private static JsonObject buildStronghold() {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enableStrongholdLoot", "Enable stronghold loot injections.");
        FabricJsonComments.put(obj, "resurfacingPotion", "Resurfacing potion loot entry.");
        FabricJsonComments.put(obj, "returningPotion", "Returning potion loot entry.");

        obj.addProperty("enableStrongholdLoot", enableStrongholdLoot);
        obj.add("resurfacingPotion", potion(enableStrongholdResurfacingPotion, strongholdResurfacingWeight, strongholdResurfacingMinCount, strongholdResurfacingMaxCount));
        obj.add("returningPotion", potion(enableStrongholdReturningPotion, strongholdReturningWeight, strongholdReturningMinCount, strongholdReturningMaxCount));
        return obj;
    }

    private static JsonObject buildAncient() {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enableAncientCityLoot", "Enable ancient city loot injections.");
        FabricJsonComments.put(obj, "resurfacingPotion", "Resurfacing potion loot entry.");
        FabricJsonComments.put(obj, "returningPotion", "Returning potion loot entry.");

        obj.addProperty("enableAncientCityLoot", enableAncientCityLoot);
        obj.add("resurfacingPotion", potion(enableAncientCityResurfacingPotion, ancientCityResurfacingWeight, ancientCityResurfacingMinCount, ancientCityResurfacingMaxCount));
        obj.add("returningPotion", potion(enableAncientCityReturningPotion, ancientCityReturningWeight, ancientCityReturningMinCount, ancientCityReturningMaxCount));
        return obj;
    }

    private static JsonObject buildEndCity() {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enableEndCityLoot", "Enable end city loot injections.");
        FabricJsonComments.put(obj, "returningPotion", "Returning potion loot entry.");
        FabricJsonComments.put(obj, "aerodynamicBook", "Aerodynamic enchanted book loot entry.");

        obj.addProperty("enableEndCityLoot", enableEndCityLoot);
        obj.add("returningPotion", potion(enableEndCityReturningPotion, endCityReturningWeight, endCityReturningMinCount, endCityReturningMaxCount));

        JsonObject book = new JsonObject();

        FabricJsonComments.put(book, "enabled", "Enable Aerodynamic book appearing in end city loot.");
        FabricJsonComments.put(book, "chance", "Chance (0.0-1.0) of Aerodynamic book appearing.");

        book.addProperty("enabled", enableEndCityAerodynamicBook);
        book.addProperty("chance", endCityAerodynamicBookChance);
        obj.add("aerodynamicBook", book);

        return obj;
    }

    private static JsonObject buildDungeon() {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enableSimpleDungeonLoot", "Enable simple dungeon loot injections.");
        FabricJsonComments.put(obj, "resurfacingPotion", "Resurfacing potion loot entry.");
        FabricJsonComments.put(obj, "returningPotion", "Returning potion loot entry.");

        obj.addProperty("enableSimpleDungeonLoot", enableSimpleDungeonLoot);
        obj.add("resurfacingPotion", potion(enableSimpleDungeonResurfacingPotion, simpleDungeonResurfacingWeight, simpleDungeonResurfacingMinCount, simpleDungeonResurfacingMaxCount));
        obj.add("returningPotion", potion(enableSimpleDungeonReturningPotion, simpleDungeonReturningWeight, simpleDungeonReturningMinCount, simpleDungeonReturningMaxCount));
        return obj;
    }

    private static JsonObject buildTrial() {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enableTrialChamberLoot", "Enable trial chamber loot injections.");
        FabricJsonComments.put(obj, "common", "Common trial loot entries.");
        FabricJsonComments.put(obj, "rare", "Rare trial loot entries.");
        FabricJsonComments.put(obj, "unique", "Unique trial loot entries.");

        obj.addProperty("enableTrialChamberLoot", enableTrialChamberLoot);

        JsonObject common = new JsonObject();

        FabricJsonComments.put(common, "resurfacingPotion", "Resurfacing potion loot entry.");

        common.add("resurfacingPotion", potion(enableTrialCommonResurfacingPotion, trialCommonResurfacingWeight, trialCommonResurfacingMinCount, trialCommonResurfacingMaxCount));
        obj.add("common", common);

        JsonObject rare = new JsonObject();

        FabricJsonComments.put(rare, "resurfacingPotion", "Resurfacing potion loot entry.");
        FabricJsonComments.put(rare, "returningPotion", "Returning potion loot entry.");

        rare.add("resurfacingPotion", potion(enableTrialRareResurfacingPotion, trialRareResurfacingWeight, trialRareResurfacingMinCount, trialRareResurfacingMaxCount));
        rare.add("returningPotion", potion(enableTrialRareReturningPotion, trialRareReturningWeight, trialRareReturningMinCount, trialRareReturningMaxCount));
        obj.add("rare", rare);

        JsonObject unique = new JsonObject();

        FabricJsonComments.put(unique, "returningPotion", "Returning potion loot entry.");

        unique.add("returningPotion", potion(enableTrialUniqueReturningPotion, trialUniqueReturningWeight, trialUniqueReturningMinCount, trialUniqueReturningMaxCount));
        obj.add("unique", unique);

        return obj;
    }

    private static JsonObject potion(boolean enabled, int weight, int minCount, int maxCount) {
        JsonObject obj = new JsonObject();

        FabricJsonComments.put(obj, "enabled", "Enable this loot entry.");
        FabricJsonComments.put(obj, "weight", "Relative weight used by the loot table.");
        FabricJsonComments.put(obj, "minCount", "Minimum stack count.");
        FabricJsonComments.put(obj, "maxCount", "Maximum stack count.");

        obj.addProperty("enabled", enabled);
        obj.addProperty("weight", weight);
        obj.addProperty("minCount", minCount);
        obj.addProperty("maxCount", maxCount);
        return obj;
    }

    private static JsonObject getObject(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonObject()) return null;
        return el.getAsJsonObject();
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isBoolean()) return def;
        return el.getAsBoolean();
    }

    private static int getInt(JsonObject obj, String key, int def) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) return def;
        return el.getAsInt();
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) return def;
        return el.getAsDouble();
    }

    private static void clampCounts() {
        mineshaftResurfacingMinCount = clamp(mineshaftResurfacingMinCount, 1, 8);
        mineshaftResurfacingMaxCount = clamp(mineshaftResurfacingMaxCount, 1, 8);
        mineshaftReturningMinCount = clamp(mineshaftReturningMinCount, 1, 8);
        mineshaftReturningMaxCount = clamp(mineshaftReturningMaxCount, 1, 8);
        mineshaftHasteMinCount = clamp(mineshaftHasteMinCount, 1, 8);
        mineshaftHasteMaxCount = clamp(mineshaftHasteMaxCount, 1, 8);
        mineshaftStrongHasteMinCount = clamp(mineshaftStrongHasteMinCount, 1, 8);
        mineshaftStrongHasteMaxCount = clamp(mineshaftStrongHasteMaxCount, 1, 8);

        strongholdResurfacingMinCount = clamp(strongholdResurfacingMinCount, 1, 8);
        strongholdResurfacingMaxCount = clamp(strongholdResurfacingMaxCount, 1, 8);
        strongholdReturningMinCount = clamp(strongholdReturningMinCount, 1, 8);
        strongholdReturningMaxCount = clamp(strongholdReturningMaxCount, 1, 8);

        ancientCityResurfacingMinCount = clamp(ancientCityResurfacingMinCount, 1, 8);
        ancientCityResurfacingMaxCount = clamp(ancientCityResurfacingMaxCount, 1, 8);
        ancientCityReturningMinCount = clamp(ancientCityReturningMinCount, 1, 8);
        ancientCityReturningMaxCount = clamp(ancientCityReturningMaxCount, 1, 8);

        endCityReturningMinCount = clamp(endCityReturningMinCount, 1, 8);
        endCityReturningMaxCount = clamp(endCityReturningMaxCount, 1, 8);

        simpleDungeonResurfacingMinCount = clamp(simpleDungeonResurfacingMinCount, 1, 8);
        simpleDungeonResurfacingMaxCount = clamp(simpleDungeonResurfacingMaxCount, 1, 8);
        simpleDungeonReturningMinCount = clamp(simpleDungeonReturningMinCount, 1, 8);
        simpleDungeonReturningMaxCount = clamp(simpleDungeonReturningMaxCount, 1, 8);

        trialCommonResurfacingMinCount = clamp(trialCommonResurfacingMinCount, 1, 8);
        trialCommonResurfacingMaxCount = clamp(trialCommonResurfacingMaxCount, 1, 8);
        trialRareResurfacingMinCount = clamp(trialRareResurfacingMinCount, 1, 8);
        trialRareResurfacingMaxCount = clamp(trialRareResurfacingMaxCount, 1, 8);
        trialRareReturningMinCount = clamp(trialRareReturningMinCount, 1, 8);
        trialRareReturningMaxCount = clamp(trialRareReturningMaxCount, 1, 8);
        trialUniqueReturningMinCount = clamp(trialUniqueReturningMinCount, 1, 8);
        trialUniqueReturningMaxCount = clamp(trialUniqueReturningMaxCount, 1, 8);

        fixMinMax();
    }

    private static void fixMinMax() {
        if (mineshaftResurfacingMaxCount < mineshaftResurfacingMinCount) mineshaftResurfacingMaxCount = mineshaftResurfacingMinCount;
        if (mineshaftReturningMaxCount < mineshaftReturningMinCount) mineshaftReturningMaxCount = mineshaftReturningMinCount;
        if (mineshaftHasteMaxCount < mineshaftHasteMinCount) mineshaftHasteMaxCount = mineshaftHasteMinCount;
        if (mineshaftStrongHasteMaxCount < mineshaftStrongHasteMinCount) mineshaftStrongHasteMaxCount = mineshaftStrongHasteMinCount;

        if (strongholdResurfacingMaxCount < strongholdResurfacingMinCount) strongholdResurfacingMaxCount = strongholdResurfacingMinCount;
        if (strongholdReturningMaxCount < strongholdReturningMinCount) strongholdReturningMaxCount = strongholdReturningMinCount;

        if (ancientCityResurfacingMaxCount < ancientCityResurfacingMinCount) ancientCityResurfacingMaxCount = ancientCityResurfacingMinCount;
        if (ancientCityReturningMaxCount < ancientCityReturningMinCount) ancientCityReturningMaxCount = ancientCityReturningMinCount;

        if (endCityReturningMaxCount < endCityReturningMinCount) endCityReturningMaxCount = endCityReturningMinCount;

        if (simpleDungeonResurfacingMaxCount < simpleDungeonResurfacingMinCount) simpleDungeonResurfacingMaxCount = simpleDungeonResurfacingMinCount;
        if (simpleDungeonReturningMaxCount < simpleDungeonReturningMinCount) simpleDungeonReturningMaxCount = simpleDungeonReturningMinCount;

        if (trialCommonResurfacingMaxCount < trialCommonResurfacingMinCount) trialCommonResurfacingMaxCount = trialCommonResurfacingMinCount;
        if (trialRareResurfacingMaxCount < trialRareResurfacingMinCount) trialRareResurfacingMaxCount = trialRareResurfacingMinCount;
        if (trialRareReturningMaxCount < trialRareReturningMinCount) trialRareReturningMaxCount = trialRareReturningMinCount;
        if (trialUniqueReturningMaxCount < trialUniqueReturningMinCount) trialUniqueReturningMaxCount = trialUniqueReturningMinCount;
    }

    private static void clampChance() {
        if (endCityAerodynamicBookChance < 0.0D) endCityAerodynamicBookChance = 0.0D;
        if (endCityAerodynamicBookChance > 1.0D) endCityAerodynamicBookChance = 1.0D;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
