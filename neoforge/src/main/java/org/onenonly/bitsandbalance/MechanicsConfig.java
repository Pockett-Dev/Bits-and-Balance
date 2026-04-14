package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;


@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class MechanicsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static { BUILDER.comment("Mechanics"); BUILDER.push("mechanics"); }

    // Toggle Stance
    static { BUILDER.comment("Toggle Stance"); BUILDER.push("toggleStance"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TOGGLE_STANCE = BUILDER
            .comment("Enable the Toggle Stance keybind to switch between standing/sneaking (and crawling if enabled).")
            .define("enableToggleStance", true);
    static { BUILDER.pop(); }

    // Crawling
    static { BUILDER.comment("Crawling"); BUILDER.push("crawling"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CRAWLING_MECHANIC = BUILDER
            .comment("Enable the Crawling mechanic allowing players to explicitly enter a crawling stance.")
            .define("enableCrawlingMechanic", true);
    static { BUILDER.pop(); }

    // Friendly Fire Friendlies
    static { BUILDER.comment("Friendly Fire Friendlies"); BUILDER.push("friendlyFireFriendlies"); }
    private static final ModConfigSpec.BooleanValue ENABLE_FRIENDLY_FIRE_FRIENDLIES = BUILDER
            .comment("Prevent players from accidentally attacking their own tamed pets.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Sitting
    static { BUILDER.comment("Sitting"); BUILDER.push("sitting"); }
    private static final ModConfigSpec.BooleanValue SIT_TOGGLE_MODE = BUILDER
            .comment("If true, the Sit key acts as a toggle. If false, it acts as hold-to-sit.")
            .define("toggleSitting", true);
    static { BUILDER.pop(); }

    // Item Sharing
    static { BUILDER.comment("Item Sharing"); BUILDER.push("itemSharing"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ITEM_SHARING = BUILDER
            .comment("Enable: Press Shift+Chat while hovering an item stack in an inventory to post a hoverable link in chat.")
            .define("enableItemSharing", true);
    private static final ModConfigSpec.DoubleValue ITEM_SHARE_COOLDOWN_SECONDS = BUILDER
            .comment("Minimum time between item-share messages per player (seconds). Set to 0 to disable cooldown.")
            .defineInRange("cooldownSeconds", 1.0D, 0.0D, 60.0D);
    static { BUILDER.pop(); }

    // Campfires Ignite (top-level)
    static { BUILDER.comment("Campfires Ignite"); BUILDER.push("campfiresIgnite"); }
    private static final ModConfigSpec.BooleanValue CAMPFIRES_IGNITE_ENABLED = BUILDER
            .comment("Enable campfires to ignite entities standing on them, cooking them.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Cozy Campfire (top-level)
    static { BUILDER.comment("Cozy Campfire"); BUILDER.push("cozyCampfire"); }
    private static final ModConfigSpec.BooleanValue ENABLE_COZY_CAMPFIRE = BUILDER
            .comment("Enable: Receive Regeneration near a lit campfire every interval while within range.")
            .define("enabled", true);
    private static final ModConfigSpec.DoubleValue COZY_CAMPFIRE_RANGE = BUILDER
            .comment("Range in blocks to search for a lit campfire.")
            .defineInRange("range", 8.0D, 1.0D, 64.0D);
    private static final ModConfigSpec.IntValue COZY_CAMPFIRE_DURATION_TICKS = BUILDER
            .comment("Duration in ticks for the Regeneration effect when applied.")
            .defineInRange("durationTicks", 100, 1, 12000);
    private static final ModConfigSpec.IntValue COZY_CAMPFIRE_INTERVAL_TICKS = BUILDER
            .comment("Interval in ticks between effect applications while in range.")
            .defineInRange("intervalTicks", 100, 1, 12000);
    private static final ModConfigSpec.IntValue COZY_CAMPFIRE_AMPLIFIER = BUILDER
            .comment("Effect amplifier (0 = Regeneration I, 1 = Regeneration II, etc.).")
            .defineInRange("amplifier", 0, 0, 10);
    private static final ModConfigSpec.BooleanValue COZY_CAMPFIRE_AFFECT_BEES = BUILDER
            .comment("Enable: Bees also receive the Regeneration effect when near a lit campfire.")
            .define("affectBees", true);
    static { BUILDER.pop(); }


    // Dismount Entities
    static { BUILDER.comment("Dismount Entities"); BUILDER.push("dismountEntities"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DISMOUNT_ENTITIES = BUILDER
            .comment("Enable the Dismount Entities mechanic allowing players to crouch + right-click vehicles to eject passengers.")
            .define("enableDismountEntities", true);
    private static final ModConfigSpec.BooleanValue DISMOUNT_ALLOW_PLAYERS = BUILDER
            .comment("Allow dismounting other players from vehicles by crouch + right-clicking. Disabled by default.")
            .define("allowDismountPlayers", false);
    private static final ModConfigSpec.DoubleValue DISMOUNT_VERTICAL_VELOCITY = BUILDER
            .comment("Vertical velocity applied when ejecting passengers from vehicles (0.3 = half of original, 0.6 = original).")
            .defineInRange("verticalVelocity", 0.3, 0.0, 2.0);
    private static final ModConfigSpec.DoubleValue DISMOUNT_HORIZONTAL_VELOCITY = BUILDER
            .comment("Horizontal velocity applied when ejecting passengers from vehicles (0.5 = enhanced travel distance).")
            .defineInRange("horizontalVelocity", 0.5, 0.0, 2.0);
    static { BUILDER.pop(); }


    // Villagers Follow Emeralds
    static { BUILDER.comment("Villagers Follow Emeralds"); BUILDER.push("villagersFollowEmeralds"); }
    private static final ModConfigSpec.BooleanValue ENABLE_VILLAGERS_FOLLOW_EMERALDS = BUILDER
            .comment("Enable: Holding an Emerald or Emerald Block makes nearby villagers follow you, similar to animal temptation.")
            .define("enabled", true);
    private static final ModConfigSpec.DoubleValue VILLAGER_EMERALD_FOLLOW_SPEED = BUILDER
            .comment("Tempt follow speed modifier used by villagers when following emeralds (1.0 = normal movement speed).")
            .defineInRange("speedModifier", 0.6D, 0.25D, 3.0D);
    private static final ModConfigSpec.BooleanValue VILLAGER_EMERALD_FOLLOW_SHOW_PARTICLES = BUILDER
            .comment("Show a small burst of happy villager particles when a villager begins following a player with emeralds.")
            .define("showStartParticles", true);
    private static final ModConfigSpec.IntValue VILLAGER_EMERALD_FOLLOW_PARTICLES_COUNT = BUILDER
            .comment("How many happy villager particles to show when following begins.")
            .defineInRange("startParticlesCount", 3, 1, 40);
    static { BUILDER.pop(); }

    // Speedy Wolves
    static { BUILDER.comment("Speedy Wolves"); BUILDER.push("speedyWolves"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SPEEDY_WOLVES = BUILDER
            .comment("Enable: Tamed wolves move faster to keep up with their owner when they fall behind.")
            .define("enabled", true);
    private static final ModConfigSpec.DoubleValue SPEEDY_WOLVES_SPEED_MULTIPLIER = BUILDER
        .comment("Maximum movement speed multiplier applied to tamed wolves when keeping up (1.0 = no change). Actual boost dynamically scales with owner speed.")
        .defineInRange("speedMultiplier", 1.3D, 1.0D, 2.0D);
    private static final ModConfigSpec.DoubleValue SPEEDY_WOLVES_DISTANCE_THRESHOLD = BUILDER
        .comment("Distance from owner beyond which the speed boost applies (blocks).")
        .defineInRange("distanceThreshold", 6.0D, 1.0D, 48.0D);
    static { BUILDER.pop(); }

    // Quick Harvesting
    static { BUILDER.comment("Quick Harvesting"); BUILDER.push("quickHarvesting"); }
    private static final ModConfigSpec.BooleanValue ENABLE_QUICK_HARVESTING = BUILDER
            .comment("Enable all Quick Harvesting behaviors (master toggle).")
            .define("enabled", true);
    // Hoes subsection
    static { BUILDER.comment("Quick Harvesting with Hoes"); BUILDER.push("hoes"); }
    private static final ModConfigSpec.BooleanValue ENABLE_QH_HOES = BUILDER
            .comment("Enable: Right-click mature crops to harvest and auto-replant. Holding a hoe harvests a 3x3 area.")
            .define("enabled", true);
    static { BUILDER.pop(); }
    // Axes subsection
    static { BUILDER.comment("Quick Harvesting with Axes"); BUILDER.push("axes"); }
    private static final ModConfigSpec.BooleanValue ENABLE_QH_AXES = BUILDER
            .comment("Enable: Right-click with an axe to harvest axe-based crops (pumpkins, melons, cocoa, and compatible modded crops). Replants when applicable (e.g., cocoa).")
            .define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    // Door Knocking
    static { BUILDER.comment("Door Knocking"); BUILDER.push("doorKnocking"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DOOR_KNOCKING = BUILDER
            .comment("Enable: Left-clicking a door plays a knocking sound matching the door material.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Coyote Time Jump
    static { BUILDER.comment("Coyote Time Jump"); BUILDER.push("coyoteTimeJump"); }
    private static final ModConfigSpec.BooleanValue ENABLE_COYOTE_TIME_JUMP = BUILDER
            .comment("Enable coyote-time jump: allows jumping for a short time after falling off a block without jumping first.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue COYOTE_TIME_DELAY_MS = BUILDER
            .comment("Delay (ms) after starting to fall before coyote-time jump becomes available.")
            .defineInRange("delayMs", 50, 50, 1000);
    private static final ModConfigSpec.IntValue COYOTE_TIME_WINDOW_MS = BUILDER
            .comment("Maximum time (ms) after starting to fall during which coyote-time jump is allowed.")
            .defineInRange("windowMs", 1000, 200, 1000);
    private static final ModConfigSpec.BooleanValue COYOTE_TIME_DEBUG = BUILDER
            .comment("Enable debug logging for coyote time jump (shows detailed information in console).")
            .define("debug", false);
    static { BUILDER.pop(); }

    // Rapid Fire Jump
    static { BUILDER.comment("Rapid Fire Jump"); BUILDER.push("rapidFireJump"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RAPID_FIRE_JUMP = BUILDER
            .comment("Enable rapid fire jump: hold the jump key to continuously execute jumps for quick navigation in 2-block gaps.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue RAPID_FIRE_JUMP_INTERVAL = BUILDER
            .comment("Interval in ticks between rapid fire jumps when holding the jump key (20 ticks = 1 second).")
            .defineInRange("intervalTicks", 1, 1, 20);
    private static final ModConfigSpec.BooleanValue RAPID_FIRE_JUMP_DEBUG = BUILDER
            .comment("Enable debug logging for rapid fire jump (shows detailed information in console).")
            .define("debug", false);
    static { BUILDER.pop(); }

    // Leashed Teleport
    static { BUILDER.comment("Leashed Teleport"); BUILDER.push("leashedTeleport"); }
    private static final ModConfigSpec.BooleanValue ENABLE_LEASHED_TELEPORT = BUILDER
            .comment("Enable Leashed Teleport: leashed mobs follow players through any teleportation (commands, chorus fruit, ender pearls, portals, etc.).")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue LEASHED_TELEPORT_ALLOW_CROSS_DIMENSION = BUILDER
            .comment("Allow leashed mobs to follow players across dimensions (nether portals, end portals, etc.).")
            .define("allowCrossDimension", true);
    private static final ModConfigSpec.IntValue LEASHED_TELEPORT_MAX_FOLLOWERS = BUILDER
            .comment("Maximum number of leashed mobs that can follow a player through teleportation.")
            .defineInRange("maxFollowers", 12, 1, 50);
    private static final ModConfigSpec.DoubleValue LEASHED_TELEPORT_SCAN_RADIUS = BUILDER
            .comment("Radius in blocks to search for leashed mobs around the player.")
            .defineInRange("scanRadius", 48.0D, 16.0D, 128.0D);
    private static final ModConfigSpec.IntValue LEASHED_TELEPORT_SAFE_PLACEMENT_TRIES = BUILDER
            .comment("Number of attempts to find a safe placement spot for teleported mobs.")
            .defineInRange("safePlacementTries", 16, 4, 32);
    private static final ModConfigSpec.DoubleValue LEASHED_TELEPORT_BASE_RADIUS = BUILDER
            .comment("Base radius around player for safe placement attempts.")
            .defineInRange("baseRadius", 2.5D, 1.0D, 8.0D);
    private static final ModConfigSpec.DoubleValue LEASHED_TELEPORT_MAX_RADIUS = BUILDER
            .comment("Maximum radius around player for safe placement attempts.")
            .defineInRange("maxRadius", 5.0D, 2.0D, 16.0D);
    private static final ModConfigSpec.BooleanValue LEASHED_TELEPORT_POST_TELEPORT_LEASH = BUILDER
            .comment("Re-attach leashes after teleporting mobs to ensure they stay connected.")
            .define("postTeleportLeash", true);
    private static final ModConfigSpec.IntValue LEASHED_TELEPORT_COOLDOWN_TICKS = BUILDER
            .comment("Cooldown in ticks between teleportation processing for each player (prevents spam).")
            .defineInRange("cooldownTicks", 10, 1, 100);
    static { BUILDER.pop(); }

    // Chat Mentions
    static { BUILDER.comment("Chat Mentions"); BUILDER.push("chatMentions"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CHAT_MENTIONS = BUILDER
            .comment("Enable Chat Mentions: typing @username or username in chat highlights their name and sends them a ping sound.")
            .define("enabled", true);
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> TRIGGER_MODES = BUILDER
            .comment("Trigger modes: AT_NAME (@username), PLAIN_NAME (username)")
            .defineList("triggerModes", java.util.List.of("AT_NAME", "PLAIN_NAME"), obj -> obj instanceof String);
    private static final ModConfigSpec.BooleanValue CASE_INSENSITIVE = BUILDER
            .comment("Make mention detection case-insensitive.")
            .define("caseInsensitive", true);
    private static final ModConfigSpec.BooleanValue WORD_BOUNDARY = BUILDER
            .comment("Use word boundaries for mention detection (prevents 'jakub' matching 'hijakub').")
            .define("wordBoundary", true);
    private static final ModConfigSpec.BooleanValue RECIPIENT_ONLY_HIGHLIGHT = BUILDER
            .comment("Only highlight mentions for the mentioned player (others see normal text).")
            .define("recipientOnlyHighlight", true);
    private static final ModConfigSpec.IntValue MAX_MENTIONS_PER_MESSAGE = BUILDER
            .comment("Maximum number of mentions allowed per message.")
            .defineInRange("maxMentionsPerMessage", 3, 1, 10);
    private static final ModConfigSpec.IntValue COOLDOWN_MS_PER_SENDER = BUILDER
            .comment("Cooldown in milliseconds between mentions from the same sender.")
            .defineInRange("cooldownMsPerSender", 30000, 0, 10000);
    private static final ModConfigSpec.BooleanValue ALLOW_SELF_PING = BUILDER
            .comment("Allow players to mention themselves.")
            .define("allowSelfPing", false);
    private static final ModConfigSpec.BooleanValue TAB_COMPLETE_USERNAMES = BUILDER
            .comment("Allow pressing Tab after typing @ to auto-complete online usernames in the chat box.")
            .define("tabCompleteUsernames", true);
    private static final ModConfigSpec.BooleanValue INPUT_HIGHLIGHT_USERNAMES = BUILDER
            .comment("Highlight fully typed online usernames in the local chat box before sending the message.")
            .define("inputHighlightUsernames", true);
    
    // Style settings
    static { BUILDER.comment("Style"); BUILDER.push("style"); }
    private static final ModConfigSpec.ConfigValue<String> HIGHLIGHT_COLOR_HEX = BUILDER
            .comment("Hex color for highlighting mentioned usernames (without #).")
            .define("highlightColorHex", "FF3B30");
    private static final ModConfigSpec.ConfigValue<String> HOVER_TEXT = BUILDER
            .comment("Hover text shown when hovering over a mentioned username.")
            .define("hoverText", "You were mentioned");
    static { BUILDER.pop(); }
    
    // Sound settings
    static { BUILDER.comment("Sound"); BUILDER.push("sound"); }
    private static final ModConfigSpec.ConfigValue<String> SOUND_EVENT = BUILDER
            .comment("Sound event to play when mentioned.")
            .define("soundEvent", "minecraft:block.note_block.pling");
    private static final ModConfigSpec.ConfigValue<String> SOUND_SOURCE = BUILDER
            .comment("Sound source category.")
            .define("soundSource", "players");
    private static final ModConfigSpec.DoubleValue SOUND_VOLUME = BUILDER
            .comment("Sound volume (0.0 to 1.0).")
            .defineInRange("soundVolume", 0.9, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue SOUND_PITCH = BUILDER
            .comment("Sound pitch (0.5 to 2.0).")
            .defineInRange("soundPitch", 1.2, 0.5, 2.0);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    // Navigator Compass
    static { BUILDER.comment("Navigator Compass"); BUILDER.push("navigatorCompass"); }
    private static final ModConfigSpec.BooleanValue ENABLE_NAVIGATOR_COMPASS = BUILDER
            .comment("Enable Navigator Compass: Right-click a compass to set custom coordinates for navigation.")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue NAVIGATOR_COMPASS_SHOW_DISTANCE = BUILDER
            .comment("Show distance to target coordinates in compass tooltip.")
            .define("showDistance", true);
    private static final ModConfigSpec.BooleanValue NAVIGATOR_COMPASS_SHOW_COORDINATES = BUILDER
            .comment("Show target coordinates in compass tooltip.")
            .define("showCoordinates", true);
    static { BUILDER.pop(); }

    // Bottle of Cloud
    static { BUILDER.comment("Bottle of Cloud"); BUILDER.push("bottleOfCloud"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BOTTLE_OF_CLOUD = BUILDER
            .comment("Enable Bottle of Cloud: Right-click a glass bottle on a cloud to capture it. The cloud is permanently altered. Place the bottle to spawn a temporary cloud that fades after 20 seconds.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue BOTTLE_OF_CLOUD_DURATION_TICKS = BUILDER
            .comment("Duration in ticks for placed clouds to exist before disappearing (20 ticks = 1 second).")
            .defineInRange("durationTicks", 400, 100, 6000);
    private static final ModConfigSpec.IntValue BOTTLE_OF_CLOUD_FADE_START_TICKS = BUILDER
            .comment("How many ticks before disappearing the cloud starts to fade (20 ticks = 1 second).")
            .defineInRange("fadeStartTicks", 100, 20, 600);
    private static final ModConfigSpec.DoubleValue BOTTLE_OF_CLOUD_PLACEMENT_DISTANCE = BUILDER
            .comment("Distance in blocks from the player to place the cloud.")
            .defineInRange("placementDistance", 2.0D, 1.0D, 5.0D);
    static { BUILDER.pop(); }

    // Glow Goo
    static { BUILDER.comment("Glow Goo"); BUILDER.push("glowGoo"); }
    private static final ModConfigSpec.BooleanValue ENABLE_GLOW_GOO = BUILDER
            .comment("Enable Glow Goo: a throwable splatter item crafted from slime and glow ink.")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED = BUILDER
            .comment("Enable the recipe using a slime ball and two glow ink sacs.")
            .define("twoGlowInkRecipeEnabled", true);
    private static final ModConfigSpec.BooleanValue GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED = BUILDER
            .comment("Enable the recipe using four glow ink sacs.")
            .define("fourGlowInkRecipeEnabled", true);
    private static final ModConfigSpec.BooleanValue GLOW_GOO_IMPACT_PARTICLES_ENABLED = BUILDER
            .comment("Spawn glow particles when Glow Goo hits a block or entity.")
            .define("impactParticlesEnabled", true);
    private static final ModConfigSpec.IntValue GLOW_GOO_IMPACT_PARTICLE_COUNT = BUILDER
            .comment("Number of glow particles spawned on impact.")
            .defineInRange("impactParticleCount", 16, 0, 128);
    private static final ModConfigSpec.BooleanValue GLOW_GOO_BIOLUMINESCENCE_ENABLED = BUILDER
            .comment("Apply the Bioluminescence effect to entities hit by Glow Goo.")
            .define("bioluminescenceEnabled", true);
    private static final ModConfigSpec.IntValue GLOW_GOO_BIOLUMINESCENCE_DURATION_SECONDS = BUILDER
            .comment("Duration of the Bioluminescence effect after a Glow Goo hit, in seconds.")
            .defineInRange("bioluminescenceDurationSeconds", 20, 1, 600);
    private static final ModConfigSpec.IntValue GLOW_GOO_BIOLUMINESCENCE_LIGHT_LEVEL = BUILDER
            .comment("Configured light level target for Bioluminescence.")
            .defineInRange("bioluminescenceLightLevel", 14, 0, 15);
    static { BUILDER.pop(); }

    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        // Mechanics
        Config.enableToggleStance = ENABLE_TOGGLE_STANCE.get();
        Config.enableCrawlingMechanic = ENABLE_CRAWLING_MECHANIC.get();
                Config.enableFriendlyFireFriendlies = ENABLE_FRIENDLY_FIRE_FRIENDLIES.get();
        Config.toggleSitting = SIT_TOGGLE_MODE.get();
        Config.enableItemSharing = ENABLE_ITEM_SHARING.get();
        Config.itemShareCooldownSeconds = ITEM_SHARE_COOLDOWN_SECONDS.get();
        // Campfires Ignite
        Config.enableCampfiresIgniteEntities = CAMPFIRES_IGNITE_ENABLED.get();
        
        // Cozy Campfire
        Config.enableCozyCampfire = ENABLE_COZY_CAMPFIRE.get();
        Config.cozyCampfireRange = COZY_CAMPFIRE_RANGE.get();
        Config.cozyCampfireDurationTicks = COZY_CAMPFIRE_DURATION_TICKS.get();
        Config.cozyCampfireIntervalTicks = COZY_CAMPFIRE_INTERVAL_TICKS.get();
        Config.cozyCampfireAmplifier = COZY_CAMPFIRE_AMPLIFIER.get();
        Config.cozyCampfireAffectBees = COZY_CAMPFIRE_AFFECT_BEES.get();

        Config.enableDismountEntities = ENABLE_DISMOUNT_ENTITIES.get();
        Config.allowDismountPlayers = DISMOUNT_ALLOW_PLAYERS.get();
        Config.dismountVerticalVelocity = DISMOUNT_VERTICAL_VELOCITY.get();
        Config.dismountHorizontalVelocity = DISMOUNT_HORIZONTAL_VELOCITY.get();


        // Villagers Follow Emeralds
        Config.enableVillagersFollowEmeralds = ENABLE_VILLAGERS_FOLLOW_EMERALDS.get();
        Config.villagerEmeraldFollowSpeed = VILLAGER_EMERALD_FOLLOW_SPEED.get();
        Config.villagerEmeraldShowParticles = VILLAGER_EMERALD_FOLLOW_SHOW_PARTICLES.get();
        Config.villagerEmeraldParticlesCount = VILLAGER_EMERALD_FOLLOW_PARTICLES_COUNT.get();

        // Speedy Wolves
        Config.enableSpeedyWolves = ENABLE_SPEEDY_WOLVES.get();
        Config.speedyWolvesSpeedMultiplier = SPEEDY_WOLVES_SPEED_MULTIPLIER.get();
        Config.speedyWolvesDistanceThreshold = SPEEDY_WOLVES_DISTANCE_THRESHOLD.get();
        
        // Quick Harvesting
        Config.enableQuickHarvesting = ENABLE_QUICK_HARVESTING.get();
        Config.enableQuickHarvestingHoes = ENABLE_QH_HOES.get();
        Config.enableQuickHarvestingAxes = ENABLE_QH_AXES.get();
        
        // Door Knocking
        Config.enableDoorKnocking = ENABLE_DOOR_KNOCKING.get();
        
        // Coyote Time Jump
        Config.enableCoyoteTimeJump = ENABLE_COYOTE_TIME_JUMP.get();
        Config.coyoteTimeDelayMs = COYOTE_TIME_DELAY_MS.get();
        Config.coyoteTimeWindowMs = COYOTE_TIME_WINDOW_MS.get();
        Config.coyoteTimeDebug = COYOTE_TIME_DEBUG.get();

        // Enforce intended coyote-time behavior: available after 50ms, expires at 1000ms.
        if (Config.coyoteTimeDelayMs < 50) Config.coyoteTimeDelayMs = 50;
        if (Config.coyoteTimeDelayMs > 1000) Config.coyoteTimeDelayMs = 1000;
        if (Config.coyoteTimeWindowMs < 200) Config.coyoteTimeWindowMs = 200;
        if (Config.coyoteTimeWindowMs > 1000) Config.coyoteTimeWindowMs = 1000;
        if (Config.coyoteTimeDelayMs > Config.coyoteTimeWindowMs) Config.coyoteTimeDelayMs = Config.coyoteTimeWindowMs;
        
        // Rapid Fire Jump
        Config.enableRapidFireJump = ENABLE_RAPID_FIRE_JUMP.get();
        Config.rapidFireJumpInterval = RAPID_FIRE_JUMP_INTERVAL.get();
        Config.rapidFireJumpDebug = RAPID_FIRE_JUMP_DEBUG.get();
        
        // Leashed Teleport
        Config.enableLeashedTeleport = ENABLE_LEASHED_TELEPORT.get();
        Config.leashedTeleportAllowCrossDimension = LEASHED_TELEPORT_ALLOW_CROSS_DIMENSION.get();
        Config.leashedTeleportMaxFollowers = LEASHED_TELEPORT_MAX_FOLLOWERS.get();
        Config.leashedTeleportScanRadius = LEASHED_TELEPORT_SCAN_RADIUS.get();
        Config.leashedTeleportSafePlacementTries = LEASHED_TELEPORT_SAFE_PLACEMENT_TRIES.get();
        Config.leashedTeleportBaseRadius = LEASHED_TELEPORT_BASE_RADIUS.get();
        Config.leashedTeleportMaxRadius = LEASHED_TELEPORT_MAX_RADIUS.get();
        Config.leashedTeleportPostTeleportLeash = LEASHED_TELEPORT_POST_TELEPORT_LEASH.get();
        Config.leashedTeleportCooldownTicks = LEASHED_TELEPORT_COOLDOWN_TICKS.get();
        Config.leashedTeleportBlacklistTypes = java.util.Set.of(); // Default empty blacklist
        
        // Chat Mentions
        Config.enableChatMentions = ENABLE_CHAT_MENTIONS.get();
        Config.mentionTriggerModes = new java.util.ArrayList<>(TRIGGER_MODES.get());
        Config.mentionCaseInsensitive = CASE_INSENSITIVE.get();
        Config.mentionWordBoundary = WORD_BOUNDARY.get();
        Config.mentionRecipientOnlyHighlight = RECIPIENT_ONLY_HIGHLIGHT.get();
        Config.mentionMaxMentionsPerMessage = MAX_MENTIONS_PER_MESSAGE.get();
        Config.mentionCooldownMsPerSender = COOLDOWN_MS_PER_SENDER.get();
        Config.mentionAllowSelfPing = ALLOW_SELF_PING.get();
        Config.mentionTabCompleteUsernames = TAB_COMPLETE_USERNAMES.get();
        Config.mentionInputHighlightUsernames = INPUT_HIGHLIGHT_USERNAMES.get();
        Config.mentionHighlightColorHex = HIGHLIGHT_COLOR_HEX.get();
        Config.mentionHoverText = HOVER_TEXT.get();
        Config.mentionSoundEvent = SOUND_EVENT.get();
        Config.mentionSoundSource = SOUND_SOURCE.get();
        Config.mentionSoundVolume = SOUND_VOLUME.get();
        Config.mentionSoundPitch = SOUND_PITCH.get();
        
        // Navigator Compass
        Config.enableNavigatorCompass = ENABLE_NAVIGATOR_COMPASS.get();
        Config.navigatorCompassShowDistance = NAVIGATOR_COMPASS_SHOW_DISTANCE.get();
        Config.navigatorCompassShowCoordinates = NAVIGATOR_COMPASS_SHOW_COORDINATES.get();

        // Bottle of Cloud
        Config.enableBottleOfCloud = ENABLE_BOTTLE_OF_CLOUD.get();
        Config.bottleOfCloudDurationTicks = BOTTLE_OF_CLOUD_DURATION_TICKS.get();
        Config.bottleOfCloudFadeStartTicks = BOTTLE_OF_CLOUD_FADE_START_TICKS.get();
        Config.bottleOfCloudPlacementDistance = BOTTLE_OF_CLOUD_PLACEMENT_DISTANCE.get();
        Config.enableGlowGoo = ENABLE_GLOW_GOO.get();
        Config.glowGooTwoGlowInkRecipeEnabled = GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED.get();
        Config.glowGooFourGlowInkRecipeEnabled = GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED.get();
        Config.glowGooImpactParticlesEnabled = GLOW_GOO_IMPACT_PARTICLES_ENABLED.get();
        Config.glowGooImpactParticleCount = GLOW_GOO_IMPACT_PARTICLE_COUNT.get();
        Config.glowGooBioluminescenceEnabled = GLOW_GOO_BIOLUMINESCENCE_ENABLED.get();
        Config.glowGooBioluminescenceDurationSeconds = GLOW_GOO_BIOLUMINESCENCE_DURATION_SECONDS.get();
        Config.glowGooBioluminescenceLightLevel = GLOW_GOO_BIOLUMINESCENCE_LIGHT_LEVEL.get();

                BottleOfCloudRuntime.enabled = Config.enableBottleOfCloud;
                BottleOfCloudRuntime.durationTicks = Config.bottleOfCloudDurationTicks;
                BottleOfCloudRuntime.fadeStartTicks = Config.bottleOfCloudFadeStartTicks;
                BottleOfCloudRuntime.placementDistance = Config.bottleOfCloudPlacementDistance;
                GlowGooRuntime.enabled = Config.enableGlowGoo;
                GlowGooRuntime.impactParticlesEnabled = Config.glowGooImpactParticlesEnabled;
                GlowGooRuntime.impactParticleCount = Config.glowGooImpactParticleCount;
                GlowGooRuntime.bioluminescenceEnabled = Config.glowGooBioluminescenceEnabled;
                GlowGooRuntime.bioluminescenceDurationTicks = Config.glowGooBioluminescenceDurationSeconds * 20;
                GlowGooRuntime.bioluminescenceLightLevel = Config.glowGooBioluminescenceLightLevel;
        
    }
}
