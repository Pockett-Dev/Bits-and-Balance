package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.LevelResource;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeDatapackSync;
import org.onenonly.bitsandbalance.common.recipe.DynamicRecipeInputFingerprint;
import org.onenonly.bitsandbalance.common.recipe.FamilyCompatRecipeGenerator;
import org.onenonly.bitsandbalance.fabric.config.FabricRecipesConfig;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Fabric port: Alternative Recipes (dynamic datapack in world folder).
 * Creates/deletes a world datapack based on FabricRecipesConfig and triggers a resource reload.
 */
public final class FabricAlternativeRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();

    private static final String DATAPACK_NAME = "bitsandbalance_dynamic";

  private static final String RECIPE_DIR = "recipe";

    private static final String PACK_MCMETA = """
            {
              \"pack\": {
                \"description\": \"Dynamic Bits and Balance Alternative Recipes\",
                \"pack_format\": 88,
                \"min_format\": 88,
                \"max_format\": 88
              }
            }
            """;

    private static final String ALTERNATIVE_REPEATER_RECIPE = """
            {
              \"type\": \"minecraft:crafting_shaped\",
              \"category\": \"redstone\",
              \"pattern\": [
                \"R R\",
                \"SRS\",
                \"TTT\"
              ],
              \"key\": {
                \"R\": \"minecraft:redstone\",
                \"S\": \"minecraft:stick\",
                \"T\": \"minecraft:stone\"
              },
              \"result\": {
                \"id\": \"minecraft:repeater\",
                \"count\": 1
              }
            }
            """;

    private static final String CHEST_FROM_LOGS_RECIPE = """
            {
              \"type\": \"minecraft:crafting_shaped\",
              \"category\": \"misc\",
              \"pattern\": [
                \"LLL\",
                \"L L\",
                \"LLL\"
              ],
              \"key\": {
                \"L\": \"#minecraft:logs\"
              },
              \"result\": {
                \"id\": \"minecraft:chest\",
                \"count\": 4
              }
            }
            """;

    private static final String MAP_INK_SAC_RECIPE = """
            {
              \"type\": \"minecraft:crafting_shaped\",
              \"category\": \"misc\",
              \"pattern\": [
                \"PPP\",
                \"PIP\",
                \"PPP\"
              ],
              \"key\": {
                \"P\": \"minecraft:paper\",
                \"I\": \"#bitsandbalance:map_ink\"
              },
              \"result\": { \"id\": \"minecraft:map\", \"count\": 1 }
            }
            """;

    private static final String MAP_INK_TAG = """
            {
              \"replace\": false,
              \"values\": [
                \"minecraft:ink_sac\",
                \"minecraft:black_dye\"
              ]
            }
            """;

    private static final String RECOVERY_COMPASS_RECIPE = """
            {
              \"type\": \"minecraft:crafting_shaped\",
              \"category\": \"tools\",
              \"pattern\": [
                \" E \",
                \"ECE\",
                \" E \"
              ],
              \"key\": {
                \"E\": \"minecraft:echo_shard\",
                \"C\": \"minecraft:compass\"
              },
              \"result\": { \"id\": \"minecraft:recovery_compass\", \"count\": 1 }
            }
            """;

    private static final String ENDER_EYE_RECIPE = """
            {
              \"type\": \"minecraft:crafting_shaped\",
              \"category\": \"misc\",
              \"pattern\": [
                \"SW\",
                \"BE\"
              ],
              \"key\": {
                \"S\": \"minecraft:echo_shard\",
                \"W\": \"minecraft:wind_charge\",
                \"B\": \"minecraft:blaze_powder\",
                \"E\": \"minecraft:ender_pearl\"
              },
              \"result\": { \"id\": \"minecraft:ender_eye\", \"count\": 1 }
            }
            """;

              private static final String[] CREATE_SPLASHING_WASH_ITEMS = new String[] {
                "pale_oak_boat",
                "pale_oak_button",
                "pale_oak_chest_boat",
                "pale_oak_door",
                "pale_oak_fence",
                "pale_oak_fence_gate",
                "pale_oak_hanging_sign",
                "pale_oak_log",
                "pale_oak_planks",
                "pale_oak_pressure_plate",
                "pale_oak_sign",
                "pale_oak_slab",
                "pale_oak_stairs",
                "pale_oak_trapdoor",
                "pale_oak_wood",
                "stripped_pale_oak_log",
                "stripped_pale_oak_wood"
              };


    private static final String RAW_IRON_SMELTING_RECIPE = """
            {
              \"type\": \"minecraft:smelting\",
              \"ingredient\": \"#c:storage_blocks/raw_iron\",
              \"result\": { \"id\": \"minecraft:iron_block\" },
              \"experience\": 6.3,
              \"cookingtime\": 1600
            }
            """;

    private static final String RAW_GOLD_SMELTING_RECIPE = """
            {
              \"type\": \"minecraft:smelting\",
              \"ingredient\": \"#c:storage_blocks/raw_gold\",
              \"result\": { \"id\": \"minecraft:gold_block\" },
              \"experience\": 6.3,
              \"cookingtime\": 1600
            }
            """;

    private static final String RAW_COPPER_SMELTING_RECIPE = """
            {
              \"type\": \"minecraft:smelting\",
              \"ingredient\": \"#c:storage_blocks/raw_copper\",
              \"result\": { \"id\": \"minecraft:copper_block\" },
              \"experience\": 6.3,
              \"cookingtime\": 1600
            }
            """;

    private static final String RAW_IRON_BLASTING_RECIPE = """
            {
              \"type\": \"minecraft:blasting\",
              \"ingredient\": \"#c:storage_blocks/raw_iron\",
              \"result\": { \"id\": \"minecraft:iron_block\" },
              \"experience\": 6.3,
              \"cookingtime\": 800
            }
            """;

    private static final String RAW_GOLD_BLASTING_RECIPE = """
            {
              \"type\": \"minecraft:blasting\",
              \"ingredient\": \"#c:storage_blocks/raw_gold\",
              \"result\": { \"id\": \"minecraft:gold_block\" },
              \"experience\": 6.3,
              \"cookingtime\": 800
            }
            """;

    private static final String RAW_COPPER_BLASTING_RECIPE = """
            {
              \"type\": \"minecraft:blasting\",
              \"ingredient\": \"#c:storage_blocks/raw_copper\",
              \"result\": { \"id\": \"minecraft:copper_block\" },
              \"experience\": 6.3,
              \"cookingtime\": 800
            }
            """;

    private FabricAlternativeRecipes() {
    }

    static String packName() {
      return DATAPACK_NAME;
    }

    static String packMcmeta() {
      return PACK_MCMETA;
    }

    public static void init() {
    }

    public static boolean prepareWorldDatapack(Path worldRoot, PackRepository packRepository, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
      return syncDatapack(worldRoot, packRepository, recipeManager, registryAccess, null);
    }

    private static boolean syncDatapack(
      Path worldRoot,
      PackRepository packRepository,
      RecipeManager recipeManager,
      RegistryAccess registryAccess,
      MinecraftServer server
    ) throws IOException {
      Path datapackPath = FabricGeneratedWorldDataPacks.getPackPath(DATAPACK_NAME);
      FabricGeneratedWorldDataPacks.ensurePackRoot(DATAPACK_NAME);

        boolean anyEnabled = FabricRecipesConfig.anyEnabled();
      String inputFingerprint = buildInputFingerprint(recipeManager, registryAccess);

      if (anyEnabled && DynamicRecipeDatapackSync.matchesInputFingerprint(datapackPath, inputFingerprint)) {
        boolean selectionChanged = enableDatapack(packRepository, false);
        LOGGER.info("[{}] Alternative recipe inputs unchanged; skipped regeneration", BitsAndBalanceCommon.MOD_ID);
        return selectionChanged;
      }

      DynamicRecipeDatapackSync.Result result = DynamicRecipeDatapackSync.sync(
          datapackPath,
          PACK_MCMETA,
          buildDatapackFiles(recipeManager, registryAccess),
          inputFingerprint
      );
      boolean changed = result != DynamicRecipeDatapackSync.Result.UNCHANGED;

        if (anyEnabled) {
        boolean selectionChanged = enableDatapack(packRepository, changed);
        changed |= selectionChanged;

        if (server != null && changed) {
                reloadWithCommand(server);
            FabricDynamicRecipeDatapackCoordinator.scheduleAllPlayers(server);
            }
          LOGGER.info(
          "[{}] Alternative recipes datapack {}: {}",
              BitsAndBalanceCommon.MOD_ID,
          result == DynamicRecipeDatapackSync.Result.UNCHANGED ? "unchanged" : "synced",
              datapackPath.toAbsolutePath()
          );
        } else {
        changed |= disableDatapack(packRepository, changed);
          LOGGER.info(
              "[{}] Alternative recipes disabled; dynamic datapack not created (enable in config/bitsandbalance-recipes.json)",
              BitsAndBalanceCommon.MOD_ID
          );
        }

      return changed;
    }

    private static String buildInputFingerprint(RecipeManager recipeManager, RegistryAccess registryAccess) {
      DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
      builder.add("schema_version", 2);
      builder.add("alt_repeater", FabricRecipesConfig.enableAlternativeRepeaterRecipe);
      builder.add("chest_from_logs", FabricRecipesConfig.enableChestFromLogsRecipe);
      builder.add("raw_iron_smelting", FabricRecipesConfig.enableRawIronSmeltingRecipe);
      builder.add("raw_gold_smelting", FabricRecipesConfig.enableRawGoldSmeltingRecipe);
      builder.add("raw_copper_smelting", FabricRecipesConfig.enableRawCopperSmeltingRecipe);
      builder.add("raw_iron_blasting", FabricRecipesConfig.enableRawIronBlastingRecipe);
      builder.add("raw_gold_blasting", FabricRecipesConfig.enableRawGoldBlastingRecipe);
      builder.add("raw_copper_blasting", FabricRecipesConfig.enableRawCopperBlastingRecipe);
      builder.add("recovery_compass", FabricRecipesConfig.enableRecoveryCompassRecipe);
      builder.add("map_ink_sac", FabricRecipesConfig.enableMapInkSacRecipe);
      builder.add("ender_eye", FabricRecipesConfig.enableEnderEyeRecipe);
      builder.add("stair_override", FabricRecipesConfig.enableStairRecipeOverride);
      builder.add("compact_stair", FabricRecipesConfig.enableCompactStairRecipe);
      if (FabricRecipesConfig.enableStairRecipeOverride || FabricRecipesConfig.enableCompactStairRecipe) {
        builder.add("stair_families", FamilyCompatRecipeGenerator.buildStairFamilyFingerprint());
      }

      return builder.build();
    }

    private static Map<String, String> buildDatapackFiles(RecipeManager recipeManager, RegistryAccess registryAccess) {
      Map<String, String> files = new LinkedHashMap<>();

      if (!FabricRecipesConfig.anyEnabled()) {
        return files;
      }

      if (FabricRecipesConfig.enableAlternativeRepeaterRecipe) {
        files.put("data/bitsandbalance/recipe/alternative_repeater.json", ALTERNATIVE_REPEATER_RECIPE);
        LOGGER.info("[{}] Wrote dynamic recipe: bitsandbalance:alternative_repeater", BitsAndBalanceCommon.MOD_ID);
      }

      if (FabricRecipesConfig.enableChestFromLogsRecipe) {
        files.put("data/bitsandbalance/recipe/chest_from_logs.json", CHEST_FROM_LOGS_RECIPE);
        LOGGER.info("[{}] Wrote dynamic recipe: bitsandbalance:chest_from_logs", BitsAndBalanceCommon.MOD_ID);
      }

      if (FabricRecipesConfig.enableRawIronSmeltingRecipe) {
        files.put("data/bitsandbalance/recipe/smelting/raw_iron_block.json", RAW_IRON_SMELTING_RECIPE);
      }

      if (FabricRecipesConfig.enableRawGoldSmeltingRecipe) {
        files.put("data/bitsandbalance/recipe/smelting/raw_gold_block.json", RAW_GOLD_SMELTING_RECIPE);
      }

      if (FabricRecipesConfig.enableRawCopperSmeltingRecipe) {
        files.put("data/bitsandbalance/recipe/smelting/raw_copper_block.json", RAW_COPPER_SMELTING_RECIPE);
      }

      if (FabricRecipesConfig.enableRawIronBlastingRecipe) {
        files.put("data/bitsandbalance/recipe/blasting/raw_iron_block.json", RAW_IRON_BLASTING_RECIPE);
      }

      if (FabricRecipesConfig.enableRawGoldBlastingRecipe) {
        files.put("data/bitsandbalance/recipe/blasting/raw_gold_block.json", RAW_GOLD_BLASTING_RECIPE);
      }

      if (FabricRecipesConfig.enableRawCopperBlastingRecipe) {
        files.put("data/bitsandbalance/recipe/blasting/raw_copper_block.json", RAW_COPPER_BLASTING_RECIPE);
      }

      if (FabricRecipesConfig.enableRecoveryCompassRecipe) {
        files.put("data/minecraft/recipe/recovery_compass.json", RECOVERY_COMPASS_RECIPE);
        LOGGER.info("[{}] Wrote dynamic recipe override: minecraft:recovery_compass", BitsAndBalanceCommon.MOD_ID);
      }

      if (FabricRecipesConfig.enableMapInkSacRecipe) {
        files.put("data/minecraft/recipe/map.json", MAP_INK_SAC_RECIPE);
        LOGGER.info("[{}] Wrote dynamic recipe override: minecraft:map", BitsAndBalanceCommon.MOD_ID);
        files.put("data/bitsandbalance/tags/items/map_ink.json", MAP_INK_TAG);
        files.put("data/bitsandbalance/tags/item/map_ink.json", MAP_INK_TAG);
      }

      if (FabricRecipesConfig.enableEnderEyeRecipe) {
        files.put("data/minecraft/recipe/ender_eye.json", ENDER_EYE_RECIPE);
        LOGGER.info("[{}] Wrote dynamic recipe: minecraft:ender_eye", BitsAndBalanceCommon.MOD_ID);
      }

      addStairRecipeFiles(files, recipeManager, registryAccess);
      return files;
    }

    private static void createDatapack(Path datapackPath, RecipeManager recipeManager, RegistryAccess registryAccess) throws IOException {
        Files.createDirectories(datapackPath);
        Files.writeString(datapackPath.resolve("pack.mcmeta"), PACK_MCMETA, StandardCharsets.UTF_8, StandardOpenOption.CREATE);

      Path bbRecipeDir = datapackPath.resolve("data").resolve("bitsandbalance").resolve(RECIPE_DIR);
        Files.createDirectories(bbRecipeDir);

        if (FabricRecipesConfig.enableAlternativeRepeaterRecipe) {
            write(bbRecipeDir.resolve("alternative_repeater.json"), ALTERNATIVE_REPEATER_RECIPE);
          LOGGER.info("[{}] Wrote dynamic recipe: bitsandbalance:alternative_repeater", BitsAndBalanceCommon.MOD_ID);
        }

        if (FabricRecipesConfig.enableChestFromLogsRecipe) {
            write(bbRecipeDir.resolve("chest_from_logs.json"), CHEST_FROM_LOGS_RECIPE);
          LOGGER.info("[{}] Wrote dynamic recipe: bitsandbalance:chest_from_logs", BitsAndBalanceCommon.MOD_ID);
        }

        if (FabricRecipesConfig.enableRawIronSmeltingRecipe) {
            Path dir = bbRecipeDir.resolve("smelting");
            Files.createDirectories(dir);
            write(dir.resolve("raw_iron_block.json"), RAW_IRON_SMELTING_RECIPE);
        }

        if (FabricRecipesConfig.enableRawGoldSmeltingRecipe) {
            Path dir = bbRecipeDir.resolve("smelting");
            Files.createDirectories(dir);
            write(dir.resolve("raw_gold_block.json"), RAW_GOLD_SMELTING_RECIPE);
        }

        if (FabricRecipesConfig.enableRawCopperSmeltingRecipe) {
            Path dir = bbRecipeDir.resolve("smelting");
            Files.createDirectories(dir);
            write(dir.resolve("raw_copper_block.json"), RAW_COPPER_SMELTING_RECIPE);
        }

        if (FabricRecipesConfig.enableRawIronBlastingRecipe) {
            Path dir = bbRecipeDir.resolve("blasting");
            Files.createDirectories(dir);
            write(dir.resolve("raw_iron_block.json"), RAW_IRON_BLASTING_RECIPE);
        }

        if (FabricRecipesConfig.enableRawGoldBlastingRecipe) {
            Path dir = bbRecipeDir.resolve("blasting");
            Files.createDirectories(dir);
            write(dir.resolve("raw_gold_block.json"), RAW_GOLD_BLASTING_RECIPE);
        }

        if (FabricRecipesConfig.enableRawCopperBlastingRecipe) {
            Path dir = bbRecipeDir.resolve("blasting");
            Files.createDirectories(dir);
            write(dir.resolve("raw_copper_block.json"), RAW_COPPER_BLASTING_RECIPE);
        }

        Path minecraftRecipeDir = datapackPath.resolve("data").resolve("minecraft").resolve(RECIPE_DIR);
        Files.createDirectories(minecraftRecipeDir);

        if (FabricRecipesConfig.enableRecoveryCompassRecipe) {
          // Vanilla recipe id is `minecraft:recovery_compass` (data/minecraft/recipes/recovery_compass.json).
          // Avoid auto-detection here because other packs/mod resources can introduce
          // alternate recipe ids (e.g., `minecraft:misc/recovery_compass`) which would
          // prevent overriding the vanilla recipe.
          try {
            Files.deleteIfExists(minecraftRecipeDir.resolve("misc").resolve("recovery_compass.json"));
          } catch (IOException ignored) {
          }
          Path target = minecraftRecipeDir.resolve("recovery_compass.json");
          write(target, RECOVERY_COMPASS_RECIPE);
          LOGGER.info("[{}] Wrote dynamic recipe override: minecraft:recovery_compass", BitsAndBalanceCommon.MOD_ID);
        }

        if (FabricRecipesConfig.enableMapInkSacRecipe) {
            write(minecraftRecipeDir.resolve("map.json"), MAP_INK_SAC_RECIPE);
          LOGGER.info("[{}] Wrote dynamic recipe override: minecraft:map", BitsAndBalanceCommon.MOD_ID);

          Path tagsRoot = datapackPath.resolve("data").resolve("bitsandbalance").resolve("tags");

          Path tagsItemsDir = tagsRoot.resolve("items");
          Files.createDirectories(tagsItemsDir);
          write(tagsItemsDir.resolve("map_ink.json"), MAP_INK_TAG);

          // Some versions/loaders use "tags/item" rather than "tags/items".
          Path tagsItemDir = tagsRoot.resolve("item");
          Files.createDirectories(tagsItemDir);
          write(tagsItemDir.resolve("map_ink.json"), MAP_INK_TAG);
        }

        if (FabricRecipesConfig.enableEnderEyeRecipe) {
          write(minecraftRecipeDir.resolve("ender_eye.json"), ENDER_EYE_RECIPE);
          LOGGER.info("[{}] Wrote dynamic recipe: minecraft:ender_eye", BitsAndBalanceCommon.MOD_ID);
        }

        writeStairRecipes(recipeManager, registryAccess, datapackPath);

      }

    private static boolean enableDatapack(PackRepository repo, boolean reloadRepository) {
      boolean changed = FabricGeneratedWorldDataPacks.enableGeneratedPack(repo, DATAPACK_NAME, reloadRepository);
      String packId = findDynamicPackId(repo);
      boolean packExists = repo.getAvailablePacks().stream().anyMatch(p -> p.getId().equals(packId));
      if (!packExists) {
        LOGGER.warn(
            "[{}] Alternative recipes datapack not available after repo.reload(): {} (check pack.mcmeta)",
            BitsAndBalanceCommon.MOD_ID,
            packId
        );
        return changed;
      }

        boolean alreadyEnabled = repo.getSelectedPacks().stream().anyMatch(p -> p.getId().equals(packId));
        ArrayList<String> next = new ArrayList<>(repo.getSelectedPacks().stream().map(Pack::getId).toList());

        if (alreadyEnabled) {
            LOGGER.info(
                    "[{}] Alternative recipes datapack already enabled: {}",
                    BitsAndBalanceCommon.MOD_ID,
                    packId
            );
        } else {
            LOGGER.info(
                    "[{}] Enabled alternative recipes datapack: {} (selected packs now: {})",
                    BitsAndBalanceCommon.MOD_ID,
                    packId,
                    next
            );
        }
        return changed;
    }

    private static boolean disableDatapack(PackRepository repo, boolean reloadRepository) {
      boolean alreadyEnabled = repo.getSelectedPacks().stream().anyMatch(p -> isDynamicPackId(p.getId()));
        if (!alreadyEnabled) return false;

      return FabricGeneratedWorldDataPacks.disableGeneratedPack(repo, DATAPACK_NAME, reloadRepository);
    }

    private static boolean isDynamicPackId(String id) {
      if (id == null) return false;
      if (id.equals("bitsandbalance/generated/" + DATAPACK_NAME)) return true;
      if (id.equals("file/" + DATAPACK_NAME)) return true;
      // Some pack finders use different prefixes; match by suffix to be resilient.
      return id.endsWith("/" + DATAPACK_NAME) || id.endsWith(DATAPACK_NAME);
    }

    private static String findDynamicPackId(PackRepository repo) {
      // Prefer the actual id reported by the pack repository.
      try {
        for (Pack p : repo.getAvailablePacks()) {
          if (p == null) continue;
          String id = p.getId();
          if (isDynamicPackId(id)) {
            return id;
          }
        }
      } catch (Throwable ignored) {
      }
      return "bitsandbalance/generated/" + DATAPACK_NAME;
    }

    private static void reloadWithCommand(MinecraftServer server) {
      try {
        LOGGER.info("[{}] Running server reload command...", BitsAndBalanceCommon.MOD_ID);
        server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "reload");
      } catch (Exception e) {
        LOGGER.warn("[{}] Failed to run reload command: {}", BitsAndBalanceCommon.MOD_ID, e.toString());
      }
    }

    private static void addStairRecipeFiles(Map<String, String> files, RecipeManager recipeManager, RegistryAccess registryAccess) {
      if (FabricRecipesConfig.enableStairRecipeOverride) {
        var stairRecipes = FamilyCompatRecipeGenerator.generateStairOverrideRecipes(recipeManager, registryAccess);
        for (var entry : stairRecipes.entrySet()) {
          Identifier recipeId = entry.getKey();
          files.put("data/" + recipeId.getNamespace() + "/recipe/" + recipeId.getPath() + ".json", entry.getValue());
        }
        LOGGER.info(
            "[{}] Created {} stair recipe overrides from discovered stair families",
            BitsAndBalanceCommon.MOD_ID,
            stairRecipes.size()
        );
      }

      if (FabricRecipesConfig.enableCompactStairRecipe) {
        var compactStairRecipes = FamilyCompatRecipeGenerator.generateCompactStairRecipes();
        for (var entry : compactStairRecipes.entrySet()) {
          Identifier recipeId = entry.getKey();
          files.put("data/" + recipeId.getNamespace() + "/recipe/" + recipeId.getPath() + ".json", entry.getValue());
        }
        LOGGER.info(
            "[{}] Created {} compact 2x2 stair recipes from discovered stair families",
            BitsAndBalanceCommon.MOD_ID,
            compactStairRecipes.size()
        );
      }
    }

    private static void writeStairRecipes(RecipeManager recipeManager, RegistryAccess registryAccess, Path datapackPath) throws IOException {
      Map<String, String> files = new LinkedHashMap<>();
      addStairRecipeFiles(files, recipeManager, registryAccess);
      writeRelativeFiles(datapackPath, files);
    }

    private static void writeRelativeFiles(Path datapackPath, Map<String, String> files) throws IOException {
      for (Map.Entry<String, String> entry : files.entrySet()) {
        Path target = datapackPath.resolve(entry.getKey());
        Path parent = target.getParent();
        if (parent != null) {
          Files.createDirectories(parent);
        }
        write(target, entry.getValue());
      }
    }

    private static void write(Path path, String content) throws IOException {
        Files.createDirectories(path.getParent());
      Files.writeString(
        path,
        content,
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.TRUNCATE_EXISTING,
        StandardOpenOption.WRITE
      );
    }

    private static void deleteRecursively(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (var stream = Files.list(path)) {
                for (Path child : stream.toList()) {
                    deleteRecursively(child);
                }
            }
        }
        Files.deleteIfExists(path);
    }
}
