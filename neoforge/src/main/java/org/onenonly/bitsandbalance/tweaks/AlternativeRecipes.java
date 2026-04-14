package org.onenonly.bitsandbalance.tweaks;

import com.mojang.logging.LogUtils;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.Pack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.FamilyCompatRecipeGenerator;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.onenonly.bitsandbalance.Config;
/**
 * Alternative Recipes Management - Data Pack Edition
 * Dynamically creates/deletes data packs in world directories based on configuration.
 * Works in both development and production environments!
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class AlternativeRecipes {
    
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String DATAPACK_NAME = "bitsandbalance_dynamic";
    
    // Track players who need recipe updates
    private static final java.util.Set<String> playersNeedingRecipes = new java.util.concurrent.ConcurrentHashMap<String, Boolean>().keySet(Boolean.TRUE);
    
    // Helper method to check if any alternative recipes are enabled
    private static boolean anyRecipesEnabled() {
        return Config.enableAlternativeRepeaterRecipe ||
               Config.enableChestFromLogsRecipe ||
               Config.enableMapInkSacRecipe ||
               Config.enableRecoveryCompassRecipe ||
               Config.enableEnderEyeRecipe ||
               Config.enableRawIronSmeltingRecipe ||
               Config.enableRawGoldSmeltingRecipe ||
               Config.enableRawCopperSmeltingRecipe ||
               Config.enableRawIronBlastingRecipe ||
               Config.enableRawGoldBlastingRecipe ||
               Config.enableRawCopperBlastingRecipe ||
               Config.enableStairRecipeOverride ||
               Config.enableCompactStairRecipe;
    }
    
    
    // Data pack pack.mcmeta content
    private static final String PACK_MCMETA = """
        {
          "pack": {
                        "description": "Dynamic Bits and Balance Alternative Recipes",
                        "pack_format": 88,
                                                "min_format": 88,
                                                "max_format": 88
          }
        }
        """;

    static String packName() {
        return DATAPACK_NAME;
    }

    static String packMcmeta() {
        return PACK_MCMETA;
    }

    static boolean anyRecipesEnabledForBootstrap() {
        return anyRecipesEnabled();
    }

    static String inputFingerprintForBootstrap() {
        return buildInputFingerprint(null);
    }

    static Map<String, String> buildDatapackFilesForBootstrap() {
        return buildDatapackFiles(null);
    }
    
    // Recipe JSON content for all alternative recipes
    private static final String ALTERNATIVE_REPEATER_RECIPE = """
        {
          "type": "minecraft:crafting_shaped",
                    "category": "redstone",
          "pattern": [
            "R R",
            "SRS",
            "TTT"
          ],
          "key": {
                                                                                                "R": "minecraft:redstone",
                                                                                                "S": "minecraft:stick",
                                                                                                "T": "minecraft:stone"
          },
          "result": {
            "id": "minecraft:repeater",
            "count": 1
          }
        }
        """;
    
    private static final String CHEST_FROM_LOGS_RECIPE = """
        {
          "type": "minecraft:crafting_shaped",
                    "category": "misc",
          "pattern": [
            "LLL",
            "L L",
            "LLL"
          ],
          "key": {
                                                                                                "L": "#minecraft:logs"
          },
          "result": {
            "id": "minecraft:chest",
            "count": 4
          }
        }
        """;
    
    private static final String MAP_INK_SAC_RECIPE = """
        {
          "type": "minecraft:crafting_shaped",
                    "category": "misc",
          "pattern": [
            "PPP",
            "PIP",
            "PPP"
          ],
          "key": {
                                        "P": "minecraft:paper",
                                                "I": "#bitsandbalance:map_ink"
          },
          "result": { "id": "minecraft:map", "count": 1 }
        }
        """;

        private static final String MAP_INK_TAG = """
                        {
                            "replace": false,
                            "values": [
                                "minecraft:ink_sac",
                                "minecraft:black_dye"
                            ]
                        }
                        """;
    
    private static final String RECOVERY_COMPASS_RECIPE = """
        {
          "type": "minecraft:crafting_shaped",
                    "category": "tools",
          "pattern": [
            " E ",
            "ECE",
            " E "
          ],
          "key": {
                                                                                                "E": "minecraft:echo_shard",
                                                                                                "C": "minecraft:compass"
          },
          "result": { "id": "minecraft:recovery_compass", "count": 1 }
        }
        """;
    
    // Removed WOOL_DYEING_RECIPE - it was duplicating vanilla functionality
    
    private static final String ENDER_EYE_RECIPE = """
        {
          "type": "minecraft:crafting_shaped",
                    "category": "misc",
          "pattern": [
            "SW",
            "BE"
          ],
          "key": {
                                                                                                "S": "minecraft:echo_shard",
                                                                                                "W": "minecraft:wind_charge",
                                                                                                "B": "minecraft:blaze_powder",
                                                                                                "E": "minecraft:ender_pearl"
          },
          "result": { "id": "minecraft:ender_eye", "count": 1 }
        }
        """;
    
    
    private static final String RAW_IRON_SMELTING_RECIPE = """
        {
          "type": "minecraft:smelting",
                                                                                "ingredient": "#c:storage_blocks/raw_iron",
                    "result": { "id": "minecraft:iron_block" },
          "experience": 6.3,
          "cookingtime": 1600
        }
        """;
    
    private static final String RAW_GOLD_SMELTING_RECIPE = """
        {
          "type": "minecraft:smelting",
                                                                                "ingredient": "#c:storage_blocks/raw_gold",
                    "result": { "id": "minecraft:gold_block" },
          "experience": 6.3,
          "cookingtime": 1600
        }
        """;
    
    private static final String RAW_COPPER_SMELTING_RECIPE = """
        {
          "type": "minecraft:smelting",
                                                                                "ingredient": "#c:storage_blocks/raw_copper",
                    "result": { "id": "minecraft:copper_block" },
          "experience": 6.3,
          "cookingtime": 1600
        }
        """;
    
    private static final String RAW_IRON_BLASTING_RECIPE = """
        {
          "type": "minecraft:blasting",
                                                                                "ingredient": "#c:storage_blocks/raw_iron",
                    "result": { "id": "minecraft:iron_block" },
          "experience": 6.3,
          "cookingtime": 800
        }
        """;
    
    private static final String RAW_GOLD_BLASTING_RECIPE = """
        {
          "type": "minecraft:blasting",
                                                                                "ingredient": "#c:storage_blocks/raw_gold",
                    "result": { "id": "minecraft:gold_block" },
          "experience": 6.3,
          "cookingtime": 800
        }
        """;
    
    private static final String RAW_COPPER_BLASTING_RECIPE = """
        {
          "type": "minecraft:blasting",
                                                                                "ingredient": "#c:storage_blocks/raw_copper",
                    "result": { "id": "minecraft:copper_block" },
          "experience": 6.3,
          "cookingtime": 800
        }
        """;
    
    // Dynamic stair recipe scanning - finds ALL stair recipes from any mod
    private static java.util.Map<net.minecraft.resources.Identifier, String> scanStairRecipes(MinecraftServer server) {
        var stairRecipes = new java.util.HashMap<net.minecraft.resources.Identifier, String>();
        var recipeManager = server.getRecipeManager();
        
        for (var recipe : recipeManager.getRecipes()) {
            var recipeId = recipe.id().identifier();
            var recipeValue = recipe.value();
            
            // Check if this is a crafting recipe that outputs stairs
            if (recipeValue instanceof net.minecraft.world.item.crafting.ShapedRecipe shapedRecipe) {
                var result = shapedRecipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY, server.registryAccess());
                
                // Check if the result item is a stair block (ends with _stairs)
                var resultItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
                if (resultItemId != null && resultItemId.getPath().contains("stairs")) {
                    // Extract the recipe as JSON string for modification
                    String recipeJson = convertRecipeToJson(shapedRecipe, server);
                    if (recipeJson != null) {
                        stairRecipes.put(recipeId, recipeJson);
                        LOGGER.debug("[{}] Found stair recipe: {}", BitsAndBalance.MODID, recipeId);
                    }
                }
            }
        }
        
        LOGGER.info("[{}] Scanned {} stair recipes from all loaded mods", BitsAndBalance.MODID, stairRecipes.size());
        return stairRecipes;
    }
    
    // Convert a ShapedRecipe to JSON with modified count (8 instead of 4)
    private static String convertRecipeToJson(net.minecraft.world.item.crafting.ShapedRecipe recipe, MinecraftServer server) {
        try {
            var result = recipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY, server.registryAccess());
            var ingredients = recipe.getIngredients();
            int width = recipe.getWidth();
            int height = recipe.getHeight();
            
            // Build the recipe JSON manually
            var json = new StringBuilder();
            json.append("{\n");
            json.append("  \"type\": \"minecraft:crafting_shaped\",\n");
            
            // Add pattern
            json.append("  \"pattern\": [\n");
            for (int i = 0; i < height; i++) {
                json.append("    \"");
                for (int j = 0; j < width; j++) {
                    int index = i * width + j;
                    if (index < ingredients.size() && ingredients.get(index).isPresent()) {
                        json.append("#");
                    } else {
                        json.append(" ");
                    }
                }
                json.append("\"");
                if (i < height - 1) json.append(",");
                json.append("\n");
            }
            json.append("  ],\n");
            
            // Add key - simplified to use first ingredient
            json.append("  \"key\": {\n");
            json.append("    \"#\": ");
            
            // Get first non-empty ingredient
            for (var ingredientOpt : ingredients) {
                if (ingredientOpt.isPresent()) {
                    var ingredient = ingredientOpt.get();
                    var firstItem = ingredient.items().findFirst().orElse(null);
                    if (firstItem != null) {
                        var itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(firstItem.value());
                        json.append("\"").append(itemId).append("\"");
                        break;
                    }
                }
            }
            json.append("\n  },\n");
            
            // Add result with count of 8
            var resultId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
            json.append("  \"result\": {\n");
            json.append("    \"id\": \"").append(resultId).append("\",\n");
            json.append("    \"count\": 8\n");
            json.append("  }\n");
            json.append("}");
            
            return json.toString();
        } catch (Exception e) {
            LOGGER.warn("[{}] Failed to convert recipe to JSON: {}", BitsAndBalance.MODID, e.getMessage());
            return null;
        }
    }
    
    // Scan and generate compact 2x2 stair recipes - finds ALL stair types from any mod
    private static java.util.Map<net.minecraft.resources.Identifier, String> scanCompactStairRecipes(MinecraftServer server) {
        var compactStairRecipes = new java.util.HashMap<net.minecraft.resources.Identifier, String>();
        var recipeManager = server.getRecipeManager();
        var processedStairs = new java.util.HashSet<net.minecraft.resources.Identifier>();
        
        for (var recipe : recipeManager.getRecipes()) {
            var recipeValue = recipe.value();
            
            // Check if this is a crafting recipe that outputs stairs
            if (recipeValue instanceof net.minecraft.world.item.crafting.ShapedRecipe shapedRecipe) {
                var result = shapedRecipe.assemble(net.minecraft.world.item.crafting.CraftingInput.EMPTY, server.registryAccess());
                
                // Check if the result item is a stair block
                var resultItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
                if (resultItemId != null && resultItemId.getPath().contains("stairs")) {
                    var stairItemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(result.getItem());
                    
                    // Only process each stair type once
                    if (!processedStairs.contains(stairItemId)) {
                        processedStairs.add(stairItemId);
                        
                        // Get the base material from the first ingredient
                        var ingredients = shapedRecipe.getIngredients();
                        String materialItem = null;
                        
                        for (var ingredientOpt : ingredients) {
                            if (ingredientOpt.isPresent()) {
                                var ingredient = ingredientOpt.get();
                                var firstItem = ingredient.items().findFirst().orElse(null);
                                if (firstItem != null) {
                                    materialItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(firstItem.value()).toString();
                                    break;
                                }
                            }
                        }
                        
                        if (materialItem != null) {
                            // Create compact 2x2 recipe: 3 blocks in corner = 4 stairs
                            String recipeJson = createCompact2x2StairRecipe(stairItemId.toString(), materialItem);
                            
                            // Create unique recipe ID for compact version
                            var compactRecipeId = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                                stairItemId.getNamespace(),
                                stairItemId.getPath() + "_compact"
                            );
                            
                            compactStairRecipes.put(compactRecipeId, recipeJson);
                            LOGGER.debug("[{}] Created compact stair recipe: {} using {}", 
                                BitsAndBalance.MODID, compactRecipeId, materialItem);
                        }
                    }
                }
            }
        }
        
        LOGGER.info("[{}] Generated {} compact 2x2 stair recipes from all loaded mods", 
            BitsAndBalance.MODID, compactStairRecipes.size());
        return compactStairRecipes;
    }
    
    // Create a compact 2x2 stair recipe JSON (3 blocks in corner = 4 stairs)
    private static String createCompact2x2StairRecipe(String stairId, String materialItem) {
        return String.format("""
            {
              "type": "minecraft:crafting_shaped",
              "pattern": [
                "# ",
                "##"
              ],
              "key": {
                                                                                                                                "#": "%s"
              },
              "result": {
                "id": "%s",
                "count": 4
              }
            }
            """, materialItem, stairId);
    }
    
    public static boolean prepareWorldDatapack(MinecraftServer server) {
        LOGGER.info("[{}] Preparing alternative recipe datapack...", BitsAndBalance.MODID);

        Path dataPackPath = NeoForgeGeneratedServerDataPacks.getPackPath(DATAPACK_NAME);

        if (anyRecipesEnabled()) {
            LOGGER.info("[{}] Current config values:", BitsAndBalance.MODID);
            LOGGER.info("[{}]   enableAlternativeRepeaterRecipe: {}", BitsAndBalance.MODID, Config.enableAlternativeRepeaterRecipe);
            LOGGER.info("[{}]   enableChestFromLogsRecipe: {}", BitsAndBalance.MODID, Config.enableChestFromLogsRecipe);
            LOGGER.info("[{}]   enableRecoveryCompassRecipe: {}", BitsAndBalance.MODID, Config.enableRecoveryCompassRecipe);
            LOGGER.info("[{}]   enableStairRecipeOverride: {}", BitsAndBalance.MODID, Config.enableStairRecipeOverride);
            LOGGER.info("[{}]   enableCompactStairRecipe: {}", BitsAndBalance.MODID, Config.enableCompactStairRecipe);
        }

        String inputFingerprint = buildInputFingerprint(server);
        try {
            if (anyRecipesEnabled() && DynamicRecipeDatapackSync.matchesInputFingerprint(dataPackPath, inputFingerprint)) {
                NeoForgeGeneratedServerDataPacks.writeBootstrapFingerprint(DATAPACK_NAME, NeoForgeGeneratedServerDataPacks.buildAlternativeBootstrapFingerprint(server));
                LOGGER.info("[{}] Alternative recipe inputs unchanged; skipped regeneration", BitsAndBalance.MODID);
                return false;
            }
        } catch (IOException e) {
            LOGGER.warn("[{}] Failed reading alternative recipe input fingerprint: {}", BitsAndBalance.MODID, e.getMessage());
        }

        DynamicRecipeDatapackSync.Result result;
        try {
            result = DynamicRecipeDatapackSync.sync(dataPackPath, PACK_MCMETA, buildDatapackFiles(server), inputFingerprint);
        } catch (IOException e) {
            LOGGER.error("[{}] Failed to sync alternative recipe datapack: {}", BitsAndBalance.MODID, e.getMessage(), e);
            return false;
        }

        if (anyRecipesEnabled()) {
            try {
                NeoForgeGeneratedServerDataPacks.writeBootstrapFingerprint(DATAPACK_NAME, NeoForgeGeneratedServerDataPacks.buildAlternativeBootstrapFingerprint(server));
            } catch (IOException e) {
                LOGGER.warn("[{}] Failed to write alternative recipe bootstrap fingerprint: {}", BitsAndBalance.MODID, e.getMessage());
            }
            if (result == DynamicRecipeDatapackSync.Result.UNCHANGED) {
                LOGGER.info("[{}] Alternative recipe datapack unchanged; skipped rewrite", BitsAndBalance.MODID);
            } else {
                LOGGER.info("[{}] Alternative recipe datapack prepared", BitsAndBalance.MODID);
            }
        } else {
            try {
                boolean changed = NeoForgeGeneratedServerDataPacks.clearPack(DATAPACK_NAME, PACK_MCMETA);
                NeoForgeGeneratedServerDataPacks.writeBootstrapFingerprint(DATAPACK_NAME, NeoForgeGeneratedServerDataPacks.buildAlternativeBootstrapFingerprint(server));
                if (changed) {
                    LOGGER.info("[{}] Cleared alternative recipe datapack contents because all alternatives are disabled", BitsAndBalance.MODID);
                }
            } catch (IOException e) {
                LOGGER.warn("[{}] Failed clearing disabled alternative recipe datapack: {}", BitsAndBalance.MODID, e.getMessage());
            }
            LOGGER.info("[{}] No alternative recipes enabled, skipping alternative recipe datapack", BitsAndBalance.MODID);
        }

        var recipes = server.getRecipeManager().getRecipes();
        LOGGER.info("[{}] Alternative recipe datapack prep complete. {} recipes currently loaded in recipe manager",
            BitsAndBalance.MODID, recipes.size());
        return result != DynamicRecipeDatapackSync.Result.UNCHANGED;
    }

    private static String buildInputFingerprint(MinecraftServer server) {
        var builder = org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprint.builder();
        builder.add("schema_version", 4);
        builder.add("alt_repeater", Config.enableAlternativeRepeaterRecipe);
        builder.add("chest_from_logs", Config.enableChestFromLogsRecipe);
        builder.add("map_ink_sac", Config.enableMapInkSacRecipe);
        builder.add("recovery_compass", Config.enableRecoveryCompassRecipe);
        builder.add("ender_eye", Config.enableEnderEyeRecipe);
        builder.add("raw_iron_smelting", Config.enableRawIronSmeltingRecipe);
        builder.add("raw_gold_smelting", Config.enableRawGoldSmeltingRecipe);
        builder.add("raw_copper_smelting", Config.enableRawCopperSmeltingRecipe);
        builder.add("raw_iron_blasting", Config.enableRawIronBlastingRecipe);
        builder.add("raw_gold_blasting", Config.enableRawGoldBlastingRecipe);
        builder.add("raw_copper_blasting", Config.enableRawCopperBlastingRecipe);
        builder.add("stair_override", Config.enableStairRecipeOverride);
        builder.add("compact_stair", Config.enableCompactStairRecipe);
        builder.add("stair_families", FamilyCompatRecipeGenerator.buildStairFamilyFingerprint());

        return builder.build();
    }

    private static Map<String, String> buildDatapackFiles(MinecraftServer server) {
        Map<String, String> files = new LinkedHashMap<>();

        if (!anyRecipesEnabled()) {
            return files;
        }

        if (Config.enableAlternativeRepeaterRecipe) {
            files.put("data/bitsandbalance/recipe/alternative_repeater.json", ALTERNATIVE_REPEATER_RECIPE);
        }

        if (Config.enableChestFromLogsRecipe) {
            files.put("data/bitsandbalance/recipe/chest_from_logs.json", CHEST_FROM_LOGS_RECIPE);
        }

        if (Config.enableRecoveryCompassRecipe) {
            files.put("data/minecraft/recipe/recovery_compass.json", RECOVERY_COMPASS_RECIPE);
        }

        if (Config.enableMapInkSacRecipe) {
            files.put("data/minecraft/recipe/map.json", MAP_INK_SAC_RECIPE);
            files.put("data/bitsandbalance/tags/items/map_ink.json", MAP_INK_TAG);
            files.put("data/bitsandbalance/tags/item/map_ink.json", MAP_INK_TAG);
        }

        if (Config.enableEnderEyeRecipe) {
            files.put("data/minecraft/recipe/ender_eye.json", ENDER_EYE_RECIPE);
        }

        if (Config.enableRawIronSmeltingRecipe) {
            files.put("data/bitsandbalance/recipe/smelting/raw_iron_block.json", RAW_IRON_SMELTING_RECIPE);
        }

        if (Config.enableRawGoldSmeltingRecipe) {
            files.put("data/bitsandbalance/recipe/smelting/raw_gold_block.json", RAW_GOLD_SMELTING_RECIPE);
        }

        if (Config.enableRawCopperSmeltingRecipe) {
            files.put("data/bitsandbalance/recipe/smelting/raw_copper_block.json", RAW_COPPER_SMELTING_RECIPE);
        }

        if (Config.enableRawIronBlastingRecipe) {
            files.put("data/bitsandbalance/recipe/blasting/raw_iron_block.json", RAW_IRON_BLASTING_RECIPE);
        }

        if (Config.enableRawGoldBlastingRecipe) {
            files.put("data/bitsandbalance/recipe/blasting/raw_gold_block.json", RAW_GOLD_BLASTING_RECIPE);
        }

        if (Config.enableRawCopperBlastingRecipe) {
            files.put("data/bitsandbalance/recipe/blasting/raw_copper_block.json", RAW_COPPER_BLASTING_RECIPE);
        }

        if (Config.enableStairRecipeOverride) {
            var stairRecipes = FamilyCompatRecipeGenerator.generateStairOverrideRecipes();
            for (var entry : stairRecipes.entrySet()) {
                files.put("data/" + entry.getKey().getNamespace() + "/recipe/" + entry.getKey().getPath() + ".json", entry.getValue());
            }
        }

        if (Config.enableCompactStairRecipe) {
            var compactStairRecipes = FamilyCompatRecipeGenerator.generateCompactStairRecipes();
            for (var entry : compactStairRecipes.entrySet()) {
                files.put("data/" + entry.getKey().getNamespace() + "/recipe/" + entry.getKey().getPath() + ".json", entry.getValue());
            }
        }

        return files;
    }
    
    // Removed server tick scanning - datapack management now happens only when needed
    
    // Removed manageRecipeDataPacks - now using forceRecreateDataPacks for all operations
    
    private static void createDataPackFilesOnly(MinecraftServer server) {
        try {
            LOGGER.info("[{}] Creating data pack files without reload...", BitsAndBalance.MODID);
            
            for (ServerLevel level : server.getAllLevels()) {
                try {
                    Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
                    Path dataPackPath = worldPath.resolve("datapacks").resolve(DATAPACK_NAME);
                    
                    boolean existed = Files.exists(dataPackPath);
                    LOGGER.info("[{}] World {}: dataPack exists={}", BitsAndBalance.MODID, level.dimension().identifier(), existed);
                    
                    if (existed) {
                        LOGGER.info("[{}] Deleting existing data pack for world {}", BitsAndBalance.MODID, level.dimension().identifier());
                        deleteDataPack(dataPackPath);
                    }
                    
                    if (anyRecipesEnabled()) {
                        LOGGER.info("[{}] Creating new data pack for world {}", BitsAndBalance.MODID, level.dimension().identifier());
                        createDataPack(dataPackPath, server);
                    } else {
                        LOGGER.info("[{}] No recipes enabled, skipping data pack creation for world {}", BitsAndBalance.MODID, level.dimension().identifier());
                    }
                    
                } catch (Exception e) {
                    LOGGER.warn("[{}] Could not create data pack for world {}: {}", 
                        BitsAndBalance.MODID, level.dimension().identifier(), e.getMessage());
                }
            }
                
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to create data pack files: {}", BitsAndBalance.MODID, e.getMessage(), e);
        }
    }
    
    private static void createDataPack(Path dataPackPath, MinecraftServer server) throws IOException {
        DynamicRecipeDatapackSync.sync(
                dataPackPath,
                PACK_MCMETA,
                buildDatapackFiles(server),
                buildInputFingerprint(server)
        );
    }
    
    private static void deleteDataPack(Path dataPackPath) throws IOException {
        if (Files.exists(dataPackPath)) {
            deleteRecursively(dataPackPath);
        }
    }
    
    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteRecursively(child);
                    } catch (IOException e) {
                        // Ignore deletion errors
                    }
                });
            }
        }
        Files.deleteIfExists(path);
    }
    
    private static void enableDataPack(MinecraftServer server) {
        try {
            if (anyRecipesEnabled()) {
                PackRepository packRepository = server.getPackRepository();
                String packId = "file/" + DATAPACK_NAME;
                
                var availablePacks = packRepository.getAvailablePacks();
                boolean packExists = availablePacks.stream()
                    .anyMatch(pack -> pack.getId().equals(packId));
                
                if (packExists) {
                    var enabledPacks = packRepository.getSelectedPacks();
                    boolean isEnabled = enabledPacks.stream()
                        .anyMatch(pack -> pack.getId().equals(packId));
                    
                    if (!isEnabled) {
                        var newSelection = new java.util.ArrayList<>(enabledPacks);
                        var ourPack = availablePacks.stream()
                            .filter(pack -> pack.getId().equals(packId))
                            .findFirst();
                        
                        if (ourPack.isPresent()) {
                            newSelection.add(ourPack.get());
                            packRepository.setSelected(newSelection.stream().map(Pack::getId).toList());
                        }
                    }
                } else {
                    packRepository.reload();
                }
            } else {
                disableDataPack(server);
            }
            
            } catch (Exception e) {
            LOGGER.warn("[{}] Could not manage data pack enablement: {}", BitsAndBalance.MODID, e.getMessage());
        }
    }
    
    private static void disableDataPack(MinecraftServer server) {
        try {
            PackRepository packRepository = server.getPackRepository();
            String packId = "file/" + DATAPACK_NAME;
            
            var enabledPacks = packRepository.getSelectedPacks();
            boolean isEnabled = enabledPacks.stream()
                .anyMatch(pack -> pack.getId().equals(packId));
            
            if (isEnabled) {
                var newSelection = enabledPacks.stream()
                    .filter(pack -> !pack.getId().equals(packId))
                    .map(Pack::getId)
                    .toList();
                
                packRepository.setSelected(newSelection);
            }
            
        } catch (Exception e) {
            LOGGER.warn("[{}] Could not disable data pack: {}", BitsAndBalance.MODID, e.getMessage());
        }
    }
    
    private static void forceRecreateDataPacks(MinecraftServer server) {
        try {
            int dataPacksRecreated = 0;
            
            LOGGER.info("[{}] Force recreating data packs - anyRecipesEnabled(): {}", BitsAndBalance.MODID, anyRecipesEnabled());
            
            for (ServerLevel level : server.getAllLevels()) {
                try {
                    Path worldPath = level.getServer().getWorldPath(LevelResource.ROOT);
                    Path dataPackPath = worldPath.resolve("datapacks").resolve(DATAPACK_NAME);
                    
                    boolean existed = Files.exists(dataPackPath);
                    LOGGER.info("[{}] World {}: dataPack exists={}", BitsAndBalance.MODID, level.dimension().identifier(), existed);
                    
                    if (existed) {
                        LOGGER.info("[{}] Deleting existing data pack for world {}", BitsAndBalance.MODID, level.dimension().identifier());
                        deleteDataPack(dataPackPath);
                    }
                    
                    if (anyRecipesEnabled()) {
                        LOGGER.info("[{}] Creating new data pack for world {}", BitsAndBalance.MODID, level.dimension().identifier());
                        createDataPack(dataPackPath, server);
                        dataPacksRecreated++;
                    } else {
                        LOGGER.info("[{}] No recipes enabled, skipping data pack creation for world {}", BitsAndBalance.MODID, level.dimension().identifier());
                    }
                    
                } catch (Exception e) {
                    LOGGER.warn("[{}] Could not recreate data pack for world {}: {}", 
                        BitsAndBalance.MODID, level.dimension().identifier(), e.getMessage());
                }
            }
            
            if (dataPacksRecreated > 0 && anyRecipesEnabled()) {
                LOGGER.info("[{}] Enabling data pack and reloading resources", BitsAndBalance.MODID);
                enableDataPack(server);
                reloadResourcesAndSync(server);
            } else {
                LOGGER.info("[{}] No data packs recreated, skipping enable/reload", BitsAndBalance.MODID);
            }
                
        } catch (Exception e) {
            LOGGER.error("[{}] Failed to recreate data packs: {}", BitsAndBalance.MODID, e.getMessage(), e);
        }
    }
    
    private static void triggerRecipeReload(MinecraftServer server) {
        if (server != null) {
            try {
                if (anyRecipesEnabled()) {
                    enableDataPack(server);
                } else {
                    disableDataPack(server);
                }
                
                reloadResourcesAndSync(server);
            } catch (Exception e) {
                LOGGER.warn("[{}] Could not trigger data pack reload: {}", BitsAndBalance.MODID, e.getMessage());
            }
        }
    }
    
    private static void reloadResourcesAndSync(MinecraftServer server) {
        try {
            LOGGER.info("[{}] Starting resource reload...", BitsAndBalance.MODID);
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
            LOGGER.info("[{}] Reload complete", BitsAndBalance.MODID);
        } catch (Exception e) {
            LOGGER.warn("[{}] Could not trigger resource reload: {}", BitsAndBalance.MODID, e.getMessage());
        }
    }
    
    
    
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var server = player.level().getServer();
            if (server != null && anyRecipesEnabled()) {
                LOGGER.info("[{}] Player {} logged in, syncing recipes from bootstrapped datapacks...",
                    BitsAndBalance.MODID, player.getName().getString());

                playersNeedingRecipes.add(player.getUUID().toString());
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.level().isClientSide()) {
            String playerId = player.getUUID().toString();
            
            // Check if this player needs recipes and is ready
            if (playersNeedingRecipes.contains(playerId) && anyRecipesEnabled()) {
                // Check if player has been in the world for at least one tick (indicating they're ready)
                if (player.tickCount > 1) { // Immediately after first tick - as early as possible!
                    try {
                        LOGGER.info("[{}] Player {} ready, refreshing recipe book (lightweight approach)", 
                            BitsAndBalance.MODID, player.getName().getString());
                        
                        // Lightweight approach: Use the recipe book API directly
                        var server = player.level().getServer();
                        if (server != null) {
                            var recipeManager = server.getRecipeManager();
                            var recipes = recipeManager.getRecipes();
                            
                            // First, send recipe update packet to sync the recipe manager with our custom recipes
                            var packet = new net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket(
                                recipeManager.getSynchronizedItemProperties(),
                                recipeManager.getSynchronizedStonecutterRecipes()
                            );
                            player.connection.send(packet);
                            
                            // Now unlock all recipes in the player's recipe book silently
                            var recipeBook = player.getRecipeBook();
                            for (var recipe : recipes) {
                                if (recipe != null) {
                                    recipeBook.add(recipe.id());
                                }
                            }
                            recipeBook.sendInitialRecipeBook(player);
                            
                            LOGGER.info("[{}] Unlocked {} recipes in recipe book for player: {} (including custom recipes)", 
                                BitsAndBalance.MODID, recipes.size(), player.getName().getString());
                        }
                        
                        // Remove from needing recipes
                        playersNeedingRecipes.remove(playerId);
                        
                    } catch (Exception e) {
                        LOGGER.warn("[{}] Failed recipe refresh for player {}: {}", 
                            BitsAndBalance.MODID, player.getName().getString(), e.getMessage());
                    }
                }
            }
        }
    }
    
    // Removed schedulePostLoginDataPackReload - now handled directly in onPlayerLogin
    
    /**
     * Manual method to force config reload and data pack update.
     * Call this after changing TOML files to apply changes without server restart.
     */
    public static void forceConfigReload(MinecraftServer server) {
        LOGGER.info("[{}] Forcing config reload and data pack update...", BitsAndBalance.MODID);
        
        // Force recreate data packs based on current config state
        forceRecreateDataPacks(server);
        
        // Mark all current players for recipe refresh
        var players = server.getPlayerList().getPlayers();
        for (ServerPlayer player : players) {
            playersNeedingRecipes.add(player.getUUID().toString());
        }
        
        LOGGER.info("[{}] Config reload complete. Marked {} players for recipe refresh", 
            BitsAndBalance.MODID, players.size());
    }
}
