package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.crafting.RecipeManager;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.StepStonecuttingRecipeGenerator;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.recipe.impl.BaseBlockRecoveryRecipe;
import org.onenonly.bitsandbalance.fabric.recipe.impl.StepConvertRecipe;
import org.onenonly.bitsandbalance.fabric.recipe.impl.StepFromSlabRecipe;
import org.onenonly.bitsandbalance.fabric.recipe.impl.VerticalStepFromVerticalSlabRecipe;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricStepRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATAPACK_NAME = "bitsandbalance_steps";
    private static final String PACK_MCMETA = """
            {
                            "pack": {
                                "description": "Bits and Balance: Step Recipes",
                                "pack_format": 88,
                                "min_format": 88,
                                "max_format": 88
              }
            }
            """;

        private static final String BAMBOO_MOSAIC_RECIPE = """
                        {
                            "type": "minecraft:crafting_shaped",
                            "pattern": [
                                "A ",
                                " A"
                            ],
                            "key": {
                                "A": "minecraft:bamboo_slab"
                            },
                            "result": {
                                "id": "minecraft:bamboo_mosaic",
                                "count": 1
                            }
                        }
                        """;

    private FabricStepRecipes() {
    }

    static String packName() {
        return DATAPACK_NAME;
    }

    static String packMcmeta() {
        return PACK_MCMETA;
    }

    public static void init() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, StepFromSlabRecipe.SERIALIZER_ID, StepFromSlabRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, VerticalStepFromVerticalSlabRecipe.SERIALIZER_ID, VerticalStepFromVerticalSlabRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, StepConvertRecipe.SERIALIZER_ID, StepConvertRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BaseBlockRecoveryRecipe.SERIALIZER_ID, BaseBlockRecoveryRecipe.Serializer.INSTANCE);
    }

    public static boolean prepareWorldDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        return syncDatapack(worldRoot, packRepository, recipeManager, registryAccess, false);
    }

    private static boolean syncDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess, boolean reloadAfterSync) throws IOException {
        Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(DATAPACK_NAME);
        FabricGeneratedWorldDataPacks.ensurePackRoot(DATAPACK_NAME);

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, PACK_MCMETA, Map.of());
            if (result == DynamicRecipeDatapackSync.Result.REMOVED) {
                LOGGER.info("[{}] Removed step recipe datapack because Steps and Vertical Steps are disabled", BitsAndBalanceCommon.MOD_ID);
            }
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildStepFingerprint(recipeManager, registryAccess);
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Step recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(StepStonecuttingRecipeGenerator.generate(recipeManager, registryAccess));
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No step stonecutting recipes to generate", BitsAndBalanceCommon.MOD_ID);
            return false;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        files.put("data/minecraft/recipe/bamboo_mosaic.json", BAMBOO_MOSAIC_RECIPE);

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} step stonecutting recipes", BitsAndBalanceCommon.MOD_ID, recipes.size());
        } else {
            LOGGER.info("[{}] Step recipe datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }
        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }
}
