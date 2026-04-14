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
 * Fabric-side config for gameplay tweaks.
 *
 * Config file: config/Bits and Balance/bitsandbalance-tweaks.json
 */
public final class FabricTweaksConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-tweaks-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-gameplay.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-tweaks.json";

    public static volatile boolean enableReturnToKiller = true;
    public static volatile boolean enableTippedArrowLingeringClouds = true;
    public static volatile boolean enableSpeedyHappyGhasts = true;
    public static volatile boolean enableCampfiresIgniteEntities = true;

    public static volatile boolean allowMixedCandlePlacement = true;

    public static volatile boolean enableArmedArmorStands = true;

    public static volatile boolean enableDoubleDoorOpening = true;
    public static volatile boolean doubleDoorCrouchSingle = true;
    public static volatile boolean doubleDoorChainTrapdoors = true;
    public static volatile boolean doubleDoorSameBlockOnly = true;
    public static volatile boolean doubleDoorsWithRedstone = true;
    public static volatile boolean doubleDoorRedstoneIncludeIron = true;
    public static volatile boolean doubleDoorRedstoneIncludeTrapdoors = true;

    public static volatile boolean enableClimbableChainPlacement = true;

    public static volatile boolean enableFastClimbableSlide = true;
    public static volatile double fastClimbableSlideSpeed = 0.4D;
    public static volatile int fastClimbableLookDownMinDeg = 45;
    public static volatile int fastClimbableLookDownMaxDeg = 90;
    public static volatile boolean enableFastClimbableAscend = true;
    public static volatile double fastClimbableAscendSpeed = 0.25D;
    public static volatile int fastClimbableLookUpMinDeg = 45;
    public static volatile int fastClimbableLookUpMaxDeg = 90;

    public static volatile boolean enableSophisticatedScaffolding = true;

    public static volatile boolean enableEnderdragonEggAlways = true;
    public static volatile boolean enderdragonEggUseRandomPlacement = false;
    public static volatile int enderdragonEggSpawnRadius = 10;
    public static volatile int enderdragonEggSpawnDelaySeconds = 10;
    public static volatile boolean enderdragonEggShowParticles = true;

    public static volatile boolean enableImprovedRecoveryCompass = true;
    public static volatile boolean recoveryCompassKeepOnDeath = true;
    public static volatile boolean recoveryCompassShowDistance = true;
    public static volatile boolean recoveryCompassShowCoordsOnRightClick = true;
    public static volatile boolean recoveryCompassSculkParticles = true;

    public static volatile boolean enableAutomaticToolRestock = true;
    public static volatile boolean enableAutomaticBlockRestock = true;

    public static volatile boolean toggleSitting = true;

    public static volatile boolean brushingXpEnabled = true;
    public static volatile int brushingXpMin = 1;
    public static volatile int brushingXpMax = 10;

    public static volatile boolean enableResponsiveShields = true;
    public static volatile int shieldRaiseTime = 0;

    public static volatile boolean enableTreasureEnchantmentGoldColor = true;

    public static volatile boolean enableSugarcaneSand = true;

    public static volatile boolean enableMoreMiningXp = true;
    public static volatile int coalXpMin = 1;
    public static volatile int coalXpMax = 3;
    public static volatile int diamondXpMin = 5;
    public static volatile int diamondXpMax = 10;
    public static volatile int emeraldXpMin = 5;
    public static volatile int emeraldXpMax = 10;
    public static volatile int lapisXpMin = 4;
    public static volatile int lapisXpMax = 8;
    public static volatile int redstoneXpMin = 2;
    public static volatile int redstoneXpMax = 6;

    public static volatile boolean enableGlowingGlowberries = true;
    public static volatile int glowingGlowberriesDuration = 15;
    public static volatile int foxGlowingDuration = 5;

    public static volatile boolean enableRespawnAnchorAnywhere = true;

    public static volatile boolean enableUncapMenuFps = true;

    public static volatile boolean enableItemSharing = true;
    public static volatile double itemShareCooldownSeconds = 1.0D;

    public static volatile boolean curseHidePumpkinOverlayOnVanishing = true;

    public static volatile boolean enableCreeperSunlightBurn = true;

    public static volatile boolean enableCompostableItems = true;
    public static volatile boolean enableCompostableRottenFlesh = true;
    public static volatile boolean enableCompostablePoisonousPotato = true;

    public static volatile boolean enableTorchFuel = true;
    public static volatile int torchFuelBurnTime = 400;

    public static volatile boolean despawnVexWithEvoker = true;
    public static volatile boolean despawnBulletsWithShulker = true;

    public static volatile boolean enableUnbreakableTrialSpawners = true;
    public static volatile boolean enableUnbreakableVaults = true;

    public static volatile boolean enableEnhancedSlabs = true;
    public static volatile boolean enhancedSlabsPlaceOnTop = true;
    public static volatile boolean enhancedSlabsHangBelow = true;
    public static volatile boolean enhancedSlabsKneeSlabMining = true;
    public static volatile boolean enhancedSlabsMixedDoubleSlabs = true;
    public static volatile boolean enhancedSlabsVerticalSlabs = true;
    public static volatile boolean enhancedSlabsSteps = true;

    private FabricTweaksConfig() {
    }

    public static boolean isAnyEnhancedSlabBehaviorEnabled() {
        return enableEnhancedSlabs || enhancedSlabsVerticalSlabs || enhancedSlabsSteps;
    }

    public static boolean isGeneratedEnhancedSlabContentEnabled() {
        return enhancedSlabsVerticalSlabs || enhancedSlabsSteps;
    }

    public static boolean isVerticalSlabRuntimeEnabled() {
        return enhancedSlabsVerticalSlabs;
    }

    public static boolean isStepRuntimeEnabled() {
        return enhancedSlabsSteps;
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

            JsonObject legacyTweaks = getObject(obj, "tweaks");
            JsonObject legacySource = legacyTweaks != null ? legacyTweaks : obj;

            JsonObject returnToKiller = getObject(obj, "returnToKiller");
            if (returnToKiller != null) {
                enableReturnToKiller = getBoolean(returnToKiller, "enabled", enableReturnToKiller);
            } else {
                enableReturnToKiller = getBoolean(legacySource, "enableReturnToKiller", enableReturnToKiller);
            }

            JsonObject tippedArrowLingeringClouds = getObject(obj, "tippedArrowLingeringClouds");
            if (tippedArrowLingeringClouds != null) {
                enableTippedArrowLingeringClouds = getBoolean(tippedArrowLingeringClouds, "enabled", enableTippedArrowLingeringClouds);
            } else {
                enableTippedArrowLingeringClouds = getBoolean(legacySource, "enableTippedArrowLingeringClouds", enableTippedArrowLingeringClouds);
            }

            JsonObject speedyHappyGhasts = getObject(obj, "speedyHappyGhasts");
            if (speedyHappyGhasts != null) {
                enableSpeedyHappyGhasts = getBoolean(speedyHappyGhasts, "enabled", enableSpeedyHappyGhasts);
            } else {
                enableSpeedyHappyGhasts = getBoolean(legacySource, "enableSpeedyHappyGhasts", enableSpeedyHappyGhasts);
            }

            JsonObject campfires = getObject(obj, "campfires");
            if (campfires != null) {
                enableCampfiresIgniteEntities = getBoolean(campfires, "igniteEntities", enableCampfiresIgniteEntities);
            } else {
                enableCampfiresIgniteEntities = getBoolean(legacySource, "enableCampfiresIgniteEntities", enableCampfiresIgniteEntities);
            }

            JsonObject candleBundles = getObject(obj, "candleBundles");
            if (candleBundles != null) {
                allowMixedCandlePlacement = getBoolean(candleBundles, "allowMixedCandlePlacement", allowMixedCandlePlacement);
            } else {
                allowMixedCandlePlacement = getBoolean(legacySource, "allowMixedCandlePlacement", allowMixedCandlePlacement);
            }

            JsonObject armedArmorStands = getObject(obj, "armedArmorStands");
            if (armedArmorStands != null) {
                enableArmedArmorStands = getBoolean(armedArmorStands, "enabled", enableArmedArmorStands);
            } else {
                enableArmedArmorStands = getBoolean(legacySource, "enableArmedArmorStands", enableArmedArmorStands);
            }

            JsonObject doubleDoorOpening = getObject(obj, "doubleDoorOpening");
            if (doubleDoorOpening != null) {
                enableDoubleDoorOpening = getBoolean(doubleDoorOpening, "enabled", enableDoubleDoorOpening);
                doubleDoorCrouchSingle = getBoolean(doubleDoorOpening, "crouchSingle", doubleDoorCrouchSingle);
                doubleDoorChainTrapdoors = getBoolean(doubleDoorOpening, "chainTrapdoors", doubleDoorChainTrapdoors);
                doubleDoorSameBlockOnly = getBoolean(doubleDoorOpening, "sameBlockOnly", doubleDoorSameBlockOnly);
                doubleDoorsWithRedstone = getBoolean(doubleDoorOpening, "withRedstone", doubleDoorsWithRedstone);
                doubleDoorRedstoneIncludeIron = getBoolean(doubleDoorOpening, "redstoneIncludeIron", doubleDoorRedstoneIncludeIron);
                doubleDoorRedstoneIncludeTrapdoors = getBoolean(doubleDoorOpening, "redstoneIncludeTrapdoors", doubleDoorRedstoneIncludeTrapdoors);
            } else {
                enableDoubleDoorOpening = getBoolean(legacySource, "enableDoubleDoorOpening", enableDoubleDoorOpening);
                doubleDoorCrouchSingle = getBoolean(legacySource, "doubleDoorCrouchSingle", doubleDoorCrouchSingle);
                doubleDoorChainTrapdoors = getBoolean(legacySource, "doubleDoorChainTrapdoors", doubleDoorChainTrapdoors);
                doubleDoorSameBlockOnly = getBoolean(legacySource, "doubleDoorSameBlockOnly", doubleDoorSameBlockOnly);
                doubleDoorsWithRedstone = getBoolean(legacySource, "doubleDoorsWithRedstone", doubleDoorsWithRedstone);
                doubleDoorRedstoneIncludeIron = getBoolean(legacySource, "doubleDoorRedstoneIncludeIron", doubleDoorRedstoneIncludeIron);
                doubleDoorRedstoneIncludeTrapdoors = getBoolean(legacySource, "doubleDoorRedstoneIncludeTrapdoors", doubleDoorRedstoneIncludeTrapdoors);
            }

            JsonObject improvedClimbing = getObject(obj, "improvedClimbing");
            if (improvedClimbing != null) {
                enableClimbableChainPlacement = getBoolean(improvedClimbing, "chainPlacement", enableClimbableChainPlacement);
                enableFastClimbableSlide = getBoolean(improvedClimbing, "enabled", enableFastClimbableSlide);
                fastClimbableSlideSpeed = getDouble(improvedClimbing, "downSpeed", fastClimbableSlideSpeed);
                fastClimbableLookDownMinDeg = getInt(improvedClimbing, "lookDownMinDeg", fastClimbableLookDownMinDeg);
                fastClimbableLookDownMaxDeg = getInt(improvedClimbing, "lookDownMaxDeg", fastClimbableLookDownMaxDeg);
                enableFastClimbableAscend = getBoolean(improvedClimbing, "upEnabled", enableFastClimbableAscend);
                fastClimbableAscendSpeed = getDouble(improvedClimbing, "upSpeed", fastClimbableAscendSpeed);
                fastClimbableLookUpMinDeg = getInt(improvedClimbing, "lookUpMinDeg", fastClimbableLookUpMinDeg);
                fastClimbableLookUpMaxDeg = getInt(improvedClimbing, "lookUpMaxDeg", fastClimbableLookUpMaxDeg);
            } else {
                JsonObject climbableChains = getObject(obj, "climbableChains");
                if (climbableChains != null) {
                    enableClimbableChainPlacement = getBoolean(climbableChains, "enabled", enableClimbableChainPlacement);
                } else {
                    enableClimbableChainPlacement = getBoolean(legacySource, "enableClimbableChainPlacement", enableClimbableChainPlacement);
                }

                JsonObject fastClimbableSlide = getObject(obj, "fastClimbableSlide");
                if (fastClimbableSlide != null) {
                    enableFastClimbableSlide = getBoolean(fastClimbableSlide, "enabled", enableFastClimbableSlide);
                    fastClimbableSlideSpeed = getDouble(fastClimbableSlide, "speed", fastClimbableSlideSpeed);
                    fastClimbableLookDownMinDeg = getInt(fastClimbableSlide, "lookDownMinDeg", fastClimbableLookDownMinDeg);
                    fastClimbableLookDownMaxDeg = getInt(fastClimbableSlide, "lookDownMaxDeg", fastClimbableLookDownMaxDeg);
                    enableFastClimbableAscend = getBoolean(fastClimbableSlide, "upEnabled", enableFastClimbableAscend);
                    fastClimbableAscendSpeed = getDouble(fastClimbableSlide, "upSpeed", fastClimbableAscendSpeed);
                    fastClimbableLookUpMinDeg = getInt(fastClimbableSlide, "lookUpMinDeg", fastClimbableLookUpMinDeg);
                    fastClimbableLookUpMaxDeg = getInt(fastClimbableSlide, "lookUpMaxDeg", fastClimbableLookUpMaxDeg);
                } else {
                    enableFastClimbableSlide = getBoolean(legacySource, "enableFastClimbableSlide", enableFastClimbableSlide);
                    fastClimbableSlideSpeed = getDouble(legacySource, "fastClimbableSlideSpeed", fastClimbableSlideSpeed);
                    fastClimbableLookDownMinDeg = getInt(legacySource, "fastClimbableLookDownMinDeg", fastClimbableLookDownMinDeg);
                    fastClimbableLookDownMaxDeg = getInt(legacySource, "fastClimbableLookDownMaxDeg", fastClimbableLookDownMaxDeg);
                    enableFastClimbableAscend = getBoolean(legacySource, "enableFastClimbableAscend", enableFastClimbableAscend);
                    fastClimbableAscendSpeed = getDouble(legacySource, "fastClimbableAscendSpeed", fastClimbableAscendSpeed);
                    fastClimbableLookUpMinDeg = getInt(legacySource, "fastClimbableLookUpMinDeg", fastClimbableLookUpMinDeg);
                    fastClimbableLookUpMaxDeg = getInt(legacySource, "fastClimbableLookUpMaxDeg", fastClimbableLookUpMaxDeg);
                }
            }

            JsonObject sophisticatedScaffolding = getObject(obj, "sophisticatedScaffolding");
            if (sophisticatedScaffolding != null) {
                enableSophisticatedScaffolding = getBoolean(sophisticatedScaffolding, "enabled", enableSophisticatedScaffolding);
            } else {
                enableSophisticatedScaffolding = getBoolean(legacySource, "enableSophisticatedScaffolding", enableSophisticatedScaffolding);
            }

            JsonObject enderdragonEgg = getObject(obj, "enderdragonEgg");
            if (enderdragonEgg != null) {
                enableEnderdragonEggAlways = getBoolean(enderdragonEgg, "enabled", enableEnderdragonEggAlways);
                enderdragonEggUseRandomPlacement = getBoolean(enderdragonEgg, "useRandomPlacement", enderdragonEggUseRandomPlacement);
                enderdragonEggSpawnRadius = getInt(enderdragonEgg, "spawnRadius", enderdragonEggSpawnRadius);
                enderdragonEggSpawnDelaySeconds = getInt(enderdragonEgg, "spawnDelaySeconds", enderdragonEggSpawnDelaySeconds);
                enderdragonEggShowParticles = getBoolean(enderdragonEgg, "showParticles", enderdragonEggShowParticles);
            } else {
                enableEnderdragonEggAlways = getBoolean(legacySource, "enableEnderdragonEggAlways", enableEnderdragonEggAlways);
                enderdragonEggUseRandomPlacement = getBoolean(legacySource, "enderdragonEggUseRandomPlacement", enderdragonEggUseRandomPlacement);
                enderdragonEggSpawnRadius = getInt(legacySource, "enderdragonEggSpawnRadius", enderdragonEggSpawnRadius);
                enderdragonEggSpawnDelaySeconds = getInt(legacySource, "enderdragonEggSpawnDelaySeconds", enderdragonEggSpawnDelaySeconds);
                enderdragonEggShowParticles = getBoolean(legacySource, "enderdragonEggShowParticles", enderdragonEggShowParticles);
            }

            JsonObject improvedRecoveryCompass = getObject(obj, "improvedRecoveryCompass");
            if (improvedRecoveryCompass != null) {
                enableImprovedRecoveryCompass = getBoolean(improvedRecoveryCompass, "enabled", enableImprovedRecoveryCompass);
                recoveryCompassKeepOnDeath = getBoolean(improvedRecoveryCompass, "keepOnDeath", recoveryCompassKeepOnDeath);
                recoveryCompassShowDistance = getBoolean(improvedRecoveryCompass, "showDistance", recoveryCompassShowDistance);
                recoveryCompassShowCoordsOnRightClick = getBoolean(improvedRecoveryCompass, "showCoordsOnRightClick", recoveryCompassShowCoordsOnRightClick);
                recoveryCompassSculkParticles = getBoolean(improvedRecoveryCompass, "sculkParticles", recoveryCompassSculkParticles);
            } else {
                enableImprovedRecoveryCompass = getBoolean(legacySource, "enableImprovedRecoveryCompass", enableImprovedRecoveryCompass);
                recoveryCompassKeepOnDeath = getBoolean(legacySource, "recoveryCompassKeepOnDeath", recoveryCompassKeepOnDeath);
                recoveryCompassShowDistance = getBoolean(legacySource, "recoveryCompassShowDistance", recoveryCompassShowDistance);
                recoveryCompassShowCoordsOnRightClick = getBoolean(legacySource, "recoveryCompassShowCoordsOnRightClick", recoveryCompassShowCoordsOnRightClick);
                recoveryCompassSculkParticles = getBoolean(legacySource, "recoveryCompassSculkParticles", recoveryCompassSculkParticles);
            }

            JsonObject automaticToolRestock = getObject(obj, "automaticToolRestock");
            if (automaticToolRestock != null) {
                enableAutomaticToolRestock = getBoolean(automaticToolRestock, "enabled", enableAutomaticToolRestock);
            } else {
                enableAutomaticToolRestock = getBoolean(legacySource, "enableAutomaticToolRestock", enableAutomaticToolRestock);
            }

            JsonObject automaticBlockRestock = getObject(obj, "automaticBlockRestock");
            if (automaticBlockRestock != null) {
                enableAutomaticBlockRestock = getBoolean(automaticBlockRestock, "enabled", enableAutomaticBlockRestock);
            } else {
                enableAutomaticBlockRestock = getBoolean(legacySource, "enableAutomaticBlockRestock", enableAutomaticBlockRestock);
            }

            JsonObject sitting = getObject(obj, "sitting");
            if (sitting != null) {
                toggleSitting = getBoolean(sitting, "toggle", toggleSitting);
            } else {
                toggleSitting = getBoolean(legacySource, "toggleSitting", toggleSitting);
            }

            JsonObject responsiveShields = getObject(obj, "responsiveShields");
            if (responsiveShields != null) {
                enableResponsiveShields = getBoolean(responsiveShields, "enabled", enableResponsiveShields);
                shieldRaiseTime = getInt(responsiveShields, "raiseTime", shieldRaiseTime);
            } else {
                enableResponsiveShields = getBoolean(legacySource, "enableResponsiveShields", enableResponsiveShields);
                shieldRaiseTime = getInt(legacySource, "shieldRaiseTime", shieldRaiseTime);
            }

            JsonObject treasureEnchantmentColors = getObject(obj, "treasureEnchantmentColors");
            if (treasureEnchantmentColors != null) {
                enableTreasureEnchantmentGoldColor = getBoolean(treasureEnchantmentColors, "goldColor", enableTreasureEnchantmentGoldColor);
            } else {
                enableTreasureEnchantmentGoldColor = getBoolean(legacySource, "enableTreasureEnchantmentGoldColor", enableTreasureEnchantmentGoldColor);
            }

            JsonObject sugarcane = getObject(obj, "sugarcane");
            if (sugarcane != null) {
                enableSugarcaneSand = getBoolean(sugarcane, "allowSand", enableSugarcaneSand);
            } else {
                enableSugarcaneSand = getBoolean(legacySource, "enableSugarcaneSand", enableSugarcaneSand);
            }

            JsonObject moreMiningXp = getObject(obj, "moreMiningXp");
            if (moreMiningXp != null) {
                enableMoreMiningXp = getBoolean(moreMiningXp, "enabled", enableMoreMiningXp);
                coalXpMin = getInt(moreMiningXp, "coalMin", coalXpMin);
                coalXpMax = getInt(moreMiningXp, "coalMax", coalXpMax);
                diamondXpMin = getInt(moreMiningXp, "diamondMin", diamondXpMin);
                diamondXpMax = getInt(moreMiningXp, "diamondMax", diamondXpMax);
                emeraldXpMin = getInt(moreMiningXp, "emeraldMin", emeraldXpMin);
                emeraldXpMax = getInt(moreMiningXp, "emeraldMax", emeraldXpMax);
                lapisXpMin = getInt(moreMiningXp, "lapisMin", lapisXpMin);
                lapisXpMax = getInt(moreMiningXp, "lapisMax", lapisXpMax);
                redstoneXpMin = getInt(moreMiningXp, "redstoneMin", redstoneXpMin);
                redstoneXpMax = getInt(moreMiningXp, "redstoneMax", redstoneXpMax);
            } else {
                enableMoreMiningXp = getBoolean(legacySource, "enableMoreMiningXp", enableMoreMiningXp);
                coalXpMin = getInt(legacySource, "coalXpMin", coalXpMin);
                coalXpMax = getInt(legacySource, "coalXpMax", coalXpMax);
                diamondXpMin = getInt(legacySource, "diamondXpMin", diamondXpMin);
                diamondXpMax = getInt(legacySource, "diamondXpMax", diamondXpMax);
                emeraldXpMin = getInt(legacySource, "emeraldXpMin", emeraldXpMin);
                emeraldXpMax = getInt(legacySource, "emeraldXpMax", emeraldXpMax);
                lapisXpMin = getInt(legacySource, "lapisXpMin", lapisXpMin);
                lapisXpMax = getInt(legacySource, "lapisXpMax", lapisXpMax);
                redstoneXpMin = getInt(legacySource, "redstoneXpMin", redstoneXpMin);
                redstoneXpMax = getInt(legacySource, "redstoneXpMax", redstoneXpMax);
            }

            JsonObject glowingGlowberries = getObject(obj, "glowingGlowberries");
            if (glowingGlowberries != null) {
                enableGlowingGlowberries = getBoolean(glowingGlowberries, "enabled", enableGlowingGlowberries);
                glowingGlowberriesDuration = getInt(glowingGlowberries, "playerDurationSeconds", glowingGlowberriesDuration);
                foxGlowingDuration = getInt(glowingGlowberries, "foxDurationSeconds", foxGlowingDuration);
            } else {
                enableGlowingGlowberries = getBoolean(legacySource, "enableGlowingGlowberries", enableGlowingGlowberries);
                glowingGlowberriesDuration = getInt(legacySource, "glowingGlowberriesDuration", glowingGlowberriesDuration);
                foxGlowingDuration = getInt(legacySource, "foxGlowingDuration", foxGlowingDuration);
            }

            JsonObject respawnAnchor = getObject(obj, "respawnAnchor");
            if (respawnAnchor != null) {
                enableRespawnAnchorAnywhere = getBoolean(respawnAnchor, "allowAnywhere", enableRespawnAnchorAnywhere);
            } else {
                enableRespawnAnchorAnywhere = getBoolean(legacySource, "enableRespawnAnchorAnywhere", enableRespawnAnchorAnywhere);
            }

            JsonObject performance = getObject(obj, "performance");
            if (performance != null) {
                enableUncapMenuFps = getBoolean(performance, "enableUncapMenuFps", enableUncapMenuFps);
            } else {
                enableUncapMenuFps = getBoolean(legacySource, "enableUncapMenuFps", enableUncapMenuFps);
            }

            JsonObject itemSharing = getObject(obj, "itemSharing");
            if (itemSharing != null) {
                enableItemSharing = getBoolean(itemSharing, "enabled", enableItemSharing);
                itemShareCooldownSeconds = getDouble(itemSharing, "cooldownSeconds", itemShareCooldownSeconds);
            } else {
                enableItemSharing = getBoolean(legacySource, "enableItemSharing", enableItemSharing);
                itemShareCooldownSeconds = getDouble(legacySource, "itemShareCooldownSeconds", itemShareCooldownSeconds);
            }

            JsonObject curses = getObject(obj, "curses");
            if (curses != null) {
                curseHidePumpkinOverlayOnVanishing = getBoolean(curses, "hidePumpkinOverlayOnVanishing", curseHidePumpkinOverlayOnVanishing);
            } else {
                curseHidePumpkinOverlayOnVanishing = getBoolean(legacySource, "curseHidePumpkinOverlayOnVanishing", curseHidePumpkinOverlayOnVanishing);
            }

            JsonObject creeperSunlightBurn = getObject(obj, "creeperSunlightBurn");
            if (creeperSunlightBurn != null) {
                enableCreeperSunlightBurn = getBoolean(creeperSunlightBurn, "enabled", enableCreeperSunlightBurn);
            } else {
                enableCreeperSunlightBurn = getBoolean(legacySource, "enableCreeperSunlightBurn", enableCreeperSunlightBurn);
            }

            JsonObject compostableItems = getObject(obj, "compostableItems");
            if (compostableItems != null) {
                enableCompostableItems = getBoolean(compostableItems, "enabled", enableCompostableItems);
                enableCompostableRottenFlesh = getBoolean(compostableItems, "rottenFlesh", enableCompostableRottenFlesh);
                enableCompostablePoisonousPotato = getBoolean(compostableItems, "poisonousPotato", enableCompostablePoisonousPotato);
            } else {
                enableCompostableItems = getBoolean(legacySource, "enableCompostableItems", enableCompostableItems);
                enableCompostableRottenFlesh = getBoolean(legacySource, "enableCompostableRottenFlesh", enableCompostableRottenFlesh);
                enableCompostablePoisonousPotato = getBoolean(legacySource, "enableCompostablePoisonousPotato", enableCompostablePoisonousPotato);
            }

            JsonObject fuelTweaks = getObject(obj, "fuelTweaks");
            if (fuelTweaks != null) {
                enableTorchFuel = getBoolean(fuelTweaks, "enableTorchFuel", enableTorchFuel);
                torchFuelBurnTime = getInt(fuelTweaks, "torchBurnTime", torchFuelBurnTime);
            } else {
                enableTorchFuel = getBoolean(legacySource, "enableTorchFuel", enableTorchFuel);
                torchFuelBurnTime = getInt(legacySource, "torchFuelBurnTime", torchFuelBurnTime);
            }

            JsonObject despawnWithMaster = getObject(obj, "despawnWithMaster");
            if (despawnWithMaster != null) {
                despawnVexWithEvoker = getBoolean(despawnWithMaster, "vexWithEvoker", despawnVexWithEvoker);
                despawnBulletsWithShulker = getBoolean(despawnWithMaster, "bulletsWithShulker", despawnBulletsWithShulker);
            } else {
                despawnVexWithEvoker = getBoolean(legacySource, "despawnVexWithEvoker", despawnVexWithEvoker);
                despawnBulletsWithShulker = getBoolean(legacySource, "despawnBulletsWithShulker", despawnBulletsWithShulker);
            }

            JsonObject enhancedSlabs = getObject(obj, "enhancedSlabs");
            if (enhancedSlabs != null) {
                enableEnhancedSlabs = getBoolean(enhancedSlabs, "enabled", enableEnhancedSlabs);
                enhancedSlabsPlaceOnTop = getBoolean(enhancedSlabs, "placeOnTop", enhancedSlabsPlaceOnTop);
                enhancedSlabsHangBelow = getBoolean(enhancedSlabs, "hangBelow", enhancedSlabsHangBelow);
                enhancedSlabsKneeSlabMining = getBoolean(enhancedSlabs, "kneeSlabMining", enhancedSlabsKneeSlabMining);
                enhancedSlabsMixedDoubleSlabs = getBoolean(enhancedSlabs, "mixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
            } else {
                enableEnhancedSlabs = getBoolean(legacySource, "enableEnhancedSlabs", enableEnhancedSlabs);
                enhancedSlabsPlaceOnTop = getBoolean(legacySource, "enhancedSlabsPlaceOnTop", enhancedSlabsPlaceOnTop);
                enhancedSlabsHangBelow = getBoolean(legacySource, "enhancedSlabsHangBelow", enhancedSlabsHangBelow);
                enhancedSlabsKneeSlabMining = getBoolean(legacySource, "enhancedSlabsKneeSlabMining", enhancedSlabsKneeSlabMining);
                enhancedSlabsMixedDoubleSlabs = getBoolean(legacySource, "enhancedSlabsMixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
            }

            JsonObject unbreakableBlocks = getObject(obj, "unbreakableBlocks");
            if (unbreakableBlocks != null) {
                enableUnbreakableTrialSpawners = getBoolean(unbreakableBlocks, "trialSpawners", enableUnbreakableTrialSpawners);
                enableUnbreakableVaults = getBoolean(unbreakableBlocks, "vaults", enableUnbreakableVaults);
            } else {
                enableUnbreakableTrialSpawners = getBoolean(legacySource, "enableUnbreakableTrialSpawners", enableUnbreakableTrialSpawners);
                enableUnbreakableVaults = getBoolean(legacySource, "enableUnbreakableVaults", enableUnbreakableVaults);
            }

            enhancedSlabsVerticalSlabs = FabricBuildingConfig.enhancedSlabsVerticalSlabs;
            enhancedSlabsSteps = FabricBuildingConfig.enhancedSlabsSteps;

            JsonObject brushingXp = getObject(obj, "brushingXp");
            if (brushingXp != null) {
                brushingXpEnabled = getBoolean(brushingXp, "enabled", brushingXpEnabled);
                brushingXpMin = getInt(brushingXp, "min", brushingXpMin);
                brushingXpMax = getInt(brushingXp, "max", brushingXpMax);
            } else {
                brushingXpEnabled = getBoolean(obj, "brushingXpEnabled", brushingXpEnabled);
                brushingXpMin = getInt(obj, "brushingXpMin", brushingXpMin);
                brushingXpMax = getInt(obj, "brushingXpMax", brushingXpMax);
            }

            if (brushingXpMin < 0) brushingXpMin = 0;
            if (brushingXpMax < brushingXpMin) brushingXpMax = brushingXpMin;
            if (shieldRaiseTime < 0) shieldRaiseTime = 0;

            coalXpMin = clampInt(coalXpMin, 0, 100);
            coalXpMax = clampInt(Math.max(coalXpMin, coalXpMax), 0, 100);
            diamondXpMin = clampInt(diamondXpMin, 0, 100);
            diamondXpMax = clampInt(Math.max(diamondXpMin, diamondXpMax), 0, 100);
            emeraldXpMin = clampInt(emeraldXpMin, 0, 100);
            emeraldXpMax = clampInt(Math.max(emeraldXpMin, emeraldXpMax), 0, 100);
            lapisXpMin = clampInt(lapisXpMin, 0, 100);
            lapisXpMax = clampInt(Math.max(lapisXpMin, lapisXpMax), 0, 100);
            redstoneXpMin = clampInt(redstoneXpMin, 0, 100);
            redstoneXpMax = clampInt(Math.max(redstoneXpMin, redstoneXpMax), 0, 100);

            glowingGlowberriesDuration = clampInt(glowingGlowberriesDuration, 1, 300);
            foxGlowingDuration = clampInt(foxGlowingDuration, 1, 300);
            enderdragonEggSpawnRadius = clampInt(enderdragonEggSpawnRadius, 1, 50);
            enderdragonEggSpawnDelaySeconds = clampInt(enderdragonEggSpawnDelaySeconds, 1, 60);
            itemShareCooldownSeconds = clampDouble(itemShareCooldownSeconds, 0.0D, 60.0D);
            torchFuelBurnTime = Math.max(0, torchFuelBurnTime);
            fastClimbableSlideSpeed = clampDouble(fastClimbableSlideSpeed, 0.05D, 1.0D);
            fastClimbableLookDownMinDeg = clampInt(fastClimbableLookDownMinDeg, 0, 90);
            fastClimbableLookDownMaxDeg = clampInt(Math.max(fastClimbableLookDownMinDeg, fastClimbableLookDownMaxDeg), 0, 90);
            fastClimbableAscendSpeed = clampDouble(fastClimbableAscendSpeed, 0.2D, 1.0D);
            fastClimbableLookUpMinDeg = clampInt(fastClimbableLookUpMinDeg, 0, 90);
            fastClimbableLookUpMaxDeg = clampInt(Math.max(fastClimbableLookUpMinDeg, fastClimbableLookUpMaxDeg), 0, 90);

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

        JsonObject returnToKiller = new JsonObject();
        FabricJsonComments.put(root, "returnToKiller", "Return items and XP toward the killer.");
        FabricJsonComments.put(returnToKiller, "enabled", "Enable the Return-to-Killer mechanic.");
        returnToKiller.addProperty("enabled", enableReturnToKiller);
        root.add("returnToKiller", returnToKiller);

        JsonObject tippedArrowLingeringClouds = new JsonObject();
        FabricJsonComments.put(root, "tippedArrowLingeringClouds", "Allow tipped arrows to create lingering effect clouds.");
        FabricJsonComments.put(tippedArrowLingeringClouds, "enabled", "Enable lingering clouds from tipped arrows.");
        tippedArrowLingeringClouds.addProperty("enabled", enableTippedArrowLingeringClouds);
        root.add("tippedArrowLingeringClouds", tippedArrowLingeringClouds);

        JsonObject speedyHappyGhasts = new JsonObject();
        FabricJsonComments.put(root, "speedyHappyGhasts", "Speed adjustments for happy ghasts.");
        FabricJsonComments.put(speedyHappyGhasts, "enabled", "Enable faster happy ghast behavior tweaks.");
        speedyHappyGhasts.addProperty("enabled", enableSpeedyHappyGhasts);
        root.add("speedyHappyGhasts", speedyHappyGhasts);

        JsonObject campfires = new JsonObject();
        FabricJsonComments.put(root, "campfires", "Campfire behavior tweaks.");
        FabricJsonComments.put(campfires, "igniteEntities", "Allow campfires to ignite entities standing on them.");
        campfires.addProperty("igniteEntities", enableCampfiresIgniteEntities);
        root.add("campfires", campfires);

        JsonObject candleBundles = new JsonObject();
        FabricJsonComments.put(root, "candleBundles", "Multi-candle placement behavior.");
        FabricJsonComments.put(candleBundles, "allowMixedCandlePlacement", "Allow mixing different candle items in one candle bundle block.");
        candleBundles.addProperty("allowMixedCandlePlacement", allowMixedCandlePlacement);
        root.add("candleBundles", candleBundles);

        JsonObject armedArmorStands = new JsonObject();
        FabricJsonComments.put(root, "armedArmorStands", "Armor stand hand-item display behavior.");
        FabricJsonComments.put(armedArmorStands, "enabled", "Allow armor stands to display held items.");
        armedArmorStands.addProperty("enabled", enableArmedArmorStands);
        root.add("armedArmorStands", armedArmorStands);

        JsonObject doubleDoorOpening = new JsonObject();
        FabricJsonComments.put(root, "doubleDoorOpening", "Open adjacent doors and trapdoors together.");
        FabricJsonComments.put(doubleDoorOpening, "enabled", "Enable paired door and trapdoor opening.");
        FabricJsonComments.put(doubleDoorOpening, "crouchSingle", "When sneaking, only open the interacted door or trapdoor.");
        FabricJsonComments.put(doubleDoorOpening, "chainTrapdoors", "Include chained trapdoors in double-door logic.");
        FabricJsonComments.put(doubleDoorOpening, "sameBlockOnly", "Only pair doors or chain trapdoors when the adjacent block is the exact same block.");
        FabricJsonComments.put(doubleDoorOpening, "withRedstone", "Allow redstone to open paired doors.");
        FabricJsonComments.put(doubleDoorOpening, "redstoneIncludeIron", "Include iron and copper doors in redstone pairing.");
        FabricJsonComments.put(doubleDoorOpening, "redstoneIncludeTrapdoors", "Include trapdoors in redstone pairing.");
        doubleDoorOpening.addProperty("enabled", enableDoubleDoorOpening);
        doubleDoorOpening.addProperty("crouchSingle", doubleDoorCrouchSingle);
        doubleDoorOpening.addProperty("chainTrapdoors", doubleDoorChainTrapdoors);
        doubleDoorOpening.addProperty("sameBlockOnly", doubleDoorSameBlockOnly);
        doubleDoorOpening.addProperty("withRedstone", doubleDoorsWithRedstone);
        doubleDoorOpening.addProperty("redstoneIncludeIron", doubleDoorRedstoneIncludeIron);
        doubleDoorOpening.addProperty("redstoneIncludeTrapdoors", doubleDoorRedstoneIncludeTrapdoors);
        root.add("doubleDoorOpening", doubleDoorOpening);

        JsonObject improvedClimbing = new JsonObject();
        FabricJsonComments.put(root, "improvedClimbing", "Improved climbing behavior for every climbable block.");
        FabricJsonComments.put(improvedClimbing, "chainPlacement", "Allow placing another climbable at the end of an existing climbable chain.");
        FabricJsonComments.put(improvedClimbing, "enabled", "Enable faster downward climbing while looking down.");
        FabricJsonComments.put(improvedClimbing, "downSpeed", "Maximum downward climbing speed while active.");
        FabricJsonComments.put(improvedClimbing, "lookDownMinDeg", "Minimum look-down angle in degrees to start faster descent.");
        FabricJsonComments.put(improvedClimbing, "lookDownMaxDeg", "Maximum look-down angle used for full descent speed.");
        FabricJsonComments.put(improvedClimbing, "upEnabled", "Enable faster upward climbing while looking up.");
        FabricJsonComments.put(improvedClimbing, "upSpeed", "Maximum upward climbing speed while active.");
        FabricJsonComments.put(improvedClimbing, "lookUpMinDeg", "Minimum look-up angle in degrees to start faster ascent.");
        FabricJsonComments.put(improvedClimbing, "lookUpMaxDeg", "Maximum look-up angle used for full ascent speed.");
        improvedClimbing.addProperty("chainPlacement", enableClimbableChainPlacement);
        improvedClimbing.addProperty("enabled", enableFastClimbableSlide);
        improvedClimbing.addProperty("downSpeed", fastClimbableSlideSpeed);
        improvedClimbing.addProperty("lookDownMinDeg", fastClimbableLookDownMinDeg);
        improvedClimbing.addProperty("lookDownMaxDeg", fastClimbableLookDownMaxDeg);
        improvedClimbing.addProperty("upEnabled", enableFastClimbableAscend);
        improvedClimbing.addProperty("upSpeed", fastClimbableAscendSpeed);
        improvedClimbing.addProperty("lookUpMinDeg", fastClimbableLookUpMinDeg);
        improvedClimbing.addProperty("lookUpMaxDeg", fastClimbableLookUpMaxDeg);
        root.add("improvedClimbing", improvedClimbing);

        JsonObject sophisticatedScaffolding = new JsonObject();
        FabricJsonComments.put(root, "sophisticatedScaffolding", "Additional scaffolding behavior tweaks.");
        FabricJsonComments.put(sophisticatedScaffolding, "enabled", "Enable sophisticated scaffolding behavior.");
        sophisticatedScaffolding.addProperty("enabled", enableSophisticatedScaffolding);
        root.add("sophisticatedScaffolding", sophisticatedScaffolding);

        JsonObject enderdragonEgg = new JsonObject();
        FabricJsonComments.put(root, "enderdragonEgg", "Dragon egg spawning behavior after defeating the dragon.");
        FabricJsonComments.put(enderdragonEgg, "enabled", "Always spawn or respawn the dragon egg.");
        FabricJsonComments.put(enderdragonEgg, "useRandomPlacement", "Randomize egg placement within the spawn radius.");
        FabricJsonComments.put(enderdragonEgg, "spawnRadius", "Radius around the portal where the egg may appear.");
        FabricJsonComments.put(enderdragonEgg, "spawnDelaySeconds", "Delay before the egg appears in seconds.");
        FabricJsonComments.put(enderdragonEgg, "showParticles", "Show particles while the egg is waiting to spawn.");
        enderdragonEgg.addProperty("enabled", enableEnderdragonEggAlways);
        enderdragonEgg.addProperty("useRandomPlacement", enderdragonEggUseRandomPlacement);
        enderdragonEgg.addProperty("spawnRadius", clampInt(enderdragonEggSpawnRadius, 1, 50));
        enderdragonEgg.addProperty("spawnDelaySeconds", clampInt(enderdragonEggSpawnDelaySeconds, 1, 60));
        enderdragonEgg.addProperty("showParticles", enderdragonEggShowParticles);
        root.add("enderdragonEgg", enderdragonEgg);

        JsonObject improvedRecoveryCompass = new JsonObject();
        FabricJsonComments.put(root, "improvedRecoveryCompass", "Additional recovery compass features.");
        FabricJsonComments.put(improvedRecoveryCompass, "enabled", "Enable improved recovery compass behavior.");
        FabricJsonComments.put(improvedRecoveryCompass, "keepOnDeath", "Keep the recovery compass after death.");
        FabricJsonComments.put(improvedRecoveryCompass, "showDistance", "Show distance to the last death location.");
        FabricJsonComments.put(improvedRecoveryCompass, "showCoordsOnRightClick", "Show last death coordinates when right-clicking.");
        FabricJsonComments.put(improvedRecoveryCompass, "sculkParticles", "Launch a short-lived soul trail toward the death location as a hint.");
        improvedRecoveryCompass.addProperty("enabled", enableImprovedRecoveryCompass);
        improvedRecoveryCompass.addProperty("keepOnDeath", recoveryCompassKeepOnDeath);
        improvedRecoveryCompass.addProperty("showDistance", recoveryCompassShowDistance);
        improvedRecoveryCompass.addProperty("showCoordsOnRightClick", recoveryCompassShowCoordsOnRightClick);
        improvedRecoveryCompass.addProperty("sculkParticles", recoveryCompassSculkParticles);
        root.add("improvedRecoveryCompass", improvedRecoveryCompass);

        JsonObject automaticToolRestock = new JsonObject();
        FabricJsonComments.put(root, "automaticToolRestock", "Automatically replace broken hotbar tools from inventory.");
        FabricJsonComments.put(automaticToolRestock, "enabled", "Enable automatic tool restocking.");
        automaticToolRestock.addProperty("enabled", enableAutomaticToolRestock);
        root.add("automaticToolRestock", automaticToolRestock);

        JsonObject automaticBlockRestock = new JsonObject();
        FabricJsonComments.put(root, "automaticBlockRestock", "Automatically refill hotbar building blocks from inventory.");
        FabricJsonComments.put(automaticBlockRestock, "enabled", "Enable automatic block restocking.");
        automaticBlockRestock.addProperty("enabled", enableAutomaticBlockRestock);
        root.add("automaticBlockRestock", automaticBlockRestock);

        JsonObject sitting = new JsonObject();
        FabricJsonComments.put(root, "sitting", "Controls whether sitting uses toggle or hold behavior.");
        FabricJsonComments.put(sitting, "toggle", "If true, sitting is toggled; if false, sitting is hold-to-sit.");
        sitting.addProperty("toggle", toggleSitting);
        root.add("sitting", sitting);

        JsonObject brushingXp = new JsonObject();
        FabricJsonComments.put(root, "brushingXp", "XP rewards granted when brushing suspicious blocks.");
        FabricJsonComments.put(brushingXp, "enabled", "Whether brushing XP rewards are enabled.");
        FabricJsonComments.put(brushingXp, "min", "Minimum XP rewarded per brushing action.");
        FabricJsonComments.put(brushingXp, "max", "Maximum XP rewarded per brushing action.");
        brushingXp.addProperty("enabled", brushingXpEnabled);
        brushingXp.addProperty("min", Math.max(0, brushingXpMin));
        brushingXp.addProperty("max", Math.max(Math.max(0, brushingXpMin), brushingXpMax));
        root.add("brushingXp", brushingXp);

        JsonObject responsiveShields = new JsonObject();
        FabricJsonComments.put(root, "responsiveShields", "Shield raise timing tweaks.");
        FabricJsonComments.put(responsiveShields, "enabled", "Enable more responsive shields.");
        FabricJsonComments.put(responsiveShields, "raiseTime", "Shield raise time in ticks (0 = instant).");
        responsiveShields.addProperty("enabled", enableResponsiveShields);
        responsiveShields.addProperty("raiseTime", Math.max(0, shieldRaiseTime));
        root.add("responsiveShields", responsiveShields);

        JsonObject treasureEnchantmentColors = new JsonObject();
        FabricJsonComments.put(root, "treasureEnchantmentColors", "Treasure enchantment name styling.");
        FabricJsonComments.put(treasureEnchantmentColors, "goldColor", "Render treasure enchantment names in gold.");
        treasureEnchantmentColors.addProperty("goldColor", enableTreasureEnchantmentGoldColor);
        root.add("treasureEnchantmentColors", treasureEnchantmentColors);

        JsonObject sugarcane = new JsonObject();
        FabricJsonComments.put(root, "sugarcane", "Sugarcane placement and growth tweaks.");
        FabricJsonComments.put(sugarcane, "allowSand", "Allow sugarcane to grow on sand.");
        sugarcane.addProperty("allowSand", enableSugarcaneSand);
        root.add("sugarcane", sugarcane);

        JsonObject moreMiningXp = new JsonObject();
        FabricJsonComments.put(root, "moreMiningXp", "Additional XP rewards from mining ores.");
        FabricJsonComments.put(moreMiningXp, "enabled", "Increase mining XP drops.");
        FabricJsonComments.put(moreMiningXp, "coalMin", "Minimum XP dropped when mining coal.");
        FabricJsonComments.put(moreMiningXp, "coalMax", "Maximum XP dropped when mining coal.");
        FabricJsonComments.put(moreMiningXp, "diamondMin", "Minimum XP dropped when mining diamonds.");
        FabricJsonComments.put(moreMiningXp, "diamondMax", "Maximum XP dropped when mining diamonds.");
        FabricJsonComments.put(moreMiningXp, "emeraldMin", "Minimum XP dropped when mining emeralds.");
        FabricJsonComments.put(moreMiningXp, "emeraldMax", "Maximum XP dropped when mining emeralds.");
        FabricJsonComments.put(moreMiningXp, "lapisMin", "Minimum XP dropped when mining lapis.");
        FabricJsonComments.put(moreMiningXp, "lapisMax", "Maximum XP dropped when mining lapis.");
        FabricJsonComments.put(moreMiningXp, "redstoneMin", "Minimum XP dropped when mining redstone.");
        FabricJsonComments.put(moreMiningXp, "redstoneMax", "Maximum XP dropped when mining redstone.");
        moreMiningXp.addProperty("enabled", enableMoreMiningXp);
        moreMiningXp.addProperty("coalMin", clampInt(coalXpMin, 0, 100));
        moreMiningXp.addProperty("coalMax", clampInt(Math.max(coalXpMin, coalXpMax), 0, 100));
        moreMiningXp.addProperty("diamondMin", clampInt(diamondXpMin, 0, 100));
        moreMiningXp.addProperty("diamondMax", clampInt(Math.max(diamondXpMin, diamondXpMax), 0, 100));
        moreMiningXp.addProperty("emeraldMin", clampInt(emeraldXpMin, 0, 100));
        moreMiningXp.addProperty("emeraldMax", clampInt(Math.max(emeraldXpMin, emeraldXpMax), 0, 100));
        moreMiningXp.addProperty("lapisMin", clampInt(lapisXpMin, 0, 100));
        moreMiningXp.addProperty("lapisMax", clampInt(Math.max(lapisXpMin, lapisXpMax), 0, 100));
        moreMiningXp.addProperty("redstoneMin", clampInt(redstoneXpMin, 0, 100));
        moreMiningXp.addProperty("redstoneMax", clampInt(Math.max(redstoneXpMin, redstoneXpMax), 0, 100));
        root.add("moreMiningXp", moreMiningXp);

        JsonObject glowingGlowberries = new JsonObject();
        FabricJsonComments.put(root, "glowingGlowberries", "Glowberries that apply glowing to entities.");
        FabricJsonComments.put(glowingGlowberries, "enabled", "Enable glowing glowberries.");
        FabricJsonComments.put(glowingGlowberries, "playerDurationSeconds", "Glowing duration in seconds for players.");
        FabricJsonComments.put(glowingGlowberries, "foxDurationSeconds", "Glowing duration in seconds for foxes.");
        glowingGlowberries.addProperty("enabled", enableGlowingGlowberries);
        glowingGlowberries.addProperty("playerDurationSeconds", clampInt(glowingGlowberriesDuration, 1, 300));
        glowingGlowberries.addProperty("foxDurationSeconds", clampInt(foxGlowingDuration, 1, 300));
        root.add("glowingGlowberries", glowingGlowberries);

        JsonObject respawnAnchor = new JsonObject();
        FabricJsonComments.put(root, "respawnAnchor", "Respawn anchor behavior outside the Nether.");
        FabricJsonComments.put(respawnAnchor, "allowAnywhere", "Allow respawn anchors to function outside the Nether.");
        respawnAnchor.addProperty("allowAnywhere", enableRespawnAnchorAnywhere);
        root.add("respawnAnchor", respawnAnchor);

        JsonObject performance = new JsonObject();
        FabricJsonComments.put(root, "performance", "Menu performance tweaks.");
        FabricJsonComments.put(performance, "enableUncapMenuFps", "Uncap FPS on title and pause/menu screens.");
        performance.addProperty("enableUncapMenuFps", enableUncapMenuFps);
        root.add("performance", performance);

        JsonObject itemSharing = new JsonObject();
        FabricJsonComments.put(root, "itemSharing", "Item share gameplay settings.");
        FabricJsonComments.put(itemSharing, "enabled", "Enable the item sharing feature.");
        FabricJsonComments.put(itemSharing, "cooldownSeconds", "Minimum time between item-share messages per player in seconds.");
        itemSharing.addProperty("enabled", enableItemSharing);
        itemSharing.addProperty("cooldownSeconds", clampDouble(itemShareCooldownSeconds, 0.0D, 60.0D));
        root.add("itemSharing", itemSharing);

        JsonObject curses = new JsonObject();
        FabricJsonComments.put(root, "curses", "Curse-related UI behavior tweaks.");
        FabricJsonComments.put(curses, "hidePumpkinOverlayOnVanishing", "Hide the pumpkin overlay for items with Curse of Vanishing.");
        curses.addProperty("hidePumpkinOverlayOnVanishing", curseHidePumpkinOverlayOnVanishing);
        root.add("curses", curses);

        JsonObject creeperSunlightBurn = new JsonObject();
        FabricJsonComments.put(root, "creeperSunlightBurn", "Creeper sunlight burning behavior.");
        FabricJsonComments.put(creeperSunlightBurn, "enabled", "Make creepers burn in sunlight.");
        creeperSunlightBurn.addProperty("enabled", enableCreeperSunlightBurn);
        root.add("creeperSunlightBurn", creeperSunlightBurn);

        JsonObject compostableItems = new JsonObject();
        FabricJsonComments.put(root, "compostableItems", "Extra composting entries.");
        FabricJsonComments.put(compostableItems, "enabled", "Enable additional compostable item entries.");
        FabricJsonComments.put(compostableItems, "rottenFlesh", "Allow rotten flesh to be composted.");
        FabricJsonComments.put(compostableItems, "poisonousPotato", "Allow poisonous potatoes to be composted.");
        compostableItems.addProperty("enabled", enableCompostableItems);
        compostableItems.addProperty("rottenFlesh", enableCompostableRottenFlesh);
        compostableItems.addProperty("poisonousPotato", enableCompostablePoisonousPotato);
        root.add("compostableItems", compostableItems);

        JsonObject fuelTweaks = new JsonObject();
        FabricJsonComments.put(root, "fuelTweaks", "Fuel-related tweaks.");
        FabricJsonComments.put(fuelTweaks, "enableTorchFuel", "Allow torches to be used as fuel.");
        FabricJsonComments.put(fuelTweaks, "torchBurnTime", "Burn time in ticks for torches used as fuel.");
        fuelTweaks.addProperty("enableTorchFuel", enableTorchFuel);
        fuelTweaks.addProperty("torchBurnTime", Math.max(0, torchFuelBurnTime));
        root.add("fuelTweaks", fuelTweaks);

        JsonObject despawnWithMaster = new JsonObject();
        FabricJsonComments.put(root, "despawnWithMaster", "Despawn dependent entities with their master mob.");
        FabricJsonComments.put(despawnWithMaster, "vexWithEvoker", "Despawn vexes when their evoker despawns or dies.");
        FabricJsonComments.put(despawnWithMaster, "bulletsWithShulker", "Despawn shulker bullets when their shulker despawns or dies.");
        despawnWithMaster.addProperty("vexWithEvoker", despawnVexWithEvoker);
        despawnWithMaster.addProperty("bulletsWithShulker", despawnBulletsWithShulker);
        root.add("despawnWithMaster", despawnWithMaster);

        JsonObject enhancedSlabs = new JsonObject();
        FabricJsonComments.put(root, "enhancedSlabs", "Enhanced slab-family interaction and mining behavior.");
        FabricJsonComments.put(enhancedSlabs, "enabled", "Master toggle for enhanced slab behavior.");
        FabricJsonComments.put(enhancedSlabs, "placeOnTop", "Legacy toggle for top-placement behavior; kept for compatibility.");
        FabricJsonComments.put(enhancedSlabs, "hangBelow", "Allow hanging lanterns, chains, and signs below supported slabs and steps.");
        FabricJsonComments.put(enhancedSlabs, "kneeSlabMining", "Allow targeted half-mining for supported slab-family blocks.");
        FabricJsonComments.put(enhancedSlabs, "mixedDoubleSlabs", "Allow different slab types to combine into mixed double slabs.");
        FabricJsonComments.put(enhancedSlabs, "description", "Vertical slabs and steps now live in the building config.");
        enhancedSlabs.addProperty("enabled", enableEnhancedSlabs);
        enhancedSlabs.addProperty("placeOnTop", enhancedSlabsPlaceOnTop);
        enhancedSlabs.addProperty("hangBelow", enhancedSlabsHangBelow);
        enhancedSlabs.addProperty("kneeSlabMining", enhancedSlabsKneeSlabMining);
        enhancedSlabs.addProperty("mixedDoubleSlabs", enhancedSlabsMixedDoubleSlabs);
        root.add("enhancedSlabs", enhancedSlabs);

        JsonObject unbreakableBlocks = new JsonObject();
        FabricJsonComments.put(root, "unbreakableBlocks", "Unbreakable trial structure blocks.");
        FabricJsonComments.put(unbreakableBlocks, "trialSpawners", "Make trial spawners unbreakable.");
        FabricJsonComments.put(unbreakableBlocks, "vaults", "Make vaults unbreakable.");
        unbreakableBlocks.addProperty("trialSpawners", enableUnbreakableTrialSpawners);
        unbreakableBlocks.addProperty("vaults", enableUnbreakableVaults);
        root.add("unbreakableBlocks", unbreakableBlocks);

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

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clampDouble(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
