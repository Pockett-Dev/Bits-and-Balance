package org.onenonly.bitsandbalance;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.onenonly.bitsandbalance.common.mechanics.BottleOfCloudRuntime;
import org.onenonly.bitsandbalance.common.mechanics.PistonBlockEntityMoveRuntime;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class GameplayConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    private GameplayConfig() {
    }

    private static Boolean getLegacyBoolean(UnmodifiableConfig data, String dottedPath) {
        if (data == null) return null;
        Object raw = data.get(dottedPath);
        if (raw instanceof Boolean b) return b;
        return null;
    }

    private static Integer getLegacyInt(UnmodifiableConfig data, String dottedPath) {
        if (data == null) return null;
        Object raw = data.get(dottedPath);
        if (raw instanceof Number number) return number.intValue();
        return null;
    }

    private static Double getLegacyDouble(UnmodifiableConfig data, String dottedPath) {
        if (data == null) return null;
        Object raw = data.get(dottedPath);
        if (raw instanceof Number number) return number.doubleValue();
        return null;
    }

    private static boolean validateEntityTypeName(final Object obj) {
        return obj instanceof String name && BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(name));
    }

    private static boolean validateResourceLocationName(final Object obj) {
        if (!(obj instanceof String s)) return false;
        try {
            Identifier.parse(s);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static { BUILDER.comment("Combat"); BUILDER.push("combat"); }

    static { BUILDER.comment("Second Chance"); BUILDER.push("secondChance"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SECOND_CHANCE = BUILDER
            .comment("Enable the Second Chance anti-one-shot mechanic for players (excludes fall damage by default). ")
            .define("enableSecondChance", true);
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_EXCLUDE_FALL = BUILDER
            .comment("Exclude fall damage from triggering Second Chance.")
            .define("excludeFallDamage", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_COOLDOWN_DAYS = BUILDER
            .comment("Cooldown in Minecraft days between Second Chance triggers (1 day = 24000 ticks). Set to 0 to disable cooldown.")
            .defineInRange("cooldownDays", 7, 0, 3650);
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_RESET_ON_RESPAWN = BUILDER
            .comment("Reset Second Chance cooldown on respawn (death). If false, cooldown persists across deaths.")
            .define("resetOnRespawn", true);
    static { BUILDER.comment("Apply Resistance effect when Second Chance triggers."); BUILDER.push("applyResistance"); }
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_RESISTANCE_ENABLED = BUILDER
            .comment("Enable applying Resistance when Second Chance triggers.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_RESISTANCE_LEVEL = BUILDER
            .comment("Resistance effect level to apply (1 = Resistance I).")
            .defineInRange("level", 1, 1, 10);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Apply Nausea effect when Second Chance triggers."); BUILDER.push("applyNausea"); }
    private static final ModConfigSpec.BooleanValue SECOND_CHANCE_NAUSEA_ENABLED = BUILDER
            .comment("Enable applying Nausea when Second Chance triggers.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue SECOND_CHANCE_NAUSEA_LEVEL = BUILDER
            .comment("Nausea effect level to apply (1 = Nausea I).")
            .defineInRange("level", 2, 1, 10);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Maintain Experience"); BUILDER.push("maintainExperience"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MAINTAIN_EXPERIENCE = BUILDER
            .comment("Enable Maintain Experience: players lose a percentage of their total XP on death, not a percentage of displayed levels.")
            .define("enableMaintainExperience", true);
    private static final ModConfigSpec.IntValue MAINTAIN_EXPERIENCE_LOSS_PERCENT = BUILDER
            .comment("Percentage of total XP points to lose on death (0-100), not displayed levels.\n Default: 50\n Range: 0 ~ 100")
            .defineInRange("lossPercent", 50, 0, 100);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Snowball Rework"); BUILDER.push("snowball"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SNOWBALL_REWORK = BUILDER
            .comment("Enable Snowball Rework: snowballs deal damage and apply freezing.")
            .define("enableSnowballRework", true);
    private static final ModConfigSpec.DoubleValue SNOWBALL_BASE_DAMAGE = BUILDER
            .comment("Base damage dealt by snowballs to entities.")
            .defineInRange("snowballBaseDamage", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue SNOWBALL_NETHER_DAMAGE = BUILDER
            .comment("Damage dealt by snowballs to entities considered Nether-native.")
            .defineInRange("snowballNetherDamage", 1.0D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue SNOWBALL_MIN_FREEZE_SECONDS = BUILDER
            .comment("Minimum freeze duration in seconds applied by snowballs.")
            .defineInRange("snowballMinFreezeSeconds", 5, 0, 60);
    private static final ModConfigSpec.IntValue SNOWBALL_MAX_FREEZE_SECONDS = BUILDER
            .comment("Maximum freeze duration in seconds applied by snowballs.")
            .defineInRange("snowballMaxFreezeSeconds", 8, 0, 60);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SNOWBALL_NETHER_ENTITIES = BUILDER
            .comment("Entity types treated as Nether-native for snowball damage (resource locations).")
            .defineList("snowballNetherEntities",
                    List.of(
                            "minecraft:blaze",
                            "minecraft:ghast",
                            "minecraft:magma_cube",
                            "minecraft:strider",
                            "minecraft:wither_skeleton",
                            "minecraft:piglin",
                            "minecraft:piglin_brute",
                            "minecraft:zombified_piglin",
                            "minecraft:hoglin",
                            "minecraft:zoglin"
                    ), GameplayConfig::validateEntityTypeName);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Balance"); BUILDER.push("balance"); }

    static { BUILDER.comment("Ender Dragon XP"); BUILDER.push("enderDragon"); }
    private static final ModConfigSpec.BooleanValue ENABLE_FULL_ENDER_DRAGON_XP = BUILDER
            .comment("Always drop the full first-kill XP from the Ender Dragon.")
            .define("enableFullEnderDragonXp", true);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Food"); BUILDER.push("food"); }
    private static final ModConfigSpec.BooleanValue ENABLE_FOOD_ALWAYS_EDIBLE = BUILDER
            .comment("Allow players to eat unconditionally.")
            .define("enableFoodAlwaysEdible", true);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Source Dependent Invulnerability Frames"); BUILDER.push("sourceDependentIFrames"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SOURCE_DEPENDENT_IFRAMES = BUILDER
            .comment("Give different damage sources independent victim i-frame timers so they can overlap without fully disabling cooldowns.")
            .define("enabled", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> SOURCE_IFRAME_BLACKLIST = BUILDER
            .comment("Damage types to never bypass i-frames (resource locations).")
            .define("blacklist", List.of(), obj -> {
                if (!(obj instanceof List<?> list)) return false;
                return list.stream().allMatch(item -> {
                    if (!(item instanceof String str)) return false;
                    return GameplayConfig.validateResourceLocationName(str);
                });
            });
    static { BUILDER.pop(); }

    static { BUILDER.comment("Depth Scaling Enemies"); BUILDER.push("depthScalingEnemies"); }
    private static final ModConfigSpec.BooleanValue DSE_ENABLED = BUILDER.comment("Enable depth-based scaling for Zombies and Skeletons in the Overworld.").define("enabled", true);
    private static final ModConfigSpec.BooleanValue DSE_OVERWORLD_ONLY = BUILDER.comment("Restrict depth scaling to the Overworld only.").define("overworldOnly", true);
    private static final ModConfigSpec.BooleanValue DSE_ENABLE_SURFACE_TIER = BUILDER.comment("If false, disables the surface tier from affecting spawns (no gear/weapon changes for mobs in the surface band).").define("enableSurfaceTier", false);
    private static final ModConfigSpec.BooleanValue DSE_ENABLE_MID_TIER = BUILDER.comment("If false, disables the mid tier from affecting spawns (no gear/weapon changes for mobs in the mid-depth band).").define("enableMidTier", true);
    private static final ModConfigSpec.BooleanValue DSE_ENABLE_DEEP_TIER = BUILDER.comment("If false, disables the deep tier from affecting spawns (no gear/weapon changes for mobs in the deep band).").define("enableDeepTier", true);
    private static final ModConfigSpec.IntValue DSE_MID_Y = BUILDER.comment("Y-level threshold for mid depth tier (<= this is considered mid).").defineInRange("midDepthY", 40, -2048, 4096);
    private static final ModConfigSpec.IntValue DSE_DEEP_Y = BUILDER.comment("Y-level threshold for deep tier (<= this is considered deep). Example: -16.").defineInRange("deepDepthY", 0, -2048, 4096);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_EQUIP_CHANCE = BUILDER.comment("Chance per armor slot to equip a piece at surface tier (0.0-1.0).").defineInRange("surfaceEquipChance", 0.05D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_EQUIP_CHANCE = BUILDER.comment("Chance per armor slot to equip a piece at mid tier (0.0-1.0).").defineInRange("midEquipChance", 0.15D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_EQUIP_CHANCE = BUILDER.comment("Chance per armor slot to equip a piece at deep tier (0.0-1.0).").defineInRange("deepEquipChance", 0.30D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_LEATHER = BUILDER.comment("Surface tier: probability weight for Leather armor when equipping.").defineInRange("surfaceLeatherWeight", 0.80D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_CHAIN = BUILDER.comment("Surface tier: probability weight for Chain armor when equipping.").defineInRange("surfaceChainWeight", 0.15D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_COPPER = BUILDER.comment("Surface tier: probability weight for Copper armor when equipping.").defineInRange("surfaceCopperWeight", 0.06D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_GOLD = BUILDER.comment("Surface tier: probability weight for Gold armor when equipping.").defineInRange("surfaceGoldWeight", 0.05D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_IRON = BUILDER.comment("Surface tier: probability weight for Iron armor when equipping.").defineInRange("surfaceIronWeight", 0.00D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_LEATHER = BUILDER.comment("Mid tier: probability weight for Leather armor when equipping.").defineInRange("midLeatherWeight", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_CHAIN = BUILDER.comment("Mid tier: probability weight for Chain armor when equipping.").defineInRange("midChainWeight", 0.25D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_COPPER = BUILDER.comment("Mid tier: probability weight for Copper armor when equipping.").defineInRange("midCopperWeight", 0.06D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_GOLD = BUILDER.comment("Mid tier: probability weight for Gold armor when equipping.").defineInRange("midGoldWeight", 0.05D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_IRON = BUILDER.comment("Mid tier: probability weight for Iron armor when equipping.").defineInRange("midIronWeight", 0.2D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_LEATHER = BUILDER.comment("Deep tier: probability weight for Leather armor when equipping.").defineInRange("deepLeatherWeight", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_CHAIN = BUILDER.comment("Deep tier: probability weight for Chain armor when equipping.").defineInRange("deepChainWeight", 0.45D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_COPPER = BUILDER.comment("Deep tier: probability weight for Copper armor when equipping.").defineInRange("deepCopperWeight", 0.16D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_GOLD = BUILDER.comment("Deep tier: probability weight for Gold armor when equipping.").defineInRange("deepGoldWeight", 0.15D, 0.0D, 100.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_IRON = BUILDER.comment("Deep tier: probability weight for Iron armor when equipping. Note: hard-capped by deepMaxIronPieces.").defineInRange("deepIronWeight", 0.5D, 0.0D, 100.0D);
    private static final ModConfigSpec.IntValue DSE_DEEP_MAX_IRON = BUILDER.comment("Deep tier: maximum number of Iron armor pieces allowed on a single mob (example requires 1).").defineInRange("deepMaxIronPieces", 1, 0, 4);
    private static final ModConfigSpec.DoubleValue DSE_SURFACE_ZOMBIE_WEAPON = BUILDER.comment("Surface tier: chance for Zombies to spawn with a melee weapon in main hand (0.0-1.0).").defineInRange("surfaceZombieWeaponChance", 0.05D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_MID_ZOMBIE_WEAPON = BUILDER.comment("Mid tier: chance for Zombies to spawn with a melee weapon in main hand (0.0-1.0).").defineInRange("midZombieWeaponChance", 0.15D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue DSE_DEEP_ZOMBIE_WEAPON = BUILDER.comment("Deep tier: chance for Zombies to spawn with a melee weapon in main hand (0.0-1.0).").defineInRange("deepZombieWeaponChance", 0.3D, 0.0D, 1.0D);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Balanced Elytra"); BUILDER.push("balancedElytra"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BALANCED_ELYTRA = BUILDER.comment("Enable Balanced Elytra: elytras lose durability based on distance flown (10 blocks = 1 durability) instead of time.").define("enabled", true);
    private static final ModConfigSpec.IntValue BALANCED_ELYTRA_BLOCKS_PER_DURABILITY = BUILDER.comment("How many horizontal blocks flown equals 1 durability point (default: 10). Lower = faster wear.").defineInRange("blocksPerDurability", 10, 1, 1000);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Nerfed Mending"); BUILDER.push("nerfedMending"); }
    private static final ModConfigSpec.BooleanValue ENABLE_NERFED_MENDING = BUILDER.comment("Enable nerfed Mending: items require more XP to repair.").define("enabled", true);
    private static final ModConfigSpec.IntValue NERFED_MENDING_MULTIPLIER = BUILDER.comment("XP multiplier for Mending repairs (2-10). Example: 4 = requires 4x XP for the same repair.").defineInRange("multiplier", 4, 2, 10);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Nerfed Discounts"); BUILDER.push("nerfedDiscounts"); }
    private static final ModConfigSpec.BooleanValue ENABLE_NERFED_DISCOUNTS = BUILDER.comment("Enable nerfed villager discounts system.").define("enabled", true);
    private static final ModConfigSpec.BooleanValue REMOVE_ZOMBIE_CURE_DISCOUNTS = BUILDER.comment("Remove discounts gained from curing zombie villagers.").define("removeZombieCureDiscounts", false);
    private static final ModConfigSpec.DoubleValue MAX_VILLAGER_DISCOUNT = BUILDER.comment("Maximum discount percentage any villager can offer (0.0-1.0, where 0.5 = 50%).").defineInRange("maxDiscount", 0.5D, 0.0D, 1.0D);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Mechanics"); BUILDER.push("mechanics"); }

    static { BUILDER.comment("Toggle Stance"); BUILDER.push("toggleStance"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TOGGLE_STANCE = BUILDER.comment("Enable the Toggle Stance keybind to switch between standing/sneaking (and crawling if enabled).").define("enableToggleStance", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Crawling"); BUILDER.push("crawling"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CRAWLING_MECHANIC = BUILDER.comment("Enable the Crawling mechanic allowing players to explicitly enter a crawling stance.").define("enableCrawlingMechanic", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Friendly Fire Friendlies"); BUILDER.push("friendlyFireFriendlies"); }
    private static final ModConfigSpec.BooleanValue ENABLE_FRIENDLY_FIRE_FRIENDLIES = BUILDER.comment("Prevent players from accidentally attacking their own tamed pets.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Sitting"); BUILDER.push("sitting"); }
    private static final ModConfigSpec.BooleanValue SIT_TOGGLE_MODE = BUILDER.comment("If true, the Sit key acts as a toggle. If false, it acts as hold-to-sit.").define("toggleSitting", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Item Sharing"); BUILDER.push("itemSharing"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ITEM_SHARING = BUILDER.comment("Enable: Press Shift+Chat while hovering an item stack in an inventory to post a hoverable link in chat.").define("enableItemSharing", true);
    private static final ModConfigSpec.DoubleValue ITEM_SHARE_COOLDOWN_SECONDS = BUILDER.comment("Minimum time between item-share messages per player (seconds). Set to 0 to disable cooldown.").defineInRange("cooldownSeconds", 1.0D, 0.0D, 60.0D);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Campfires Ignite"); BUILDER.push("campfiresIgnite"); }
    private static final ModConfigSpec.BooleanValue CAMPFIRES_IGNITE_ENABLED = BUILDER.comment("Enable campfires to ignite entities standing on them, cooking them.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Cozy Campfire"); BUILDER.push("cozyCampfire"); }
    private static final ModConfigSpec.BooleanValue ENABLE_COZY_CAMPFIRE = BUILDER.comment("Enable: Receive Regeneration near a lit campfire every interval while within range.").define("enabled", true);
    private static final ModConfigSpec.DoubleValue COZY_CAMPFIRE_RANGE = BUILDER.comment("Range in blocks to search for a lit campfire.").defineInRange("range", 8.0D, 1.0D, 64.0D);
    private static final ModConfigSpec.IntValue COZY_CAMPFIRE_DURATION_TICKS = BUILDER.comment("Duration in ticks for the Regeneration effect when applied.").defineInRange("durationTicks", 100, 1, 12000);
    private static final ModConfigSpec.IntValue COZY_CAMPFIRE_INTERVAL_TICKS = BUILDER.comment("Interval in ticks between effect applications while in range.").defineInRange("intervalTicks", 100, 1, 12000);
    private static final ModConfigSpec.IntValue COZY_CAMPFIRE_AMPLIFIER = BUILDER.comment("Effect amplifier (0 = Regeneration I, 1 = Regeneration II, etc.).").defineInRange("amplifier", 0, 0, 10);
    private static final ModConfigSpec.BooleanValue COZY_CAMPFIRE_AFFECT_BEES = BUILDER.comment("Enable: Bees also receive the Regeneration effect when near a lit campfire.").define("affectBees", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Dismount Entities"); BUILDER.push("dismountEntities"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DISMOUNT_ENTITIES = BUILDER.comment("Enable the Dismount Entities mechanic allowing players to crouch + right-click vehicles to eject passengers.").define("enableDismountEntities", true);
    private static final ModConfigSpec.BooleanValue DISMOUNT_ALLOW_PLAYERS = BUILDER.comment("Allow dismounting other players from vehicles by crouch + right-clicking. Disabled by default.").define("allowDismountPlayers", false);
    private static final ModConfigSpec.DoubleValue DISMOUNT_VERTICAL_VELOCITY = BUILDER.comment("Vertical velocity applied when ejecting passengers from vehicles (0.3 = half of original, 0.6 = original).").defineInRange("verticalVelocity", 0.3, 0.0, 2.0);
    private static final ModConfigSpec.DoubleValue DISMOUNT_HORIZONTAL_VELOCITY = BUILDER.comment("Horizontal velocity applied when ejecting passengers from vehicles (0.5 = enhanced travel distance).").defineInRange("horizontalVelocity", 0.5, 0.0, 2.0);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Villagers Follow Emeralds"); BUILDER.push("villagersFollowEmeralds"); }
    private static final ModConfigSpec.BooleanValue ENABLE_VILLAGERS_FOLLOW_EMERALDS = BUILDER.comment("Enable: Holding an Emerald or Emerald Block makes nearby villagers follow you, similar to animal temptation.").define("enabled", true);
    private static final ModConfigSpec.DoubleValue VILLAGER_EMERALD_FOLLOW_SPEED = BUILDER.comment("Tempt follow speed modifier used by villagers when following emeralds (1.0 = normal movement speed).").defineInRange("speedModifier", 0.6D, 0.25D, 3.0D);
    private static final ModConfigSpec.BooleanValue VILLAGER_EMERALD_FOLLOW_SHOW_PARTICLES = BUILDER.comment("Show a small burst of happy villager particles when a villager begins following a player with emeralds.").define("showStartParticles", true);
    private static final ModConfigSpec.IntValue VILLAGER_EMERALD_FOLLOW_PARTICLES_COUNT = BUILDER.comment("How many happy villager particles to show when following begins.").defineInRange("startParticlesCount", 3, 1, 40);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Speedy Wolves"); BUILDER.push("speedyWolves"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SPEEDY_WOLVES = BUILDER.comment("Enable: Tamed wolves move faster to keep up with their owner when they fall behind.").define("enabled", true);
    private static final ModConfigSpec.DoubleValue SPEEDY_WOLVES_SPEED_MULTIPLIER = BUILDER.comment("Maximum movement speed multiplier applied to tamed wolves when keeping up (1.0 = no change). Actual boost dynamically scales with owner speed.").defineInRange("speedMultiplier", 1.3D, 1.0D, 2.0D);
    private static final ModConfigSpec.DoubleValue SPEEDY_WOLVES_DISTANCE_THRESHOLD = BUILDER.comment("Distance from owner beyond which the speed boost applies (blocks).").defineInRange("distanceThreshold", 6.0D, 1.0D, 48.0D);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Quick Harvesting"); BUILDER.push("quickHarvesting"); }
    private static final ModConfigSpec.BooleanValue ENABLE_QUICK_HARVESTING = BUILDER.comment("Enable all Quick Harvesting behaviors (master toggle).").define("enabled", true);
    private static final ModConfigSpec.BooleanValue ENABLE_QH_HOME_DROPS_TO_USER = BUILDER.comment("When enabled, Quick Harvesting drops home toward the user for 1 second.").define("homeDropsToUser", true);
    static { BUILDER.comment("Quick Harvesting with Hoes"); BUILDER.push("hoes"); }
    private static final ModConfigSpec.BooleanValue ENABLE_QH_HOES = BUILDER.comment("Enable: Right-click mature crops to harvest and auto-replant. Holding a hoe harvests a 3x3 area.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Quick Harvesting with Axes"); BUILDER.push("axes"); }
    private static final ModConfigSpec.BooleanValue ENABLE_QH_AXES = BUILDER.comment("Enable: Right-click with an axe to harvest axe-based crops (pumpkins, melons, cocoa, and compatible modded crops). Replants when applicable (e.g., cocoa).").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Door Knocking"); BUILDER.push("doorKnocking"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DOOR_KNOCKING = BUILDER.comment("Enable: Left-clicking a door plays a knocking sound matching the door material.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Coyote Time Jump"); BUILDER.push("coyoteTimeJump"); }
    private static final ModConfigSpec.BooleanValue ENABLE_COYOTE_TIME_JUMP = BUILDER.comment("Enable coyote-time jump: allows jumping for a short time after falling off a block without jumping first.").define("enabled", true);
    private static final ModConfigSpec.IntValue COYOTE_TIME_DELAY_MS = BUILDER.comment("Delay (ms) after starting to fall before coyote-time jump becomes available.").defineInRange("delayMs", 50, 50, 1000);
    private static final ModConfigSpec.IntValue COYOTE_TIME_WINDOW_MS = BUILDER.comment("Maximum time (ms) after starting to fall during which coyote-time jump is allowed.").defineInRange("windowMs", 1000, 200, 1000);
    private static final ModConfigSpec.BooleanValue COYOTE_TIME_DEBUG = BUILDER.comment("Enable debug logging for coyote time jump (shows detailed information in console).").define("debug", false);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Rapid Fire Jump"); BUILDER.push("rapidFireJump"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RAPID_FIRE_JUMP = BUILDER.comment("Enable rapid fire jump: hold the jump key to continuously execute jumps for quick navigation in 2-block gaps.").define("enabled", true);
    private static final ModConfigSpec.IntValue RAPID_FIRE_JUMP_INTERVAL = BUILDER.comment("Interval in ticks between rapid fire jumps when holding the jump key (20 ticks = 1 second).").defineInRange("intervalTicks", 1, 1, 20);
    private static final ModConfigSpec.BooleanValue RAPID_FIRE_JUMP_DEBUG = BUILDER.comment("Enable debug logging for rapid fire jump (shows detailed information in console).").define("debug", false);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Leashed Teleport"); BUILDER.push("leashedTeleport"); }
    private static final ModConfigSpec.BooleanValue ENABLE_LEASHED_TELEPORT = BUILDER.comment("Enable Leashed Teleport: leashed mobs follow players through any teleportation (commands, chorus fruit, ender pearls, portals, etc.).").define("enabled", true);
    private static final ModConfigSpec.BooleanValue LEASHED_TELEPORT_ALLOW_CROSS_DIMENSION = BUILDER.comment("Allow leashed mobs to follow players across dimensions (nether portals, end portals, etc.).").define("allowCrossDimension", true);
    private static final ModConfigSpec.IntValue LEASHED_TELEPORT_MAX_FOLLOWERS = BUILDER.comment("Maximum number of leashed mobs that can follow a player through teleportation.").defineInRange("maxFollowers", 12, 1, 50);
    private static final ModConfigSpec.DoubleValue LEASHED_TELEPORT_SCAN_RADIUS = BUILDER.comment("Radius in blocks to search for leashed mobs around the player.").defineInRange("scanRadius", 48.0D, 16.0D, 128.0D);
    private static final ModConfigSpec.IntValue LEASHED_TELEPORT_SAFE_PLACEMENT_TRIES = BUILDER.comment("Number of attempts to find a safe placement spot for teleported mobs.").defineInRange("safePlacementTries", 16, 4, 32);
    private static final ModConfigSpec.DoubleValue LEASHED_TELEPORT_BASE_RADIUS = BUILDER.comment("Base radius around player for safe placement attempts.").defineInRange("baseRadius", 2.5D, 1.0D, 8.0D);
    private static final ModConfigSpec.DoubleValue LEASHED_TELEPORT_MAX_RADIUS = BUILDER.comment("Maximum radius around player for safe placement attempts.").defineInRange("maxRadius", 5.0D, 2.0D, 16.0D);
    private static final ModConfigSpec.BooleanValue LEASHED_TELEPORT_POST_TELEPORT_LEASH = BUILDER.comment("Re-attach leashes after teleporting mobs to ensure they stay connected.").define("postTeleportLeash", true);
    private static final ModConfigSpec.IntValue LEASHED_TELEPORT_COOLDOWN_TICKS = BUILDER.comment("Cooldown in ticks between teleportation processing for each player (prevents spam).").defineInRange("cooldownTicks", 10, 1, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Chat Mentions"); BUILDER.push("chatMentions"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CHAT_MENTIONS = BUILDER.comment("Enable Chat Mentions: typing @username or username in chat highlights their name and sends them a ping sound.").define("enabled", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> TRIGGER_MODES = BUILDER.comment("Trigger modes: AT_NAME (@username), PLAIN_NAME (username)").defineList("triggerModes", List.of("AT_NAME", "PLAIN_NAME"), obj -> obj instanceof String);
    private static final ModConfigSpec.BooleanValue CASE_INSENSITIVE = BUILDER.comment("Make mention detection case-insensitive.").define("caseInsensitive", true);
    private static final ModConfigSpec.BooleanValue WORD_BOUNDARY = BUILDER.comment("Use word boundaries for mention detection (prevents 'jakub' matching 'hijakub').").define("wordBoundary", true);
    private static final ModConfigSpec.BooleanValue RECIPIENT_ONLY_HIGHLIGHT = BUILDER.comment("Only highlight mentions for the mentioned player (others see normal text).").define("recipientOnlyHighlight", true);
    private static final ModConfigSpec.IntValue MAX_MENTIONS_PER_MESSAGE = BUILDER.comment("Maximum number of mentions allowed per message.").defineInRange("maxMentionsPerMessage", 3, 1, 10);
    private static final ModConfigSpec.IntValue COOLDOWN_MS_PER_SENDER = BUILDER.comment("Cooldown in milliseconds between mentions from the same sender.").defineInRange("cooldownMsPerSender", 30000, 0, 10000);
    private static final ModConfigSpec.BooleanValue ALLOW_SELF_PING = BUILDER.comment("Allow players to mention themselves.").define("allowSelfPing", false);
    private static final ModConfigSpec.BooleanValue TAB_COMPLETE_USERNAMES = BUILDER.comment("Allow pressing Tab in chat to auto-complete online usernames after typing @.").define("tabCompleteUsernames", true);
    private static final ModConfigSpec.BooleanValue INPUT_HIGHLIGHT_USERNAMES = BUILDER.comment("Highlight fully typed online usernames in the local chat box before sending, including @username mentions.").define("inputHighlightUsernames", true);
    static { BUILDER.comment("Style"); BUILDER.push("style"); }
    private static final ModConfigSpec.ConfigValue<String> HIGHLIGHT_COLOR_HEX = BUILDER.comment("Hex color for highlighting mentioned usernames (without #).").define("highlightColorHex", "FF3B30");
    private static final ModConfigSpec.ConfigValue<String> HOVER_TEXT = BUILDER.comment("Hover text shown when hovering over a mentioned username.").define("hoverText", "You were mentioned");
    static { BUILDER.pop(); }
    static { BUILDER.comment("Sound"); BUILDER.push("sound"); }
    private static final ModConfigSpec.ConfigValue<String> SOUND_EVENT = BUILDER.comment("Sound event to play when mentioned.").define("soundEvent", "minecraft:block.note_block.pling");
    private static final ModConfigSpec.ConfigValue<String> SOUND_SOURCE = BUILDER.comment("Sound source category.").define("soundSource", "players");
    private static final ModConfigSpec.DoubleValue SOUND_VOLUME = BUILDER.comment("Sound volume (0.0 to 1.0).").defineInRange("soundVolume", 0.9, 0.0, 1.0);
    private static final ModConfigSpec.DoubleValue SOUND_PITCH = BUILDER.comment("Sound pitch (0.5 to 2.0).").defineInRange("soundPitch", 1.2, 0.5, 2.0);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Navigator Compass"); BUILDER.push("navigatorCompass"); }
    private static final ModConfigSpec.BooleanValue ENABLE_NAVIGATOR_COMPASS = BUILDER.comment("Enable Navigator Compass: Right-click a compass to set custom coordinates for navigation.").define("enabled", true);
    private static final ModConfigSpec.BooleanValue NAVIGATOR_COMPASS_SHOW_DISTANCE = BUILDER.comment("Show distance to target coordinates in compass tooltip.").define("showDistance", true);
    private static final ModConfigSpec.BooleanValue NAVIGATOR_COMPASS_SHOW_COORDINATES = BUILDER.comment("Show target coordinates in compass tooltip.").define("showCoordinates", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Bottle of Cloud"); BUILDER.push("bottleOfCloud"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BOTTLE_OF_CLOUD = BUILDER.comment("Enable Bottle of Cloud: Right-click a glass bottle on a cloud to capture it. The cloud is permanently altered. Place the bottle to spawn a temporary cloud that fades after 20 seconds.").define("enabled", true);
    private static final ModConfigSpec.IntValue BOTTLE_OF_CLOUD_DURATION_TICKS = BUILDER.comment("Duration in ticks for placed clouds to exist before disappearing (20 ticks = 1 second).").defineInRange("durationTicks", 400, 100, 6000);
    private static final ModConfigSpec.IntValue BOTTLE_OF_CLOUD_FADE_START_TICKS = BUILDER.comment("How many ticks before disappearing the cloud starts to fade (20 ticks = 1 second).").defineInRange("fadeStartTicks", 100, 20, 600);
    private static final ModConfigSpec.DoubleValue BOTTLE_OF_CLOUD_PLACEMENT_DISTANCE = BUILDER.comment("Distance in blocks from the player to place the cloud.").defineInRange("placementDistance", 2.0D, 1.0D, 5.0D);
    private static final ModConfigSpec.DoubleValue BOTTLE_OF_CLOUD_FALL_DAMAGE_MULTIPLIER = BUILDER.comment("Multiplier applied to normal fall damage when landing on Cloud Blocks. 0.5 = half damage, 0.0 = no damage, 1.0 = vanilla damage.").defineInRange("fallDamageMultiplier", 0.5D, 0.0D, 1.0D);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Pistons Move Tile Entities"); BUILDER.push("pistonsMoveTileEntities"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PISTONS_MOVE_TILE_ENTITIES = BUILDER.comment("Allow pistons to push and pull blocks with block entity data. Invalid final positions break the moved block and drop its contents.").define("enabled", true);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PISTON_MOVE_BLACKLIST_BLOCK_IDS = BUILDER.comment("Exact block ids that are never moved by this feature.").defineList("blacklistBlockIds", PistonBlockEntityMoveRuntime.defaultExactBlacklistBlockIds(), obj -> obj instanceof String);
    private static final ModConfigSpec.ConfigValue<List<? extends String>> PISTON_MOVE_BLACKLIST_NAME_CONTAINS = BUILDER.comment("Case-insensitive path/name fragments that automatically blacklist matching blocks, including other mods.").defineList("blacklistNameContains", PistonBlockEntityMoveRuntime.defaultBlacklistNameContains(), obj -> obj instanceof String);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Tweaks"); BUILDER.push("tweaks"); }

    static { BUILDER.comment("Armed Armor Stands"); BUILDER.push("armedArmorStands"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ARMED_ARMOR_STANDS = BUILDER.comment("Armor Stands placed by the Armor Stand item will have arms; you may also give them items.").define("enableArmedArmorStands", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Double Door Opening"); BUILDER.push("doubleDoorOpening"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DOUBLE_DOOR_OPENING = BUILDER.comment("Double doors open together when right clicked.").define("enableDoubleDoorOpening", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_CROUCH_SINGLE = BUILDER.comment("Crouch + right-click a double door to only affect the clicked door.").define("crouchSingleDoor", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_CHAIN_TRAPDOORS = BUILDER.comment("Open up to 5 touching trapdoors at once when right-clicking.").define("chainTrapdoors", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_SAME_BLOCK_ONLY = BUILDER.comment("Only pair doors or chain trapdoors when the adjacent block is the exact same block.").define("sameBlockOnly", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOORS_WITH_REDSTONE = BUILDER.comment("When a door is opened/closed via redstone, mirror the state to its paired door.").define("doubleDoorsWithRedstone", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_REDSTONE_INCLUDE_IRON = BUILDER.comment("Also apply redstone mirroring to iron doors.").define("includeIronDoorsWithRedstone", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_REDSTONE_INCLUDE_TRAPDOORS = BUILDER.comment("Also apply redstone mirroring to touching trapdoors (chain up to 5).").define("includeTrapDoorsWithRedstone", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Improved Climbing"); BUILDER.push("improvedClimbing"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CLIMBABLE_CHAIN_PLACEMENT = BUILDER.comment("Right-clicking a climbable block with a climbable item places another climbable at the end of the chain.").define("enableChainPlacement", true);
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_CLIMBING = BUILDER.comment("Enable faster climbing and sliding on any climbable while looking up or down.").define("enableImprovedClimbing", true);
    private static final ModConfigSpec.DoubleValue IMPROVED_CLIMBING_DOWN_SPEED = BUILDER.comment("Maximum climb-down speed on climbables while looking down (blocks per tick).").defineInRange("climbDownSpeed", 0.4D, 0.05D, 1.0D);
    private static final ModConfigSpec.IntValue IMPROVED_CLIMBING_LOOK_DOWN_MIN_DEG = BUILDER.comment("Minimum look-down angle in degrees to start faster downward climbing (0-90).").defineInRange("lookDownMinDeg", 45, 0, 90);
    private static final ModConfigSpec.IntValue IMPROVED_CLIMBING_LOOK_DOWN_MAX_DEG = BUILDER.comment("Maximum look-down angle in degrees for full downward climbing speed (0-90).").defineInRange("lookDownMaxDeg", 90, 0, 90);
    private static final ModConfigSpec.BooleanValue ENABLE_FAST_CLIMBABLE_ASCEND = BUILDER.comment("Enable faster upward climbing on climbables while looking up.").define("enableFastClimbUp", true);
    private static final ModConfigSpec.DoubleValue FAST_CLIMBABLE_ASCEND_SPEED = BUILDER.comment("Maximum climb-up speed on climbables while looking up (blocks per tick).").defineInRange("climbUpSpeed", 0.25D, 0.2D, 1.0D);
    private static final ModConfigSpec.IntValue FAST_CLIMBABLE_LOOK_UP_MIN_DEG = BUILDER.comment("Minimum look-up angle in degrees to start faster upward climbing (0-90).").defineInRange("lookUpMinDeg", 45, 0, 90);
    private static final ModConfigSpec.IntValue FAST_CLIMBABLE_LOOK_UP_MAX_DEG = BUILDER.comment("Maximum look-up angle in degrees for full upward climbing speed (0-90).").defineInRange("lookUpMaxDeg", 90, 0, 90);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Curse Uses"); BUILDER.push("curseUses"); }
    private static final ModConfigSpec.BooleanValue CURSE_HIDE_PUMPKIN_OVERLAY_ON_VANISHING = BUILDER.comment("Pumpkins with Curse of Vanishing will not show the pumpkin overlay when worn by an entity (mainly for Players).").define("hidePumpkinOverlayOnVanishing", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Despawn With Master"); BUILDER.push("despawnWithMaster"); }
    private static final ModConfigSpec.BooleanValue DESPAWN_VEX_WITH_EVOKER = BUILDER.comment("When an Evoker is removed from the world, despawn all Vex summoned by that Evoker.").define("despawnVexWithEvoker", true);
    private static final ModConfigSpec.BooleanValue DESPAWN_BULLETS_WITH_SHULKER = BUILDER.comment("When a Shulker is removed from the world, despawn all Shulker Bullets fired by that Shulker.").define("despawnBulletsWithShulker", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Creeper Sunlight Burn"); BUILDER.push("creeperSunlightBurn"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CREEPER_SUNLIGHT_BURN = BUILDER.comment("Creepers burn when in direct sunlight, just like Zombies and Skeletons do.").define("enableCreeperSunlightBurn", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Sophisticated Scaffolding"); BUILDER.push("sophisticatedScaffolding"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SOPHISTICATED_SCAFFOLDING = BUILDER.comment("When breaking scaffolding, all drops appear at your location.").define("enableSophisticatedScaffolding", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Brushing XP Rewards"); BUILDER.push("brushingXp"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BRUSHING_XP = BUILDER.comment("Spawn XP when finishing brushing a brushable block; scales with loot rarity.").define("enabled", true);
    private static final ModConfigSpec.IntValue BRUSHING_XP_MIN = BUILDER.comment("Minimum XP to spawn when brushing completes (inclusive).").defineInRange("min", 1, 0, 100);
    private static final ModConfigSpec.IntValue BRUSHING_XP_MAX = BUILDER.comment("Maximum XP to spawn when brushing completes (inclusive).").defineInRange("max", 10, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Improved Recovery Compass"); BUILDER.push("improvedRecoveryCompass"); }
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_RECOVERY_COMPASS = BUILDER.comment("Enable improvements to the Recovery Compass.").define("enableImprovedRecoveryCompass", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_KEEP_ON_DEATH = BUILDER.comment("Keep Recovery Compass in your inventory when you die (ignores keepInventory gamerule).").define("keepCompassOnDeath", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_SHOW_DISTANCE = BUILDER.comment("Show distance to death location in action bar when holding recovery compass.").define("showDistance", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_SHOW_COORDS_ON_RIGHT_CLICK = BUILDER.comment("Toggle between showing distance and coordinates when right-clicking recovery compass.").define("showCoordsOnRightClick", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_SCULK_PARTICLES = BUILDER.comment("Launch a short-lived soul trail toward the death location when shift + right-clicking the recovery compass.").define("sculkParticles", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Compostable Items"); BUILDER.push("compostableItems"); }
    private static final ModConfigSpec.BooleanValue ENABLE_COMPOSTABLE_ITEMS = BUILDER.comment("Enable compostable items feature (master toggle).").define("enableCompostableItems", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COMPOSTABLE_ROTTEN_FLESH = BUILDER.comment("Allow Rotten Flesh to be composted (30% chance).").define("enableCompostableRottenFlesh", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COMPOSTABLE_POISONOUS_POTATO = BUILDER.comment("Allow Poisonous Potatoes to be composted (65% chance).").define("enableCompostablePoisonousPotato", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Responsive Shields"); BUILDER.push("responsiveShields"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RESPONSIVE_SHIELDS = BUILDER.comment("Enable responsive shields feature (removes shield blocking delay).").define("enableResponsiveShields", true);
    private static final ModConfigSpec.IntValue SHIELD_RAISE_TIME = BUILDER.comment("Number of ticks from when you start blocking to when the shield actually blocks damage. 0 = instant blocking, 5 = vanilla delay.").defineInRange("shieldRaiseTime", 0, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Automatic Tool Restock"); BUILDER.push("automaticToolRestock"); }
    private static final ModConfigSpec.BooleanValue ENABLE_AUTOMATIC_TOOL_RESTOCK = BUILDER.comment("When a tool breaks on your hotbar, automatically move a replacement of the same type from your main inventory into that hotbar slot.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Automatic Block Restock"); BUILDER.push("automaticBlockRestock"); }
    private static final ModConfigSpec.BooleanValue ENABLE_AUTOMATIC_BLOCK_RESTOCK = BUILDER.comment("When you run out of blocks in your hotbar while building, automatically move more of the same block from your main inventory into that hotbar slot.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Treasure Enchantment Gold Color"); BUILDER.push("treasureEnchantmentGoldColor"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TREASURE_ENCHANTMENT_GOLD_COLOR = BUILDER.comment("Make beneficial treasure enchantments (Mending, Frost Walker, Soul Speed, Swift Sneak) display in gold color. Curses remain red.").define("enableTreasureEnchantmentGoldColor", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Glowing Glowberries"); BUILDER.push("glowingGlowberries"); }
    private static final ModConfigSpec.BooleanValue ENABLE_GLOWING_GLOWBERRIES = BUILDER.comment("Give the Glowing effect to entities that consume glowberries.").define("enableGlowingGlowberries", true);
    private static final ModConfigSpec.IntValue GLOWING_GLOWBERRIES_DURATION = BUILDER.comment("Duration of the Glowing effect in seconds when consuming glowberries.").defineInRange("glowingDuration", 15, 1, 300);
    private static final ModConfigSpec.IntValue FOX_GLOWING_DURATION = BUILDER.comment("Duration of the Glowing effect in seconds for foxes when they hold glowberries.").defineInRange("foxGlowingDuration", 5, 1, 300);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Enderdragon Egg Always"); BUILDER.push("enderdragonEggAlways"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ENDERDRAGON_EGG_ALWAYS = BUILDER.comment("Ensures the Ender Dragon always drops a dragon egg when killed, not just the first time.").define("enableEnderdragonEggAlways", true);
    private static final ModConfigSpec.BooleanValue ENDERDRAGON_EGG_USE_RANDOM_PLACEMENT = BUILDER.comment("If true, eggs spawn randomly within the configured radius. If false, eggs spawn at vanilla location (center of podium).").define("useRandomPlacement", false);
    private static final ModConfigSpec.IntValue ENDERDRAGON_EGG_SPAWN_RADIUS = BUILDER.comment("Radius in blocks around the End fountain where eggs can spawn (only used if useRandomPlacement is true).").defineInRange("spawnRadius", 10, 1, 50);
    private static final ModConfigSpec.IntValue ENDERDRAGON_EGG_SPAWN_DELAY = BUILDER.comment("Delay in seconds after dragon death before the egg spawns.").defineInRange("spawnDelaySeconds", 10, 1, 60);
    private static final ModConfigSpec.BooleanValue ENDERDRAGON_EGG_SHOW_PARTICLES = BUILDER.comment("Show purple particle effects when the dragon egg spawns.").define("showParticles", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Sugarcane Sand"); BUILDER.push("sugarcaneSand"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SUGARCANE_SAND = BUILDER.comment("Enable Sugarcane Sand: sugarcane grows 25% faster when planted on sand instead of grass/dirt.").define("enableSugarcaneSand", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("More Mining XP"); BUILDER.push("moreMiningXp"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MORE_MINING_XP = BUILDER.comment("Enable More Mining XP: increase XP drops from mining ores (affects both vanilla and variant ores).").define("enabled", true);
    static { BUILDER.comment("Coal Ore XP"); BUILDER.push("coal"); }
    private static final ModConfigSpec.IntValue COAL_XP_MIN = BUILDER.comment("Minimum XP dropped by coal ore (inclusive).").defineInRange("min", 1, 0, 100);
    private static final ModConfigSpec.IntValue COAL_XP_MAX = BUILDER.comment("Maximum XP dropped by coal ore (inclusive).").defineInRange("max", 3, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Diamond Ore XP"); BUILDER.push("diamond"); }
    private static final ModConfigSpec.IntValue DIAMOND_XP_MIN = BUILDER.comment("Minimum XP dropped by diamond ore (inclusive).").defineInRange("min", 5, 0, 100);
    private static final ModConfigSpec.IntValue DIAMOND_XP_MAX = BUILDER.comment("Maximum XP dropped by diamond ore (inclusive).").defineInRange("max", 10, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Emerald Ore XP"); BUILDER.push("emerald"); }
    private static final ModConfigSpec.IntValue EMERALD_XP_MIN = BUILDER.comment("Minimum XP dropped by emerald ore (inclusive).").defineInRange("min", 5, 0, 100);
    private static final ModConfigSpec.IntValue EMERALD_XP_MAX = BUILDER.comment("Maximum XP dropped by emerald ore (inclusive).").defineInRange("max", 10, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Lapis Lazuli Ore XP"); BUILDER.push("lapis"); }
    private static final ModConfigSpec.IntValue LAPIS_XP_MIN = BUILDER.comment("Minimum XP dropped by lapis lazuli ore (inclusive).").defineInRange("min", 4, 0, 100);
    private static final ModConfigSpec.IntValue LAPIS_XP_MAX = BUILDER.comment("Maximum XP dropped by lapis lazuli ore (inclusive).").defineInRange("max", 8, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Redstone Ore XP"); BUILDER.push("redstone"); }
    private static final ModConfigSpec.IntValue REDSTONE_XP_MIN = BUILDER.comment("Minimum XP dropped by redstone ore (inclusive).").defineInRange("min", 2, 0, 100);
    private static final ModConfigSpec.IntValue REDSTONE_XP_MAX = BUILDER.comment("Maximum XP dropped by redstone ore (inclusive).").defineInRange("max", 6, 0, 100);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Respawn Anchor Anywhere"); BUILDER.push("respawn_anchor_anywhere"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RESPAWN_ANCHOR_ANYWHERE = BUILDER.comment("Enable Respawn Anchors to work in any dimension (not just the Nether).").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Return To Killer"); BUILDER.push("returnToKiller"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RETURN_TO_KILLER = BUILDER.comment("When killing flying mobs, make their XP and drops home toward the killer for easier pickup.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Tipped Arrows Lingering Clouds"); BUILDER.push("tippedArrowLingeringClouds"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TIPPED_ARROW_LINGERING_CLOUDS = BUILDER.comment("Tipped arrows create an area effect cloud of their potion effect when they land, then become normal arrows.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Speedy Happy Ghasts"); BUILDER.push("speedyHappyGhasts"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SPEEDY_HAPPY_GHASTS = BUILDER.comment("Happy Ghasts are affected by Swiftness and Slowness (applied to flying speed).").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Candle Bundles"); BUILDER.push("candleBundles"); }
    private static final ModConfigSpec.BooleanValue ALLOW_MIXED_CANDLE_PLACEMENT = BUILDER.comment("Allow placing different candle items into the same multi-candle block (mixed candle bundles).").define("allowMixedCandlePlacement", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Unbreakable Trial Spawners"); BUILDER.push("unbreakableTrialSpawners"); }
    private static final ModConfigSpec.BooleanValue ENABLE_UNBREAKABLE_TRIAL_SPAWNERS = BUILDER.comment("Make Trial Spawners completely unbreakable. They cannot be destroyed by mining, explosions, or any other means.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Unbreakable Vaults"); BUILDER.push("unbreakableVaults"); }
    private static final ModConfigSpec.BooleanValue ENABLE_UNBREAKABLE_VAULTS = BUILDER.comment("Make Vaults completely unbreakable. They cannot be destroyed by mining, explosions, or any other means.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Enhanced Slab Behavior", "Hang items below slabs, enable KneeSlab-like mining, and mixed double slabs."); BUILDER.push("enhancedSlabs"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ENHANCED_SLABS = BUILDER.comment("Master toggle for all Enhanced Slab Behavior features.").define("enabled", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_PLACE_ON_TOP = BUILDER.comment("(Currently disabled) Formerly allowed placing items like torches directly on top of bottom/double slabs.").define("allowPlaceOnTop", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_HANG_BELOW = BUILDER.comment("Allow hanging items like lanterns, chains, hanging signs below top/double slabs.").define("allowHangBelow", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_KNEE_SLAB_MINING = BUILDER.comment("Enable KneeSlab-like mining: break each half of a double slab independently in survival mode. Mining speed depends on the targeted half and your tool.").define("kneeSlabMining", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_MIXED_DOUBLE_SLABS = BUILDER.comment("Allow placing different slab types together to create mixed double slabs (e.g. oak bottom + stone top in one block position).").define("mixedDoubleSlabs", true);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Mobs"); BUILDER.push("mobs"); }

    static { BUILDER.comment("Cave Spiders"); BUILDER.push("caveSpiders"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CAVE_SPIDERS_IN_CAVES = BUILDER.comment("Allow Cave Spiders to spawn in place of Spiders in caves.").define("enableCaveSpidersInCaves", true);
    private static final ModConfigSpec.DoubleValue CAVE_SPIDER_REPLACEMENT_CHANCE = BUILDER.comment("Chance for a Spider in a cave to be replaced by a Cave Spider (0.0-1.0).").defineInRange("caveSpiderReplacementChance", 0.20D, 0.0D, 1.0D);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Disable Nitwits"); BUILDER.push("disableNitwits"); }
    private static final ModConfigSpec.BooleanValue DISABLE_NITWITS = BUILDER.comment("Prevent Nitwit villagers from spawning naturally.").define("disableNitwits", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Improved Phantoms"); BUILDER.push("improvedPhantoms"); }
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_PHANTOMS = BUILDER.comment("Enable improved phantom mechanics.").define("enableImprovedPhantoms", true);
    static { BUILDER.comment("Phantom Pickup"); BUILDER.push("phantomPickup"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_PICKUP = BUILDER.comment("Allow phantoms to pick up and carry players.").define("enablePhantomPickup", true);
    private static final ModConfigSpec.DoubleValue PHANTOM_PICKUP_CHANCE = BUILDER.comment("Chance for a phantom to attempt picking up a player when attacking (0.0-1.0).").defineInRange("phantomPickupChance", 0.3D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue PHANTOM_PICKUP_MIN_DELAY = BUILDER.comment("Minimum delay in seconds before player can dismount from phantom.").defineInRange("phantomPickupMinDelay", 1, 0, 10);
    private static final ModConfigSpec.DoubleValue PHANTOM_PICKUP_SPEED_MULTIPLIER = BUILDER.comment("Speed multiplier for phantoms when carrying a player (1.0 = normal speed, 0.5 = half speed).").defineInRange("phantomPickupSpeedMultiplier", 0.5D, 0.1D, 1.0D);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Phantom Slowness Stacking"); BUILDER.push("phantomSlowness"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_SLOWNESS_STACKING = BUILDER.comment("Enable stacking slowness effect when hit by phantoms.").define("enablePhantomSlownessStacking", true);
    private static final ModConfigSpec.IntValue PHANTOM_SLOWNESS_DURATION = BUILDER.comment("Duration of slowness effect in seconds.").defineInRange("phantomSlownessDuration", 10, 1, 60);
    private static final ModConfigSpec.IntValue PHANTOM_SLOWNESS_MAX_LEVEL = BUILDER.comment("Maximum slowness level that can be stacked (1-4).").defineInRange("phantomSlownessMaxLevel", 4, 1, 4);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Double Damage When Carried"); BUILDER.push("phantomDoubleDamage"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_DOUBLE_DAMAGE = BUILDER.comment("Enable double damage when hitting a phantom while being carried by it.").define("enablePhantomDoubleDamage", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Automatic Drop"); BUILDER.push("phantomAutoDrop"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_AUTO_DROP = BUILDER.comment("Automatically make phantoms drop carried players after the configured carry time.").define("enablePhantomAutoDrop", true);
    private static final ModConfigSpec.IntValue PHANTOM_AUTO_DROP_SECONDS = BUILDER.comment("How many seconds a phantom carries a player before automatically dropping them.").defineInRange("phantomAutoDropSeconds", 3, 1, 60);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Drop On Hit"); BUILDER.push("phantomDropOnHit"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_DROP_ON_HIT = BUILDER.comment("Make phantoms immediately drop players who hit them while being carried.").define("enablePhantomDropOnHit", true);
    static { BUILDER.pop(); }
        static { BUILDER.comment("Hit Dismount Slow Falling"); BUILDER.push("phantomDismountSlowFalling"); }
        private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_SLOW_FALLING_ON_HIT_DISMOUNT = BUILDER.comment("Give players 2 seconds of Slow Falling I when a phantom carry ends after they hit the carrying phantom.").define("enablePhantomSlowFallingOnHitDismount", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Better Combat Integration"); BUILDER.push("betterCombatIntegration"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BETTER_COMBAT_INTEGRATION = BUILDER.comment("Enable Better Combat mod integration for enhanced phantom combat mechanics. Only takes effect if Better Combat is installed.").define("enableBetterCombatIntegration", true);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    private static final ModConfigSpec.BooleanValue IRON_GOLEMS_KILL_CREEPERS = BUILDER.comment("Enable: Iron Golems will actively target and kill Creepers. Creepers will not retaliate/ignite when attacked by Iron Golems.").define("ironGolemsKillCreepers", true);
    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;

        UnmodifiableConfig configData = null;
        var loadedConfig = event.getConfig().getLoadedConfig();
        if (loadedConfig != null) {
            configData = loadedConfig.config();
        }

        Config.enableSecondChance = ENABLE_SECOND_CHANCE.get();
        Config.secondChanceExcludeFall = SECOND_CHANCE_EXCLUDE_FALL.get();
        Config.secondChanceCooldownDays = SECOND_CHANCE_COOLDOWN_DAYS.get();
        Config.secondChanceResetOnRespawn = SECOND_CHANCE_RESET_ON_RESPAWN.get();
        Config.secondChanceResistanceEnabled = SECOND_CHANCE_RESISTANCE_ENABLED.get();
        Config.secondChanceResistanceLevel = SECOND_CHANCE_RESISTANCE_LEVEL.get();
        Config.secondChanceNauseaEnabled = SECOND_CHANCE_NAUSEA_ENABLED.get();
        Config.secondChanceNauseaLevel = SECOND_CHANCE_NAUSEA_LEVEL.get();
        Config.enableMaintainExperience = ENABLE_MAINTAIN_EXPERIENCE.get();
        Config.maintainExperienceLossPercent = MAINTAIN_EXPERIENCE_LOSS_PERCENT.get();
        Config.enableSnowballRework = ENABLE_SNOWBALL_REWORK.get();
        Config.snowballBaseDamage = SNOWBALL_BASE_DAMAGE.get();
        Config.snowballNetherDamage = SNOWBALL_NETHER_DAMAGE.get();
        Config.snowballMinFreezeSeconds = SNOWBALL_MIN_FREEZE_SECONDS.get();
        Config.snowballMaxFreezeSeconds = SNOWBALL_MAX_FREEZE_SECONDS.get();
        Config.snowballNetherEntityTypes = SNOWBALL_NETHER_ENTITIES.get().stream()
                .map(Identifier::parse)
                .map(BuiltInRegistries.ENTITY_TYPE::get)
                .flatMap(java.util.Optional::stream)
                .map(net.minecraft.core.Holder.Reference::value)
                .collect(Collectors.toSet());

        Config.enableFullEnderDragonXp = ENABLE_FULL_ENDER_DRAGON_XP.get();
        Config.enableFoodAlwaysEdible = ENABLE_FOOD_ALWAYS_EDIBLE.get();
        Config.enableSourceDependentIFrames = ENABLE_SOURCE_DEPENDENT_IFRAMES.get();
        Config.sourceIFrameBlacklist = SOURCE_IFRAME_BLACKLIST.get().stream().map(Identifier::parse).collect(Collectors.toSet());
        Config.dseEnabled = DSE_ENABLED.get();
        Config.dseOverworldOnly = DSE_OVERWORLD_ONLY.get();
        Config.dseEnableSurfaceTier = DSE_ENABLE_SURFACE_TIER.get();
        Config.dseEnableMidTier = DSE_ENABLE_MID_TIER.get();
        Config.dseEnableDeepTier = DSE_ENABLE_DEEP_TIER.get();
        Config.dseMidDepthY = DSE_MID_Y.get();
        Config.dseDeepDepthY = DSE_DEEP_Y.get();
        Config.dseSurfaceEquipChance = DSE_SURFACE_EQUIP_CHANCE.get();
        Config.dseMidEquipChance = DSE_MID_EQUIP_CHANCE.get();
        Config.dseDeepEquipChance = DSE_DEEP_EQUIP_CHANCE.get();
        Config.dseSurfaceLeatherWeight = DSE_SURFACE_LEATHER.get();
        Config.dseSurfaceChainWeight = DSE_SURFACE_CHAIN.get();
        Config.dseSurfaceCopperWeight = DSE_SURFACE_COPPER.get();
        Config.dseSurfaceIronWeight = DSE_SURFACE_IRON.get();
        Config.dseSurfaceGoldWeight = DSE_SURFACE_GOLD.get();
        Config.dseMidLeatherWeight = DSE_MID_LEATHER.get();
        Config.dseMidChainWeight = DSE_MID_CHAIN.get();
        Config.dseMidCopperWeight = DSE_MID_COPPER.get();
        Config.dseMidIronWeight = DSE_MID_IRON.get();
        Config.dseMidGoldWeight = DSE_MID_GOLD.get();
        Config.dseDeepLeatherWeight = DSE_DEEP_LEATHER.get();
        Config.dseDeepChainWeight = DSE_DEEP_CHAIN.get();
        Config.dseDeepCopperWeight = DSE_DEEP_COPPER.get();
        Config.dseDeepIronWeight = DSE_DEEP_IRON.get();
        Config.dseDeepGoldWeight = DSE_DEEP_GOLD.get();
        Config.dseDeepMaxIronPieces = DSE_DEEP_MAX_IRON.get();
        Config.dseSurfaceZombieWeaponChance = DSE_SURFACE_ZOMBIE_WEAPON.get();
        Config.dseMidZombieWeaponChance = DSE_MID_ZOMBIE_WEAPON.get();
        Config.dseDeepZombieWeaponChance = DSE_DEEP_ZOMBIE_WEAPON.get();
        Config.enableBalancedElytra = ENABLE_BALANCED_ELYTRA.get();
        Config.balancedElytraBlocksPerDurability = BALANCED_ELYTRA_BLOCKS_PER_DURABILITY.get();
        Config.enableNerfedMending = ENABLE_NERFED_MENDING.get();
        Config.nerfedMendingMultiplier = NERFED_MENDING_MULTIPLIER.get();
        Config.enableNerfedDiscounts = ENABLE_NERFED_DISCOUNTS.get();
        Config.removeZombieCureDiscounts = REMOVE_ZOMBIE_CURE_DISCOUNTS.get();
        Config.maxVillagerDiscount = MAX_VILLAGER_DISCOUNT.get();

        Config.enableToggleStance = ENABLE_TOGGLE_STANCE.get();
        Config.enableCrawlingMechanic = ENABLE_CRAWLING_MECHANIC.get();
        Config.enableFriendlyFireFriendlies = ENABLE_FRIENDLY_FIRE_FRIENDLIES.get();
        Config.toggleSitting = SIT_TOGGLE_MODE.get();
        Config.enableItemSharing = ENABLE_ITEM_SHARING.get();
        Config.itemShareCooldownSeconds = ITEM_SHARE_COOLDOWN_SECONDS.get();
        Config.enableCampfiresIgniteEntities = CAMPFIRES_IGNITE_ENABLED.get();
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
        Config.enableVillagersFollowEmeralds = ENABLE_VILLAGERS_FOLLOW_EMERALDS.get();
        Config.villagerEmeraldFollowSpeed = VILLAGER_EMERALD_FOLLOW_SPEED.get();
        Config.villagerEmeraldShowParticles = VILLAGER_EMERALD_FOLLOW_SHOW_PARTICLES.get();
        Config.villagerEmeraldParticlesCount = VILLAGER_EMERALD_FOLLOW_PARTICLES_COUNT.get();
        Config.enableSpeedyWolves = ENABLE_SPEEDY_WOLVES.get();
        Config.speedyWolvesSpeedMultiplier = SPEEDY_WOLVES_SPEED_MULTIPLIER.get();
        Config.speedyWolvesDistanceThreshold = SPEEDY_WOLVES_DISTANCE_THRESHOLD.get();
        Config.enableQuickHarvesting = ENABLE_QUICK_HARVESTING.get();
        Config.enableQuickHarvestingHomeDropsToUser = ENABLE_QH_HOME_DROPS_TO_USER.get();
        Config.enableQuickHarvestingHoes = ENABLE_QH_HOES.get();
        Config.enableQuickHarvestingAxes = ENABLE_QH_AXES.get();
        Config.enableDoorKnocking = ENABLE_DOOR_KNOCKING.get();
        Config.enableCoyoteTimeJump = ENABLE_COYOTE_TIME_JUMP.get();
        Config.coyoteTimeDelayMs = COYOTE_TIME_DELAY_MS.get();
        Config.coyoteTimeWindowMs = COYOTE_TIME_WINDOW_MS.get();
        Config.coyoteTimeDebug = COYOTE_TIME_DEBUG.get();
        if (Config.coyoteTimeDelayMs < 50) Config.coyoteTimeDelayMs = 50;
        if (Config.coyoteTimeDelayMs > 1000) Config.coyoteTimeDelayMs = 1000;
        if (Config.coyoteTimeWindowMs < 200) Config.coyoteTimeWindowMs = 200;
        if (Config.coyoteTimeWindowMs > 1000) Config.coyoteTimeWindowMs = 1000;
        if (Config.coyoteTimeDelayMs > Config.coyoteTimeWindowMs) Config.coyoteTimeDelayMs = Config.coyoteTimeWindowMs;
        Config.enableRapidFireJump = ENABLE_RAPID_FIRE_JUMP.get();
        Config.rapidFireJumpInterval = RAPID_FIRE_JUMP_INTERVAL.get();
        Config.rapidFireJumpDebug = RAPID_FIRE_JUMP_DEBUG.get();
        Config.enableLeashedTeleport = ENABLE_LEASHED_TELEPORT.get();
        Config.leashedTeleportAllowCrossDimension = LEASHED_TELEPORT_ALLOW_CROSS_DIMENSION.get();
        Config.leashedTeleportMaxFollowers = LEASHED_TELEPORT_MAX_FOLLOWERS.get();
        Config.leashedTeleportScanRadius = LEASHED_TELEPORT_SCAN_RADIUS.get();
        Config.leashedTeleportSafePlacementTries = LEASHED_TELEPORT_SAFE_PLACEMENT_TRIES.get();
        Config.leashedTeleportBaseRadius = LEASHED_TELEPORT_BASE_RADIUS.get();
        Config.leashedTeleportMaxRadius = LEASHED_TELEPORT_MAX_RADIUS.get();
        Config.leashedTeleportPostTeleportLeash = LEASHED_TELEPORT_POST_TELEPORT_LEASH.get();
        Config.leashedTeleportCooldownTicks = LEASHED_TELEPORT_COOLDOWN_TICKS.get();
        Config.leashedTeleportBlacklistTypes = Set.of();
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
        Config.enableNavigatorCompass = ENABLE_NAVIGATOR_COMPASS.get();
        Config.navigatorCompassShowDistance = NAVIGATOR_COMPASS_SHOW_DISTANCE.get();
        Config.navigatorCompassShowCoordinates = NAVIGATOR_COMPASS_SHOW_COORDINATES.get();
        Config.enableBottleOfCloud = ENABLE_BOTTLE_OF_CLOUD.get();
        Config.bottleOfCloudDurationTicks = BOTTLE_OF_CLOUD_DURATION_TICKS.get();
        Config.bottleOfCloudFadeStartTicks = BOTTLE_OF_CLOUD_FADE_START_TICKS.get();
        Config.bottleOfCloudPlacementDistance = BOTTLE_OF_CLOUD_PLACEMENT_DISTANCE.get();
        Config.bottleOfCloudFallDamageMultiplier = BOTTLE_OF_CLOUD_FALL_DAMAGE_MULTIPLIER.get();
        PistonBlockEntityMoveRuntime.applyConfig(
                ENABLE_PISTONS_MOVE_TILE_ENTITIES.get(),
                new java.util.ArrayList<>(PISTON_MOVE_BLACKLIST_BLOCK_IDS.get()),
                new java.util.ArrayList<>(PISTON_MOVE_BLACKLIST_NAME_CONTAINS.get())
        );
        BottleOfCloudRuntime.enabled = Config.enableBottleOfCloud;
        BottleOfCloudRuntime.durationTicks = Config.bottleOfCloudDurationTicks;
        BottleOfCloudRuntime.fadeStartTicks = Config.bottleOfCloudFadeStartTicks;
        BottleOfCloudRuntime.placementDistance = Config.bottleOfCloudPlacementDistance;
        BottleOfCloudRuntime.fallDamageMultiplier = Config.bottleOfCloudFallDamageMultiplier;

        Config.enableArmedArmorStands = ENABLE_ARMED_ARMOR_STANDS.get();
        Config.enableDoubleDoorOpening = ENABLE_DOUBLE_DOOR_OPENING.get();
        Config.doubleDoorCrouchSingle = DOUBLE_DOOR_CROUCH_SINGLE.get();
        Config.doubleDoorChainTrapdoors = DOUBLE_DOOR_CHAIN_TRAPDOORS.get();
        Config.doubleDoorSameBlockOnly = DOUBLE_DOOR_SAME_BLOCK_ONLY.get();
        Config.doubleDoorsWithRedstone = DOUBLE_DOORS_WITH_REDSTONE.get();
        Config.doubleDoorRedstoneIncludeIron = DOUBLE_DOOR_REDSTONE_INCLUDE_IRON.get();
        Config.doubleDoorRedstoneIncludeTrapdoors = DOUBLE_DOOR_REDSTONE_INCLUDE_TRAPDOORS.get();
        Boolean legacyChainPlacement = getLegacyBoolean(configData, "tweaks.improvedClimbables.enableLadderChainPlacement");
        Config.enableClimbableChainPlacement = legacyChainPlacement != null ? legacyChainPlacement : ENABLE_CLIMBABLE_CHAIN_PLACEMENT.get();
        Boolean legacyImprovedClimbing = getLegacyBoolean(configData, "tweaks.improvedClimbables.enableFastLadderSlide");
        Config.enableFastClimbableSlide = legacyImprovedClimbing != null ? legacyImprovedClimbing : ENABLE_IMPROVED_CLIMBING.get();
        Integer legacyMinDown = getLegacyInt(configData, "tweaks.improvedClimbables.fastLadderLookDownMinDeg");
        Integer legacyMaxDown = getLegacyInt(configData, "tweaks.improvedClimbables.fastLadderLookDownMaxDeg");
        Double legacyDownSpeed = getLegacyDouble(configData, "tweaks.improvedClimbables.fastLadderSlideSpeed");
        int minDownTmp = legacyMinDown != null ? legacyMinDown : IMPROVED_CLIMBING_LOOK_DOWN_MIN_DEG.get();
        int maxDownTmp = legacyMaxDown != null ? legacyMaxDown : IMPROVED_CLIMBING_LOOK_DOWN_MAX_DEG.get();
        minDownTmp = Math.max(0, Math.min(90, minDownTmp));
        maxDownTmp = Math.max(0, Math.min(90, maxDownTmp));
        if (minDownTmp > maxDownTmp) maxDownTmp = minDownTmp;
        Config.fastClimbableLookDownMinDeg = minDownTmp;
        Config.fastClimbableLookDownMaxDeg = maxDownTmp;
        Config.fastClimbableSlideSpeed = legacyDownSpeed != null ? Math.max(0.05D, Math.min(1.0D, legacyDownSpeed)) : IMPROVED_CLIMBING_DOWN_SPEED.get();
        Config.enableFastClimbableAscend = ENABLE_FAST_CLIMBABLE_ASCEND.get();
        int minUpTmp = FAST_CLIMBABLE_LOOK_UP_MIN_DEG.get();
        int maxUpTmp = FAST_CLIMBABLE_LOOK_UP_MAX_DEG.get();
        minUpTmp = Math.max(0, Math.min(90, minUpTmp));
        maxUpTmp = Math.max(0, Math.min(90, maxUpTmp));
        if (minUpTmp > maxUpTmp) maxUpTmp = minUpTmp;
        Config.fastClimbableLookUpMinDeg = minUpTmp;
        Config.fastClimbableLookUpMaxDeg = maxUpTmp;
        Config.fastClimbableAscendSpeed = FAST_CLIMBABLE_ASCEND_SPEED.get();
        Config.curseHidePumpkinOverlayOnVanishing = CURSE_HIDE_PUMPKIN_OVERLAY_ON_VANISHING.get();
        Config.despawnVexWithEvoker = DESPAWN_VEX_WITH_EVOKER.get();
        Config.despawnBulletsWithShulker = DESPAWN_BULLETS_WITH_SHULKER.get();
        Config.enableCreeperSunlightBurn = ENABLE_CREEPER_SUNLIGHT_BURN.get();
        Config.enableSophisticatedScaffolding = ENABLE_SOPHISTICATED_SCAFFOLDING.get();
        Config.brushingXpEnabled = ENABLE_BRUSHING_XP.get();
        int brushingMin = BRUSHING_XP_MIN.get();
        int brushingMax = BRUSHING_XP_MAX.get();
        if (brushingMin < 0) brushingMin = 0;
        if (brushingMax < brushingMin) brushingMax = brushingMin;
        if (brushingMax > 100) brushingMax = 100;
        Config.brushingXpMin = brushingMin;
        Config.brushingXpMax = brushingMax;
        Config.enableImprovedRecoveryCompass = ENABLE_IMPROVED_RECOVERY_COMPASS.get();
        Config.recoveryCompassKeepOnDeath = RECOVERY_COMPASS_KEEP_ON_DEATH.get();
        Config.recoveryCompassShowDistance = RECOVERY_COMPASS_SHOW_DISTANCE.get();
        Config.recoveryCompassShowCoordsOnRightClick = RECOVERY_COMPASS_SHOW_COORDS_ON_RIGHT_CLICK.get();
        Config.recoveryCompassSculkParticles = RECOVERY_COMPASS_SCULK_PARTICLES.get();
        Config.enableCompostableItems = ENABLE_COMPOSTABLE_ITEMS.get();
        Config.enableCompostableRottenFlesh = ENABLE_COMPOSTABLE_ROTTEN_FLESH.get();
        Config.enableCompostablePoisonousPotato = ENABLE_COMPOSTABLE_POISONOUS_POTATO.get();
        Config.enableResponsiveShields = ENABLE_RESPONSIVE_SHIELDS.get();
        Config.shieldRaiseTime = SHIELD_RAISE_TIME.get();
        Config.enableAutomaticToolRestock = ENABLE_AUTOMATIC_TOOL_RESTOCK.get();
        Config.enableAutomaticBlockRestock = ENABLE_AUTOMATIC_BLOCK_RESTOCK.get();
        Config.enableTreasureEnchantmentGoldColor = ENABLE_TREASURE_ENCHANTMENT_GOLD_COLOR.get();
        Config.enableGlowingGlowberries = ENABLE_GLOWING_GLOWBERRIES.get();
        Config.glowingGlowberriesDuration = GLOWING_GLOWBERRIES_DURATION.get();
        Config.foxGlowingDuration = FOX_GLOWING_DURATION.get();
        Config.enableEnderdragonEggAlways = ENABLE_ENDERDRAGON_EGG_ALWAYS.get();
        Config.enderdragonEggUseRandomPlacement = ENDERDRAGON_EGG_USE_RANDOM_PLACEMENT.get();
        Config.enderdragonEggSpawnRadius = ENDERDRAGON_EGG_SPAWN_RADIUS.get();
        Config.enderdragonEggSpawnDelay = ENDERDRAGON_EGG_SPAWN_DELAY.get();
        Config.enderdragonEggShowParticles = ENDERDRAGON_EGG_SHOW_PARTICLES.get();
        Config.enableSugarcaneSand = ENABLE_SUGARCANE_SAND.get();
        Config.enableMoreMiningXp = ENABLE_MORE_MINING_XP.get();
        int coalMin = COAL_XP_MIN.get();
        int coalMax = COAL_XP_MAX.get();
        if (coalMin < 0) coalMin = 0;
        if (coalMax < coalMin) coalMax = coalMin;
        if (coalMax > 100) coalMax = 100;
        Config.coalXpMin = coalMin;
        Config.coalXpMax = coalMax;
        int diamondMin = DIAMOND_XP_MIN.get();
        int diamondMax = DIAMOND_XP_MAX.get();
        if (diamondMin < 0) diamondMin = 0;
        if (diamondMax < diamondMin) diamondMax = diamondMin;
        if (diamondMax > 100) diamondMax = 100;
        Config.diamondXpMin = diamondMin;
        Config.diamondXpMax = diamondMax;
        int emeraldMin = EMERALD_XP_MIN.get();
        int emeraldMax = EMERALD_XP_MAX.get();
        if (emeraldMin < 0) emeraldMin = 0;
        if (emeraldMax < emeraldMin) emeraldMax = emeraldMin;
        if (emeraldMax > 100) emeraldMax = 100;
        Config.emeraldXpMin = emeraldMin;
        Config.emeraldXpMax = emeraldMax;
        int lapisMin = LAPIS_XP_MIN.get();
        int lapisMax = LAPIS_XP_MAX.get();
        if (lapisMin < 0) lapisMin = 0;
        if (lapisMax < lapisMin) lapisMax = lapisMin;
        if (lapisMax > 100) lapisMax = 100;
        Config.lapisXpMin = lapisMin;
        Config.lapisXpMax = lapisMax;
        int redstoneMin = REDSTONE_XP_MIN.get();
        int redstoneMax = REDSTONE_XP_MAX.get();
        if (redstoneMin < 0) redstoneMin = 0;
        if (redstoneMax < redstoneMin) redstoneMax = redstoneMin;
        if (redstoneMax > 100) redstoneMax = 100;
        Config.redstoneXpMin = redstoneMin;
        Config.redstoneXpMax = redstoneMax;
        Boolean legacyRespawnAnchorAnywhere = getLegacyBoolean(configData, "tweaks.moreMiningXp.respawn_anchor_anywhere.enabled");
        Config.enableRespawnAnchorAnywhere = legacyRespawnAnchorAnywhere != null ? legacyRespawnAnchorAnywhere : ENABLE_RESPAWN_ANCHOR_ANYWHERE.get();
        Boolean legacyReturnToKiller = getLegacyBoolean(configData, "tweaks.moreMiningXp.returnToKiller.enabled");
        Config.enableReturnToKiller = legacyReturnToKiller != null ? legacyReturnToKiller : ENABLE_RETURN_TO_KILLER.get();
        Boolean legacyTippedArrowLingeringClouds = getLegacyBoolean(configData, "tweaks.moreMiningXp.tippedArrowLingeringClouds.enabled");
        Config.enableTippedArrowLingeringClouds = legacyTippedArrowLingeringClouds != null ? legacyTippedArrowLingeringClouds : ENABLE_TIPPED_ARROW_LINGERING_CLOUDS.get();
        Boolean legacySpeedyHappyGhasts = getLegacyBoolean(configData, "tweaks.moreMiningXp.speedyHappyGhasts.enabled");
        Config.enableSpeedyHappyGhasts = legacySpeedyHappyGhasts != null ? legacySpeedyHappyGhasts : ENABLE_SPEEDY_HAPPY_GHASTS.get();
        Config.allowMixedCandlePlacement = ALLOW_MIXED_CANDLE_PLACEMENT.get();
        Config.enableUnbreakableTrialSpawners = ENABLE_UNBREAKABLE_TRIAL_SPAWNERS.get();
        Config.enableUnbreakableVaults = ENABLE_UNBREAKABLE_VAULTS.get();
        Config.enableEnhancedSlabs = ENABLE_ENHANCED_SLABS.get();
        Config.enhancedSlabsPlaceOnTop = ENHANCED_SLABS_PLACE_ON_TOP.get();
        Config.enhancedSlabsHangBelow = ENHANCED_SLABS_HANG_BELOW.get();
        Config.enhancedSlabsKneeSlabMining = ENHANCED_SLABS_KNEE_SLAB_MINING.get();
        Config.enhancedSlabsMixedDoubleSlabs = ENHANCED_SLABS_MIXED_DOUBLE_SLABS.get();

        Config.enableCaveSpidersInCaves = ENABLE_CAVE_SPIDERS_IN_CAVES.get();
        Config.caveSpiderReplacementChance = CAVE_SPIDER_REPLACEMENT_CHANCE.get();
        Config.disableNitwits = DISABLE_NITWITS.get();
        Config.enableImprovedPhantoms = ENABLE_IMPROVED_PHANTOMS.get();
        Config.enablePhantomPickup = ENABLE_PHANTOM_PICKUP.get();
        Config.phantomPickupChance = PHANTOM_PICKUP_CHANCE.get();
        Config.phantomPickupMinDelay = PHANTOM_PICKUP_MIN_DELAY.get();
        Config.phantomPickupSpeedMultiplier = PHANTOM_PICKUP_SPEED_MULTIPLIER.get();
        Config.enablePhantomSlownessStacking = ENABLE_PHANTOM_SLOWNESS_STACKING.get();
        Config.phantomSlownessDuration = PHANTOM_SLOWNESS_DURATION.get();
        Config.phantomSlownessMaxLevel = PHANTOM_SLOWNESS_MAX_LEVEL.get();
        Config.enablePhantomDoubleDamage = ENABLE_PHANTOM_DOUBLE_DAMAGE.get();
        Config.enablePhantomAutoDrop = ENABLE_PHANTOM_AUTO_DROP.get();
        Config.phantomAutoDropSeconds = PHANTOM_AUTO_DROP_SECONDS.get();
        Config.enablePhantomDropOnHit = ENABLE_PHANTOM_DROP_ON_HIT.get();
                Boolean legacyPhantomSlowFallingOnHitDismount = getLegacyBoolean(configData, "mobs.improvedPhantoms.phantomDismountSlowFalling.enablePhantomSlowFallingOnNonHitDismount");
                Config.enablePhantomSlowFallingOnHitDismount = legacyPhantomSlowFallingOnHitDismount != null ? legacyPhantomSlowFallingOnHitDismount : ENABLE_PHANTOM_SLOW_FALLING_ON_HIT_DISMOUNT.get();
        Config.enableBetterCombatIntegration = ENABLE_BETTER_COMBAT_INTEGRATION.get();
        Config.enableIronGolemsKillCreepers = IRON_GOLEMS_KILL_CREEPERS.get();
    }
}
