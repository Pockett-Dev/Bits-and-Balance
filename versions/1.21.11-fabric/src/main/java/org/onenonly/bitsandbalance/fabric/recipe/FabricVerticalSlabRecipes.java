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
import org.onenonly.bitsandbalance.common.recipe.FamilyCompatRecipeGenerator;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabStonecuttingRecipeGenerator;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabConvertRecipe;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabFromPlanksRecipe;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class FabricVerticalSlabRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();
                private static final String STATIC_DATAPACK_NAME = "bitsandbalance_vertical_slabs_static";
                private static final String MIRROR_DATAPACK_NAME = "bitsandbalance_vertical_slabs";
                private static final String STATIC_PACK_MCMETA = """
                                                {
                                                        "pack": {
                                                                "description": "Bits and Balance: Vertical Slab Static Recipes",
                                                                "pack_format": 88,
                                                                "min_format": 88,
                                                                "max_format": 88
                                                        }
                                                }
                                                """;
                private static final String MIRROR_PACK_MCMETA = """
                        {
                            "pack": {
                                                                "description": "Bits and Balance: Vertical Slab Stonecutting Mirrors",
                                "pack_format": 88,
                                "min_format": 88,
                                "max_format": 88
                            }
                        }
                        """;

    private FabricVerticalSlabRecipes() {
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

    @SuppressWarnings({"unused", "null"})
    public static void init() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, VerticalSlabFromPlanksRecipe.SERIALIZER_ID, VerticalSlabFromPlanksRecipe.Serializer.INSTANCE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, VerticalSlabConvertRecipe.SERIALIZER_ID, VerticalSlabConvertRecipe.Serializer.INSTANCE);
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

        if (!FabricBuildingConfig.enhancedSlabsVerticalSlabs) {
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, Map.of());
            if (result == DynamicRecipeDatapackSync.Result.REMOVED) {
                LOGGER.info("[{}] Removed vertical slab static recipe datapack because Vertical Slabs are disabled", BitsAndBalanceCommon.MOD_ID);
            }
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildVerticalSlabStaticFingerprint();
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Vertical slab static recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(FamilyCompatRecipeGenerator.generateStaticVerticalSlabRecipes());
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No vertical slab static recipes to generate", BitsAndBalanceCommon.MOD_ID);
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, Map.of());
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} vertical slab static recipe files", BitsAndBalanceCommon.MOD_ID, files.size());
        } else {
            LOGGER.info("[{}] Vertical slab static datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }
        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, STATIC_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    private static boolean syncMirrorDatapack(PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(MIRROR_DATAPACK_NAME);
        FabricGeneratedWorldDataPacks.ensurePackRoot(MIRROR_DATAPACK_NAME);

        if (!FabricBuildingConfig.enhancedSlabsVerticalSlabs) {
            DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, MIRROR_PACK_MCMETA, Map.of());
            FabricGeneratedWorldDataPacks.disableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.REMOVED);
            return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildVerticalSlabMirrorFingerprint(recipeManager, registryAccess);
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Vertical slab mirror recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(VerticalSlabStonecuttingRecipeGenerator.generateStonecuttingMirrorRecipes(recipeManager, registryAccess));
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No vertical slab mirror recipes to generate", BitsAndBalanceCommon.MOD_ID);
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
            LOGGER.info("[{}] Generated {} vertical slab mirror recipe files", BitsAndBalanceCommon.MOD_ID, files.size());
        } else {
            LOGGER.info("[{}] Vertical slab mirror datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }
        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, MIRROR_DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }
}

