package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.crafting.RecipeManager;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabStonecuttingRecipeGenerator;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.recipe.impl.VerticalSlabConvertRecipe;
import org.onenonly.bitsandbalance.fabric.recipe.impl.VerticalSlabFromPlanksRecipe;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricVerticalSlabRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATAPACK_NAME = "bitsandbalance_vertical_slabs";
    private static final String PACK_MCMETA = """
            {
              "pack": {
                "description": "Bits and Balance: Vertical Slab Stonecutting Recipes",
                "pack_format": 88,
                "min_format": 88,
                "max_format": 88
              }
            }
            """;

    private FabricVerticalSlabRecipes() {
    }

    static String packName() {
        return DATAPACK_NAME;
    }

    static String packMcmeta() {
        return PACK_MCMETA;
    }

    @SuppressWarnings({"unused", "null"})
    public static void init() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, VerticalSlabFromPlanksRecipe.SERIALIZER_ID, VerticalSlabFromPlanksRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, VerticalSlabConvertRecipe.SERIALIZER_ID, VerticalSlabConvertRecipe.Serializer.INSTANCE);
    }

    public static boolean prepareWorldDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        return syncDatapack(worldRoot, packRepository, recipeManager, registryAccess, false);
    }

    private static boolean syncDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess, boolean reloadAfterSync) throws IOException {
        Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(DATAPACK_NAME);
        FabricGeneratedWorldDataPacks.ensurePackRoot(DATAPACK_NAME);

        if (!FabricBuildingConfig.enhancedSlabsVerticalSlabs) {
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, PACK_MCMETA, Map.of());
            if (result == DynamicRecipeDatapackSync.Result.REMOVED) {
                LOGGER.info("[{}] Removed vertical slab recipe datapack because Vertical Slabs are disabled", BitsAndBalanceCommon.MOD_ID);
            }
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildVerticalSlabFingerprint(recipeManager, registryAccess);
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Vertical slab recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(VerticalSlabStonecuttingRecipeGenerator.generate(recipeManager, registryAccess));
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No vertical slab stonecutting recipes to generate", BitsAndBalanceCommon.MOD_ID);
            return false;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} vertical slab stonecutting recipes", BitsAndBalanceCommon.MOD_ID, recipes.size());
        } else {
            LOGGER.info("[{}] Vertical slab datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }
        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }
}

