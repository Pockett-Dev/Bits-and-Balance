package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.item.crafting.RecipeManager;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.BaseBlockRecoveryRecipe;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.FamilyCompatRecipeGenerator;
import org.onenonly.bitsandbalance.common.recipe.StepStonecuttingRecipeGenerator;
import org.onenonly.bitsandbalance.common.recipe.StepConvertRecipe;
import org.onenonly.bitsandbalance.common.recipe.StepFromSlabRecipe;
import org.onenonly.bitsandbalance.common.recipe.VerticalStepFromVerticalSlabRecipe;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricStepRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String STATIC_DATAPACK_NAME = "bitsandbalance_steps_static";
    private static final String MIRROR_DATAPACK_NAME = "bitsandbalance_steps";
    private static final String STATIC_PACK_MCMETA = """
            {
                            "pack": {
                                "description": "Bits and Balance: Step Static Recipes",
                                "pack_format": 88,
                                "min_format": 88,
                                "max_format": 88
              }
            }
            """;
    private static final String MIRROR_PACK_MCMETA = """
            {
                            "pack": {
                                "description": "Bits and Balance: Step Stonecutting Mirrors",
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
        return mirrorPackName();
    }

    static String staticPackName() {
        return STATIC_DATAPACK_NAME;
    }

    static String mirrorPackName() {
        return MIRROR_DATAPACK_NAME;
    }

    static String packMcmeta() {
        return mirrorPackMcmeta();
    }

    static String staticPackMcmeta() {
        return STATIC_PACK_MCMETA;
    }

    static String mirrorPackMcmeta() {
        return MIRROR_PACK_MCMETA;
    }

    public static void init() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, StepFromSlabRecipe.SERIALIZER_ID, StepFromSlabRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, VerticalStepFromVerticalSlabRecipe.SERIALIZER_ID, VerticalStepFromVerticalSlabRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, StepConvertRecipe.SERIALIZER_ID, StepConvertRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, BaseBlockRecoveryRecipe.SERIALIZER_ID, BaseBlockRecoveryRecipe.Serializer.INSTANCE);
    }

    public static boolean prepareBeforeWorldStemDatapack(Path worldRoot, PackRepository packRepository) throws IOException {
        return syncStaticDatapack(packRepository);
    }

    public static boolean prepareWorldDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        return syncStaticDatapack(packRepository);
    }

    public static boolean prepareMirrorDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        return syncMirrorDatapack(packRepository, recipeManager, registryAccess);
    }

    private static boolean syncStaticDatapack(PackRepository packRepository) throws IOException {
        Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(STATIC_DATAPACK_NAME);
        FabricGeneratedWorldDataPacks.ensurePackRoot(STATIC_DATAPACK_NAME);

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, Map.of());
            if (result == DynamicRecipeDatapackSync.Result.REMOVED) {
                LOGGER.info("[{}] Removed step static recipe datapack because Steps and Vertical Steps are disabled", BitsAndBalanceCommon.MOD_ID);
            }
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildStepStaticFingerprint();
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Step static recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(FamilyCompatRecipeGenerator.generateStaticStepAndRecoveryRecipes(FabricBuildingConfig.enhancedSlabsVerticalSlabs));
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No step static recipes to generate", BitsAndBalanceCommon.MOD_ID);
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, Map.of());
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        files.put("data/minecraft/recipe/bamboo_mosaic.json", BAMBOO_MOSAIC_RECIPE);

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} step static recipe files", BitsAndBalanceCommon.MOD_ID, files.size());
        } else {
            LOGGER.info("[{}] Step static datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }
        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    private static boolean syncMirrorDatapack(PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(MIRROR_DATAPACK_NAME);
        FabricGeneratedWorldDataPacks.ensurePackRoot(MIRROR_DATAPACK_NAME);

        if (!FabricBuildingConfig.enhancedSlabsSteps) {
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, MIRROR_PACK_MCMETA, Map.of());
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildStepMirrorFingerprint(recipeManager, registryAccess);
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Step mirror recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(StepStonecuttingRecipeGenerator.generateMirroredStonecuttingRecipes(
                recipeManager,
                registryAccess,
                FabricBuildingConfig.enhancedSlabsVerticalSlabs
        ));
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No step mirror recipes to generate", BitsAndBalanceCommon.MOD_ID);
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, MIRROR_PACK_MCMETA, Map.of());
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, MIRROR_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} step mirror recipe files", BitsAndBalanceCommon.MOD_ID, files.size());
        } else {
            LOGGER.info("[{}] Step mirror datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }
        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }
}
