package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.onenonly.bitsandbalance.common.mechanics.GlowGooRuntime;

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

    static { BUILDER.comment("Building").push("building"); }
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
    static { BUILDER.pop(); }

    private static final ModConfigSpec.BooleanValue ENABLE_AERODYNAMIC = BUILDER.comment("Enable the Aerodynamic enchantment effect.").define("aerodynamic.enabled", true);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_BASE_DRAG_REDUCTION = BUILDER.comment("Base horizontal drag reduction at full strength. 0.5 = 50% less horizontal drag (recommended).").defineInRange("aerodynamic.baseDragReduction", 0.5D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue AERODYNAMIC_MIN_ALTITUDE = BUILDER.comment("Minimum Y where Aerodynamic begins to take effect.").defineInRange("aerodynamic.minAltitude", 192, -2032, 4096);
    private static final ModConfigSpec.IntValue AERODYNAMIC_MAX_ALTITUDE = BUILDER.comment("Y where Aerodynamic reaches full strength.").defineInRange("aerodynamic.maxAltitude", 320, -2032, 4096);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_CLOUD_LEVEL = BUILDER.comment("Cloud level used as the midpoint of the curve.").defineInRange("aerodynamic.cloudLevel", 192.0D, -2032.0D, 4096.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_SPEED_MULTIPLIER = BUILDER.comment("Global multiplier on the effect strength.").defineInRange("aerodynamic.speedMultiplier", 1.0D, 0.0D, 10.0D);
    private static final ModConfigSpec.DoubleValue AERODYNAMIC_MAX_FLIGHT_SPEED = BUILDER.comment("Maximum elytra flight speed (m/s) while Aerodynamic is applying its boost.").defineInRange("aerodynamic.maxFlightSpeed", 50.0D, 0.0D, 200.0D);

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
    static { BUILDER.comment("Fuel Tweaks"); BUILDER.push("fuelTweaks"); }
    private static final ModConfigSpec.BooleanValue ENABLE_TORCH_FUEL = BUILDER.comment("Enable torch as fuel for furnaces.").define("enableTorchFuel", true);
    private static final ModConfigSpec.IntValue TORCH_BURN_TIME = BUILDER.comment("Burn time for torches in ticks (400 ticks = 20 seconds).").defineInRange("torchBurnTime", 400, 0, 10000);
    static { BUILDER.pop(); }
    static { BUILDER.pop(); }

    static { BUILDER.comment("Loot Tables Configuration"); BUILDER.push("lootTables"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_LOOT_TABLES = BUILDER.comment("Enable custom loot table modifications (master toggle).").define("enableCustomLootTables", true);
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
        if (event.getConfig().getSpec() != SPEC) return;

        Config.enableGlowGoo = ENABLE_GLOW_GOO.get();
        Config.glowGooTwoGlowInkRecipeEnabled = GLOW_GOO_TWO_GLOW_INK_RECIPE_ENABLED.get();
        Config.glowGooFourGlowInkRecipeEnabled = GLOW_GOO_FOUR_GLOW_INK_RECIPE_ENABLED.get();
        Config.glowGooImpactParticlesEnabled = ENABLE_GLOW_GOO_IMPACT_PARTICLES.get();
        Config.glowGooImpactParticleCount = GLOW_GOO_IMPACT_PARTICLE_COUNT.get();
        Config.glowGooAmbientSplatterParticlesEnabled = ENABLE_GLOW_GOO_SPLATTER_AMBIENT_PARTICLES.get();
        Config.glowGooBioluminescenceEnabled = ENABLE_GLOW_GOO_BIOLUMINESCENCE.get();
        Config.glowGooBioluminescenceDurationSeconds = GLOW_GOO_BIOLUMINESCENCE_DURATION_SECONDS.get();
        Config.glowGooBioluminescenceLightLevel = GLOW_GOO_BIOLUMINESCENCE_LIGHT_LEVEL.get();
        GlowGooRuntime.enabled = Config.enableGlowGoo;
        GlowGooRuntime.impactParticlesEnabled = Config.glowGooImpactParticlesEnabled;
        GlowGooRuntime.impactParticleCount = Config.glowGooImpactParticleCount;
        GlowGooRuntime.splatterAmbientParticlesEnabled = Config.glowGooAmbientSplatterParticlesEnabled;
        GlowGooRuntime.bioluminescenceEnabled = Config.glowGooBioluminescenceEnabled;
        GlowGooRuntime.bioluminescenceDurationTicks = Config.glowGooBioluminescenceDurationSeconds * 20;
        GlowGooRuntime.bioluminescenceLightLevel = Config.glowGooBioluminescenceLightLevel;
        Config.enableWitheringPotion = ENABLE_WITHERING_POTION.get();
        Config.enableResurfacingPotion = ENABLE_RESURFACING_POTION.get();
        Config.enableDisplacementPotion = ENABLE_DISPLACEMENT_POTION.get();
        Config.enableReturningPotion = ENABLE_RETURNING_POTION.get();
        Config.enableLevitationPotion = ENABLE_LEVITATION_POTION.get();
        Config.enableHastePotion = ENABLE_HASTE_POTION.get();
        Config.enableGlowingPotion = ENABLE_GLOWING_POTION.get();
        Config.enableBioluminescencePotion = ENABLE_BIOLUMINESCENCE_POTION.get();
        Config.enhancedSlabsVerticalSlabs = VERTICAL_SLABS_ENABLED.get();
        Config.enhancedSlabsSteps = STEPS_AND_VERTICAL_STEPS_ENABLED.get();
        Config.enableAerodynamic = ENABLE_AERODYNAMIC.get();
        Config.aerodynamicBaseDragReduction = AERODYNAMIC_BASE_DRAG_REDUCTION.get();
        Config.aerodynamicMinAltitude = AERODYNAMIC_MIN_ALTITUDE.get();
        Config.aerodynamicMaxAltitude = AERODYNAMIC_MAX_ALTITUDE.get();
        Config.aerodynamicCloudLevel = AERODYNAMIC_CLOUD_LEVEL.get();
        Config.aerodynamicSpeedMultiplier = AERODYNAMIC_SPEED_MULTIPLIER.get();
        Config.aerodynamicMaxFlightSpeed = AERODYNAMIC_MAX_FLIGHT_SPEED.get();
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
        Config.enableStairRecipeOverride = ENABLE_STAIR_RECIPE_OVERRIDE.get();
        Config.enableCompactStairRecipe = ENABLE_COMPACT_STAIR_RECIPE.get();
        Config.enableTorchFuel = ENABLE_TORCH_FUEL.get();
        Config.torchBurnTime = TORCH_BURN_TIME.get();
        Config.enableCustomLootTables = ENABLE_CUSTOM_LOOT_TABLES.get();
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
