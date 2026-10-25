package org.onenonly.bitsandbalance;

import org.onenonly.bitsandbalance.common.loot.BrushingLootSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;
import org.onenonly.bitsandbalance.common.mechanics.OreSmeltingXpRuntime;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class ContentConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Glow Goo").push("glowGoo");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_GLOW_GOO = BUILDER
            .comment("Enable Glow Goo: throw a blob that splats into a bright, waterloggable light source.")
            .define("enabled", true);
    private static final ModConfigSpec.BooleanValue GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED = BUILDER
            .comment("If true, Glow Goo can be crafted from 2 glow ink sacs plus 1 slime ball.")
            .define("twoGlowInkRecipeEnabled", true);
    private static final ModConfigSpec.BooleanValue GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED = BUILDER
            .comment("If true, Glow Goo can also be crafted from 4 glow ink sacs.")
            .define("fourGlowInkRecipeEnabled", true);
    private static final ModConfigSpec.BooleanValue ENABLE_GLOW_GOO_IMPACT_PARTICLES = BUILDER
            .comment("If true, Glow Goo plays glow-ink particles when it hits a block or entity.")
            .define("impactParticlesEnabled", true);
    private static final ModConfigSpec.IntValue GLOW_GOO_IMPACT_PARTICLE_COUNT = BUILDER
            .comment("How many glow-ink particles Glow Goo spawns on impact.")
            .defineInRange("impactParticleCount", 16, 0, 64);
    private static final ModConfigSpec.BooleanValue ENABLE_GLOW_GOO_SPLATTER_AMBIENT_PARTICLES = BUILDER
            .comment("If true, placed Glow Goo Splatter occasionally emits glow-squid ink particles.")
            .define("ambientSplatterParticlesEnabled", true);
    private static final ModConfigSpec.BooleanValue ENABLE_GLOW_GOO_SPLATTER_DECAY = BUILDER
            .comment("If true, newly placed Glow Goo Splatter disappears after the configured lifetime. Disabled by default.")
            .define("splatterDecayEnabled", false);
    private static final ModConfigSpec.IntValue GLOW_GOO_SPLATTER_DECAY_TIME_SECONDS = BUILDER
            .comment("How long newly placed Glow Goo Splatter lasts when decay is enabled, in seconds.")
            .defineInRange("splatterDecayTimeSeconds", 300, 1, 604_800);
    private static final ModConfigSpec.BooleanValue ENABLE_GLOW_GOO_BIOLUMINESCENCE = BUILDER
            .comment("If true, entities hit by Glow Goo gain Bioluminescence and emit light while the effect lasts.")
            .define("bioluminescenceEnabled", true);
    private static final ModConfigSpec.IntValue GLOW_GOO_BIOLUMINESCENCE_DURATION_SECONDS = BUILDER
            .comment("How long Bioluminescence lasts after a Glow Goo hit, in seconds.")
            .defineInRange("bioluminescenceDurationSeconds", 60, 1, 600);
    private static final ModConfigSpec.IntValue GLOW_GOO_BIOLUMINESCENCE_LIGHT_LEVEL = BUILDER
            .comment("Light level emitted by Bioluminescence while active.")
            .defineInRange("bioluminescenceLightLevel", 14, 0, 15);
    static {
        BUILDER.pop();
    }

    static {
        BUILDER.comment("Potions");
        BUILDER.push("potions");
    }
    static {
        BUILDER.comment("Withering Potion");
        BUILDER.push("withering");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_WITHERING_POTION = BUILDER
            .comment("Enable brewing for Withering potions.",
                    "Recipe: Poison/Harming Potion + Wither Rose = Withering Potion",
                    "Upgrades: Redstone for extended, Glowstone for stronger variants")
            .define("enableWitheringPotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacing");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_RESURFACING_POTION = BUILDER
            .comment("Enable the Resurfacing potion effect (teleports player to surface).",
                    "Recipe: Currently no brewing recipe - obtained via creative mode or commands.")
            .define("enableResurfacingPotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Displacement Potion");
        BUILDER.push("displacement");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_DISPLACEMENT_POTION = BUILDER
            .comment("Enable the Displacement potion effect (randomly teleports player within 100-block radius).",
                    "Recipe: Awkward Potion + Ender Eye = Displacement Potion")
            .define("enableDisplacementPotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returning");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_RETURNING_POTION = BUILDER
            .comment("Enable the Returning potion effect (teleports player to their latest death location).",
                    "Recipe: Displacement Potion + Echo Shard = Returning Potion")
            .define("enableReturningPotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Levitation Potion");
        BUILDER.push("levitation");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_LEVITATION_POTION = BUILDER
            .comment("Enable brewing for Levitation potions.",
                    "Recipe: Slow Falling Potion + Shulker Shell = Levitation Potion",
                    "Upgrades: Redstone for extended, Glowstone for stronger variants")
            .define("enableLevitationPotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Haste Potion");
        BUILDER.push("haste");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_HASTE_POTION = BUILDER
            .comment("Enable brewing for Haste potions.",
                    "Recipe: Speed Potion + Quartz = Haste Potion",
                    "Upgrades: Redstone for extended, Glowstone for stronger variants")
            .define("enableHastePotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Blindness Potion");
        BUILDER.push("blindness");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_BLINDNESS_POTION = BUILDER
            .comment("Enable Blindness potions and limit blinded mobs to 3-block sight and aggro range.",
                    "Recipe: Awkward Potion + Ink Sac = Blindness Potion",
                    "Upgrades: Redstone for an extended variant")
            .define("enableBlindnessPotion", true);
    static { BUILDER.pop(); }

    static {
        BUILDER.comment("Glowing Potion");
        BUILDER.push("glowing");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_GLOWING_POTION = BUILDER
            .comment("Enable brewing for Glowing potions.",
                    "Recipe: Awkward Potion + Glow Berries = Glowing Potion",
                    "Upgrades: Redstone for an extended variant")
            .define("enableGlowingPotion", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Bioluminescence Potion");
        BUILDER.push("bioluminescence");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_BIOLUMINESCENCE_POTION = BUILDER
            .comment("Enable brewing for Bioluminescence potions.",
                    "Recipe: Glowing Potion + Glow Goo = Bioluminescence Potion",
                    "Upgrades: Glowstone for Bioluminescence II; Redstone extends Bioluminescence II to 4 minutes; Long Glowing Potion + Glow Goo brews the 8-minute base variant")
            .define("enableBioluminescencePotion", true);
    static { BUILDER.pop(); BUILDER.pop(); }

    static { BUILDER.comment("Enhanced Slab Behavior", "Hang items below slabs, enable KneeSlab-like mining, and mixed double slabs.").push("enhancedSlabs"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ENHANCED_SLABS = BUILDER.comment("Master toggle for all Enhanced Slab Behavior features.").define("enabled", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_PLACE_ON_TOP = BUILDER.comment("(Currently disabled) Formerly allowed placing items like torches directly on top of bottom/double slabs.").define("placeOnTop", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_HANG_BELOW = BUILDER.comment("Allow hanging items like lanterns, chains, hanging signs below top/double slabs.").define("hangBelow", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_KNEE_SLAB_MINING = BUILDER.comment("Enable KneeSlab-like mining: break each half of a double slab independently in survival mode. Mining speed depends on the targeted half and your tool.").define("kneeSlabMining", true);
    private static final ModConfigSpec.BooleanValue ENHANCED_SLABS_MIXED_DOUBLE_SLABS = BUILDER.comment("Allow placing different slab types together to create mixed double slabs (e.g. oak bottom + stone top in one block position).").define("mixedDoubleSlabs", true);
    static { BUILDER.pop(); }

    static {
        BUILDER.comment("Vertical Slabs", "Dynamic vertical slab variants crafted from slabs.").push("verticalSlabs");
    }
    private static final ModConfigSpec.BooleanValue VERTICAL_SLABS_ENABLED = BUILDER
            .comment("Enable Vertical Slabs. Disabling this stops dynamic vertical slab generation and generated recipes, and turns off related placement and presentation.")
            .define("enabled", true);
    static { BUILDER.pop(); }
    static {
        BUILDER.comment("Steps and Vertical Steps", "Quarter-block step variants crafted from slabs and vertical slabs.").push("stepsAndVerticalSteps");
    }
    private static final ModConfigSpec.BooleanValue STEPS_AND_VERTICAL_STEPS_ENABLED = BUILDER
            .comment("Enable Steps and Vertical Steps. Disabling this stops dynamic step/vertical-step generation and generated recipes, and turns off related placement and presentation.")
            .define("enabled", true);
    static { BUILDER.pop(); }

    private static final ModConfigSpec.BooleanValue ENABLE_CASCADING = BUILDER
            .comment("Enable the experimental Cascading ore mining enchantment. Crouching always bypasses it.")
            .define("cascading.enabled", true);
    private static final ModConfigSpec.IntValue CASCADING_MAX_ACTIVE_LEVEL = BUILDER
            .comment("Highest Cascading level that can trigger, from 1 to 3.")
            .defineInRange("cascading.maxActiveLevel", 1, 1, 3);
    private static final ModConfigSpec.IntValue CASCADING_LEVEL_ONE_MAX_ORES = BUILDER
            .comment("Maximum total ores at Cascading I, including the first ore.")
            .defineInRange("cascading.levelOneMaxOres", 8, 1, 64);
    private static final ModConfigSpec.IntValue CASCADING_LEVEL_TWO_MAX_ORES = BUILDER
            .comment("Maximum total ores at Cascading II, including the first ore.")
            .defineInRange("cascading.levelTwoMaxOres", 16, 1, 64);
    private static final ModConfigSpec.IntValue CASCADING_LEVEL_THREE_MAX_ORES = BUILDER
            .comment("Maximum total ores at Cascading III, including the first ore.")
            .defineInRange("cascading.levelThreeMaxOres", 24, 1, 64);

    private static final ModConfigSpec.BooleanValue ENABLE_FORTUNE_BRUSH = BUILDER
            .comment("Enable Fortune brushing drop and XP bonuses: +50% XP per level, common drops up to triple, rare finds capped at two. Fortune remains applicable through an anvil when disabled; rewards are datapack controlled.")
            .define("fortuneBrush.enabled", true);
    private static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> FORTUNE_BRUSH_BLACKLIST = BUILDER
            .comment("Item IDs excluded from Fortune brushing quantity bonuses, e.g. minecraft:diamond. Keeps the normal reward and XP bonus. Applies when loot is first revealed; already revealed rewards keep their saved count.")
            .defineList("fortuneBrush.blacklist", java.util.List.of(), value -> value instanceof String);
    private static final ModConfigSpec.BooleanValue ENABLE_AERODYNAMIC = BUILDER.comment("Enable the Aerodynamic enchantment effect.").define("aerodynamic.enabled", true);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_BASE_DRAG_REDUCTION = BUILDER.comment("Base horizontal drag reduction at full strength. 0.5 = 50% less horizontal drag (recommended).").defineInRange("aerodynamic.baseDragReduction", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue AERODYNAMIC_MIN_ALTITUDE = BUILDER.comment("Y where Aerodynamic reaches full strength; below this it still applies at reduced strength.").defineInRange("aerodynamic.minAltitude", 192, -2032, 4096);
    private static final ModConfigSpec.IntValue AERODYNAMIC_MAX_ALTITUDE = BUILDER.comment("Legacy ramp reference. The distance from minAltitude controls how gradually Aerodynamic fades below full strength.").defineInRange("aerodynamic.maxAltitude", 320, -2032, 4096);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLOUD_LEVEL = BUILDER.comment("Legacy setting kept for config compatibility. Full strength is controlled by minAltitude.").defineInRange("aerodynamic.cloudLevel", 192.0D, -2032.0D, 4096.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SPEED_MULTIPLIER = BUILDER.comment("Global multiplier on the effect strength.").defineInRange("aerodynamic.speedMultiplier", 1.0D, 0.0D, 10.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_MAX_FLIGHT_SPEED = BUILDER.comment("Maximum Aerodynamic-modified elytra flight speed (m/s) at full altitude strength. Scales down below full-strength altitude.").defineInRange("aerodynamic.maxFlightSpeed", 50.0D, 0.0D, 200.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_LOW_ALTITUDE_STRENGTH_FLOOR = BUILDER.comment("Minimum Aerodynamic strength below the altitude ramp. 0.5 = at least half strength.").defineInRange("aerodynamic.lowAltitudeStrengthFloor", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_GLIDE_PITCH_RANGE = BUILDER.comment("Pitch angle in degrees where level-glide efficiency fades out. Larger values make the glide bonus apply over steeper pitch.").defineInRange("aerodynamic.glidePitchRange", 60.0D, 1.0D, 180.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_UPWARD_PITCH_START = BUILDER.comment("Upward pitch angle in degrees where climb efficiency starts.").defineInRange("aerodynamic.upwardPitchStart", 5.0D, 0.0D, 90.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_UPWARD_PITCH_RANGE = BUILDER.comment("Additional upward pitch range in degrees needed to reach full climb pitch efficiency.").defineInRange("aerodynamic.upwardPitchRange", 40.0D, 1.0D, 180.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_UPWARD_MOTION_FULL_SPEED = BUILDER.comment("Upward velocity in blocks per tick that counts as full climb motion efficiency.").defineInRange("aerodynamic.upwardMotionFullSpeed", 0.25D, 0.01D, 10.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLIMB_ASSIST_SCALE = BUILDER.comment("How much climb efficiency keeps drag reduction active while pitched upward.").defineInRange("aerodynamic.climbAssistScale", 0.2D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SINK_REDUCTION_SCALE = BUILDER.comment("How strongly Aerodynamic reduces downward sink from its current drag-reduction strength.").defineInRange("aerodynamic.sinkReductionScale", 0.6D, 0.0D, 10.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_MAX_SINK_REDUCTION = BUILDER.comment("Maximum fraction of downward sink Aerodynamic can remove. 0.75 = up to 75% less sink.").defineInRange("aerodynamic.maxSinkReduction", 0.75D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SINK_REDUCTION_MIN_HORIZONTAL_SPEED = BUILDER.comment("Horizontal speed in blocks per tick where sink reduction starts ramping in.").defineInRange("aerodynamic.sinkReductionMinHorizontalSpeed", 0.5D, 0.0D, 20.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SINK_REDUCTION_FULL_HORIZONTAL_SPEED = BUILDER.comment("Horizontal speed in blocks per tick where sink reduction reaches full configured strength.").defineInRange("aerodynamic.sinkReductionFullHorizontalSpeed", 1.5D, 0.01D, 20.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLIMB_RESTORE_CAP = BUILDER.comment("Maximum fraction of upward speed loss that Aerodynamic can restore per tick.").defineInRange("aerodynamic.climbRestoreCap", 0.15D, 0.0D, 1.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLIMB_RESTORE_SCALE = BUILDER.comment("How strongly climb efficiency restores upward speed loss.").defineInRange("aerodynamic.climbRestoreScale", 0.3D, 0.0D, 10.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_TURN_RESPONSE = BUILDER.comment("Direction-change amount needed to reach full turn speed preservation bonus. Lower values make turns count sooner.").defineInRange("aerodynamic.turnResponse", 0.5D, 0.01D, 2.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_TURN_RESTORE_BONUS = BUILDER.comment("Extra multiplier applied to lost horizontal speed restoration while turning.").defineInRange("aerodynamic.turnRestoreBonus", 0.25D, 0.0D, 10.0D);
    private static final ModConfigSpec.BooleanValue ENABLE_ENCHANCEMENT_ELYTRA_DURABILITY_COMPAT = BUILDER.comment("When Enchancement is installed, keep vanilla Elytra durability loss even if Enchancement's Disable Durability setting is enabled.").define("aerodynamic.enchancementElytraDurabilityCompat", true);

    static {
        BUILDER.comment("Recipes");
        BUILDER.push("recipes");
    }
    static {
        BUILDER.comment("Alternative Recipes");
        BUILDER.push("alternativeRecipes");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_ALTERNATIVE_REPEATER_RECIPE = BUILDER.comment("Enable alternative crafting recipe for Redstone Repeater using sticks and stone.").define("enableAlternativeRepeaterRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_CHEST_FROM_LOGS_RECIPE = BUILDER.comment("Enable alternative crafting recipe for Chest using logs (4 chests per recipe).").define("enableChestFromLogsRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_MAP_INK_SAC_RECIPE = BUILDER.comment("Enable alternative crafting recipe for Map using ink sac instead of compass.").define("enableMapInkSacRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RECOVERY_COMPASS_RECIPE = BUILDER.comment("Enable alternative crafting recipe for Recovery Compass using echo shards and compass.").define("enableRecoveryCompassRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_ENDER_EYE_RECIPE = BUILDER.comment("Enable alternative crafting recipe for Ender Eye using echo shard, wind charge, blaze powder, and ender pearl.").define("enableEnderEyeRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_IRON_SMELTING_RECIPE = BUILDER.comment("Enable smelting recipe for raw iron block to iron block.").define("enableRawIronSmeltingRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_GOLD_SMELTING_RECIPE = BUILDER.comment("Enable smelting recipe for raw gold block to gold block.").define("enableRawGoldSmeltingRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_COPPER_SMELTING_RECIPE = BUILDER.comment("Enable smelting recipe for raw copper block to copper block.").define("enableRawCopperSmeltingRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_IRON_BLASTING_RECIPE = BUILDER.comment("Enable blasting recipe for raw iron block to iron block (faster than smelting).").define("enableRawIronBlastingRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_GOLD_BLASTING_RECIPE = BUILDER.comment("Enable blasting recipe for raw gold block to gold block (faster than smelting).").define("enableRawGoldBlastingRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_COPPER_BLASTING_RECIPE = BUILDER.comment("Enable blasting recipe for raw copper block to copper block (faster than smelting).").define("enableRawCopperBlastingRecipe", true);
    private static final ModConfigSpec.BooleanValue ENABLE_STAIR_RECIPE_OVERRIDE = BUILDER.comment("Override ALL stair recipes (vanilla, modded, and custom) to output 8 stairs instead of 4.").define("enableStairRecipeOverride", true);
    private static final ModConfigSpec.BooleanValue ENABLE_COMPACT_STAIR_RECIPE = BUILDER.comment("Add compact 2x2 stair recipes for ALL stair types (3 blocks in corner = 4 stairs). Works with any mod!").define("enableCompactStairRecipe", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Ore Smelting XP"); BUILDER.push("oreSmeltingXp"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ORE_SMELTING_XP = BUILDER.comment("Enable configurable experience for smelting or blasting vanilla ore blocks.").define("enabled", true);
    private static final ModConfigSpec.DoubleValue COAL_ORE_SMELTING_XP = BUILDER.comment("XP for Coal Ore and Deepslate Coal Ore.").defineInRange("coal", 0.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue LAPIS_ORE_SMELTING_XP = BUILDER.comment("XP for Lapis Ore and Deepslate Lapis Ore.").defineInRange("lapis", 1.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue NETHER_QUARTZ_ORE_SMELTING_XP = BUILDER.comment("XP for Nether Quartz Ore.").defineInRange("netherQuartz", 1.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue IRON_ORE_SMELTING_XP = BUILDER.comment("XP for Iron Ore and Deepslate Iron Ore.").defineInRange("iron", 3.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue COPPER_ORE_SMELTING_XP = BUILDER.comment("XP for Copper Ore and Deepslate Copper Ore.").defineInRange("copper", 3.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue REDSTONE_ORE_SMELTING_XP = BUILDER.comment("XP for Redstone Ore and Deepslate Redstone Ore.").defineInRange("redstone", 3.5D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue GOLD_ORE_SMELTING_XP = BUILDER.comment("XP for Gold Ore and Deepslate Gold Ore.").defineInRange("gold", 5.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue NETHER_GOLD_ORE_SMELTING_XP = BUILDER.comment("XP for Nether Gold Ore.").defineInRange("netherGold", 5.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue DIAMOND_ORE_SMELTING_XP = BUILDER.comment("XP for Diamond Ore and Deepslate Diamond Ore.").defineInRange("diamond", 5.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue EMERALD_ORE_SMELTING_XP = BUILDER.comment("XP for Emerald Ore and Deepslate Emerald Ore.").defineInRange("emerald", 5.0D, 0.0D, 1000.0D);
    private static final ModConfigSpec.DoubleValue ANCIENT_DEBRIS_SMELTING_XP = BUILDER.comment("XP for Ancient Debris.").defineInRange("ancientDebris", 10.0D, 0.0D, 1000.0D);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Fuel Tweaks"); BUILDER.push("fuelTweaks"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TORCH_FUEL = BUILDER.comment("Enable torch as fuel for furnaces.").define("enableTorchFuel", true);
    private static final ModConfigSpec.IntValue TORCH_BURN_TIME = BUILDER.comment("Burn time for torches in ticks (400 ticks = 20 seconds).").defineInRange("torchBurnTime", 400, 0, 10000);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Loot Tables Configuration"); BUILDER.push("lootTables"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_LOOT_TABLES = BUILDER.comment("Enable custom loot table modifications (master toggle).").define("enableCustomLootTables", true);
    static { BUILDER.comment("Brushing Loot"); BUILDER.push("brushingLoot"); }
    private static final ModConfigSpec.BooleanValue BRUSHING_LOOT_ENABLED = BUILDER
            .comment("Improve ordinary archaeology finds while preserving artifact and Sniffer Egg odds. Also requires enableCustomLootTables.")
            .define("enabled", true);
    static { BUILDER.comment("Desert Wells"); BUILDER.push("desertWell"); }
    private static final ModConfigSpec.BooleanValue BRUSHING_DESERT_WELL_ENABLED = BUILDER
            .comment("Enable ordinary-find replacements at Desert Wells.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Desert Pyramids"); BUILDER.push("desertPyramid"); }
    private static final ModConfigSpec.BooleanValue BRUSHING_DESERT_PYRAMID_ENABLED = BUILDER
            .comment("Enable ordinary-find replacements at Desert Pyramids.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Trail Ruins"); BUILDER.push("trailRuins"); }
    private static final ModConfigSpec.BooleanValue BRUSHING_TRAIL_RUINS_ENABLED = BUILDER
            .comment("Enable ordinary-find replacements at Trail Ruins.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Cold Ocean Ruins"); BUILDER.push("coldOceanRuins"); }
    private static final ModConfigSpec.BooleanValue BRUSHING_COLD_OCEAN_RUINS_ENABLED = BUILDER
            .comment("Enable ordinary-find replacements at Cold Ocean Ruins.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Warm Ocean Ruins"); BUILDER.push("warmOceanRuins"); }
    private static final ModConfigSpec.BooleanValue BRUSHING_WARM_OCEAN_RUINS_ENABLED = BUILDER
            .comment("Enable ordinary-find replacements at Warm Ocean Ruins.").define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Dog Music Disc"); BUILDER.push("dogMusicDisc"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DOG_MUSIC_DISC = BUILDER
            .comment("Enable the Dog music disc feature.", "When disabled:", "  - The disc is hidden from creative tabs", "  - The disc will not be added to custom chest loot", "  - Skeleton-killed creepers will never drop the Dog disc", "Note: The item still exists and can be obtained via commands. Requires restart.")
            .define("enabled", true);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Mineshaft Loot"); BUILDER.push("mineshaftLoot"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_LOOT = BUILDER.comment("Enable custom loot additions to abandoned mineshaft chests.").define("enableMineshaftLoot", true);
    static { BUILDER.comment("Resurfacing Potion"); BUILDER.push("resurfacingPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_RESURFACING_POTION = BUILDER.comment("Add Resurfacing Potion to mineshaft loot tables.").define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_RESURFACING_WEIGHT = BUILDER.comment("Weight for Resurfacing Potion in mineshaft loot (higher = more common).").defineInRange("resurfacingPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_RESURFACING_MIN_COUNT = BUILDER.comment("Minimum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_RESURFACING_MAX_COUNT = BUILDER.comment("Maximum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_RETURNING_POTION = BUILDER.comment("Add Returning Potion to mineshaft loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in mineshaft loot (higher = more common).").defineInRange("returningPotionWeight", 1, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Haste Potion"); BUILDER.push("hastePotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_HASTE_POTION = BUILDER.comment("Add Haste Potion to mineshaft loot tables.").define("enableHastePotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_HASTE_WEIGHT = BUILDER.comment("Weight for Haste Potion in mineshaft loot (higher = more common).").defineInRange("hastePotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_HASTE_MIN_COUNT = BUILDER.comment("Minimum count of Haste Potions when found.").defineInRange("hastePotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_HASTE_MAX_COUNT = BUILDER.comment("Maximum count of Haste Potions when found.").defineInRange("hastePotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Strong Haste Potion"); BUILDER.push("strongHastePotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_STRONG_HASTE_POTION = BUILDER.comment("Add Strong Haste Potion to mineshaft loot tables.").define("enableStrongHastePotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_STRONG_HASTE_WEIGHT = BUILDER.comment("Weight for Strong Haste Potion in mineshaft loot (higher = more common).").defineInRange("strongHastePotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_STRONG_HASTE_MIN_COUNT = BUILDER.comment("Minimum count of Strong Haste Potions when found.").defineInRange("strongHastePotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_STRONG_HASTE_MAX_COUNT = BUILDER.comment("Maximum count of Strong Haste Potions when found.").defineInRange("strongHastePotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Stronghold Loot"); BUILDER.push("strongholdLoot"); }
    private static final ModConfigSpec.BooleanValue ENABLE_STRONGHOLD_LOOT = BUILDER.comment("Enable custom loot additions to stronghold chests.").define("enableStrongholdLoot", true);
    static { BUILDER.comment("Resurfacing Potion"); BUILDER.push("resurfacingPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_STRONGHOLD_RESURFACING_POTION = BUILDER.comment("Add Resurfacing Potion to stronghold loot tables.").define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue STRONGHOLD_RESURFACING_WEIGHT = BUILDER.comment("Weight for Resurfacing Potion in stronghold loot (higher = more common).").defineInRange("resurfacingPotionWeight", 4, 1, 100);
    private static final ModConfigSpec.IntValue STRONGHOLD_RESURFACING_MIN_COUNT = BUILDER.comment("Minimum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue STRONGHOLD_RESURFACING_MAX_COUNT = BUILDER.comment("Maximum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_STRONGHOLD_RETURNING_POTION = BUILDER.comment("Add Returning Potion to stronghold loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue STRONGHOLD_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in stronghold loot (higher = more common).").defineInRange("returningPotionWeight", 3, 1, 100);
    private static final ModConfigSpec.IntValue STRONGHOLD_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue STRONGHOLD_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Ancient City Loot"); BUILDER.push("ancientCityLoot"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ANCIENT_CITY_LOOT = BUILDER.comment("Enable custom loot additions to ancient city chests.").define("enableAncientCityLoot", true);
    static { BUILDER.comment("Resurfacing Potion"); BUILDER.push("resurfacingPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ANCIENT_CITY_RESURFACING_POTION = BUILDER.comment("Add Resurfacing Potion to ancient city loot tables.").define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RESURFACING_WEIGHT = BUILDER.comment("Weight for Resurfacing Potion in ancient city loot (higher = more common).").defineInRange("resurfacingPotionWeight", 1, 1, 100);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RESURFACING_MIN_COUNT = BUILDER.comment("Minimum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RESURFACING_MAX_COUNT = BUILDER.comment("Maximum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_ANCIENT_CITY_RETURNING_POTION = BUILDER.comment("Add Returning Potion to ancient city loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in ancient city loot (higher = more common).").defineInRange("returningPotionWeight", 3, 1, 100);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("End City Loot"); BUILDER.push("endCityLoot"); }
    private static final ModConfigSpec.BooleanValue ENABLE_END_CITY_LOOT = BUILDER.comment("Enable custom loot additions to end city chests.").define("enableEndCityLoot", true);
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_END_CITY_RETURNING_POTION = BUILDER.comment("Add Returning Potion to end city loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue END_CITY_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in end city loot (higher = more common).").defineInRange("returningPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue END_CITY_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue END_CITY_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Simple Dungeon Loot"); BUILDER.push("simpleDungeonLoot"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_DUNGEON_LOOT = BUILDER.comment("Enable custom loot additions to simple dungeon chests.").define("enableSimpleDungeonLoot", true);
    static { BUILDER.comment("Resurfacing Potion"); BUILDER.push("resurfacingPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_DUNGEON_RESURFACING_POTION = BUILDER.comment("Add Resurfacing Potion to simple dungeon loot tables.").define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RESURFACING_WEIGHT = BUILDER.comment("Weight for Resurfacing Potion in simple dungeon loot (higher = more common).").defineInRange("resurfacingPotionWeight", 3, 1, 100);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RESURFACING_MIN_COUNT = BUILDER.comment("Minimum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RESURFACING_MAX_COUNT = BUILDER.comment("Maximum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_DUNGEON_RETURNING_POTION = BUILDER.comment("Add Returning Potion to simple dungeon loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in simple dungeon loot (higher = more common).").defineInRange("returningPotionWeight", 6, 1, 100);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Trial Chamber Loot"); BUILDER.push("trialChamberLoot"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_CHAMBER_LOOT = BUILDER.comment("Enable custom loot additions to trial chamber chests.").define("enableTrialChamberLoot", true);
    static { BUILDER.comment("Common Rewards"); BUILDER.push("common"); }
    static { BUILDER.comment("Resurfacing Potion"); BUILDER.push("resurfacingPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_COMMON_RESURFACING_POTION = BUILDER.comment("Add Resurfacing Potion to trial chamber common loot tables.").define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_COMMON_RESURFACING_WEIGHT = BUILDER.comment("Weight for Resurfacing Potion in trial chamber common loot (higher = more common).").defineInRange("resurfacingPotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_COMMON_RESURFACING_MIN_COUNT = BUILDER.comment("Minimum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_COMMON_RESURFACING_MAX_COUNT = BUILDER.comment("Maximum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Rare Rewards"); BUILDER.push("rare"); }
    static { BUILDER.comment("Resurfacing Potion"); BUILDER.push("resurfacingPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_RARE_RESURFACING_POTION = BUILDER.comment("Add Resurfacing Potion to trial chamber rare loot tables.").define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RESURFACING_WEIGHT = BUILDER.comment("Weight for Resurfacing Potion in trial chamber rare loot (higher = more common).").defineInRange("resurfacingPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RESURFACING_MIN_COUNT = BUILDER.comment("Minimum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RESURFACING_MAX_COUNT = BUILDER.comment("Maximum count of Resurfacing Potions when found.").defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_RARE_RETURNING_POTION = BUILDER.comment("Add Returning Potion to trial chamber rare loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in trial chamber rare loot (higher = more common).").defineInRange("returningPotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.comment("Unique Rewards"); BUILDER.push("unique"); }
    static { BUILDER.comment("Returning Potion"); BUILDER.push("returningPotion"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_UNIQUE_RETURNING_POTION = BUILDER.comment("Add Returning Potion to trial chamber unique loot tables.").define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_UNIQUE_RETURNING_WEIGHT = BUILDER.comment("Weight for Returning Potion in trial chamber unique loot (higher = more common).").defineInRange("returningPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_UNIQUE_RETURNING_MIN_COUNT = BUILDER.comment("Minimum count of Returning Potions when found.").defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_UNIQUE_RETURNING_MAX_COUNT = BUILDER.comment("Maximum count of Returning Potions when found.").defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ContentConfig() {
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event != null && event.getConfig().getSpec() != SPEC) return;

        refreshRuntime();
    }

    public static void refreshRuntime() {
        Config.enableGlowGoo = ENABLE_GLOW_GOO.get();
        Config.glowGooTwoGlowInkRecipeEnabled = GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED.get();
        Config.glowGooFourGlowInkRecipeEnabled = GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED.get();
        Config.glowGooImpactParticlesEnabled = ENABLE_GLOW_GOO_IMPACT_PARTICLES.get();
        Config.glowGooImpactParticleCount = GLOW_GOO_IMPACT_PARTICLE_COUNT.get();
        Config.glowGooAmbientSplatterParticlesEnabled = ENABLE_GLOW_GOO_SPLATTER_AMBIENT_PARTICLES.get();
        Config.glowGooSplatterDecayEnabled = ENABLE_GLOW_GOO_SPLATTER_DECAY.get();
        Config.glowGooSplatterDecayTimeSeconds = GLOW_GOO_SPLATTER_DECAY_TIME_SECONDS.get();
        Config.glowGooBioluminescenceEnabled = ENABLE_GLOW_GOO_BIOLUMINESCENCE.get();
        Config.glowGooBioluminescenceDurationSeconds = GLOW_GOO_BIOLUMINESCENCE_DURATION_SECONDS.get();
        Config.glowGooBioluminescenceLightLevel = GLOW_GOO_BIOLUMINESCENCE_LIGHT_LEVEL.get();
        GlowGooRuntime.enabled = Config.enableGlowGoo;
        GlowGooRuntime.impactParticlesEnabled = Config.glowGooImpactParticlesEnabled;
        GlowGooRuntime.impactParticleCount = Config.glowGooImpactParticleCount;
        GlowGooRuntime.splatterAmbientParticlesEnabled = Config.glowGooAmbientSplatterParticlesEnabled;
        GlowGooRuntime.splatterDecayEnabled = Config.glowGooSplatterDecayEnabled;
        GlowGooRuntime.splatterDecayTicks = Config.glowGooSplatterDecayTimeSeconds * 20;
        GlowGooRuntime.bioluminescenceEnabled = Config.glowGooBioluminescenceEnabled;
        GlowGooRuntime.bioluminescenceDurationTicks = Config.glowGooBioluminescenceDurationSeconds * 20;
        GlowGooRuntime.bioluminescenceLightLevel = Config.glowGooBioluminescenceLightLevel;
        Config.enableWitheringPotion = ENABLE_WITHERING_POTION.get();
        Config.enableResurfacingPotion = ENABLE_RESURFACING_POTION.get();
        org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags.setResurfacingPotionEnabled(Config.enableResurfacingPotion);
        Config.enableDisplacementPotion = ENABLE_DISPLACEMENT_POTION.get();
        Config.enableReturningPotion = ENABLE_RETURNING_POTION.get();
        Config.enableLevitationPotion = ENABLE_LEVITATION_POTION.get();
        Config.enableHastePotion = ENABLE_HASTE_POTION.get();
        Config.enableBlindnessPotion = ENABLE_BLINDNESS_POTION.get();
        org.onenonly.bitsandbalance.common.potions.PotionFeatureFlags.setBlindnessPotionEnabled(Config.enableBlindnessPotion);
        Config.enableGlowingPotion = ENABLE_GLOWING_POTION.get();
        Config.enableBioluminescencePotion = ENABLE_BIOLUMINESCENCE_POTION.get();
                PotionFeatureFlags.syncRuntimeFlags(
                        Config.enableWitheringPotion
                                || Config.enableDisplacementPotion
                                || Config.enableReturningPotion
                                || Config.enableLevitationPotion
                                || Config.enableHastePotion
                                || Config.enableBlindnessPotion
                                || Config.enableGlowingPotion
                                || Config.enableBioluminescencePotion,
                        Config.enableWitheringPotion,
                        Config.enableResurfacingPotion,
                        Config.enableDisplacementPotion,
                        Config.enableReturningPotion,
                        Config.enableLevitationPotion,
                        Config.enableHastePotion,
                        Config.enableBlindnessPotion,
                        Config.enableGlowingPotion,
                        Config.enableBioluminescencePotion
                );
        Config.enableEnhancedSlabs = ENABLE_ENHANCED_SLABS.get();
        Config.enhancedSlabsPlaceOnTop = ENHANCED_SLABS_PLACE_ON_TOP.get();
        Config.enhancedSlabsHangBelow = ENHANCED_SLABS_HANG_BELOW.get();
        Config.enhancedSlabsKneeSlabMining = ENHANCED_SLABS_KNEE_SLAB_MINING.get();
        Config.enhancedSlabsMixedDoubleSlabs = ENHANCED_SLABS_MIXED_DOUBLE_SLABS.get();
        if (!Config.enhancedSlabsPlaceOnTop
                && !Config.enhancedSlabsHangBelow
                && !Config.enhancedSlabsKneeSlabMining
                && !Config.enhancedSlabsMixedDoubleSlabs) {
            Config.enableEnhancedSlabs = false;
        }
        Config.enhancedSlabsVerticalSlabs = VERTICAL_SLABS_ENABLED.get();
        Config.enhancedSlabsSteps = STEPS_AND_VERTICAL_STEPS_ENABLED.get();
        Config.enableCascading = ENABLE_CASCADING.get();
        org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining.enabled = Config.enableCascading;
        Config.cascadingMaxActiveLevel = CASCADING_MAX_ACTIVE_LEVEL.get();
        org.onenonly.bitsandbalance.common.mechanics.CascadingOreMining.configuredMaxLevel = Config.cascadingMaxActiveLevel;
        Config.cascadingLevelOneMaxOres = CASCADING_LEVEL_ONE_MAX_ORES.get();
        Config.cascadingLevelTwoMaxOres = CASCADING_LEVEL_TWO_MAX_ORES.get();
        Config.cascadingLevelThreeMaxOres = CASCADING_LEVEL_THREE_MAX_ORES.get();
        org.onenonly.bitsandbalance.common.loot.BrushFortune.enabled = ENABLE_FORTUNE_BRUSH.get();
        org.onenonly.bitsandbalance.common.loot.BrushFortune.setItemBlacklist(FORTUNE_BRUSH_BLACKLIST.get());
        Config.enableAerodynamic = ENABLE_AERODYNAMIC.get();
        Config.aerodynamicBaseDragReduction = AERODYNAMIC_BASE_DRAG_REDUCTION.get();
        Config.aerodynamicMinAltitude = AERODYNAMIC_MIN_ALTITUDE.get();
        Config.aerodynamicMaxAltitude = AERODYNAMIC_MAX_ALTITUDE.get();
        Config.aerodynamicCloudLevel = AERODYNAMIC_CLOUD_LEVEL.get();
        Config.aerodynamicSpeedMultiplier = AERODYNAMIC_SPEED_MULTIPLIER.get();
        Config.aerodynamicMaxFlightSpeed = AERODYNAMIC_MAX_FLIGHT_SPEED.get();
        Config.aerodynamicLowAltitudeStrengthFloor = AERODYNAMIC_LOW_ALTITUDE_STRENGTH_FLOOR.get();
        Config.aerodynamicGlidePitchRange = AERODYNAMIC_GLIDE_PITCH_RANGE.get();
        Config.aerodynamicUpwardPitchStart = AERODYNAMIC_UPWARD_PITCH_START.get();
        Config.aerodynamicUpwardPitchRange = AERODYNAMIC_UPWARD_PITCH_RANGE.get();
        Config.aerodynamicUpwardMotionFullSpeed = AERODYNAMIC_UPWARD_MOTION_FULL_SPEED.get();
        Config.aerodynamicClimbAssistScale = AERODYNAMIC_CLIMB_ASSIST_SCALE.get();
        Config.aerodynamicSinkReductionScale = AERODYNAMIC_SINK_REDUCTION_SCALE.get();
        Config.aerodynamicMaxSinkReduction = AERODYNAMIC_MAX_SINK_REDUCTION.get();
        Config.aerodynamicSinkReductionMinHorizontalSpeed = AERODYNAMIC_SINK_REDUCTION_MIN_HORIZONTAL_SPEED.get();
        Config.aerodynamicSinkReductionFullHorizontalSpeed = AERODYNAMIC_SINK_REDUCTION_FULL_HORIZONTAL_SPEED.get();
        Config.aerodynamicClimbRestoreCap = AERODYNAMIC_CLIMB_RESTORE_CAP.get();
        Config.aerodynamicClimbRestoreScale = AERODYNAMIC_CLIMB_RESTORE_SCALE.get();
        Config.aerodynamicTurnResponse = AERODYNAMIC_TURN_RESPONSE.get();
        Config.aerodynamicTurnRestoreBonus = AERODYNAMIC_TURN_RESTORE_BONUS.get();
        Config.enableEnchancementElytraDurabilityCompat = ENABLE_ENCHANCEMENT_ELYTRA_DURABILITY_COMPAT.get();
        Config.enableAlternativeRepeaterRecipe = ENABLE_ALTERNATIVE_REPEATER_RECIPE.get();
        Config.enableChestFromLogsRecipe = ENABLE_CHEST_FROM_LOGS_RECIPE.get();
        Config.enableMapInkSacRecipe = ENABLE_MAP_INK_SAC_RECIPE.get();
        Config.enableRecoveryCompassRecipe = ENABLE_RECOVERY_COMPASS_RECIPE.get();
        Config.enableEnderEyeRecipe = ENABLE_ENDER_EYE_RECIPE.get();
        Config.enableRawIronSmeltingRecipe = ENABLE_RAW_IRON_SMELTING_RECIPE.get();
        Config.enableRawGoldSmeltingRecipe = ENABLE_RAW_GOLD_SMELTING_RECIPE.get();
        Config.enableRawCopperSmeltingRecipe = ENABLE_RAW_COPPER_SMELTING_RECIPE.get();
        Config.enableRawIronBlastingRecipe = ENABLE_RAW_IRON_BLASTING_RECIPE.get();
        Config.enableRawGoldBlastingRecipe = ENABLE_RAW_GOLD_BLASTING_RECIPE.get();
        Config.enableRawCopperBlastingRecipe = ENABLE_RAW_COPPER_BLASTING_RECIPE.get();
        Config.enableOreSmeltingXp = ENABLE_ORE_SMELTING_XP.get();
        Config.coalOreSmeltingXp = COAL_ORE_SMELTING_XP.get();
        Config.lapisOreSmeltingXp = LAPIS_ORE_SMELTING_XP.get();
        Config.netherQuartzOreSmeltingXp = NETHER_QUARTZ_ORE_SMELTING_XP.get();
        Config.ironOreSmeltingXp = IRON_ORE_SMELTING_XP.get();
        Config.copperOreSmeltingXp = COPPER_ORE_SMELTING_XP.get();
        Config.redstoneOreSmeltingXp = REDSTONE_ORE_SMELTING_XP.get();
        Config.goldOreSmeltingXp = GOLD_ORE_SMELTING_XP.get();
        Config.netherGoldOreSmeltingXp = NETHER_GOLD_ORE_SMELTING_XP.get();
        Config.diamondOreSmeltingXp = DIAMOND_ORE_SMELTING_XP.get();
        Config.emeraldOreSmeltingXp = EMERALD_ORE_SMELTING_XP.get();
        Config.ancientDebrisSmeltingXp = ANCIENT_DEBRIS_SMELTING_XP.get();
        OreSmeltingXpRuntime.enabled = Config.enableOreSmeltingXp;
        OreSmeltingXpRuntime.coal = Config.coalOreSmeltingXp;
        OreSmeltingXpRuntime.lapis = Config.lapisOreSmeltingXp;
        OreSmeltingXpRuntime.netherQuartz = Config.netherQuartzOreSmeltingXp;
        OreSmeltingXpRuntime.iron = Config.ironOreSmeltingXp;
        OreSmeltingXpRuntime.copper = Config.copperOreSmeltingXp;
        OreSmeltingXpRuntime.redstone = Config.redstoneOreSmeltingXp;
        OreSmeltingXpRuntime.gold = Config.goldOreSmeltingXp;
        OreSmeltingXpRuntime.netherGold = Config.netherGoldOreSmeltingXp;
        OreSmeltingXpRuntime.diamond = Config.diamondOreSmeltingXp;
        OreSmeltingXpRuntime.emerald = Config.emeraldOreSmeltingXp;
        OreSmeltingXpRuntime.ancientDebris = Config.ancientDebrisSmeltingXp;
        Config.enableStairRecipeOverride = ENABLE_STAIR_RECIPE_OVERRIDE.get();
        Config.enableCompactStairRecipe = ENABLE_COMPACT_STAIR_RECIPE.get();
        Config.enableTorchFuel = ENABLE_TORCH_FUEL.get();
        Config.torchBurnTime = TORCH_BURN_TIME.get();
        Config.enableCustomLootTables = ENABLE_CUSTOM_LOOT_TABLES.get();
        BrushingLootSettings.configure(Config.enableCustomLootTables && BRUSHING_LOOT_ENABLED.get(), java.util.Map.of(
                BrushingLootSettings.Site.DESERT_WELL, BRUSHING_DESERT_WELL_ENABLED.get(),
                BrushingLootSettings.Site.DESERT_PYRAMID, BRUSHING_DESERT_PYRAMID_ENABLED.get(),
                BrushingLootSettings.Site.TRAIL_RUINS, BRUSHING_TRAIL_RUINS_ENABLED.get(),
                BrushingLootSettings.Site.COLD_OCEAN_RUINS, BRUSHING_COLD_OCEAN_RUINS_ENABLED.get(),
                BrushingLootSettings.Site.WARM_OCEAN_RUINS, BRUSHING_WARM_OCEAN_RUINS_ENABLED.get()
        ));
        Config.enableDogMusicDisc = ENABLE_DOG_MUSIC_DISC.get();
        Config.enableMineshaftLoot = ENABLE_MINESHAFT_LOOT.get();
        Config.enableMineshaftResurfacingPotion = ENABLE_MINESHAFT_RESURFACING_POTION.get();
        Config.mineshaftResurfacingWeight = MINESHAFT_RESURFACING_WEIGHT.get();
        Config.mineshaftResurfacingMinCount = MINESHAFT_RESURFACING_MIN_COUNT.get();
        Config.mineshaftResurfacingMaxCount = MINESHAFT_RESURFACING_MAX_COUNT.get();
        Config.enableMineshaftReturningPotion = ENABLE_MINESHAFT_RETURNING_POTION.get();
        Config.mineshaftReturningWeight = MINESHAFT_RETURNING_WEIGHT.get();
        Config.mineshaftReturningMinCount = MINESHAFT_RETURNING_MIN_COUNT.get();
        Config.mineshaftReturningMaxCount = MINESHAFT_RETURNING_MAX_COUNT.get();
        Config.enableMineshaftHastePotion = ENABLE_MINESHAFT_HASTE_POTION.get();
        Config.mineshaftHasteWeight = MINESHAFT_HASTE_WEIGHT.get();
        Config.mineshaftHasteMinCount = MINESHAFT_HASTE_MIN_COUNT.get();
        Config.mineshaftHasteMaxCount = MINESHAFT_HASTE_MAX_COUNT.get();
        Config.enableMineshaftStrongHastePotion = ENABLE_MINESHAFT_STRONG_HASTE_POTION.get();
        Config.mineshaftStrongHasteWeight = MINESHAFT_STRONG_HASTE_WEIGHT.get();
        Config.mineshaftStrongHasteMinCount = MINESHAFT_STRONG_HASTE_MIN_COUNT.get();
        Config.mineshaftStrongHasteMaxCount = MINESHAFT_STRONG_HASTE_MAX_COUNT.get();
        Config.enableStrongholdLoot = ENABLE_STRONGHOLD_LOOT.get();
        Config.enableStrongholdResurfacingPotion = ENABLE_STRONGHOLD_RESURFACING_POTION.get();
        Config.strongholdResurfacingWeight = STRONGHOLD_RESURFACING_WEIGHT.get();
        Config.strongholdResurfacingMinCount = STRONGHOLD_RESURFACING_MIN_COUNT.get();
        Config.strongholdResurfacingMaxCount = STRONGHOLD_RESURFACING_MAX_COUNT.get();
        Config.enableStrongholdReturningPotion = ENABLE_STRONGHOLD_RETURNING_POTION.get();
        Config.strongholdReturningWeight = STRONGHOLD_RETURNING_WEIGHT.get();
        Config.strongholdReturningMinCount = STRONGHOLD_RETURNING_MIN_COUNT.get();
        Config.strongholdReturningMaxCount = STRONGHOLD_RETURNING_MAX_COUNT.get();
        Config.enableAncientCityLoot = ENABLE_ANCIENT_CITY_LOOT.get();
        Config.enableAncientCityResurfacingPotion = ENABLE_ANCIENT_CITY_RESURFACING_POTION.get();
        Config.ancientCityResurfacingWeight = ANCIENT_CITY_RESURFACING_WEIGHT.get();
        Config.ancientCityResurfacingMinCount = ANCIENT_CITY_RESURFACING_MIN_COUNT.get();
        Config.ancientCityResurfacingMaxCount = ANCIENT_CITY_RESURFACING_MAX_COUNT.get();
        Config.enableAncientCityReturningPotion = ENABLE_ANCIENT_CITY_RETURNING_POTION.get();
        Config.ancientCityReturningWeight = ANCIENT_CITY_RETURNING_WEIGHT.get();
        Config.ancientCityReturningMinCount = ANCIENT_CITY_RETURNING_MIN_COUNT.get();
        Config.ancientCityReturningMaxCount = ANCIENT_CITY_RETURNING_MAX_COUNT.get();
        Config.enableEndCityLoot = ENABLE_END_CITY_LOOT.get();
        Config.enableEndCityReturningPotion = ENABLE_END_CITY_RETURNING_POTION.get();
        Config.endCityReturningWeight = END_CITY_RETURNING_WEIGHT.get();
        Config.endCityReturningMinCount = END_CITY_RETURNING_MIN_COUNT.get();
        Config.endCityReturningMaxCount = END_CITY_RETURNING_MAX_COUNT.get();
        Config.enableSimpleDungeonLoot = ENABLE_SIMPLE_DUNGEON_LOOT.get();
        Config.enableSimpleDungeonResurfacingPotion = ENABLE_SIMPLE_DUNGEON_RESURFACING_POTION.get();
        Config.simpleDungeonResurfacingWeight = SIMPLE_DUNGEON_RESURFACING_WEIGHT.get();
        Config.simpleDungeonResurfacingMinCount = SIMPLE_DUNGEON_RESURFACING_MIN_COUNT.get();
        Config.simpleDungeonResurfacingMaxCount = SIMPLE_DUNGEON_RESURFACING_MAX_COUNT.get();
        Config.enableSimpleDungeonReturningPotion = ENABLE_SIMPLE_DUNGEON_RETURNING_POTION.get();
        Config.simpleDungeonReturningWeight = SIMPLE_DUNGEON_RETURNING_WEIGHT.get();
        Config.simpleDungeonReturningMinCount = SIMPLE_DUNGEON_RETURNING_MIN_COUNT.get();
        Config.simpleDungeonReturningMaxCount = SIMPLE_DUNGEON_RETURNING_MAX_COUNT.get();
        Config.enableTrialChamberLoot = ENABLE_TRIAL_CHAMBER_LOOT.get();
        Config.enableTrialCommonResurfacingPotion = ENABLE_TRIAL_COMMON_RESURFACING_POTION.get();
        Config.trialCommonResurfacingWeight = TRIAL_COMMON_RESURFACING_WEIGHT.get();
        Config.trialCommonResurfacingMinCount = TRIAL_COMMON_RESURFACING_MIN_COUNT.get();
        Config.trialCommonResurfacingMaxCount = TRIAL_COMMON_RESURFACING_MAX_COUNT.get();
        Config.enableTrialRareResurfacingPotion = ENABLE_TRIAL_RARE_RESURFACING_POTION.get();
        Config.trialRareResurfacingWeight = TRIAL_RARE_RESURFACING_WEIGHT.get();
        Config.trialRareResurfacingMinCount = TRIAL_RARE_RESURFACING_MIN_COUNT.get();
        Config.trialRareResurfacingMaxCount = TRIAL_RARE_RESURFACING_MAX_COUNT.get();
        Config.enableTrialRareReturningPotion = ENABLE_TRIAL_RARE_RETURNING_POTION.get();
        Config.trialRareReturningWeight = TRIAL_RARE_RETURNING_WEIGHT.get();
        Config.trialRareReturningMinCount = TRIAL_RARE_RETURNING_MIN_COUNT.get();
        Config.trialRareReturningMaxCount = TRIAL_RARE_RETURNING_MAX_COUNT.get();
        Config.enableTrialUniqueReturningPotion = ENABLE_TRIAL_UNIQUE_RETURNING_POTION.get();
        Config.trialUniqueReturningWeight = TRIAL_UNIQUE_RETURNING_WEIGHT.get();
        Config.trialUniqueReturningMinCount = TRIAL_UNIQUE_RETURNING_MIN_COUNT.get();
        Config.trialUniqueReturningMaxCount = TRIAL_UNIQUE_RETURNING_MAX_COUNT.get();
    }
}
