package org.onenonly.bitsandbalance.fabric.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimal Fabric-side config for mechanics features.
 *
 * Config file: config/Bits and Balance/bitsandbalance-mechanics.json
 */
public final class FabricMechanicsConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("bitsandbalance-mechanics-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String FILE_NAME = "bitsandbalance-gameplay.json";
    private static final String LEGACY_FILE_NAME = "bitsandbalance-mechanics.json";

    // Leashed Teleport (mirrors MechanicsConfig defaults)
    public static volatile boolean enableLeashedTeleport = true;
    public static volatile boolean leashedTeleportAllowCrossDimension = true;
    public static volatile int leashedTeleportMaxFollowers = 12;
    public static volatile double leashedTeleportScanRadius = 48.0D;
    public static volatile int leashedTeleportSafePlacementTries = 16;
    public static volatile double leashedTeleportBaseRadius = 2.5D;
    public static volatile double leashedTeleportMaxRadius = 5.0D;
    public static volatile boolean leashedTeleportPostTeleportLeash = true;
    public static volatile int leashedTeleportCooldownTicks = 10;
    public static volatile boolean leashedTeleportDebug = false;

    // Coyote Time Jump
    public static volatile boolean enableCoyoteTimeJump = true;
    public static volatile int coyoteTimeDelayMs = 50;
    public static volatile int coyoteTimeWindowMs = 1000;
    public static volatile boolean coyoteTimeDebug = false;

    // Crawling + Toggle Stance
    public static volatile boolean enableCrawlingMechanic = true;
    public static volatile boolean enableToggleStance = true;

    // Friendly Fire Friendlies
    public static volatile boolean enableFriendlyFireFriendlies = true;

    // Door Knocking
    public static volatile boolean enableDoorKnocking = true;

    // Dismount Entities
    public static volatile boolean enableDismountEntities = true;
    public static volatile boolean dismountAllowPlayers = false;
    public static volatile double dismountVerticalVelocity = 0.3D;
    public static volatile double dismountHorizontalVelocity = 0.5D;

    // Speedy Wolves
    public static volatile boolean enableSpeedyWolves = true;
    public static volatile double speedyWolvesSpeedMultiplier = 1.3D;
    public static volatile double speedyWolvesDistanceThreshold = 6.0D;

    // Quick Harvesting
    public static volatile boolean enableQuickHarvesting = true;
    public static volatile boolean enableQuickHarvestingHoes = true;
    public static volatile boolean enableQuickHarvestingAxes = true;
    public static volatile boolean quickHarvestingHomeDropsToUser = true;

    // Cozy Campfire
    public static volatile boolean enableCozyCampfire = true;
    public static volatile double cozyCampfireRange = 8.0D;
    public static volatile int cozyCampfireDurationTicks = 100;
    public static volatile int cozyCampfireIntervalTicks = 40;
    public static volatile int cozyCampfireAmplifier = 0;
    public static volatile boolean cozyCampfireAffectBees = true;

    // Chat Mentions (client-side rendering/sounds; config stored here for convenience)
    public static volatile boolean enableChatMentions = true;
    public static volatile java.util.List<String> mentionTriggerModes = java.util.List.of("AT_NAME", "PLAIN_NAME");
    public static volatile boolean mentionCaseInsensitive = true;
    public static volatile boolean mentionWordBoundary = true;
    public static volatile boolean mentionRecipientOnlyHighlight = true;
    public static volatile int mentionMaxMentionsPerMessage = 3;
    public static volatile int mentionCooldownMsPerSender = 10000;
    public static volatile boolean mentionAllowSelfPing = false;
    public static volatile boolean mentionTabCompleteUsernames = true;
    public static volatile boolean mentionInputHighlightUsernames = true;
    public static volatile String mentionHighlightColorHex = "FF3B30";
    public static volatile String mentionHoverText = "You were mentioned";
    public static volatile String mentionSoundEvent = "minecraft:block.note_block.pling";
    public static volatile String mentionSoundSource = "PLAYERS";
    public static volatile double mentionSoundVolume = 0.9D;
    public static volatile double mentionSoundPitch = 1.2D;

    // Villagers Follow Emeralds
    public static volatile boolean enableVillagersFollowEmeralds = true;
    public static volatile double villagerEmeraldFollowSpeed = 0.6D;
    public static volatile boolean villagerEmeraldShowParticles = true;
    public static volatile int villagerEmeraldParticlesCount = 3;

    // Navigator Compass
    public static volatile boolean enableNavigatorCompass = true;
    public static volatile boolean navigatorCompassShowCoordinates = true;
    public static volatile boolean navigatorCompassShowDistance = true;

    // Bottle of Cloud
    public static volatile boolean enableBottleOfCloud = true;
    public static volatile int bottleOfCloudDurationTicks = 400;
    public static volatile int bottleOfCloudFadeStartTicks = 100;
    public static volatile double bottleOfCloudPlacementDistance = 2.0D;
    public static volatile double bottleOfCloudFallDamageMultiplier = 0.5D;

    // Pistons Move Tile Entities
    public static volatile boolean enablePistonsMoveTileEntities = true;
    public static volatile java.util.List<String> pistonMoveBlacklistBlockIds = PistonBlockEntityMoveRuntime.defaultExactBlacklistBlockIds();
    public static volatile java.util.List<String> pistonMoveBlacklistNameContains = PistonBlockEntityMoveRuntime.defaultBlacklistNameContains();

    // Rapid Fire Jump
    public static volatile boolean enableRapidFireJump = true;
    public static volatile int rapidFireJumpInterval = 1;
    public static volatile boolean rapidFireJumpDebug = false;

    private FabricMechanicsConfig() {
    }

    public static void init() {
        loadOrCreateDefaults();
        BottleOfCloudRuntime.enabled = enableBottleOfCloud;
        BottleOfCloudRuntime.durationTicks = bottleOfCloudDurationTicks;
        BottleOfCloudRuntime.fadeStartTicks = bottleOfCloudFadeStartTicks;
        BottleOfCloudRuntime.placementDistance = bottleOfCloudPlacementDistance;
        BottleOfCloudRuntime.fallDamageMultiplier = Math.max(0.0D, Math.min(1.0D, bottleOfCloudFallDamageMultiplier));
        PistonBlockEntityMoveRuntime.applyConfig(
            enablePistonsMoveTileEntities,
            pistonMoveBlacklistBlockIds,
            pistonMoveBlacklistNameContains
        );
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

            JsonObject leashedTeleport = getObject(obj, "leashedTeleport");
            if (leashedTeleport != null) {
                enableLeashedTeleport = getBoolean(leashedTeleport, "enabled", enableLeashedTeleport);
                leashedTeleportAllowCrossDimension = getBoolean(leashedTeleport, "allowCrossDimension", leashedTeleportAllowCrossDimension);
                leashedTeleportMaxFollowers = getInt(leashedTeleport, "maxFollowers", leashedTeleportMaxFollowers);
                leashedTeleportScanRadius = getDouble(leashedTeleport, "scanRadius", leashedTeleportScanRadius);
                leashedTeleportSafePlacementTries = getInt(leashedTeleport, "safePlacementTries", leashedTeleportSafePlacementTries);
                leashedTeleportBaseRadius = getDouble(leashedTeleport, "baseRadius", leashedTeleportBaseRadius);
                leashedTeleportMaxRadius = getDouble(leashedTeleport, "maxRadius", leashedTeleportMaxRadius);
                leashedTeleportPostTeleportLeash = getBoolean(leashedTeleport, "postTeleportLeash", leashedTeleportPostTeleportLeash);
                leashedTeleportCooldownTicks = getInt(leashedTeleport, "cooldownTicks", leashedTeleportCooldownTicks);
                leashedTeleportDebug = getBoolean(leashedTeleport, "debug", leashedTeleportDebug);
            } else {
                enableLeashedTeleport = getBoolean(obj, "enableLeashedTeleport", enableLeashedTeleport);
            }

            JsonObject coyoteTime = getObject(obj, "coyoteTimeJump");
            if (coyoteTime != null) {
                enableCoyoteTimeJump = getBoolean(coyoteTime, "enabled", enableCoyoteTimeJump);
                coyoteTimeDelayMs = getInt(coyoteTime, "delayMs", coyoteTimeDelayMs);
                coyoteTimeWindowMs = getInt(coyoteTime, "windowMs", coyoteTimeWindowMs);
                coyoteTimeDebug = getBoolean(coyoteTime, "debug", coyoteTimeDebug);
            } else {
                enableCoyoteTimeJump = getBoolean(obj, "enableCoyoteTimeJump", enableCoyoteTimeJump);
                coyoteTimeDelayMs = getInt(obj, "coyoteTimeDelayMs", coyoteTimeDelayMs);
                coyoteTimeWindowMs = getInt(obj, "coyoteTimeWindowMs", coyoteTimeWindowMs);
                coyoteTimeDebug = getBoolean(obj, "coyoteTimeDebug", coyoteTimeDebug);
            }

            JsonObject crawling = getObject(obj, "crawling");
            if (crawling != null) {
                enableCrawlingMechanic = getBoolean(crawling, "enabled", enableCrawlingMechanic);
            } else {
                enableCrawlingMechanic = getBoolean(obj, "enableCrawlingMechanic", enableCrawlingMechanic);
            }

            JsonObject toggleStance = getObject(obj, "toggleStance");
            if (toggleStance != null) {
                enableToggleStance = getBoolean(toggleStance, "enabled", enableToggleStance);
            } else {
                enableToggleStance = getBoolean(obj, "enableToggleStance", enableToggleStance);
            }

            JsonObject friendlyFireFriendlies = getObject(obj, "friendlyFireFriendlies");
            if (friendlyFireFriendlies != null) {
                enableFriendlyFireFriendlies = getBoolean(friendlyFireFriendlies, "enabled", enableFriendlyFireFriendlies);
            } else {
                enableFriendlyFireFriendlies = getBoolean(obj, "enableFriendlyFireFriendlies", enableFriendlyFireFriendlies);
            }

            if (leashedTeleportMaxFollowers < 1) leashedTeleportMaxFollowers = 1;
            if (leashedTeleportMaxFollowers > 50) leashedTeleportMaxFollowers = 50;
            if (Double.isNaN(leashedTeleportScanRadius) || leashedTeleportScanRadius < 16.0D) leashedTeleportScanRadius = 16.0D;
            if (leashedTeleportScanRadius > 128.0D) leashedTeleportScanRadius = 128.0D;
            if (leashedTeleportSafePlacementTries < 4) leashedTeleportSafePlacementTries = 4;
            if (leashedTeleportSafePlacementTries > 32) leashedTeleportSafePlacementTries = 32;
            if (Double.isNaN(leashedTeleportBaseRadius) || leashedTeleportBaseRadius < 1.0D) leashedTeleportBaseRadius = 1.0D;
            if (leashedTeleportBaseRadius > 8.0D) leashedTeleportBaseRadius = 8.0D;
            if (Double.isNaN(leashedTeleportMaxRadius) || leashedTeleportMaxRadius < 2.0D) leashedTeleportMaxRadius = 2.0D;
            if (leashedTeleportMaxRadius > 16.0D) leashedTeleportMaxRadius = 16.0D;
            if (leashedTeleportMaxRadius < leashedTeleportBaseRadius) leashedTeleportMaxRadius = leashedTeleportBaseRadius;
            if (leashedTeleportCooldownTicks < 1) leashedTeleportCooldownTicks = 1;
            if (leashedTeleportCooldownTicks > 100) leashedTeleportCooldownTicks = 100;

            // Enforce intended coyote-time behavior: available after 50ms, expires at 1000ms.
            if (coyoteTimeDelayMs < 50) coyoteTimeDelayMs = 50;
            if (coyoteTimeDelayMs > 1000) coyoteTimeDelayMs = 1000;
            if (coyoteTimeWindowMs < 200) coyoteTimeWindowMs = 200;
            if (coyoteTimeWindowMs > 1000) coyoteTimeWindowMs = 1000;
            if (coyoteTimeDelayMs > coyoteTimeWindowMs) coyoteTimeDelayMs = coyoteTimeWindowMs;

            if (Double.isNaN(dismountVerticalVelocity) || dismountVerticalVelocity < 0.0D) dismountVerticalVelocity = 0.0D;
            if (dismountVerticalVelocity > 2.0D) dismountVerticalVelocity = 2.0D;
            if (Double.isNaN(dismountHorizontalVelocity) || dismountHorizontalVelocity < 0.0D) dismountHorizontalVelocity = 0.0D;
            if (dismountHorizontalVelocity > 2.0D) dismountHorizontalVelocity = 2.0D;

            if (Double.isNaN(speedyWolvesSpeedMultiplier) || speedyWolvesSpeedMultiplier < 1.0D) speedyWolvesSpeedMultiplier = 1.0D;
            if (speedyWolvesSpeedMultiplier > 2.0D) speedyWolvesSpeedMultiplier = 2.0D;
            if (Double.isNaN(speedyWolvesDistanceThreshold) || speedyWolvesDistanceThreshold < 1.0D) speedyWolvesDistanceThreshold = 1.0D;
            if (speedyWolvesDistanceThreshold > 48.0D) speedyWolvesDistanceThreshold = 48.0D;

            JsonObject doorKnocking = getObject(obj, "doorKnocking");
            if (doorKnocking != null) {
                enableDoorKnocking = getBoolean(doorKnocking, "enabled", enableDoorKnocking);
            } else {
                enableDoorKnocking = getBoolean(obj, "enableDoorKnocking", enableDoorKnocking);
            }

            JsonObject dismountEntities = getObject(obj, "dismountEntities");
            if (dismountEntities != null) {
                // NeoForge uses enableDismountEntities + allowDismountPlayers, but we support a simple "enabled" too.
                enableDismountEntities = getBoolean(dismountEntities, "enableDismountEntities", enableDismountEntities);
                enableDismountEntities = getBoolean(dismountEntities, "enabled", enableDismountEntities);
                dismountAllowPlayers = getBoolean(dismountEntities, "allowDismountPlayers", dismountAllowPlayers);
                dismountVerticalVelocity = getDouble(dismountEntities, "verticalVelocity", dismountVerticalVelocity);
                dismountHorizontalVelocity = getDouble(dismountEntities, "horizontalVelocity", dismountHorizontalVelocity);
            } else {
                enableDismountEntities = getBoolean(obj, "enableDismountEntities", enableDismountEntities);
                dismountAllowPlayers = getBoolean(obj, "dismountAllowPlayers", dismountAllowPlayers);
                dismountVerticalVelocity = getDouble(obj, "dismountVerticalVelocity", dismountVerticalVelocity);
                dismountHorizontalVelocity = getDouble(obj, "dismountHorizontalVelocity", dismountHorizontalVelocity);
            }

            JsonObject speedyWolves = getObject(obj, "speedyWolves");
            if (speedyWolves != null) {
                enableSpeedyWolves = getBoolean(speedyWolves, "enabled", enableSpeedyWolves);
                speedyWolvesSpeedMultiplier = getDouble(speedyWolves, "speedMultiplier", speedyWolvesSpeedMultiplier);
                speedyWolvesDistanceThreshold = getDouble(speedyWolves, "distanceThreshold", speedyWolvesDistanceThreshold);
            } else {
                enableSpeedyWolves = getBoolean(obj, "enableSpeedyWolves", enableSpeedyWolves);
                speedyWolvesSpeedMultiplier = getDouble(obj, "speedyWolvesSpeedMultiplier", speedyWolvesSpeedMultiplier);
                speedyWolvesDistanceThreshold = getDouble(obj, "speedyWolvesDistanceThreshold", speedyWolvesDistanceThreshold);
            }

            JsonObject quickHarvesting = getObject(obj, "quickHarvesting");
            if (quickHarvesting != null) {
                enableQuickHarvesting = getBoolean(quickHarvesting, "enabled", enableQuickHarvesting);
                quickHarvestingHomeDropsToUser = getBoolean(quickHarvesting, "homeDropsToUser", quickHarvestingHomeDropsToUser);
                enableQuickHarvestingHoes = getBoolean(quickHarvesting, "enableHoes", enableQuickHarvestingHoes);
                enableQuickHarvestingAxes = getBoolean(quickHarvesting, "enableAxes", enableQuickHarvestingAxes);
            } else {
                enableQuickHarvesting = getBoolean(obj, "enableQuickHarvesting", enableQuickHarvesting);
                quickHarvestingHomeDropsToUser = getBoolean(obj, "enableQuickHarvestingHomeDropsToUser", quickHarvestingHomeDropsToUser);
                enableQuickHarvestingHoes = getBoolean(obj, "enableQuickHarvestingHoes", enableQuickHarvestingHoes);
                enableQuickHarvestingAxes = getBoolean(obj, "enableQuickHarvestingAxes", enableQuickHarvestingAxes);
            }

            JsonObject cozyCampfire = getObject(obj, "cozyCampfire");
            if (cozyCampfire != null) {
                enableCozyCampfire = getBoolean(cozyCampfire, "enabled", enableCozyCampfire);
                cozyCampfireRange = getDouble(cozyCampfire, "range", cozyCampfireRange);
                cozyCampfireDurationTicks = getInt(cozyCampfire, "durationTicks", cozyCampfireDurationTicks);
                cozyCampfireIntervalTicks = getInt(cozyCampfire, "intervalTicks", cozyCampfireIntervalTicks);
                cozyCampfireAmplifier = getInt(cozyCampfire, "amplifier", cozyCampfireAmplifier);
                cozyCampfireAffectBees = getBoolean(cozyCampfire, "affectBees", cozyCampfireAffectBees);
            } else {
                enableCozyCampfire = getBoolean(obj, "enableCozyCampfire", enableCozyCampfire);
                cozyCampfireRange = getDouble(obj, "cozyCampfireRange", cozyCampfireRange);
                cozyCampfireDurationTicks = getInt(obj, "cozyCampfireDurationTicks", cozyCampfireDurationTicks);
                cozyCampfireIntervalTicks = getInt(obj, "cozyCampfireIntervalTicks", cozyCampfireIntervalTicks);
                cozyCampfireAmplifier = getInt(obj, "cozyCampfireAmplifier", cozyCampfireAmplifier);
                cozyCampfireAffectBees = getBoolean(obj, "cozyCampfireAffectBees", cozyCampfireAffectBees);
            }

            JsonObject chatMentions = getObject(obj, "chatMentions");
            if (chatMentions != null) {
                enableChatMentions = getBoolean(chatMentions, "enabled", enableChatMentions);
                mentionCaseInsensitive = getBoolean(chatMentions, "caseInsensitive", mentionCaseInsensitive);
                mentionWordBoundary = getBoolean(chatMentions, "wordBoundary", mentionWordBoundary);
                mentionRecipientOnlyHighlight = getBoolean(chatMentions, "recipientOnlyHighlight", mentionRecipientOnlyHighlight);
                mentionMaxMentionsPerMessage = getInt(chatMentions, "maxMentionsPerMessage", mentionMaxMentionsPerMessage);
                mentionCooldownMsPerSender = getInt(chatMentions, "cooldownMsPerSender", mentionCooldownMsPerSender);
                mentionAllowSelfPing = getBoolean(chatMentions, "allowSelfPing", mentionAllowSelfPing);
                mentionTabCompleteUsernames = getBoolean(chatMentions, "tabCompleteUsernames", mentionTabCompleteUsernames);
                mentionInputHighlightUsernames = getBoolean(chatMentions, "inputHighlightUsernames", mentionInputHighlightUsernames);
                mentionHighlightColorHex = getString(chatMentions, "highlightColorHex", mentionHighlightColorHex);
                mentionHoverText = getString(chatMentions, "hoverText", mentionHoverText);
                mentionSoundEvent = getString(chatMentions, "soundEvent", mentionSoundEvent);
                mentionSoundSource = getString(chatMentions, "soundSource", mentionSoundSource);
                mentionSoundVolume = getDouble(chatMentions, "soundVolume", mentionSoundVolume);
                mentionSoundPitch = getDouble(chatMentions, "soundPitch", mentionSoundPitch);
                mentionTriggerModes = getStringList(chatMentions, "triggerModes", mentionTriggerModes);
            } else {
                enableChatMentions = getBoolean(obj, "enableChatMentions", enableChatMentions);
            }

            JsonObject villagersFollowEmeralds = getObject(obj, "villagersFollowEmeralds");
            if (villagersFollowEmeralds != null) {
                enableVillagersFollowEmeralds = getBoolean(villagersFollowEmeralds, "enabled", enableVillagersFollowEmeralds);
                // NeoForge config key is "speedModifier"; keep backward-compat with older Fabric key "followSpeed".
                villagerEmeraldFollowSpeed = getDouble(villagersFollowEmeralds, "speedModifier",
                        getDouble(villagersFollowEmeralds, "followSpeed", villagerEmeraldFollowSpeed));
                villagerEmeraldShowParticles = getBoolean(villagersFollowEmeralds, "showParticles", villagerEmeraldShowParticles);
                villagerEmeraldParticlesCount = getInt(villagersFollowEmeralds, "particlesCount", villagerEmeraldParticlesCount);
            } else {
                enableVillagersFollowEmeralds = getBoolean(obj, "enableVillagersFollowEmeralds", enableVillagersFollowEmeralds);
            }

            JsonObject navigatorCompass = getObject(obj, "navigatorCompass");
            if (navigatorCompass != null) {
                enableNavigatorCompass = getBoolean(navigatorCompass, "enabled", enableNavigatorCompass);
                navigatorCompassShowCoordinates = getBoolean(navigatorCompass, "showCoordinates", navigatorCompassShowCoordinates);
                navigatorCompassShowDistance = getBoolean(navigatorCompass, "showDistance", navigatorCompassShowDistance);
            } else {
                enableNavigatorCompass = getBoolean(obj, "enableNavigatorCompass", enableNavigatorCompass);
            }

            JsonObject bottleOfCloud = getObject(obj, "bottleOfCloud");
            if (bottleOfCloud != null) {
                enableBottleOfCloud = getBoolean(bottleOfCloud, "enabled", enableBottleOfCloud);
                bottleOfCloudDurationTicks = getInt(bottleOfCloud, "durationTicks", bottleOfCloudDurationTicks);
                bottleOfCloudFadeStartTicks = getInt(bottleOfCloud, "fadeStartTicks", bottleOfCloudFadeStartTicks);
                bottleOfCloudPlacementDistance = getDouble(bottleOfCloud, "placementDistance", bottleOfCloudPlacementDistance);
                bottleOfCloudFallDamageMultiplier = Math.max(0.0D, Math.min(1.0D, getDouble(bottleOfCloud, "fallDamageMultiplier", bottleOfCloudFallDamageMultiplier)));
            }
            JsonObject pistonsMoveTileEntities = getObject(obj, "pistonsMoveTileEntities");
            if (pistonsMoveTileEntities != null) {
                enablePistonsMoveTileEntities = getBoolean(pistonsMoveTileEntities, "enabled", enablePistonsMoveTileEntities);
                pistonMoveBlacklistBlockIds = getStringList(pistonsMoveTileEntities, "blacklistBlockIds", pistonMoveBlacklistBlockIds);
                pistonMoveBlacklistNameContains = getStringList(pistonsMoveTileEntities, "blacklistNameContains", pistonMoveBlacklistNameContains);
            } else {
                enablePistonsMoveTileEntities = getBoolean(obj, "enablePistonsMoveTileEntities", enablePistonsMoveTileEntities);
            }
            JsonObject rapidFireJump = getObject(obj, "rapidFireJump");
            if (rapidFireJump != null) {
                enableRapidFireJump = getBoolean(rapidFireJump, "enabled", enableRapidFireJump);
                rapidFireJumpInterval = getInt(rapidFireJump, "interval", rapidFireJumpInterval);
                rapidFireJumpDebug = getBoolean(rapidFireJump, "debug", rapidFireJumpDebug);
            } else {
                enableRapidFireJump = getBoolean(obj, "enableRapidFireJump", enableRapidFireJump);
            }

            // sanitize
            if (Double.isNaN(cozyCampfireRange) || cozyCampfireRange < 1.0D) cozyCampfireRange = 1.0D;
            if (cozyCampfireRange > 32.0D) cozyCampfireRange = 32.0D;
            if (cozyCampfireDurationTicks < 20) cozyCampfireDurationTicks = 20;
            if (cozyCampfireDurationTicks > 20 * 60) cozyCampfireDurationTicks = 20 * 60;
            if (cozyCampfireIntervalTicks < 10) cozyCampfireIntervalTicks = 10;
            if (cozyCampfireIntervalTicks > 20 * 10) cozyCampfireIntervalTicks = 20 * 10;
            if (cozyCampfireAmplifier < 0) cozyCampfireAmplifier = 0;
            if (cozyCampfireAmplifier > 5) cozyCampfireAmplifier = 5;

            if (rapidFireJumpInterval < 1) rapidFireJumpInterval = 1;
            if (rapidFireJumpInterval > 20) rapidFireJumpInterval = 20;

            if (mentionMaxMentionsPerMessage < 0) mentionMaxMentionsPerMessage = 0;
            if (mentionMaxMentionsPerMessage > 25) mentionMaxMentionsPerMessage = 25;
            if (mentionCooldownMsPerSender < 0) mentionCooldownMsPerSender = 0;
            if (mentionCooldownMsPerSender > 60_000) mentionCooldownMsPerSender = 60_000;
            if (mentionHighlightColorHex == null) mentionHighlightColorHex = "FF3B30";
            mentionHighlightColorHex = mentionHighlightColorHex.replace("#", "");
            if (mentionHighlightColorHex.length() > 8) mentionHighlightColorHex = mentionHighlightColorHex.substring(0, 8);
            if (mentionSoundEvent == null) mentionSoundEvent = "minecraft:entity.experience_orb.pickup";
            if (mentionSoundSource == null) mentionSoundSource = "PLAYERS";
            if (Double.isNaN(mentionSoundVolume) || mentionSoundVolume < 0.0D) mentionSoundVolume = 0.0D;
            if (mentionSoundVolume > 10.0D) mentionSoundVolume = 10.0D;
            if (Double.isNaN(mentionSoundPitch) || mentionSoundPitch < 0.1D) mentionSoundPitch = 0.1D;
            if (mentionSoundPitch > 4.0D) mentionSoundPitch = 4.0D;

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

        JsonObject leashedTeleport = new JsonObject();

        FabricJsonComments.put(root, "leashedTeleport", "Teleport leashed mobs to the leash holder when they get too far away.");
        FabricJsonComments.put(leashedTeleport, "enabled", "Enable leashed teleport.");
        FabricJsonComments.put(leashedTeleport, "allowCrossDimension", "Allow leashed teleport across dimensions.");
        FabricJsonComments.put(leashedTeleport, "maxFollowers", "Maximum number of leashed followers to teleport at once.");
        FabricJsonComments.put(leashedTeleport, "scanRadius", "Radius to scan for leashed followers.");
        FabricJsonComments.put(leashedTeleport, "safePlacementTries", "Number of attempts to find a safe teleport location.");
        FabricJsonComments.put(leashedTeleport, "baseRadius", "Base radius around the holder used for safe placement.");
        FabricJsonComments.put(leashedTeleport, "maxRadius", "Maximum radius around the holder used for safe placement.");
        FabricJsonComments.put(leashedTeleport, "postTeleportLeash", "Re-apply leashes after teleporting.");
        FabricJsonComments.put(leashedTeleport, "cooldownTicks", "Cooldown (ticks) between leashed teleports.");
        FabricJsonComments.put(leashedTeleport, "debug", "Enable leashed teleport debug logging.");

        leashedTeleport.addProperty("enabled", enableLeashedTeleport);
        leashedTeleport.addProperty("allowCrossDimension", leashedTeleportAllowCrossDimension);
        leashedTeleport.addProperty("maxFollowers", leashedTeleportMaxFollowers);
        leashedTeleport.addProperty("scanRadius", leashedTeleportScanRadius);
        leashedTeleport.addProperty("safePlacementTries", leashedTeleportSafePlacementTries);
        leashedTeleport.addProperty("baseRadius", leashedTeleportBaseRadius);
        leashedTeleport.addProperty("maxRadius", leashedTeleportMaxRadius);
        leashedTeleport.addProperty("postTeleportLeash", leashedTeleportPostTeleportLeash);
        leashedTeleport.addProperty("cooldownTicks", leashedTeleportCooldownTicks);
        leashedTeleport.addProperty("debug", leashedTeleportDebug);
        JsonObject coyoteTime = new JsonObject();

        FabricJsonComments.put(root, "coyoteTimeJump", "Coyote-time style jump forgiveness.");
        FabricJsonComments.put(coyoteTime, "enabled", "Enable coyote time jumps.");
        FabricJsonComments.put(coyoteTime, "delayMs", "Delay (ms) after starting to fall before coyote-time jump becomes available.");
        FabricJsonComments.put(coyoteTime, "windowMs", "Maximum time (ms) after starting to fall during which coyote-time jump is allowed.");
        FabricJsonComments.put(coyoteTime, "debug", "Enable coyote time debug logging.");

        coyoteTime.addProperty("enabled", enableCoyoteTimeJump);
        coyoteTime.addProperty("delayMs", coyoteTimeDelayMs);
        coyoteTime.addProperty("windowMs", coyoteTimeWindowMs);
        coyoteTime.addProperty("debug", coyoteTimeDebug);
        root.add("coyoteTimeJump", coyoteTime);

        JsonObject crawling = new JsonObject();

        FabricJsonComments.put(root, "crawling", "Crawling mechanic settings.");
        FabricJsonComments.put(crawling, "enabled", "Enable the crawl keybind/mechanic (forces a crawling/swimming pose on land). Useful for 1-block spaces.");

        crawling.addProperty("enabled", enableCrawlingMechanic);
        root.add("crawling", crawling);

        JsonObject toggleStance = new JsonObject();

        FabricJsonComments.put(root, "toggleStance", "Toggle stance mechanic settings.");
        FabricJsonComments.put(toggleStance, "enabled", "Enable the Toggle Stance keybind. Cycles Stand -> Sneak -> Crawl (if Crawling is enabled); otherwise toggles Sneak.");

        toggleStance.addProperty("enabled", enableToggleStance);
        root.add("toggleStance", toggleStance);

        JsonObject friendlyFireFriendlies = new JsonObject();

        FabricJsonComments.put(root, "friendlyFireFriendlies", "Prevent accidentally attacking your own tamed pets.");
        FabricJsonComments.put(friendlyFireFriendlies, "enabled", "If true, melee attacks against your own tamed pets are blocked.");

        friendlyFireFriendlies.addProperty("enabled", enableFriendlyFireFriendlies);
        root.add("friendlyFireFriendlies", friendlyFireFriendlies);

        JsonObject doorKnocking = new JsonObject();

        FabricJsonComments.put(root, "doorKnocking", "Door knocking mechanic settings.");
        FabricJsonComments.put(doorKnocking, "enabled", "Enable door knocking: press the knock keybind while looking at a door to play a knock sound and notify nearby players.");

        doorKnocking.addProperty("enabled", enableDoorKnocking);
        root.add("doorKnocking", doorKnocking);

        JsonObject rapidFireJump = new JsonObject();

        FabricJsonComments.put(root, "rapidFireJump", "Rapid fire jump mechanic settings.");
        FabricJsonComments.put(rapidFireJump, "enabled", "Enable rapid fire jumping: reduces the minimum delay between consecutive jumps while holding/pressing jump.");
        FabricJsonComments.put(rapidFireJump, "interval", "Jump interval (ticks) between allowed rapid jumps.");
        FabricJsonComments.put(rapidFireJump, "debug", "Enable rapid fire jump debug logging.");

        rapidFireJump.addProperty("enabled", enableRapidFireJump);
        rapidFireJump.addProperty("interval", rapidFireJumpInterval);
        rapidFireJump.addProperty("debug", rapidFireJumpDebug);
        root.add("rapidFireJump", rapidFireJump);

        JsonObject dismountEntities = new JsonObject();

        FabricJsonComments.put(root, "dismountEntities", "Allow dismounting ridden entities with extra velocity.");
        FabricJsonComments.put(dismountEntities, "enabled", "Enable dismount boost: when you dismount a ridden entity, apply configured velocity (" +
            "useful for hopping off mounts/boats without getting stuck).");
        FabricJsonComments.put(dismountEntities, "allowDismountPlayers", "Allow dismounting player passengers.");
        FabricJsonComments.put(dismountEntities, "verticalVelocity", "Vertical velocity applied when dismounting.");
        FabricJsonComments.put(dismountEntities, "horizontalVelocity", "Horizontal velocity applied when dismounting.");

        dismountEntities.addProperty("enabled", enableDismountEntities);
        dismountEntities.addProperty("allowDismountPlayers", dismountAllowPlayers);
        dismountEntities.addProperty("verticalVelocity", dismountVerticalVelocity);
        dismountEntities.addProperty("horizontalVelocity", dismountHorizontalVelocity);
        root.add("dismountEntities", dismountEntities);

        JsonObject speedyWolves = new JsonObject();

        FabricJsonComments.put(root, "speedyWolves", "Speed up wolves when following their owner.");
        FabricJsonComments.put(speedyWolves, "enabled", "Enable speedy wolves: applies a speed multiplier to wolves while they are pathfinding to follow their owner.");
        FabricJsonComments.put(speedyWolves, "speedMultiplier", "Speed multiplier applied to following wolves.");
        FabricJsonComments.put(speedyWolves, "distanceThreshold", "Distance threshold for speed boost to apply.");

        speedyWolves.addProperty("enabled", enableSpeedyWolves);
        speedyWolves.addProperty("speedMultiplier", speedyWolvesSpeedMultiplier);
        speedyWolves.addProperty("distanceThreshold", speedyWolvesDistanceThreshold);
        root.add("speedyWolves", speedyWolves);

        JsonObject quickHarvesting = new JsonObject();

        FabricJsonComments.put(root, "quickHarvesting", "Quick harvesting for crops/blocks.");
        FabricJsonComments.put(quickHarvesting, "enabled", "Enable quick harvesting: automatically harvest/replant supported crops/blocks when using the configured tools.");
        FabricJsonComments.put(quickHarvesting, "homeDropsToUser", "When enabled, Quick Harvesting drops home toward the user for 1 second.");
        FabricJsonComments.put(quickHarvesting, "enableHoes", "Enable quick harvesting using hoes.");
        FabricJsonComments.put(quickHarvesting, "enableAxes", "Enable quick harvesting using axes.");

        quickHarvesting.addProperty("enabled", enableQuickHarvesting);
        quickHarvesting.addProperty("homeDropsToUser", quickHarvestingHomeDropsToUser);
        quickHarvesting.addProperty("enableHoes", enableQuickHarvestingHoes);
        quickHarvesting.addProperty("enableAxes", enableQuickHarvestingAxes);
        root.add("quickHarvesting", quickHarvesting);

        JsonObject cozyCampfire = new JsonObject();

        FabricJsonComments.put(root, "cozyCampfire", "Cozy campfire effect settings.");
        FabricJsonComments.put(cozyCampfire, "enabled", "Enable cozy campfire: grants a beneficial effect to nearby entities while near a campfire.");
        FabricJsonComments.put(cozyCampfire, "range", "Range of the cozy campfire effect.");
        FabricJsonComments.put(cozyCampfire, "durationTicks", "Effect duration (ticks) applied per tick/interval.");
        FabricJsonComments.put(cozyCampfire, "intervalTicks", "Interval (ticks) between effect applications.");
        FabricJsonComments.put(cozyCampfire, "amplifier", "Potion effect amplifier.");
        FabricJsonComments.put(cozyCampfire, "affectBees", "Whether bees are affected by the cozy campfire.");

        cozyCampfire.addProperty("enabled", enableCozyCampfire);
        cozyCampfire.addProperty("range", cozyCampfireRange);
        cozyCampfire.addProperty("durationTicks", cozyCampfireDurationTicks);
        cozyCampfire.addProperty("intervalTicks", cozyCampfireIntervalTicks);
        cozyCampfire.addProperty("amplifier", cozyCampfireAmplifier);
        cozyCampfire.addProperty("affectBees", cozyCampfireAffectBees);
        root.add("cozyCampfire", cozyCampfire);

        JsonObject chatMentions = new JsonObject();

        FabricJsonComments.put(root, "chatMentions", "Chat mentions client-side settings.");
        FabricJsonComments.put(chatMentions, "enabled", "Enable chat mentions: highlight and optionally play a sound when your name is mentioned in chat.");
        FabricJsonComments.put(chatMentions, "caseInsensitive", "Match mentions case-insensitively.");
        FabricJsonComments.put(chatMentions, "wordBoundary", "Require word boundary around matched names.");
        FabricJsonComments.put(chatMentions, "recipientOnlyHighlight", "Only highlight mentions for the recipient.");
        FabricJsonComments.put(chatMentions, "maxMentionsPerMessage", "Maximum mentions allowed per message.");
        FabricJsonComments.put(chatMentions, "cooldownMsPerSender", "Cooldown (ms) per sender between mention notifications.");
        FabricJsonComments.put(chatMentions, "allowSelfPing", "Allow pinging yourself.");
        FabricJsonComments.put(chatMentions, "tabCompleteUsernames", "Allow pressing Tab in chat to auto-complete online usernames after typing @.");
        FabricJsonComments.put(chatMentions, "highlightColorHex", "Highlight color as hex (RRGGBB or AARRGGBB).");
        FabricJsonComments.put(chatMentions, "hoverText", "Hover text shown on mention highlight.");
        FabricJsonComments.put(chatMentions, "soundEvent", "Sound event ID played when you are mentioned.");
        FabricJsonComments.put(chatMentions, "soundSource", "Sound category/source used for mention sound.");
        FabricJsonComments.put(chatMentions, "soundVolume", "Volume for mention sound.");
        FabricJsonComments.put(chatMentions, "soundPitch", "Pitch for mention sound.");
        FabricJsonComments.put(chatMentions, "triggerModes", "List of trigger modes used to detect mentions.");

        chatMentions.addProperty("enabled", enableChatMentions);
        chatMentions.addProperty("caseInsensitive", mentionCaseInsensitive);
        chatMentions.addProperty("wordBoundary", mentionWordBoundary);
        chatMentions.addProperty("recipientOnlyHighlight", mentionRecipientOnlyHighlight);
        chatMentions.addProperty("maxMentionsPerMessage", mentionMaxMentionsPerMessage);
        chatMentions.addProperty("cooldownMsPerSender", mentionCooldownMsPerSender);
        chatMentions.addProperty("allowSelfPing", mentionAllowSelfPing);
        chatMentions.addProperty("tabCompleteUsernames", mentionTabCompleteUsernames);
        chatMentions.addProperty("inputHighlightUsernames", mentionInputHighlightUsernames);
        chatMentions.addProperty("highlightColorHex", mentionHighlightColorHex);
        chatMentions.addProperty("hoverText", mentionHoverText);
        chatMentions.addProperty("soundEvent", mentionSoundEvent);
        chatMentions.addProperty("soundSource", mentionSoundSource);
        chatMentions.addProperty("soundVolume", mentionSoundVolume);
        chatMentions.addProperty("soundPitch", mentionSoundPitch);
        chatMentions.add("triggerModes", GSON.toJsonTree(mentionTriggerModes));
        root.add("chatMentions", chatMentions);

        JsonObject villagersFollowEmeralds = new JsonObject();

        FabricJsonComments.put(root, "villagersFollowEmeralds", "Make villagers follow emeralds held by players.");
        FabricJsonComments.put(villagersFollowEmeralds, "enabled", "Enable villagers following emeralds: villagers will path toward players holding an emerald.");
        FabricJsonComments.put(villagersFollowEmeralds, "speedModifier", "Movement speed modifier applied while following.");
        FabricJsonComments.put(villagersFollowEmeralds, "showParticles", "Show particles while a villager is following.");
        FabricJsonComments.put(villagersFollowEmeralds, "particlesCount", "Number of particles to spawn per tick/interval.");

        villagersFollowEmeralds.addProperty("enabled", enableVillagersFollowEmeralds);
        villagersFollowEmeralds.addProperty("speedModifier", villagerEmeraldFollowSpeed);
        villagersFollowEmeralds.addProperty("showParticles", villagerEmeraldShowParticles);
        villagersFollowEmeralds.addProperty("particlesCount", villagerEmeraldParticlesCount);
        root.add("villagersFollowEmeralds", villagersFollowEmeralds);

        JsonObject navigatorCompass = new JsonObject();

        FabricJsonComments.put(root, "navigatorCompass", "Navigator compass HUD/tooltips settings.");
        FabricJsonComments.put(navigatorCompass, "enabled", "Enable navigator compass: provides extra compass readouts/tooltips (optionally including coordinates and distance).");
        FabricJsonComments.put(navigatorCompass, "showCoordinates", "Show coordinates in navigator compass output.");
        FabricJsonComments.put(navigatorCompass, "showDistance", "Show distance in navigator compass output.");

        navigatorCompass.addProperty("enabled", enableNavigatorCompass);
        navigatorCompass.addProperty("showCoordinates", navigatorCompassShowCoordinates);
        navigatorCompass.addProperty("showDistance", navigatorCompassShowDistance);
        root.add("navigatorCompass", navigatorCompass);

        JsonObject bottleOfCloud = new JsonObject();

        FabricJsonComments.put(root, "bottleOfCloud", "Bottle of Cloud mechanic: capture clouds in bottles and place them temporarily.");
        FabricJsonComments.put(bottleOfCloud, "enabled", "Enable Bottle of Cloud: right-click a glass bottle on a cloud to capture it, then place it as a temporary cloud.");
        FabricJsonComments.put(bottleOfCloud, "durationTicks", "Duration in ticks for placed clouds to exist (20 ticks = 1 second).");
        FabricJsonComments.put(bottleOfCloud, "fadeStartTicks", "How many ticks before disappearing the cloud starts to fade.");
        FabricJsonComments.put(bottleOfCloud, "placementDistance", "Distance in blocks from the player to place the cloud.");
        FabricJsonComments.put(bottleOfCloud, "fallDamageMultiplier", "Multiplier applied to normal fall damage when landing on Cloud Blocks. 0.5 = half damage, 0.0 = no damage, 1.0 = vanilla damage.");

        bottleOfCloud.addProperty("enabled", enableBottleOfCloud);
        bottleOfCloud.addProperty("durationTicks", bottleOfCloudDurationTicks);
        bottleOfCloud.addProperty("fadeStartTicks", bottleOfCloudFadeStartTicks);
        bottleOfCloud.addProperty("placementDistance", bottleOfCloudPlacementDistance);
        bottleOfCloud.addProperty("fallDamageMultiplier", bottleOfCloudFallDamageMultiplier);
        root.add("bottleOfCloud", bottleOfCloud);

        JsonObject pistonsMoveTileEntities = new JsonObject();

        FabricJsonComments.put(root, "pistonsMoveTileEntities", "Allow pistons to push and pull blocks that carry block entity data.");
        FabricJsonComments.put(pistonsMoveTileEntities, "enabled", "Enable piston movement for block-entity blocks. If a moved block lands in an invalid position, it breaks and drops its contents.");
        FabricJsonComments.put(pistonsMoveTileEntities, "blacklistBlockIds", "Exact block ids that are never moved by this feature.");
        FabricJsonComments.put(pistonsMoveTileEntities, "blacklistNameContains", "Case-insensitive path/name fragments that automatically blacklist matching blocks, including other mods.");

        pistonsMoveTileEntities.addProperty("enabled", enablePistonsMoveTileEntities);
        pistonsMoveTileEntities.add("blacklistBlockIds", GSON.toJsonTree(pistonMoveBlacklistBlockIds));
        pistonsMoveTileEntities.add("blacklistNameContains", GSON.toJsonTree(pistonMoveBlacklistNameContains));
        root.add("pistonsMoveTileEntities", pistonsMoveTileEntities);

        root.add("leashedTeleport", leashedTeleport);

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

    private static String getString(JsonObject obj, String key, String def) {
        JsonElement el = obj.get(key);
        return el != null && el.isJsonPrimitive() && el.getAsJsonPrimitive().isString() ? el.getAsString() : def;
    }

    private static java.util.List<String> getStringList(JsonObject obj, String key, java.util.List<String> def) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonArray()) return def;
        try {
            java.util.ArrayList<String> out = new java.util.ArrayList<>();
            for (var entry : el.getAsJsonArray()) {
                if (entry != null && entry.isJsonPrimitive() && entry.getAsJsonPrimitive().isString()) {
                    out.add(entry.getAsString());
                }
            }
            return out.isEmpty() ? def : java.util.List.copyOf(out);
        } catch (Throwable ignored) {
            return def;
        }
    }

}
