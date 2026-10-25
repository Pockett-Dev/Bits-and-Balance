package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeManager;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.FamilyCompatRecipeGenerator;
import org.onenonly.bitsandbalance.common.recipe.LegacyRecipeJsonCompat;
import org.onenonly.bitsandbalance.common.recipe.StepStonecuttingRecipeGenerator;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StepStonecuttingRecipes {
    private static final String RECIPE_SCHEMA_VERSION = "legacy_recipe_schema_v4";

    private static final Logger LOGGER = LogUtils.getLogger();
        private static final String STATIC_DATAPACK_NAME = "bitsandbalance_steps_static";
        private static final String MIRROR_DATAPACK_NAME = "bitsandbalance_steps_mirrors";

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

    private StepStonecuttingRecipes() {}

    static String staticPackName() {
        return STATIC_DATAPACK_NAME;
    }

    static String staticPackMcmeta() {
        return STATIC_PACK_MCMETA;
    }

    static String mirrorPackName() {
        return MIRROR_DATAPACK_NAME;
    }

    static String mirrorPackMcmeta() {
        return MIRROR_PACK_MCMETA;
    }

    static boolean anyRecipesEnabledForBootstrap() {
        return Config.enhancedSlabsSteps;
    }

    public static boolean prepareWorldDatapack(MinecraftServer server) throws IOException {
        boolean changed = prepareStaticDatapack();
        changed |= prepareMirrorDatapack(server);
        return changed;
    }

    public static boolean prepareStaticDatapack() throws IOException {
        Path datapackPath = NeoForgeGeneratedServerDataPacks.getPackPath(STATIC_DATAPACK_NAME);
        boolean normalizedExisting = LegacyRecipeJsonCompat.normalizeDatapackInPlace(datapackPath);

        if (!anyRecipesEnabledForBootstrap()) {
            boolean changed = NeoForgeGeneratedServerDataPacks.clearPack(STATIC_DATAPACK_NAME, STATIC_PACK_MCMETA);
            if (changed) {
                LOGGER.info("[{}] Removed step static recipe datapack because Steps and Vertical Steps are disabled", BitsAndBalance.MODID);
            }
            return changed;
        }

        String inputFingerprint = RECIPE_SCHEMA_VERSION + "\n" + DynamicRecipeInputFingerprints.buildStepStaticFingerprint();
        if (!normalizedExisting && DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Step static recipe inputs unchanged; skipped regeneration", BitsAndBalance.MODID);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(StepStonecuttingRecipeGenerator.generateDirectStonecuttingRecipes());
        recipes.putAll(FamilyCompatRecipeGenerator.generateStaticStepAndRecoveryRecipes(Config.enhancedSlabsVerticalSlabs));
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        files.put("data/minecraft/recipe/bamboo_mosaic.json", BAMBOO_MOSAIC_RECIPE);
        files = new LinkedHashMap<>(LegacyRecipeJsonCompat.normalizeFiles(files));
        if (files.isEmpty()) {
            LOGGER.info("[{}] No step static recipes generated", BitsAndBalance.MODID);
            return false;
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} step static recipe files", BitsAndBalance.MODID, files.size());
        } else {
            LOGGER.info("[{}] Step static datapack unchanged; skipped rewrite", BitsAndBalance.MODID);
        }
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    public static boolean prepareMirrorDatapack(MinecraftServer server) throws IOException {
        return syncMirrorDatapack(server.getRecipeManager(), server.registryAccess());
    }

    public static boolean prepareMirrorDatapack(RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        return syncMirrorDatapack(recipeManager, registryAccess);
    }

    private static boolean syncMirrorDatapack(RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        Path datapackPath = NeoForgeGeneratedServerDataPacks.getPackPath(MIRROR_DATAPACK_NAME);
        boolean normalizedExisting = LegacyRecipeJsonCompat.normalizeDatapackInPlace(datapackPath);

        if (!anyRecipesEnabledForBootstrap()) {
            boolean changed = NeoForgeGeneratedServerDataPacks.clearPack(MIRROR_DATAPACK_NAME, MIRROR_PACK_MCMETA);
            if (changed) {
                LOGGER.info("[{}] Removed step mirror recipe datapack because Steps and Vertical Steps are disabled", BitsAndBalance.MODID);
            }
            return changed;
        }

        String inputFingerprint = RECIPE_SCHEMA_VERSION + "\n" + DynamicRecipeInputFingerprints.buildStepMirrorFingerprint(recipeManager, registryAccess);
        if (!normalizedExisting && DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Step mirror recipe inputs unchanged; skipped regeneration", BitsAndBalance.MODID);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(StepStonecuttingRecipeGenerator.generateMirroredStonecuttingRecipes(recipeManager, registryAccess));
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        files = new LinkedHashMap<>(LegacyRecipeJsonCompat.normalizeFiles(files));
        if (files.isEmpty()) {
            LOGGER.info("[{}] No step mirror recipes generated", BitsAndBalance.MODID);
            return false;
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, MIRROR_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} step mirror recipe files", BitsAndBalance.MODID, files.size());
        } else {
            LOGGER.info("[{}] Step mirror datapack unchanged; skipped rewrite", BitsAndBalance.MODID);
        }
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    private static void reloadWithCommand(MinecraftServer server) {
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        } catch (Exception e) {
            LOGGER.warn("[{}] Could not reload resources for step recipes: {}", BitsAndBalance.MODID, e.getMessage());
        }
    }

}
