package org.onenonly.bitsandbalance;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class RecipesConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment("Recipes");
        BUILDER.push("recipes");
    }

    // Alternative Recipes
    static {
        BUILDER.comment("Alternative Recipes");
        BUILDER.push("alternativeRecipes");
    }
    
    // Crafting Recipes
    private static final ModConfigSpec.BooleanValue ENABLE_ALTERNATIVE_REPEATER_RECIPE = BUILDER
            .comment("Enable alternative crafting recipe for Redstone Repeater using sticks and stone.")
            .define("enableAlternativeRepeaterRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_CHEST_FROM_LOGS_RECIPE = BUILDER
            .comment("Enable alternative crafting recipe for Chest using logs (4 chests per recipe).")
            .define("enableChestFromLogsRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_MAP_INK_SAC_RECIPE = BUILDER
            .comment("Enable alternative crafting recipe for Map using ink sac instead of compass.")
            .define("enableMapInkSacRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_RECOVERY_COMPASS_RECIPE = BUILDER
            .comment("Enable alternative crafting recipe for Recovery Compass using echo shards and compass.")
            .define("enableRecoveryCompassRecipe", true);
    
    
    private static final ModConfigSpec.BooleanValue ENABLE_ENDER_EYE_RECIPE = BUILDER
            .comment("Enable alternative crafting recipe for Ender Eye using echo shard, wind charge, blaze powder, and ender pearl.")
            .define("enableEnderEyeRecipe", true);
    
    
    // Smelting Recipes
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_IRON_SMELTING_RECIPE = BUILDER
            .comment("Enable smelting recipe for raw iron block to iron block.")
            .define("enableRawIronSmeltingRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_GOLD_SMELTING_RECIPE = BUILDER
            .comment("Enable smelting recipe for raw gold block to gold block.")
            .define("enableRawGoldSmeltingRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_COPPER_SMELTING_RECIPE = BUILDER
            .comment("Enable smelting recipe for raw copper block to copper block.")
            .define("enableRawCopperSmeltingRecipe", true);
    
    // Blasting Recipes
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_IRON_BLASTING_RECIPE = BUILDER
            .comment("Enable blasting recipe for raw iron block to iron block (faster than smelting).")
            .define("enableRawIronBlastingRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_GOLD_BLASTING_RECIPE = BUILDER
            .comment("Enable blasting recipe for raw gold block to gold block (faster than smelting).")
            .define("enableRawGoldBlastingRecipe", true);
    
    private static final ModConfigSpec.BooleanValue ENABLE_RAW_COPPER_BLASTING_RECIPE = BUILDER
            .comment("Enable blasting recipe for raw copper block to copper block (faster than smelting).")
            .define("enableRawCopperBlastingRecipe", true);
    
    // Stair Recipe Override
    private static final ModConfigSpec.BooleanValue ENABLE_STAIR_RECIPE_OVERRIDE = BUILDER
            .comment("Override ALL stair recipes (vanilla, modded, and custom) to output 8 stairs instead of 4.")
            .define("enableStairRecipeOverride", true);
    
    // Compact 2x2 Stair Recipe
    private static final ModConfigSpec.BooleanValue ENABLE_COMPACT_STAIR_RECIPE = BUILDER
            .comment("Add compact 2x2 stair recipes for ALL stair types (3 blocks in corner = 4 stairs). Works with any mod!")
            .define("enableCompactStairRecipe", true);
    
    static {
        BUILDER.pop();
    }

    // Fuel Tweaks
    static {
        BUILDER.comment("Fuel Tweaks");
        BUILDER.push("fuelTweaks");
    }
    
    private static final ModConfigSpec.BooleanValue ENABLE_TORCH_FUEL = BUILDER
            .comment("Enable torch as fuel for furnaces.")
            .define("enableTorchFuel", true);
    
    private static final ModConfigSpec.IntValue TORCH_BURN_TIME = BUILDER
            .comment("Burn time for torches in ticks (400 ticks = 20 seconds).")
            .defineInRange("torchBurnTime", 400, 0, 10000);
    
    static {
        BUILDER.pop();
    }

    static {
        BUILDER.pop();
    }

    public static final ModConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) return;
        
        // Alternative Recipes
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
        
        // Fuel Tweaks
        Config.enableTorchFuel = ENABLE_TORCH_FUEL.get();
        Config.torchBurnTime = TORCH_BURN_TIME.get();
    }
}
