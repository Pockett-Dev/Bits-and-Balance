package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;

/**
 * Configuration for world generation and feature toggles.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class WorldConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // ========================================
    // AZALEA WOODSET
    // ========================================
    static { BUILDER.comment("Azalea Woodset Features"); BUILDER.push("azaleaWoodset"); }
    
    public static final ModConfigSpec.BooleanValue ENABLE_AZALEA_WOODSET = BUILDER
        .comment("Enable the azalea woodset (blocks, items, boats, recipes, etc.).",
                 "When disabled:",
                 "  - Items hidden from creative tabs",
                 "  - All crafting recipes disabled",
                 "  - No fuel values, flammability, or boat behavior",
                 "Note: Blocks/items still exist and can be obtained via commands. Requires restart.")
        .define("enableAzaleaWoodset", true);
    
    public static final ModConfigSpec.BooleanValue ENABLE_AZALEA_WOOD_GENERATION = BUILDER
        .comment("Enable azalea logs in naturally-generated and sapling-grown azalea trees.",
                 "When true: Azalea trees use azalea logs",
                 "When false: Azalea trees use vanilla oak logs",
                 "Changes take effect on server restart or /reload command.")
        .define("enableAzaleaWoodGeneration", true);
    
    static { BUILDER.pop(); }

    // ========================================
    // MUSIC DISCS
    // ========================================
    static { BUILDER.comment("Music Disc Features"); BUILDER.push("dogMusicDisc"); }

    public static final ModConfigSpec.BooleanValue ENABLE_DOG_MUSIC_DISC = BUILDER
        .comment("Enable the Dog music disc feature.",
                 "When disabled:",
                 "  - The disc is hidden from creative tabs",
                 "  - The disc will not be added to custom chest loot",
                 "  - Skeleton-killed creepers will never drop the Dog disc",
                 "Note: The item still exists and can be obtained via commands. Requires restart.")
        .define("enabled", true);

    static { BUILDER.pop(); }
    
    // ========================================
    // ORE VARIANTS
    // ========================================
    static { BUILDER.comment("Ore Variants Features"); BUILDER.push("oreVariants"); }
    
    public static final ModConfigSpec.BooleanValue ENABLE_ORE_VARIANTS = BUILDER
        .comment("Enable ore variants (andesite, diorite, granite, tuff variants for all vanilla ores).",
                 "When enabled:",
                 "  - Ore variant blocks are registered and available",
                 "  - Ore variants will replace vanilla ores in world generation when touching variant stone types",
                 "When disabled:",
                 "  - Ore variant blocks are hidden from creative tabs",
                 "  - No ore replacement occurs during world generation",
                 "Requires restart.")
        .define("enableOreVariants", true);
    
    static { BUILDER.pop(); }

    // ========================================
    // VEINS (BUILT-IN DATAPACK WORLDGEN)
    // ========================================
    static { BUILDER.comment("Vein Features"); BUILDER.push("netherGoldVeins"); }

    public static final ModConfigSpec.BooleanValue ENABLE_NETHER_GOLD_VEINS = BUILDER
        .comment("Enable nether gold vein generation in the Nether.",
                 "When enabled: Large nether gold ore deposits will generate",
                 "When disabled: No nether gold veins will generate",
                 "Note: Affects new chunk generation only.")
        .define("enableNetherGoldVeins", true);

    static { BUILDER.pop(); }

    static { BUILDER.comment("Vein Features"); BUILDER.push("netherQuartzVeins"); }

    public static final ModConfigSpec.BooleanValue ENABLE_NETHER_QUARTZ_VEINS = BUILDER
        .comment("Enable nether quartz vein generation in the Nether.",
                 "When enabled: Large nether quartz ore deposits will generate",
                 "When disabled: No nether quartz veins will generate",
                 "Note: Affects new chunk generation only.")
        .define("enableNetherQuartzVeins", true);

    public static final ModConfigSpec.BooleanValue ENABLE_RAW_QUARTZ_BLOCK_IN_QUARTZ_VEINS = BUILDER
        .comment("Enable raw quartz block placement inside nether quartz veins.",
                 "When enabled: Quartz veins may include bitsandbalance:raw_quartz_block pockets",
                 "When disabled: Quartz veins will never place bitsandbalance:raw_quartz_block (veins still generate quartz ore)",
                 "Note: Affects new chunk generation only.")
        .define("enableRawQuartzBlockInQuartzVeins", true);

    static { BUILDER.pop(); }

    static { BUILDER.comment("Raw Quartz Block"); BUILDER.push("rawQuartzBlock"); }

    public static final ModConfigSpec.BooleanValue ENABLE_RAW_QUARTZ_BLOCK = BUILDER
        .comment("Master toggle for the Block of Raw Quartz.",
                 "When disabled: Raw Quartz Block will not be placed in any worldgen (overrides enableRawQuartzBlockInQuartzVeins).",
                 "Note: Affects new chunk generation only.")
        .define("enabled", true);

    static { BUILDER.pop(); }

    static { BUILDER.comment("Vein Features"); BUILDER.push("coalAndesiteVeins"); }

    public static final ModConfigSpec.BooleanValue ENABLE_COAL_ANDESITE_VEIN = BUILDER
        .comment("Enable coal andesite vein generation in the Overworld.",
                 "When enabled: Large deposits of andesite with coal ore will generate",
                 "When disabled: No coal+andesite veins will generate",
                 "Note: Affects new chunk generation only.")
        .define("enableCoalAndesiteVein", true);

    public static final ModConfigSpec.BooleanValue ENABLE_COAL_TUFF_VEINS = BUILDER
        .comment("Enable deep coal+tuff vein generation.",
                 "When enabled: Deep underground will contain tuff+deepslate_coal_ore+coal_block pockets",
                 "When disabled: No deep coal+tuff veins will generate",
                 "Note: Affects new chunk generation only.")
        .define("enableCoalTuffVeins", true);

    public static final ModConfigSpec.BooleanValue DEBUG_VEINS = BUILDER
        .comment("Enable verbose debug logging for vein generation.",
                 "When enabled: Emits additional INFO logs from the Mojang-style coal vein wrapper.",
                 "When disabled: Suppresses those logs.")
        .define("debugVeins", false);

    static { BUILDER.pop(); }

    // ========================================
    // NETHER
    // ========================================
    static { BUILDER.comment("Nether Features"); BUILDER.push("nether"); }

    public static final ModConfigSpec.BooleanValue DISABLE_NETHER_LAVA_SPRINGS = BUILDER
        .comment("Disable random Nether lava springs (single lava source blocks in terrain).",
                 "When true: Vanilla lava spring placed features are suppressed.",
                 "When false: Vanilla behavior.",
                 "Note: Affects new chunk generation only.")
        .define("disableNetherLavaSprings", false);

    public static final ModConfigSpec.BooleanValue KEEP_EXPOSED_NETHER_LAVA_SPRINGS = BUILDER
        .comment("When disableNetherLavaSprings=true, keep any lava spring placement that is exposed to air on at least one side.",
                 "When true: Only hidden/enclosed lava springs are suppressed.",
                 "When false: All nether lava springs are suppressed.")
        .define("keepExposedNetherLavaSprings", true);

    static { BUILDER.pop(); }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
    
    @SubscribeEvent
    public static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        
        // Azalea Woodset
        Config.enableAzaleaWoodset = ENABLE_AZALEA_WOODSET.get();
        Config.enableAzaleaWoodGeneration = ENABLE_AZALEA_WOOD_GENERATION.get();
        Config.enableDogMusicDisc = ENABLE_DOG_MUSIC_DISC.get();
        
        // Ore Variants
        Config.enableOreVariants = ENABLE_ORE_VARIANTS.get();

        // Veins
        Config.enableNetherGoldVeins = ENABLE_NETHER_GOLD_VEINS.get();
        Config.enableNetherQuartzVeins = ENABLE_NETHER_QUARTZ_VEINS.get();
        Config.enableRawQuartzBlockInQuartzVeins = ENABLE_RAW_QUARTZ_BLOCK_IN_QUARTZ_VEINS.get();
        Config.enableRawQuartzBlock = ENABLE_RAW_QUARTZ_BLOCK.get();
        if (!Config.enableRawQuartzBlock) Config.enableRawQuartzBlockInQuartzVeins = false;
        Config.enableCoalAndesiteVein = ENABLE_COAL_ANDESITE_VEIN.get();
        Config.enableCoalAndesiteVeins = Config.enableCoalAndesiteVein;
        Config.enableCoalTuffVeins = ENABLE_COAL_TUFF_VEINS.get();
        Config.debugVeins = DEBUG_VEINS.get();
        BitsAndBalanceCommon.setVeinsDebugEnabled(Config.debugVeins);
        BitsAndBalanceCommon.setEnableRawQuartzBlockInQuartzVeins(Config.enableRawQuartzBlockInQuartzVeins);

        // Nether
        Config.disableNetherLavaSprings = DISABLE_NETHER_LAVA_SPRINGS.get();
        Config.keepExposedNetherLavaSprings = KEEP_EXPOSED_NETHER_LAVA_SPRINGS.get();

        BitsAndBalanceCommon.setDisableNetherLavaSprings(Config.disableNetherLavaSprings);
        BitsAndBalanceCommon.setKeepExposedNetherLavaSprings(Config.keepExposedNetherLavaSprings);
    }
}

