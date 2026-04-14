package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class LootTablesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Loot Tables Configuration");
        BUILDER.push("lootTables");
    }

    // Master toggle for custom loot tables
    private static final ModConfigSpec.BooleanValue ENABLE_CUSTOM_LOOT_TABLES = BUILDER
            .comment("Enable custom loot table modifications (master toggle).")
            .define("enableCustomLootTables", true);

    // ========================================
    // MINESHAFT LOOT
    // ========================================
    static {
        BUILDER.comment("Mineshaft Loot");
        BUILDER.push("mineshaftLoot");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_LOOT = BUILDER
            .comment("Enable custom loot additions to abandoned mineshaft chests.")
            .define("enableMineshaftLoot", true);

    // Resurfacing Potion in Mineshafts
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacingPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_RESURFACING_POTION = BUILDER
            .comment("Add Resurfacing Potion to mineshaft loot tables.")
            .define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_RESURFACING_WEIGHT = BUILDER
            .comment("Weight for Resurfacing Potion in mineshaft loot (higher = more common).")
            .defineInRange("resurfacingPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_RESURFACING_MIN_COUNT = BUILDER
            .comment("Minimum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_RESURFACING_MAX_COUNT = BUILDER
            .comment("Maximum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // resurfacingPotion

    // Returning Potion in Mineshafts
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to mineshaft loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in mineshaft loot (higher = more common).")
            .defineInRange("returningPotionWeight", 1, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion

    // Haste Potion in Mineshafts
    static {
        BUILDER.comment("Haste Potion");
        BUILDER.push("hastePotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_HASTE_POTION = BUILDER
            .comment("Add Haste Potion to mineshaft loot tables.")
            .define("enableHastePotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_HASTE_WEIGHT = BUILDER
            .comment("Weight for Haste Potion in mineshaft loot (higher = more common).")
            .defineInRange("hastePotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_HASTE_MIN_COUNT = BUILDER
            .comment("Minimum count of Haste Potions when found.")
            .defineInRange("hastePotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_HASTE_MAX_COUNT = BUILDER
            .comment("Maximum count of Haste Potions when found.")
            .defineInRange("hastePotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // hastePotion

    // Strong Haste Potion in Mineshafts
    static {
        BUILDER.comment("Strong Haste Potion");
        BUILDER.push("strongHastePotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_MINESHAFT_STRONG_HASTE_POTION = BUILDER
            .comment("Add Strong Haste Potion to mineshaft loot tables.")
            .define("enableStrongHastePotion", true);
    private static final ModConfigSpec.IntValue MINESHAFT_STRONG_HASTE_WEIGHT = BUILDER
            .comment("Weight for Strong Haste Potion in mineshaft loot (higher = more common).")
            .defineInRange("strongHastePotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue MINESHAFT_STRONG_HASTE_MIN_COUNT = BUILDER
            .comment("Minimum count of Strong Haste Potions when found.")
            .defineInRange("strongHastePotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue MINESHAFT_STRONG_HASTE_MAX_COUNT = BUILDER
            .comment("Maximum count of Strong Haste Potions when found.")
            .defineInRange("strongHastePotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // strongHastePotion
    static { BUILDER.pop(); } // mineshaftLoot

    // ========================================
    // STRONGHOLD LOOT
    // ========================================
    static {
        BUILDER.comment("Stronghold Loot");
        BUILDER.push("strongholdLoot");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_STRONGHOLD_LOOT = BUILDER
            .comment("Enable custom loot additions to stronghold chests.")
            .define("enableStrongholdLoot", true);

    // Resurfacing Potion in Strongholds
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacingPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_STRONGHOLD_RESURFACING_POTION = BUILDER
            .comment("Add Resurfacing Potion to stronghold loot tables.")
            .define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue STRONGHOLD_RESURFACING_WEIGHT = BUILDER
            .comment("Weight for Resurfacing Potion in stronghold loot (higher = more common).")
            .defineInRange("resurfacingPotionWeight", 4, 1, 100);
    private static final ModConfigSpec.IntValue STRONGHOLD_RESURFACING_MIN_COUNT = BUILDER
            .comment("Minimum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue STRONGHOLD_RESURFACING_MAX_COUNT = BUILDER
            .comment("Maximum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // resurfacingPotion

    // Returning Potion in Strongholds
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_STRONGHOLD_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to stronghold loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue STRONGHOLD_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in stronghold loot (higher = more common).")
            .defineInRange("returningPotionWeight", 3, 1, 100);
    private static final ModConfigSpec.IntValue STRONGHOLD_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue STRONGHOLD_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion
    static { BUILDER.pop(); } // strongholdLoot

    // ========================================
    // ANCIENT CITY LOOT
    // ========================================
    static {
        BUILDER.comment("Ancient City Loot");
        BUILDER.push("ancientCityLoot");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_ANCIENT_CITY_LOOT = BUILDER
            .comment("Enable custom loot additions to ancient city chests.")
            .define("enableAncientCityLoot", true);

    // Resurfacing Potion in Ancient Cities
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacingPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_ANCIENT_CITY_RESURFACING_POTION = BUILDER
            .comment("Add Resurfacing Potion to ancient city loot tables.")
            .define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RESURFACING_WEIGHT = BUILDER
            .comment("Weight for Resurfacing Potion in ancient city loot (higher = more common).")
            .defineInRange("resurfacingPotionWeight", 1, 1, 100);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RESURFACING_MIN_COUNT = BUILDER
            .comment("Minimum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RESURFACING_MAX_COUNT = BUILDER
            .comment("Maximum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // resurfacingPotion

    // Returning Potion in Ancient Cities
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_ANCIENT_CITY_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to ancient city loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in ancient city loot (higher = more common).")
            .defineInRange("returningPotionWeight", 3, 1, 100);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue ANCIENT_CITY_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion
    static { BUILDER.pop(); } // ancientCityLoot

    // ========================================
    // END CITY LOOT
    // ========================================
    static {
        BUILDER.comment("End City Loot");
        BUILDER.push("endCityLoot");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_END_CITY_LOOT = BUILDER
            .comment("Enable custom loot additions to end city chests.")
            .define("enableEndCityLoot", true);

    // Returning Potion in End Cities
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_END_CITY_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to end city loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue END_CITY_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in end city loot (higher = more common).")
            .defineInRange("returningPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue END_CITY_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue END_CITY_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion
    static { BUILDER.pop(); } // endCityLoot

    // ========================================
    // SIMPLE DUNGEON LOOT
    // ========================================
    static {
        BUILDER.comment("Simple Dungeon Loot");
        BUILDER.push("simpleDungeonLoot");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_DUNGEON_LOOT = BUILDER
            .comment("Enable custom loot additions to simple dungeon chests.")
            .define("enableSimpleDungeonLoot", true);

    // Resurfacing Potion in Simple Dungeons
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacingPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_DUNGEON_RESURFACING_POTION = BUILDER
            .comment("Add Resurfacing Potion to simple dungeon loot tables.")
            .define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RESURFACING_WEIGHT = BUILDER
            .comment("Weight for Resurfacing Potion in simple dungeon loot (higher = more common).")
            .defineInRange("resurfacingPotionWeight", 3, 1, 100);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RESURFACING_MIN_COUNT = BUILDER
            .comment("Minimum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RESURFACING_MAX_COUNT = BUILDER
            .comment("Maximum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // resurfacingPotion

    // Returning Potion in Simple Dungeons
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_SIMPLE_DUNGEON_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to simple dungeon loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in simple dungeon loot (higher = more common).")
            .defineInRange("returningPotionWeight", 6, 1, 100);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue SIMPLE_DUNGEON_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion
    static { BUILDER.pop(); } // simpleDungeonLoot

    // ========================================
    // TRIAL CHAMBER LOOT
    // ========================================
    static {
        BUILDER.comment("Trial Chamber Loot");
        BUILDER.push("trialChamberLoot");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_CHAMBER_LOOT = BUILDER
            .comment("Enable custom loot additions to trial chamber chests.")
            .define("enableTrialChamberLoot", true);

    // Common Rewards
    static {
        BUILDER.comment("Common Rewards");
        BUILDER.push("common");
    }
    // Resurfacing Potion in Trial Chamber Common
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacingPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_COMMON_RESURFACING_POTION = BUILDER
            .comment("Add Resurfacing Potion to trial chamber common loot tables.")
            .define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_COMMON_RESURFACING_WEIGHT = BUILDER
            .comment("Weight for Resurfacing Potion in trial chamber common loot (higher = more common).")
            .defineInRange("resurfacingPotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_COMMON_RESURFACING_MIN_COUNT = BUILDER
            .comment("Minimum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_COMMON_RESURFACING_MAX_COUNT = BUILDER
            .comment("Maximum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // resurfacingPotion
    static { BUILDER.pop(); } // common

    // Rare Rewards
    static {
        BUILDER.comment("Rare Rewards");
        BUILDER.push("rare");
    }
    // Resurfacing Potion in Trial Chamber Rare
    static {
        BUILDER.comment("Resurfacing Potion");
        BUILDER.push("resurfacingPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_RARE_RESURFACING_POTION = BUILDER
            .comment("Add Resurfacing Potion to trial chamber rare loot tables.")
            .define("enableResurfacingPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RESURFACING_WEIGHT = BUILDER
            .comment("Weight for Resurfacing Potion in trial chamber rare loot (higher = more common).")
            .defineInRange("resurfacingPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RESURFACING_MIN_COUNT = BUILDER
            .comment("Minimum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RESURFACING_MAX_COUNT = BUILDER
            .comment("Maximum count of Resurfacing Potions when found.")
            .defineInRange("resurfacingPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // resurfacingPotion

    // Returning Potion in Trial Chamber Rare
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_RARE_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to trial chamber rare loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in trial chamber rare loot (higher = more common).")
            .defineInRange("returningPotionWeight", 2, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_RARE_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion
    static { BUILDER.pop(); } // rare

    // Unique Rewards
    static {
        BUILDER.comment("Unique Rewards");
        BUILDER.push("unique");
    }
    // Returning Potion in Trial Chamber Unique
    static {
        BUILDER.comment("Returning Potion");
        BUILDER.push("returningPotion");
    }
    private static final ModConfigSpec.BooleanValue ENABLE_TRIAL_UNIQUE_RETURNING_POTION = BUILDER
            .comment("Add Returning Potion to trial chamber unique loot tables.")
            .define("enableReturningPotion", true);
    private static final ModConfigSpec.IntValue TRIAL_UNIQUE_RETURNING_WEIGHT = BUILDER
            .comment("Weight for Returning Potion in trial chamber unique loot (higher = more common).")
            .defineInRange("returningPotionWeight", 5, 1, 100);
    private static final ModConfigSpec.IntValue TRIAL_UNIQUE_RETURNING_MIN_COUNT = BUILDER
            .comment("Minimum count of Returning Potions when found.")
            .defineInRange("returningPotionMinCount", 1, 1, 8);
    private static final ModConfigSpec.IntValue TRIAL_UNIQUE_RETURNING_MAX_COUNT = BUILDER
            .comment("Maximum count of Returning Potions when found.")
            .defineInRange("returningPotionMaxCount", 1, 1, 8);
    static { BUILDER.pop(); } // returningPotion
    static { BUILDER.pop(); } // unique
    static { BUILDER.pop(); } // trialChamberLoot

    static { BUILDER.pop(); } // lootTables

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;

        // Master toggle
        Config.enableCustomLootTables = ENABLE_CUSTOM_LOOT_TABLES.get();

        // Mineshaft loot
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

        // Stronghold loot
        Config.enableStrongholdLoot = ENABLE_STRONGHOLD_LOOT.get();
        Config.enableStrongholdResurfacingPotion = ENABLE_STRONGHOLD_RESURFACING_POTION.get();
        Config.strongholdResurfacingWeight = STRONGHOLD_RESURFACING_WEIGHT.get();
        Config.strongholdResurfacingMinCount = STRONGHOLD_RESURFACING_MIN_COUNT.get();
        Config.strongholdResurfacingMaxCount = STRONGHOLD_RESURFACING_MAX_COUNT.get();
        Config.enableStrongholdReturningPotion = ENABLE_STRONGHOLD_RETURNING_POTION.get();
        Config.strongholdReturningWeight = STRONGHOLD_RETURNING_WEIGHT.get();
        Config.strongholdReturningMinCount = STRONGHOLD_RETURNING_MIN_COUNT.get();
        Config.strongholdReturningMaxCount = STRONGHOLD_RETURNING_MAX_COUNT.get();

        // Ancient city loot
        Config.enableAncientCityLoot = ENABLE_ANCIENT_CITY_LOOT.get();
        Config.enableAncientCityResurfacingPotion = ENABLE_ANCIENT_CITY_RESURFACING_POTION.get();
        Config.ancientCityResurfacingWeight = ANCIENT_CITY_RESURFACING_WEIGHT.get();
        Config.ancientCityResurfacingMinCount = ANCIENT_CITY_RESURFACING_MIN_COUNT.get();
        Config.ancientCityResurfacingMaxCount = ANCIENT_CITY_RESURFACING_MAX_COUNT.get();
        Config.enableAncientCityReturningPotion = ENABLE_ANCIENT_CITY_RETURNING_POTION.get();
        Config.ancientCityReturningWeight = ANCIENT_CITY_RETURNING_WEIGHT.get();
        Config.ancientCityReturningMinCount = ANCIENT_CITY_RETURNING_MIN_COUNT.get();
        Config.ancientCityReturningMaxCount = ANCIENT_CITY_RETURNING_MAX_COUNT.get();

        // End city loot
        Config.enableEndCityLoot = ENABLE_END_CITY_LOOT.get();
        Config.enableEndCityReturningPotion = ENABLE_END_CITY_RETURNING_POTION.get();
        Config.endCityReturningWeight = END_CITY_RETURNING_WEIGHT.get();
        Config.endCityReturningMinCount = END_CITY_RETURNING_MIN_COUNT.get();
        Config.endCityReturningMaxCount = END_CITY_RETURNING_MAX_COUNT.get();

        // Simple dungeon loot
        Config.enableSimpleDungeonLoot = ENABLE_SIMPLE_DUNGEON_LOOT.get();
        Config.enableSimpleDungeonResurfacingPotion = ENABLE_SIMPLE_DUNGEON_RESURFACING_POTION.get();
        Config.simpleDungeonResurfacingWeight = SIMPLE_DUNGEON_RESURFACING_WEIGHT.get();
        Config.simpleDungeonResurfacingMinCount = SIMPLE_DUNGEON_RESURFACING_MIN_COUNT.get();
        Config.simpleDungeonResurfacingMaxCount = SIMPLE_DUNGEON_RESURFACING_MAX_COUNT.get();
        Config.enableSimpleDungeonReturningPotion = ENABLE_SIMPLE_DUNGEON_RETURNING_POTION.get();
        Config.simpleDungeonReturningWeight = SIMPLE_DUNGEON_RETURNING_WEIGHT.get();
        Config.simpleDungeonReturningMinCount = SIMPLE_DUNGEON_RETURNING_MIN_COUNT.get();
        Config.simpleDungeonReturningMaxCount = SIMPLE_DUNGEON_RETURNING_MAX_COUNT.get();

        // Trial chamber loot
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