package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import org.onenonly.bitsandbalance.Config;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class MobsConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Mobs");
        BUILDER.push("mobs");
    }

    // Cave Spiders replacing Spiders in caves
    static { BUILDER.comment("Cave Spiders"); BUILDER.push("caveSpiders"); }
    private static final ModConfigSpec.BooleanValue ENABLE_CAVE_SPIDERS_IN_CAVES = BUILDER
            .comment("Allow Cave Spiders to spawn in place of Spiders in caves.")
            .define("enableCaveSpidersInCaves", true);
    private static final ModConfigSpec.DoubleValue CAVE_SPIDER_REPLACEMENT_CHANCE = BUILDER
            .comment("Chance for a Spider in a cave to be replaced by a Cave Spider (0.0-1.0).")
            .defineInRange("caveSpiderReplacementChance", 0.20D, 0.0D, 1.0D);

    static { BUILDER.pop(); }

    // Disable Nitwits
    static { BUILDER.comment("Disable Nitwits"); BUILDER.push("disableNitwits"); }
    private static final ModConfigSpec.BooleanValue DISABLE_NITWITS = BUILDER
            .comment("Prevent Nitwit villagers from spawning naturally.")
            .define("disableNitwits", true);
    static { BUILDER.pop(); }

    // Improved Wandering Traders
    static { BUILDER.comment("Improved Wandering Traders"); BUILDER.push("improvedWanderingTraders"); }
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_WANDERING_TRADERS = BUILDER
            .comment("Enable improved wandering traders with custom trades.")
            .define("enableImprovedWanderingTraders", true);

    // Replace vanilla trades vs add to them
    private static final ModConfigSpec.BooleanValue REPLACE_VANILLA_TRADES = BUILDER
            .comment("If true, replace all vanilla wandering trader trades. If false, add custom trades to existing ones.")
            .define("replaceVanillaTrades", false);

    // Individual trade categories - we'll add specific trades here as you provide them
    static { BUILDER.comment("Trade Categories"); BUILDER.push("tradeCategories"); }

    // Example category structure - we'll expand this based on your trades
    static { BUILDER.comment("Building Materials"); BUILDER.push("buildingMaterials"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BUILDING_MATERIALS_TRADES = BUILDER
            .comment("Enable building materials trades.")
            .define("enableBuildingMaterialsTrades", true);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Rare Items"); BUILDER.push("rareItems"); }
    private static final ModConfigSpec.BooleanValue ENABLE_RARE_ITEMS_TRADES = BUILDER
            .comment("Enable rare items trades.")
            .define("enableRareItemsTrades", true);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Utility Items"); BUILDER.push("utilityItems"); }
    private static final ModConfigSpec.BooleanValue ENABLE_UTILITY_ITEMS_TRADES = BUILDER
            .comment("Enable utility items trades.")
            .define("enableUtilityItemsTrades", true);
    static { BUILDER.pop(); }

    static { BUILDER.comment("Decorative Items"); BUILDER.push("decorativeItems"); }
    private static final ModConfigSpec.BooleanValue ENABLE_DECORATIVE_ITEMS_TRADES = BUILDER
            .comment("Enable decorative items trades.")
            .define("enableDecorativeItemsTrades", true);
    static { BUILDER.pop(); }

    // Individual trade toggles will be added here as you provide specific trades
    // Format: ENABLE_[TRADE_NAME]_TRADE for each individual trade

    static { BUILDER.pop(); } // tradeCategories
    static { BUILDER.pop(); } // improvedWanderingTraders

    // Improved Phantoms
    static { BUILDER.comment("Improved Phantoms"); BUILDER.push("improvedPhantoms"); }
    private static final ModConfigSpec.BooleanValue ENABLE_IMPROVED_PHANTOMS = BUILDER
            .comment("Enable improved phantom mechanics.")
            .define("enableImprovedPhantoms", true);
    
    // Phantom pickup mechanics
    static { BUILDER.comment("Phantom Pickup"); BUILDER.push("phantomPickup"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_PICKUP = BUILDER
            .comment("Allow phantoms to pick up and carry players.")
            .define("enablePhantomPickup", true);
    private static final ModConfigSpec.DoubleValue PHANTOM_PICKUP_CHANCE = BUILDER
            .comment("Chance for a phantom to attempt picking up a player when attacking (0.0-1.0).")
            .defineInRange("phantomPickupChance", 0.3D, 0.0D, 1.0D);
    private static final ModConfigSpec.IntValue PHANTOM_PICKUP_MIN_DELAY = BUILDER
            .comment("Minimum delay in seconds before player can dismount from phantom.")
            .defineInRange("phantomPickupMinDelay", 1, 0, 10);
    private static final ModConfigSpec.DoubleValue PHANTOM_PICKUP_SPEED_MULTIPLIER = BUILDER
            .comment("Speed multiplier for phantoms when carrying a player (1.0 = normal speed, 0.5 = half speed).")
            .defineInRange("phantomPickupSpeedMultiplier", 0.5D, 0.1D, 1.0D);
    static { BUILDER.pop(); }

    // Phantom slowness stacking
    static { BUILDER.comment("Phantom Slowness Stacking"); BUILDER.push("phantomSlowness"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_SLOWNESS_STACKING = BUILDER
            .comment("Enable stacking slowness effect when hit by phantoms.")
            .define("enablePhantomSlownessStacking", true);
    private static final ModConfigSpec.IntValue PHANTOM_SLOWNESS_DURATION = BUILDER
            .comment("Duration of slowness effect in seconds.")
            .defineInRange("phantomSlownessDuration", 10, 1, 60);
    private static final ModConfigSpec.IntValue PHANTOM_SLOWNESS_MAX_LEVEL = BUILDER
            .comment("Maximum slowness level that can be stacked (1-4).")
            .defineInRange("phantomSlownessMaxLevel", 4, 1, 4);
    static { BUILDER.pop(); }

    // Double damage when carried
    static { BUILDER.comment("Double Damage When Carried"); BUILDER.push("phantomDoubleDamage"); }
    private static final ModConfigSpec.BooleanValue ENABLE_PHANTOM_DOUBLE_DAMAGE = BUILDER
            .comment("Enable double damage when hitting a phantom while being carried by it.")
            .define("enablePhantomDoubleDamage", true);
    static { BUILDER.pop(); }
    
    // Better Combat integration
    static { BUILDER.comment("Better Combat Integration"); BUILDER.push("betterCombatIntegration"); }
    private static final ModConfigSpec.BooleanValue ENABLE_BETTER_COMBAT_INTEGRATION = BUILDER
            .comment("Enable Better Combat mod integration for enhanced phantom combat mechanics. Only takes effect if Better Combat is installed.")
            .define("enableBetterCombatIntegration", true);
    static { BUILDER.pop(); }
    

    static { BUILDER.pop(); } // improvedPhantoms

    // Iron Golems vs Creepers
    private static final ModConfigSpec.BooleanValue IRON_GOLEMS_KILL_CREEPERS = BUILDER
            .comment("Enable: Iron Golems will actively target and kill Creepers. Creepers will not retaliate/ignite when attacked by Iron Golems.")
            .define("ironGolemsKillCreepers", true);

    static { BUILDER.pop(); } // mobs

    public static final ModConfigSpec SPEC = BUILDER.build();


    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        Config.enableCaveSpidersInCaves = ENABLE_CAVE_SPIDERS_IN_CAVES.get();
        Config.caveSpiderReplacementChance = CAVE_SPIDER_REPLACEMENT_CHANCE.get();

        Config.disableNitwits = DISABLE_NITWITS.get();

        // Load wandering trader settings
        Config.enableImprovedWanderingTraders = ENABLE_IMPROVED_WANDERING_TRADERS.get();
        Config.replaceVanillaTrades = REPLACE_VANILLA_TRADES.get();
        
        // Load category toggles
        Config.enableBuildingMaterialsTrades = ENABLE_BUILDING_MATERIALS_TRADES.get();
        Config.enableRareItemsTrades = ENABLE_RARE_ITEMS_TRADES.get();
        Config.enableUtilityItemsTrades = ENABLE_UTILITY_ITEMS_TRADES.get();
        Config.enableDecorativeItemsTrades = ENABLE_DECORATIVE_ITEMS_TRADES.get();
        
        // Load improved phantoms settings
        Config.enableImprovedPhantoms = ENABLE_IMPROVED_PHANTOMS.get();
        Config.enablePhantomPickup = ENABLE_PHANTOM_PICKUP.get();
        Config.phantomPickupChance = PHANTOM_PICKUP_CHANCE.get();
        Config.phantomPickupMinDelay = PHANTOM_PICKUP_MIN_DELAY.get();
        Config.phantomPickupSpeedMultiplier = PHANTOM_PICKUP_SPEED_MULTIPLIER.get();
        Config.enablePhantomSlownessStacking = ENABLE_PHANTOM_SLOWNESS_STACKING.get();
        Config.phantomSlownessDuration = PHANTOM_SLOWNESS_DURATION.get();
        Config.phantomSlownessMaxLevel = PHANTOM_SLOWNESS_MAX_LEVEL.get();
        Config.enablePhantomDoubleDamage = ENABLE_PHANTOM_DOUBLE_DAMAGE.get();
        Config.enableBetterCombatIntegration = ENABLE_BETTER_COMBAT_INTEGRATION.get();

        // Iron Golems vs Creepers
        Config.enableIronGolemsKillCreepers = IRON_GOLEMS_KILL_CREEPERS.get();
        
        // Individual trade toggles will be loaded here as we add them
    }
}
