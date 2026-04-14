package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.core.RegistryAccess;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class FamilyCompatRecipeGenerator {
    private static final Set<String> SPECIAL_PLURAL_SUFFIXES = Set.of("brick", "tile");

    private FamilyCompatRecipeGenerator() {
    }

    public static Map<String, String> generateStaticVerticalSlabRecipes() {
        Map<String, String> recipes = new LinkedHashMap<>();

        for (SlabFamily family : discoverSlabFamilies()) {
            if (family.verticalSlab() == null || family.wooden()) {
                continue;
            }

            Item inputItem = family.stonecuttingInput();
            if (!isRealItem(inputItem)) {
                continue;
            }

            Item outputItem = family.verticalSlab().asItem();
            if (!isRealItem(outputItem)) {
                continue;
            }

            addStonecuttingRecipe(recipes, "vertical_slab_sc", inputItem, outputItem, 2);
        }

        return recipes;
    }

    public static Map<String, String> generateStaticStepAndRecoveryRecipes() {
        Map<String, String> recipes = new LinkedHashMap<>();

        for (SlabFamily family : discoverSlabFamilies()) {
            if (family.wooden()) {
                continue;
            }

            Item inputItem = family.stonecuttingInput();
            if (!isRealItem(inputItem)) {
                continue;
            }

            if (family.step() != null && isRealItem(family.step().asItem())) {
                addStonecuttingRecipe(recipes, "step_sc", inputItem, family.step().asItem(), 4);
            }

            if (family.verticalStep() != null && isRealItem(family.verticalStep().asItem())) {
                addStonecuttingRecipe(recipes, "step_sc", inputItem, family.verticalStep().asItem(), 4);
            }
        }

        return recipes;
    }

    public static Map<Identifier, String> generateStairOverrideRecipes() {
        return generateStairOverrideRecipes(null, null);
    }

    public static Map<Identifier, String> generateStairOverrideRecipes(@Nullable RecipeManager recipeManager, @Nullable RegistryAccess registryAccess) {
        Map<Identifier, String> recipes = new LinkedHashMap<>();
        Map<Identifier, Set<Identifier>> existingRecipeIds = findExistingStairRecipeIds(recipeManager, registryAccess);

        for (StairFamily family : discoverStairFamilies()) {
            Item baseItem = family.baseBlock().asItem();
            Item stairItem = family.stairBlock().asItem();
            if (!isRealItem(baseItem) || !isRealItem(stairItem)) {
                continue;
            }

            Identifier stairId = BuiltInRegistries.ITEM.getKey(stairItem);
            Identifier baseId = BuiltInRegistries.ITEM.getKey(baseItem);
            if (stairId == null || baseId == null) {
                continue;
            }

            String recipeJson = createStairOverrideRecipe(stairId, baseId);
            LinkedHashSet<Identifier> overrideIds = new LinkedHashSet<>(existingRecipeIds.getOrDefault(stairId, Set.of()));
            if (overrideIds.isEmpty()) {
                overrideIds.add(stairId);
            }

            for (Identifier overrideId : overrideIds) {
                recipes.put(overrideId, recipeJson);
            }
        }

        return recipes;
    }

    public static Map<Identifier, String> generateCompactStairRecipes() {
        Map<Identifier, String> recipes = new LinkedHashMap<>();

        for (StairFamily family : discoverStairFamilies()) {
            Item baseItem = family.baseBlock().asItem();
            Item stairItem = family.stairBlock().asItem();
            if (!isRealItem(baseItem) || !isRealItem(stairItem)) {
                continue;
            }

            Identifier stairId = BuiltInRegistries.ITEM.getKey(stairItem);
            Identifier baseId = BuiltInRegistries.ITEM.getKey(baseItem);
            if (stairId == null || baseId == null) {
                continue;
            }

            recipes.put(
                    Identifier.fromNamespaceAndPath(stairId.getNamespace(), stairId.getPath() + "_compact"),
                    createCompactStairRecipe(stairId, baseId)
            );
        }

        return recipes;
    }

    public static String buildSlabFamilyFingerprint() {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "slab_family_static");

        for (SlabFamily family : discoverSlabFamilies()) {
            builder.add("slab", id(family.slab()));
            builder.add("base", id(family.baseBlock()));
            builder.add("vertical", id(family.verticalSlab()));
            builder.add("step", id(family.step()));
            builder.add("vertical_step", id(family.verticalStep()));
            builder.add("input", itemId(family.representativeInput()));
            builder.add("wooden", family.wooden());
        }

        return builder.build();
    }

    public static String buildStairFamilyFingerprint() {
        DynamicRecipeInputFingerprint.Builder builder = DynamicRecipeInputFingerprint.builder();
        builder.add("type", "stair_family_static");

        for (StairFamily family : discoverStairFamilies()) {
            builder.add("stairs", id(family.stairBlock()));
            builder.add("base", id(family.baseBlock()));
        }

        return builder.build();
    }

    private static List<SlabFamily> discoverSlabFamilies() {
        List<SlabFamily> families = new ArrayList<>();

        for (Block block : BuiltInRegistries.BLOCK) {
            if (!(block instanceof SlabBlock)) {
                continue;
            }

            Block verticalSlab = EnhancedSlabRecipeLookup.verticalForSlab(block);
            Block step = EnhancedSlabRecipeLookup.stepForSlab(block);
            Block verticalStep = verticalSlab == null ? null : EnhancedSlabRecipeLookup.verticalStepForVerticalSlab(verticalSlab);
            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(block);
            Item representativeInput = VerticalSlabRecipeInputs.displayInputForSlab(block);
            boolean wooden = block.builtInRegistryHolder().is(BlockTags.WOODEN_SLABS);

            if (verticalSlab == null && step == null && verticalStep == null && baseBlock == null && !isRealItem(representativeInput)) {
                continue;
            }

            families.add(new SlabFamily(block, baseBlock, verticalSlab, step, verticalStep, representativeInput, wooden));
        }

        families.sort(Comparator.comparing(family -> id(family.slab())));
        return families;
    }

    private static List<StairFamily> discoverStairFamilies() {
        List<StairFamily> families = new ArrayList<>();

        for (Block stairBlock : BuiltInRegistries.BLOCK) {
            Identifier stairId = BuiltInRegistries.BLOCK.getKey(stairBlock);
            if (stairId == null || !stairId.getPath().endsWith("_stairs")) {
                continue;
            }

            Block baseBlock = getBaseBlockForStair(stairBlock);
            if (baseBlock == null || !isRealItem(baseBlock.asItem())) {
                continue;
            }

            families.add(new StairFamily(stairBlock, baseBlock));
        }

        families.sort(Comparator.comparing(family -> id(family.stairBlock())));
        return families;
    }

    private static @Nullable Block getBaseBlockForStair(Block stairBlock) {
        Identifier id = BuiltInRegistries.BLOCK.getKey(stairBlock);
        if (id == null) {
            return null;
        }

        String path = id.getPath();
        if (!path.endsWith("_stairs")) {
            return null;
        }

        String namespace = id.getNamespace();
        String basePath = path.substring(0, path.length() - "_stairs".length());
        LinkedHashSet<Identifier> candidates = new LinkedHashSet<>();

        if (stairBlock.builtInRegistryHolder().is(BlockTags.WOODEN_STAIRS)) {
            addCandidate(candidates, namespace, basePath + "_planks");
        }

        addCandidate(candidates, namespace, basePath);

        String specialBasePath = getSpecialBasePath(basePath);
        if (specialBasePath != null) {
            addCandidate(candidates, namespace, specialBasePath);
        }

        String pluralBasePath = getPluralBasePath(basePath);
        if (pluralBasePath != null) {
            addCandidate(candidates, namespace, pluralBasePath);
        }

        for (Identifier candidate : candidates) {
            if (!BuiltInRegistries.BLOCK.containsKey(candidate)) {
                continue;
            }
            return BuiltInRegistries.BLOCK.getValue(candidate);
        }

        return null;
    }

    private static void addStonecuttingRecipe(Map<String, String> out, String prefix, Item inputItem, Item outputItem, int count) {
        if (!isRealItem(inputItem) || !isRealItem(outputItem)) {
            return;
        }

        Identifier inputId = BuiltInRegistries.ITEM.getKey(inputItem);
        Identifier outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        if (inputId == null || outputId == null) {
            return;
        }

        String recipeType = "minecraft:stonecutting";
        String ingredientJson = itemIngredient(inputId);

        String safeName = StonecutterOutputSortHelper.buildDirectRecipeName(prefix, inputId, outputItem);
        String json = "{\n"
                + "  \"type\": \"" + recipeType + "\",\n"
            + "  \"ingredient\": " + ingredientJson + ",\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + outputId + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }\n"
                + "}";

        out.putIfAbsent(safeName, json);
    }

    private static Map<Identifier, Set<Identifier>> findExistingStairRecipeIds(@Nullable RecipeManager recipeManager, @Nullable RegistryAccess registryAccess) {
        if (recipeManager == null || registryAccess == null) {
            return Map.of();
        }

        Map<Identifier, Set<Identifier>> recipeIdsByResult = new LinkedHashMap<>();

        try {
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (!(holder.value() instanceof ShapedRecipe shapedRecipe) || !isStandardStairPattern(shapedRecipe)) {
                    continue;
                }

                ItemStack result = RecipeCompat.assemble(shapedRecipe, CraftingInput.EMPTY, registryAccess);
                if (result == null || result.isEmpty()) {
                    continue;
                }

                Identifier resultId = BuiltInRegistries.ITEM.getKey(result.getItem());
                Identifier recipeId = holder.id().identifier();
                if (resultId == null || recipeId == null || !resultId.getPath().endsWith("_stairs")) {
                    continue;
                }

                recipeIdsByResult.computeIfAbsent(resultId, ignored -> new LinkedHashSet<>()).add(recipeId);
            }
        } catch (Exception ignored) {
        }

        return recipeIdsByResult;
    }

    private static boolean isStandardStairPattern(ShapedRecipe shapedRecipe) {
        if (shapedRecipe.getWidth() != 3 || shapedRecipe.getHeight() != 3) {
            return false;
        }

        List<java.util.Optional<net.minecraft.world.item.crafting.Ingredient>> ingredients = shapedRecipe.getIngredients();
        int[] filledSlots = {0, 3, 4, 6, 7, 8};
        int[] emptySlots = {1, 2, 5};

        for (int slot : filledSlots) {
            if (slot >= ingredients.size() || ingredients.get(slot).isEmpty()) {
                return false;
            }
        }

        for (int slot : emptySlots) {
            if (slot < ingredients.size() && ingredients.get(slot).isPresent()) {
                return false;
            }
        }

        return true;
    }

    private static String createStairOverrideRecipe(Identifier stairId, Identifier baseId) {
        String group = stairRecipeGroup(stairId);
        return "{\n"
                + "  \"type\": \"minecraft:crafting_shaped\",\n"
                + "  \"category\": \"building\",\n"
            + "  \"group\": \"" + group + "\",\n"
                + "  \"pattern\": [\n"
                + "    \"#  \",\n"
                + "    \"## \",\n"
                + "    \"###\"\n"
                + "  ],\n"
                + "  \"key\": {\n"
                + "    \"#\": " + itemIngredient(baseId) + "\n"
                + "  },\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + stairId + "\",\n"
                + "    \"count\": 8\n"
                + "  }\n"
                + "}";
    }

    private static String createCompactStairRecipe(Identifier stairId, Identifier baseId) {
        String group = stairRecipeGroup(stairId);
        return "{\n"
                + "  \"type\": \"minecraft:crafting_shaped\",\n"
                + "  \"category\": \"building\",\n"
            + "  \"group\": \"" + group + "\",\n"
                + "  \"pattern\": [\n"
                + "    \"# \",\n"
                + "    \"##\"\n"
                + "  ],\n"
                + "  \"key\": {\n"
                + "    \"#\": " + itemIngredient(baseId) + "\n"
                + "  },\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + stairId + "\",\n"
                + "    \"count\": 4\n"
                + "  }\n"
                + "}";
    }

    private static String stairRecipeGroup(Identifier stairId) {
        return "bitsandbalance:stairs/" + stairId.getNamespace() + "/" + stairId.getPath();
    }

    private static String itemIngredient(Identifier id) {
        return "\"" + id + "\"";
    }

    private static boolean isRealItem(@Nullable Item item) {
        return item != null && item != Items.AIR;
    }

    private static void addCandidate(Set<Identifier> candidates, String namespace, @Nullable String path) {
        if (path == null || path.isEmpty()) {
            return;
        }
        candidates.add(Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static @Nullable String getSpecialBasePath(String basePath) {
        return switch (basePath) {
            case "bamboo" -> "bamboo_planks";
            case "quartz" -> "quartz_block";
            case "purpur" -> "purpur_block";
            default -> null;
        };
    }

    private static @Nullable String getPluralBasePath(String basePath) {
        if (basePath == null || basePath.isEmpty()) {
            return null;
        }

        for (String suffix : SPECIAL_PLURAL_SUFFIXES) {
            if (basePath.equals(suffix) || basePath.endsWith("_" + suffix)) {
                return basePath + "s";
            }
        }

        return null;
    }

    private static String id(@Nullable Block block) {
        if (block == null) {
            return "<null>";
        }
        Identifier id = BuiltInRegistries.BLOCK.getKey(block);
        return id == null ? "<null>" : id.toString();
    }

    private static String itemId(@Nullable Item item) {
        if (!isRealItem(item)) {
            return "<null>";
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        return id == null ? "<null>" : id.toString();
    }

    private record SlabFamily(Block slab, @Nullable Block baseBlock, @Nullable Block verticalSlab, @Nullable Block step,
                              @Nullable Block verticalStep, @Nullable Item representativeInput, boolean wooden) {
        private Item stonecuttingInput() {
            if (representativeInput != null && representativeInput != Items.AIR) {
                return representativeInput;
            }
            return baseBlock == null ? Items.AIR : baseBlock.asItem();
        }
    }

    private record StairFamily(Block stairBlock, Block baseBlock) {
    }
}