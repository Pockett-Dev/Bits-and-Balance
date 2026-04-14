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
import org.onenonly.bitsandbalance.common.recipe.WoodStonecuttingRecipeGenerator;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dynamically generates stonecutter recipes for all wood-type blocks discovered in the item
 * registry at server start.  Covers every mod that follows standard naming conventions.
 *
 * <p>Recipes are written to the {@code bitsandbalance_wood_stonecutting} world-local data pack
 * and activated via {@code /reload}.  The data pack is regenerated fresh on every server start
 * to pick up mod-list changes.</p>
 *
 * @see WoodStonecuttingRecipeGenerator
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public final class WoodStonecuttingRecipes {

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

    private WoodStonecuttingRecipes() {}

    static String packName() {
        return DATAPACK_NAME;
    }

    static String packMcmeta() {
        return PACK_MCMETA;
    }

    static boolean anyRecipesEnabledForBootstrap() {
        return true;
    }

    static String inputFingerprintForBootstrap() {
        return DynamicRecipeInputFingerprints.buildWoodStonecuttingFingerprint(
                Config.enhancedSlabsVerticalSlabs,
                false
        );
    }

    static Map<String, String> buildDatapackFilesForBootstrap() {
        Map<String, String> recipes = WoodStonecuttingRecipeGenerator.generate(
                Config.enhancedSlabsVerticalSlabs,
                false
        );
        Map<String, String> files = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : recipes.entrySet()) {
            files.put("data/bitsandbalance/recipe/" + entry.getKey() + ".json", entry.getValue());
        }
        return files;
    }

    public static boolean prepareWorldDatapack(MinecraftServer server) throws IOException {
        return syncDatapack(server, false);
    }

    // ── Datapack management ──────────────────────────────────────────────────

    private static boolean syncDatapack(MinecraftServer server, boolean reloadAfterSync) throws IOException {
        Path datapackPath = NeoForgeGeneratedServerDataPacks.getPackPath(DATAPACK_NAME);
        String inputFingerprint = inputFingerprintForBootstrap();

        if (DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
            LOGGER.info("[{}] Wood stonecutting inputs unchanged; skipped regeneration", BitsAndBalance.MODID);
            NeoForgeGeneratedServerDataPacks.writeBootstrapFingerprint(DATAPACK_NAME, NeoForgeGeneratedServerDataPacks.buildWoodBootstrapFingerprint());
            return false;
        }

        Map<String, String> files = buildDatapackFilesForBootstrap();
        if (files.isEmpty()) {
            LOGGER.info("[{}] No wood stonecutting recipes generated (no matching items found)", BitsAndBalance.MODID);
            return false;
        }

        DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(datapackPath, PACK_MCMETA, files, inputFingerprint);
        NeoForgeGeneratedServerDataPacks.writeBootstrapFingerprint(DATAPACK_NAME, NeoForgeGeneratedServerDataPacks.buildWoodBootstrapFingerprint());
        if (result == DynamicRecipeDatapackSync.Result.WRITTEN) {
            LOGGER.info("[{}] Generated {} wood stonecutting recipe files", BitsAndBalance.MODID, files.size());
        } else {
            LOGGER.info("[{}] Wood stonecutting datapack unchanged; skipped rewrite", BitsAndBalance.MODID);
        }
        if (reloadAfterSync) {
            reloadWithCommand(server);
        }
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    private static void reloadWithCommand(MinecraftServer server) {
        try {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
        } catch (Exception e) {
            LOGGER.warn("[{}] Could not reload resources for wood stonecutting: {}", BitsAndBalance.MODID, e.getMessage());
        }
    }

}
