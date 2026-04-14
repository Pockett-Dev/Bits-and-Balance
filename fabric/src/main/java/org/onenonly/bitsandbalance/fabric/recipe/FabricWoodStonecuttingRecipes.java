package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.server.packs.repository.PackRepository;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.WoodStonecuttingRecipeGenerator;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabric-side trigger for {@link WoodStonecuttingRecipeGenerator}.
 * Writes the generated recipes into the shared runtime server-data cache.
 *
 * @see WoodStonecuttingRecipeGenerator
 */
public final class FabricWoodStonecuttingRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATAPACK_NAME = "bitsandbalance_wood_stonecutting";

    private static final String PACK_MCMETA = """
            {
              "pack": {
                "description": "Bits and Balance: Wood Stonecutting Recipes",
                "pack_format": 88,
                "min_format": 88,
                "max_format": 88
              }
            }
            """;

    private FabricWoodStonecuttingRecipes() {}

    static String packName() {
        return DATAPACK_NAME;
    }

    static String packMcmeta() {
        return PACK_MCMETA;
    }

    public static void init() {
    }

    public static boolean prepareWorldDatapack(Path worldRoot, PackRepository packRepository) throws IOException {
        return syncDatapack(worldRoot, packRepository, false);
    }

    // ── Datapack management ──────────────────────────────────────────────────

    private static boolean syncDatapack(Path worldRoot, PackRepository packRepository, boolean reloadAfterSync) throws IOException {
        Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(DATAPACK_NAME);
        FabricGeneratedWorldDataPacks.ensurePackRoot(DATAPACK_NAME);
        String inputFingerprint = DynamicRecipeInputFingerprints.buildWoodStonecuttingFingerprint(
            FabricBuildingConfig.enhancedSlabsVerticalSlabs,
            false
        );

        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Wood stonecutting inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
            FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, DATAPACK_NAME, false);
            return false;
        }

        Map<String, String> recipes = WoodStonecuttingRecipeGenerator.generate(
            FabricBuildingConfig.enhancedSlabsVerticalSlabs,
            false
        );
        if (recipes.isEmpty()) {
            LOGGER.info("[{}] No wood stonecutting recipes generated (no matching items found)", BitsAndBalanceCommon.MOD_ID);
            return false;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} wood stonecutting recipes", BitsAndBalanceCommon.MOD_ID, recipes.size());
        } else {
            LOGGER.info("[{}] Wood stonecutting datapack unchanged; skipped rewrite", BitsAndBalanceCommon.MOD_ID);
        }

        FabricGeneratedWorldDataPacks.enableGeneratedPack(packRepository, DATAPACK_NAME, result == DynamicRecipeDatapackSync.Result.WRITTEN);
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }
}
