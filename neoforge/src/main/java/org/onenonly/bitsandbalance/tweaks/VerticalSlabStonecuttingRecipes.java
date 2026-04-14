package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.Config;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprints;
import org.onenonly.bitsandbalance.common.recipe.EnhancedSlabCraftingRecipeGenerator;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabStonecuttingRecipeGenerator;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dynamically generates stonecutter recipes for all registered vertical slab variants.
 *
 * <p>For every loaded {@code minecraft:stonecutting} recipe whose output is a slab block that has
 * a vertical-slab counterpart in {@link VerticalSlabDynamicRegistry}, a matching stonecutting
 * recipe is generated that produces the corresponding vertical slab (same count).  The generated
 * recipes are written to a world-folder data pack and activated via a {@code /reload}.</p>
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class VerticalSlabStonecuttingRecipes {

    private static final Logger LOGGER = LogUtils.getLogger();
        private static final String STATIC_DATAPACK_NAME = "bitsandbalance_vertical_slabs_static";
        private static final String MIRROR_DATAPACK_NAME = "bitsandbalance_vertical_slabs_mirrors";

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

    private VerticalSlabStonecuttingRecipes() {}

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
        return Config.enhancedSlabsVerticalSlabs;
    }

    public static boolean prepareWorldDatapack(MinecraftServer server) throws IOException {
        boolean changed = prepareStaticDatapack();
        changed |= prepareMirrorDatapack(server);
        return changed;
    }

    public static boolean prepareStaticDatapack() throws IOException {
        Path datapackPath = NeoForgeGeneratedServerDataPacks.getPackPath(STATIC_DATAPACK_NAME);

        if (!anyRecipesEnabledForBootstrap()) {
            boolean changed = NeoForgeGeneratedServerDataPacks.clearPack(STATIC_DATAPACK_NAME, STATIC_PACK_MCMETA);
            if (changed) {
                LOGGER.info("[{}] Removed vertical slab static recipe datapack because Vertical Slabs are disabled", BitsAndBalance.MODID);
            }
            return changed;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildVerticalSlabStaticFingerprint();
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Vertical slab static recipe inputs unchanged; skipped regeneration", BitsAndBalance.MODID);
            return false;
        }

        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : EnhancedSlabCraftingRecipeGenerator.generateVerticalSlabRecipes().entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        if (files.isEmpty()) {
            LOGGER.info("[{}] No vertical slab static recipes to generate", BitsAndBalance.MODID);
            return false;
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, STATIC_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} vertical slab static recipe files", BitsAndBalance.MODID, files.size());
        } else {
            LOGGER.info("[{}] Vertical slab static datapack unchanged; skipped rewrite", BitsAndBalance.MODID);
        }
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    public static boolean prepareMirrorDatapack(MinecraftServer server) throws IOException {
        return syncMirrorDatapack(server, false);
    }

    private static boolean syncMirrorDatapack(MinecraftServer server, boolean reloadAfterSync) throws IOException {
        Path datapackPath = NeoForgeGeneratedServerDataPacks.getPackPath(MIRROR_DATAPACK_NAME);

        if (!anyRecipesEnabledForBootstrap()) {
            boolean changed = NeoForgeGeneratedServerDataPacks.clearPack(MIRROR_DATAPACK_NAME, MIRROR_PACK_MCMETA);
            if (changed) {
                LOGGER.info("[{}] Removed vertical slab mirror recipe datapack because Vertical Slabs are disabled", BitsAndBalance.MODID);
            }
            return changed;
        }

        String inputFingerprint = DynamicRecipeInputFingerprints.buildVerticalSlabMirrorFingerprint(server.getRecipeManager(), server.registryAccess());
        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Vertical slab mirror recipe inputs unchanged; skipped regeneration", BitsAndBalance.MODID);
            return false;
        }

        Map<String, String> recipes = new LinkedHashMap<>(VerticalSlabStonecuttingRecipeGenerator.generateStonecuttingMirrorRecipes(server.getRecipeManager(), server.registryAccess()));
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        if (files.isEmpty()) {
            LOGGER.info("[{}] No vertical slab mirror recipes to generate (no slab→vertical pairs found)", BitsAndBalance.MODID);
            return false;
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, MIRROR_PACK_MCMETA, files, inputFingerprint);
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} vertical slab mirror recipe files", BitsAndBalance.MODID, files.size());
        } else {
            LOGGER.info("[{}] Vertical slab mirror datapack unchanged; skipped rewrite", BitsAndBalance.MODID);
        }
        if (reloadAfterSync) {
            reloadWithCommand(server);
        }
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    // ── Data-pack helpers ────────────────────────────────────────────────

    private static void reloadWithCommand(MinecraftServer server) {
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        } catch (Exception e) {
            LOGGER.warn("[{}] Could not reload resources for vertical slab stonecutting: {}", BitsAndBalance.MODID, e.getMessage());
        }
    }

}
