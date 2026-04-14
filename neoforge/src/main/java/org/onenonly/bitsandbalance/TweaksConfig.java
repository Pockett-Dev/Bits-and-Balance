package org.onenonly.bitsandbalance;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class TweaksConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

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

    static { BUILDER.comment("Tweaks"); BUILDER.push("tweaks"); }

    // Armed Armor Stands
    static { BUILDER.comment("Armed Armor Stands"); BUILDER.push("armedArmorStands"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ARMED_ARMOR_STANDS = BUILDER
            .comment("Armor Stands placed by the Armor Stand item will have arms; you may also give them items.")
            .define("enableArmedArmorStands", true);
    static { BUILDER.pop(); }

    // Double Door Opening
    static { BUILDER.comment("Double Door Opening"); BUILDER.push("doubleDoorOpening"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DOUBLE_DOOR_OPENING = BUILDER
            .comment("Double doors open together when right clicked.")
            .define("enableDoubleDoorOpening", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_CROUCH_SINGLE = BUILDER
            .comment("Crouch + right-click a double door to only affect the clicked door.")
            .define("crouchSingleDoor", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_CHAIN_TRAPDOORS = BUILDER
            .comment("Open up to 5 touching trapdoors at once when right-clicking.")
            .define("chainTrapdoors", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOORS_WITH_REDSTONE = BUILDER
            .comment("When a door is opened/closed via redstone, mirror the state to its paired door.")
            .define("doubleDoorsWithRedstone", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_REDSTONE_INCLUDE_IRON = BUILDER
            .comment("Also apply redstone mirroring to iron doors.")
            .define("includeIronDoorsWithRedstone", true);
    private static final ModConfigSpec.BooleanValue DOUBLE_DOOR_REDSTONE_INCLUDE_TRAPDOORS = BUILDER
            .comment("Also apply redstone mirroring to touching trapdoors (chain up to 5).")
            .define("includeTrapDoorsWithRedstone", true);
    static { BUILDER.pop(); }

    // Improved Climbing
    static { BUILDER.comment("Improved Climbing"); BUILDER.push("improvedClimbing"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CLIMBABLE_CHAIN_PLACEMENT = BUILDER
            .comment("Right-clicking a climbable block with a climbable item places another climbable at the end of the chain.")
            .define("enableChainPlacement", true);
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_CLIMBING = BUILDER
            .comment("Enable faster climbing and sliding on any climbable while looking up or down.")
            .define("enableImprovedClimbing", true);
    private static final ModConfigSpec.DoubleValue IMPROVED_CLIMBING_DOWN_SPEED = BUILDER
            .comment("Maximum climb-down speed on climbables while looking down (blocks per tick).")
            .defineInRange("climbDownSpeed", 0.4D, 0.05D, 1.0D);
    private static final ModConfigSpec.IntValue IMPROVED_CLIMBING_LOOK_DOWN_MIN_DEG = BUILDER
            .comment("Minimum look-down angle in degrees to start faster downward climbing (0-90).")
            .defineInRange("lookDownMinDeg", 45, 0, 90);
    private static final ModConfigSpec.IntValue IMPROVED_CLIMBING_LOOK_DOWN_MAX_DEG = BUILDER
            .comment("Maximum look-down angle in degrees for full downward climbing speed (0-90).")
            .defineInRange("lookDownMaxDeg", 90, 0, 90);
    private static final ModConfigSpec.BooleanValue ENABLE_FAST_CLIMBABLE_ASCEND = BUILDER
            .comment("Enable faster upward climbing on climbables while looking up.")
            .define("enableFastClimbUp", true);
    private static final ModConfigSpec.DoubleValue FAST_CLIMBABLE_ASCEND_SPEED = BUILDER
            .comment("Maximum climb-up speed on climbables while looking up (blocks per tick).")
            .defineInRange("climbUpSpeed", 0.25D, 0.2D, 1.0D);
    private static final ModConfigSpec.IntValue FAST_CLIMBABLE_LOOK_UP_MIN_DEG = BUILDER
            .comment("Minimum look-up angle in degrees to start faster upward climbing (0-90).")
            .defineInRange("lookUpMinDeg", 45, 0, 90);
    private static final ModConfigSpec.IntValue FAST_CLIMBABLE_LOOK_UP_MAX_DEG = BUILDER
            .comment("Maximum look-up angle in degrees for full upward climbing speed (0-90).")
            .defineInRange("lookUpMaxDeg", 90, 0, 90);
    static { BUILDER.pop(); }


    // Curse Uses
    static { BUILDER.comment("Curse Uses"); BUILDER.push("curseUses"); }
    private static final ModConfigSpec.BooleanValue CURSE_HIDE_PUMPKIN_OVERLAY_ON_VANISHING = BUILDER
            .comment("Pumpkins with Curse of Vanishing will not show the pumpkin overlay when worn by an entity (mainly for Players).")
            .define("hidePumpkinOverlayOnVanishing", true);
    static { BUILDER.pop(); }

    // Despawn With Master
    static { BUILDER.comment("Despawn With Master"); BUILDER.push("despawnWithMaster"); }
    private static final ModConfigSpec.BooleanValue DESPAWN_VEX_WITH_EVOKER = BUILDER
            .comment("When an Evoker is removed from the world, despawn all Vex summoned by that Evoker.")
            .define("despawnVexWithEvoker", true);
    private static final ModConfigSpec.BooleanValue DESPAWN_BULLETS_WITH_SHULKER = BUILDER
            .comment("When a Shulker is removed from the world, despawn all Shulker Bullets fired by that Shulker.")
            .define("despawnBulletsWithShulker", true);
    static { BUILDER.pop(); }

    // Creeper Sunlight Burn
    static { BUILDER.comment("Creeper Sunlight Burn"); BUILDER.push("creeperSunlightBurn"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CREEPER_SUNLIGHT_BURN = BUILDER
            .comment("Creepers burn when in direct sunlight, just like Zombies and Skeletons do.")
            .define("enableCreeperSunlightBurn", true);
    static { BUILDER.pop(); }

    // Sophisticated Scaffolding
    static { BUILDER.comment("Sophisticated Scaffolding"); BUILDER.push("sophisticatedScaffolding"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SOPHISTICATED_SCAFFOLDING = BUILDER
            .comment("When breaking scaffolding, all drops appear at your location.")
            .define("enableSophisticatedScaffolding", true);
    static { BUILDER.pop(); }


    // Brushing XP Rewards
    static { BUILDER.comment("Brushing XP Rewards"); BUILDER.push("brushingXp"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BRUSHING_XP = BUILDER
            .comment("Spawn XP when finishing brushing a brushable block; scales with loot rarity.")
            .define("enabled", true);
    private static final ModConfigSpec.IntValue BRUSHING_XP_MIN = BUILDER
            .comment("Minimum XP to spawn when brushing completes (inclusive).")
            .defineInRange("min", 1, 0, 100);
    private static final ModConfigSpec.IntValue BRUSHING_XP_MAX = BUILDER
            .comment("Maximum XP to spawn when brushing completes (inclusive).")
            .defineInRange("max", 10, 0, 100);
    static { BUILDER.pop(); }

    // Improved Recovery Compass
    static { BUILDER.comment("Improved Recovery Compass"); BUILDER.push("improvedRecoveryCompass"); }
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_RECOVERY_COMPASS = BUILDER
            .comment("Enable improvements to the Recovery Compass.")
            .define("enableImprovedRecoveryCompass", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_KEEP_ON_DEATH = BUILDER
            .comment("Keep Recovery Compass in your inventory when you die (ignores keepInventory gamerule).")
            .define("keepCompassOnDeath", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_SHOW_DISTANCE = BUILDER
            .comment("Show distance to death location in action bar when holding recovery compass.")
            .define("showDistance", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_SHOW_COORDS_ON_RIGHT_CLICK = BUILDER
            .comment("Toggle between showing distance and coordinates when right-clicking recovery compass.")
            .define("showCoordsOnRightClick", true);
    private static final ModConfigSpec.BooleanValue RECOVERY_COMPASS_SCULK_PARTICLES = BUILDER
            .comment("Launch a short-lived soul trail toward the death location when shift + right-clicking the recovery compass.")
            .define("sculkParticles", true);
    // Removed: betterRecoveryRecipe config option
    static { BUILDER.pop(); }

    // Compostable Items
    static { BUILDER.comment("Compostable Items"); BUILDER.push("compostableItems"); }
    private static final ModConfigSpec.BooleanValue ENABLE_COMPOSTABLE_ITEMS = BUILDER
            .comment("Enable compostable items feature (master toggle).")
            .define("enableCompostableItems", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COMPOSTABLE_ROTTEN_FLESH = BUILDER
            .comment("Allow Rotten Flesh to be composted (30% chance).")
            .define("enableCompostableRottenFlesh", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COMPOSTABLE_POISONOUS_POTATO = BUILDER
            .comment("Allow Poisonous Potatoes to be composted (65% chance).")
            .define("enableCompostablePoisonousPotato", true);
    static { BUILDER.pop(); }

    // Responsive Shields
    static { BUILDER.comment("Responsive Shields"); BUILDER.push("responsiveShields"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RESPONSIVE_SHIELDS = BUILDER
            .comment("Enable responsive shields feature (removes shield blocking delay).")
            .define("enableResponsiveShields", true);
    private static final ModConfigSpec.IntValue SHIELD_RAISE_TIME = BUILDER
            .comment("Number of ticks from when you start blocking to when the shield actually blocks damage. 0 = instant blocking, 5 = vanilla delay.")
            .defineInRange("shieldRaiseTime", 0, 0, 100);
    static { BUILDER.pop(); }

    // Automatic Tool Restock
    static { BUILDER.comment("Automatic Tool Restock"); BUILDER.push("automaticToolRestock"); }
    private static final ModConfigSpec.BooleanValue ENABLE_AUTOMATIC_TOOL_RESTOCK = BUILDER
            .comment("When a tool breaks on your hotbar, automatically move a replacement of the same type from your main inventory into that hotbar slot.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Automatic Block Restock
    static { BUILDER.comment("Automatic Block Restock"); BUILDER.push("automaticBlockRestock"); }
    private static final ModConfigSpec.BooleanValue ENABLE_AUTOMATIC_BLOCK_RESTOCK = BUILDER
            .comment("When you run out of blocks in your hotbar while building, automatically move more of the same block from your main inventory into that hotbar slot.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Treasure Enchantment Gold Color
    static { BUILDER.comment("Treasure Enchantment Gold Color"); BUILDER.push("treasureEnchantmentGoldColor"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TREASURE_ENCHANTMENT_GOLD_COLOR = BUILDER
            .comment("Make beneficial treasure enchantments (Mending, Frost Walker, Soul Speed, Swift Sneak) display in gold color. Curses remain red.")
            .define("enableTreasureEnchantmentGoldColor", true);
    static { BUILDER.pop(); }

    // Glowing Glowberries
    static { BUILDER.comment("Glowing Glowberries"); BUILDER.push("glowingGlowberries"); }
    private static final ModConfigSpec.BooleanValue ENABLE_GLOWING_GLOWBERRIES = BUILDER
            .comment("Give the Glowing effect to entities that consume glowberries.")
            .define("enableGlowingGlowberries", true);
    private static final ModConfigSpec.IntValue GLOWING_GLOWBERRIES_DURATION = BUILDER
            .comment("Duration of the Glowing effect in seconds when consuming glowberries.")
            .defineInRange("glowingDuration", 15, 1, 300);
    private static final ModConfigSpec.IntValue FOX_GLOWING_DURATION = BUILDER
            .comment("Duration of the Glowing effect in seconds for foxes when they hold glowberries.")
            .defineInRange("foxGlowingDuration", 5, 1, 300);
    static { BUILDER.pop(); }

    // Enderdragon Egg Always
    static { BUILDER.comment("Enderdragon Egg Always"); BUILDER.push("enderdragonEggAlways"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ENDERDRAGON_EGG_ALWAYS = BUILDER
            .comment("Ensures the Ender Dragon always drops a dragon egg when killed, not just the first time.")
            .define("enableEnderdragonEggAlways", true);
    private static final ModConfigSpec.BooleanValue ENDERDRAGON_EGG_USE_RANDOM_PLACEMENT = BUILDER
            .comment("If true, eggs spawn randomly within the configured radius. If false, eggs spawn at vanilla location (center of podium).")
            .define("useRandomPlacement", false);
    private static final ModConfigSpec.IntValue ENDERDRAGON_EGG_SPAWN_RADIUS = BUILDER
            .comment("Radius in blocks around the End fountain where eggs can spawn (only used if useRandomPlacement is true).")
            .defineInRange("spawnRadius", 10, 1, 50);
    private static final ModConfigSpec.IntValue ENDERDRAGON_EGG_SPAWN_DELAY = BUILDER
            .comment("Delay in seconds after dragon death before the egg spawns.")
            .defineInRange("spawnDelaySeconds", 10, 1, 60);
    private static final ModConfigSpec.BooleanValue ENDERDRAGON_EGG_SHOW_PARTICLES = BUILDER
            .comment("Show purple particle effects when the dragon egg spawns.")
            .define("showParticles", true);
    static { BUILDER.pop(); }

    // Sugarcane Sand
    static { BUILDER.comment("Sugarcane Sand"); BUILDER.push("sugarcaneSand"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SUGARCANE_SAND = BUILDER
            .comment("Enable Sugarcane Sand: sugarcane grows 25% faster when planted on sand instead of grass/dirt.")
            .define("enableSugarcaneSand", true);
    static { BUILDER.pop(); }

    // More Mining XP
    static { BUILDER.comment("More Mining XP"); BUILDER.push("moreMiningXp"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MORE_MINING_XP = BUILDER
            .comment("Enable More Mining XP: increase XP drops from mining ores (affects both vanilla and variant ores).")
            .define("enabled", true);
    
    // Coal XP Configuration
    static { BUILDER.comment("Coal Ore XP"); BUILDER.push("coal"); }
    private static final ModConfigSpec.IntValue COAL_XP_MIN = BUILDER
            .comment("Minimum XP dropped by coal ore (inclusive).")
            .defineInRange("min", 1, 0, 100);
    private static final ModConfigSpec.IntValue COAL_XP_MAX = BUILDER
            .comment("Maximum XP dropped by coal ore (inclusive).")
            .defineInRange("max", 3, 0, 100);
    static { BUILDER.pop(); }
    
    // Diamond XP Configuration
    static { BUILDER.comment("Diamond Ore XP"); BUILDER.push("diamond"); }
    private static final ModConfigSpec.IntValue DIAMOND_XP_MIN = BUILDER
            .comment("Minimum XP dropped by diamond ore (inclusive).")
            .defineInRange("min", 5, 0, 100);
    private static final ModConfigSpec.IntValue DIAMOND_XP_MAX = BUILDER
            .comment("Maximum XP dropped by diamond ore (inclusive).")
            .defineInRange("max", 10, 0, 100);
    static { BUILDER.pop(); }
    
    // Emerald XP Configuration
    static { BUILDER.comment("Emerald Ore XP"); BUILDER.push("emerald"); }
    private static final ModConfigSpec.IntValue EMERALD_XP_MIN = BUILDER
            .comment("Minimum XP dropped by emerald ore (inclusive).")
            .defineInRange("min", 5, 0, 100);
    private static final ModConfigSpec.IntValue EMERALD_XP_MAX = BUILDER
            .comment("Maximum XP dropped by emerald ore (inclusive).")
            .defineInRange("max", 10, 0, 100);
    static { BUILDER.pop(); }
    
    // Lapis XP Configuration
    static { BUILDER.comment("Lapis Lazuli Ore XP"); BUILDER.push("lapis"); }
    private static final ModConfigSpec.IntValue LAPIS_XP_MIN = BUILDER
            .comment("Minimum XP dropped by lapis lazuli ore (inclusive).")
            .defineInRange("min", 4, 0, 100);
    private static final ModConfigSpec.IntValue LAPIS_XP_MAX = BUILDER
            .comment("Maximum XP dropped by lapis lazuli ore (inclusive).")
            .defineInRange("max", 8, 0, 100);
    static { BUILDER.pop(); }
    
    // Redstone XP Configuration
    static { BUILDER.comment("Redstone Ore XP"); BUILDER.push("redstone"); }
    private static final ModConfigSpec.IntValue REDSTONE_XP_MIN = BUILDER
            .comment("Minimum XP dropped by redstone ore (inclusive).")
            .defineInRange("min", 2, 0, 100);
    private static final ModConfigSpec.IntValue REDSTONE_XP_MAX = BUILDER
            .comment("Maximum XP dropped by redstone ore (inclusive).")
            .defineInRange("max", 6, 0, 100);
    static { BUILDER.pop(); }

    static { BUILDER.pop(); } // moreMiningXp
    
    // Respawn Anchor Anywhere Configuration
    static { BUILDER.comment("Respawn Anchor Anywhere"); BUILDER.push("respawn_anchor_anywhere"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RESPAWN_ANCHOR_ANYWHERE = BUILDER
            .comment("Enable Respawn Anchors to work in any dimension (not just the Nether).")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Return To Killer
    static { BUILDER.comment("Return To Killer"); BUILDER.push("returnToKiller"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RETURN_TO_KILLER = BUILDER
            .comment("When killing flying mobs, make their XP and drops home toward the killer for easier pickup.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Tipped Arrows Lingering Clouds
    static { BUILDER.comment("Tipped Arrows Lingering Clouds"); BUILDER.push("tippedArrowLingeringClouds"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TIPPED_ARROW_LINGERING_CLOUDS = BUILDER
            .comment("Tipped arrows create an area effect cloud of their potion effect when they land, then become normal arrows.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Speedy Happy Ghasts
    static { BUILDER.comment("Speedy Happy Ghasts"); BUILDER.push("speedyHappyGhasts"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SPEEDY_HAPPY_GHASTS = BUILDER
            .comment("Happy Ghasts are affected by Swiftness and Slowness (applied to flying speed).")
            .define("enabled", true);
    static { BUILDER.pop(); }

	// Candle Bundles
	static { BUILDER.comment("Candle Bundles"); BUILDER.push("candleBundles"); }
	private static final ModConfigSpec.BooleanValue ALLOW_MIXED_CANDLE_PLACEMENT = BUILDER
			.comment("Allow placing different candle items into the same multi-candle block (mixed candle bundles).")
			.define("allowMixedCandlePlacement", true);
	static { BUILDER.pop(); }

    // Unbreakable Trial Spawners
    static { BUILDER.comment("Unbreakable Trial Spawners"); BUILDER.push("unbreakableTrialSpawners"); }
    private static final ModConfigSpec.BooleanValue ENABLE_UNBREAKABLE_TRIAL_SPAWNERS = BUILDER
            .comment("Make Trial Spawners completely unbreakable. They cannot be destroyed by mining, explosions, or any other means.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Unbreakable Vaults
    static { BUILDER.comment("Unbreakable Vaults"); BUILDER.push("unbreakableVaults"); }
    private static final ModConfigSpec.BooleanValue ENABLE_UNBREAKABLE_VAULTS = BUILDER
            .comment("Make Vaults completely unbreakable. They cannot be destroyed by mining, explosions, or any other means.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    // Enhanced Slab Behavior
    static { BUILDER.comment("Enhanced Slab Behavior",
            "Hang items below slabs, enable KneeSlab-like mining, and mixed double slabs."); BUILDER.push("enhancedSlabs"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ENHANCED_SLABS = BUILDER
            .comment("Master toggle for all Enhanced Slab Behavior features.")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_PLACE_ON_TOP = BUILDER
            .comment("(Currently disabled) Formerly allowed placing items like torches directly on top of bottom/double slabs.")
            .define("allowPlaceOnTop", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_HANG_BELOW = BUILDER
            .comment("Allow hanging items like lanterns, chains, hanging signs below top/double slabs.")
            .define("allowHangBelow", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_KNEE_SLAB_MINING = BUILDER
            .comment("Enable KneeSlab-like mining: break each half of a double slab independently in survival mode. Mining speed depends on the targeted half and your tool.")
            .define("kneeSlabMining", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_MIXED_DOUBLE_SLABS = BUILDER
            .comment("Allow placing different slab types together to create mixed double slabs (e.g. oak bottom + stone top in one block position).")
            .define("mixedDoubleSlabs", true);
    static { BUILDER.pop(); }
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
        // Tweaks
        Config.enableArmedArmorStands = ENABLE_ARMED_ARMOR_STANDS.get();
        Config.enableDoubleDoorOpening = ENABLE_DOUBLE_DOOR_OPENING.get();
        Config.doubleDoorCrouchSingle = DOUBLE_DOOR_CROUCH_SINGLE.get();
        Config.doubleDoorChainTrapdoors = DOUBLE_DOOR_CHAIN_TRAPDOORS.get();
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

        // Brushing XP
        Config.brushingXpEnabled = ENABLE_BRUSHING_XP.get();
        int min = BRUSHING_XP_MIN.get();
        int max = BRUSHING_XP_MAX.get();
        if (min < 0) min = 0;
        if (max < min) max = min;
        if (max > 100) max = 100;
        Config.brushingXpMin = min;
        Config.brushingXpMax = max;

        Config.enableImprovedRecoveryCompass = ENABLE_IMPROVED_RECOVERY_COMPASS.get();
        Config.recoveryCompassKeepOnDeath = RECOVERY_COMPASS_KEEP_ON_DEATH.get();
        Config.recoveryCompassShowDistance = RECOVERY_COMPASS_SHOW_DISTANCE.get();
        Config.recoveryCompassShowCoordsOnRightClick = RECOVERY_COMPASS_SHOW_COORDS_ON_RIGHT_CLICK.get();
        Config.recoveryCompassSculkParticles = RECOVERY_COMPASS_SCULK_PARTICLES.get();
        // Removed: betterRecoveryRecipe config loading
        
        Config.enableCompostableItems = ENABLE_COMPOSTABLE_ITEMS.get();
        Config.enableCompostableRottenFlesh = ENABLE_COMPOSTABLE_ROTTEN_FLESH.get();
        Config.enableCompostablePoisonousPotato = ENABLE_COMPOSTABLE_POISONOUS_POTATO.get();

        // Responsive Shields
        Config.enableResponsiveShields = ENABLE_RESPONSIVE_SHIELDS.get();
        Config.shieldRaiseTime = SHIELD_RAISE_TIME.get();

        // Automatic Tool Restock
        Config.enableAutomaticToolRestock = ENABLE_AUTOMATIC_TOOL_RESTOCK.get();

        // Automatic Block Restock
        Config.enableAutomaticBlockRestock = ENABLE_AUTOMATIC_BLOCK_RESTOCK.get();

        // Treasure Enchantment Gold Color
        Config.enableTreasureEnchantmentGoldColor = ENABLE_TREASURE_ENCHANTMENT_GOLD_COLOR.get();

        // Glowing Glowberries
        Config.enableGlowingGlowberries = ENABLE_GLOWING_GLOWBERRIES.get();
        Config.glowingGlowberriesDuration = GLOWING_GLOWBERRIES_DURATION.get();
        Config.foxGlowingDuration = FOX_GLOWING_DURATION.get();

        // Enderdragon Egg Always
        Config.enableEnderdragonEggAlways = ENABLE_ENDERDRAGON_EGG_ALWAYS.get();
        Config.enderdragonEggUseRandomPlacement = ENDERDRAGON_EGG_USE_RANDOM_PLACEMENT.get();
        Config.enderdragonEggSpawnRadius = ENDERDRAGON_EGG_SPAWN_RADIUS.get();
        Config.enderdragonEggSpawnDelay = ENDERDRAGON_EGG_SPAWN_DELAY.get();
        Config.enderdragonEggShowParticles = ENDERDRAGON_EGG_SHOW_PARTICLES.get();

        // Sugarcane Sand
        Config.enableSugarcaneSand = ENABLE_SUGARCANE_SAND.get();

        // More Mining XP
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

        // Respawn Anchor Anywhere
        Boolean legacyRespawnAnchorAnywhere = getLegacyBoolean(configData, "tweaks.moreMiningXp.respawn_anchor_anywhere.enabled");
        Config.enableRespawnAnchorAnywhere = legacyRespawnAnchorAnywhere != null ? legacyRespawnAnchorAnywhere : ENABLE_RESPAWN_ANCHOR_ANYWHERE.get();

        // Return To Killer
        Boolean legacyReturnToKiller = getLegacyBoolean(configData, "tweaks.moreMiningXp.returnToKiller.enabled");
        Config.enableReturnToKiller = legacyReturnToKiller != null ? legacyReturnToKiller : ENABLE_RETURN_TO_KILLER.get();

        // Tipped Arrows Lingering Clouds
        Boolean legacyTippedArrowLingeringClouds = getLegacyBoolean(configData, "tweaks.moreMiningXp.tippedArrowLingeringClouds.enabled");
        Config.enableTippedArrowLingeringClouds = legacyTippedArrowLingeringClouds != null ? legacyTippedArrowLingeringClouds : ENABLE_TIPPED_ARROW_LINGERING_CLOUDS.get();

        // Speedy Happy Ghasts
        Boolean legacySpeedyHappyGhasts = getLegacyBoolean(configData, "tweaks.moreMiningXp.speedyHappyGhasts.enabled");
        Config.enableSpeedyHappyGhasts = legacySpeedyHappyGhasts != null ? legacySpeedyHappyGhasts : ENABLE_SPEEDY_HAPPY_GHASTS.get();

		// Candle Bundles
		Config.allowMixedCandlePlacement = ALLOW_MIXED_CANDLE_PLACEMENT.get();

        // Unbreakable Trial Spawners
        Config.enableUnbreakableTrialSpawners = ENABLE_UNBREAKABLE_TRIAL_SPAWNERS.get();

                // Unbreakable Vaults
                Config.enableUnbreakableVaults = ENABLE_UNBREAKABLE_VAULTS.get();

        // Enhanced Slab Behavior
        Config.enableEnhancedSlabs = ENABLE_ENHANCED_SLABS.get();
        Config.enhancedSlabsPlaceOnTop = ENHANCED_SLABS_PLACE_ON_TOP.get();
        Config.enhancedSlabsHangBelow = ENHANCED_SLABS_HANG_BELOW.get();
        Config.enhancedSlabsKneeSlabMining = ENHANCED_SLABS_KNEE_SLAB_MINING.get();
        Config.enhancedSlabsMixedDoubleSlabs = ENHANCED_SLABS_MIXED_DOUBLE_SLABS.get();
    }
}
