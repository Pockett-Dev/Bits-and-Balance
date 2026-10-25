package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Live startup validation for the core dynamic slab-family crafting recipes.
 */
public final class RecipeRuntimeValidator {
    private static final Logger LOGGER = LogUtils.getLogger();

    private RecipeRuntimeValidator() {
    }

        public static boolean validate(MinecraftServer server) {
                if (server == null) return true;

        Level level = server.overworld();
                if (level == null) return true;

        RecipeManager recipeManager = server.getRecipeManager();
        HolderLookup.Provider registries = server.registryAccess();
        List<String> failures = new ArrayList<>();

        ItemStack verticalSlabSample = stack("bitsandbalance:vertical_oak_slab");
        ItemStack verticalStepSample = stack("bitsandbalance:vertical_oak_step");

        if (!verticalSlabSample.isEmpty()) {
            validate(failures, recipeManager, registries, level,
                    "vertical_slab_convert slab->vertical",
                    grid(2, 2, stack("minecraft:oak_slab"), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                    "bitsandbalance:vertical_oak_slab",
                    1);
            validate(failures, recipeManager, registries, level,
                    "vertical_slab_convert vertical->slab",
                    grid(2, 2, verticalSlabSample, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                    "minecraft:oak_slab",
                    1);
        }

        validate(failures, recipeManager, registries, level,
                "step_from_slab slab->step",
                grid(2, 2, stack("minecraft:oak_slab"), stack("minecraft:oak_slab"), ItemStack.EMPTY, ItemStack.EMPTY),
                "bitsandbalance:oak_step",
                4);
        validate(failures, recipeManager, registries, level,
                "step_from_slab step->slab",
                grid(2, 2, stack("bitsandbalance:oak_step"), stack("bitsandbalance:oak_step"), ItemStack.EMPTY, ItemStack.EMPTY),
                "minecraft:oak_slab",
                1);

        if (!verticalStepSample.isEmpty()) {
            validate(failures, recipeManager, registries, level,
                    "step_convert step->vertical_step",
                    grid(2, 2, stack("bitsandbalance:oak_step"), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                    "bitsandbalance:vertical_oak_step",
                    1);
            validate(failures, recipeManager, registries, level,
                    "step_convert vertical_step->step",
                    grid(2, 2, verticalStepSample, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY),
                    "bitsandbalance:oak_step",
                    1);
        }

        if (!verticalSlabSample.isEmpty() && !verticalStepSample.isEmpty()) {
            validate(failures, recipeManager, registries, level,
                    "vertical_step_from_vertical_slab slab->step",
                    grid(2, 2, verticalSlabSample, verticalSlabSample.copy(), ItemStack.EMPTY, ItemStack.EMPTY),
                    "bitsandbalance:vertical_oak_step",
                    4);
            validate(failures, recipeManager, registries, level,
                    "vertical_step_from_vertical_slab step->slab",
                    grid(2, 2, verticalStepSample, verticalStepSample.copy(), ItemStack.EMPTY, ItemStack.EMPTY),
                    "bitsandbalance:vertical_oak_slab",
                    1);
        }

        validateVanillaResult(failures, recipeManager, registries, level,
                "vertical stone-brick slabs retain vanilla chiseled recipe",
                grid(2, 2, stack("minecraft:stone_brick_slab"), ItemStack.EMPTY,
                        stack("minecraft:stone_brick_slab"), ItemStack.EMPTY),
                "minecraft:chiseled_stone_bricks",
                1);
        validateRecoveryRecipesRemoved(failures, recipeManager);

        if (!verticalSlabSample.isEmpty()) {
            validate(failures, recipeManager, registries, level,
                    "vertical_slab_from_planks planks->vertical_slab",
                    grid(3, 3,
                            stack("minecraft:oak_planks"), ItemStack.EMPTY, ItemStack.EMPTY,
                            stack("minecraft:oak_planks"), ItemStack.EMPTY, ItemStack.EMPTY,
                            stack("minecraft:oak_planks"), ItemStack.EMPTY, ItemStack.EMPTY),
                    "bitsandbalance:vertical_oak_slab",
                    6);
        }

        if (failures.isEmpty()) {
            LOGGER.info("[{}] Recipe runtime validation passed for core slab/step conversions", BitsAndBalanceCommon.MOD_ID);
                        return true;
        }

        for (String failure : failures) {
            LOGGER.warn("[{}] Recipe runtime validation failed: {}", BitsAndBalanceCommon.MOD_ID, failure);
        }
                return false;
    }

    private static void validate(List<String> failures,
                                 RecipeManager recipeManager,
                                 HolderLookup.Provider registries,
                                 Level level,
                                                                 String scenario,
                                 CraftingInput input,
                                 String expectedItemId,
                                 int expectedCount) {
        ItemStack expected = stack(expectedItemId, expectedCount);
        if (expected.isEmpty()) {
                        failures.add(scenario + " expected item missing: " + expectedItemId);
            return;
        }

                Optional<RecipeHolder<Recipe<CraftingInput>>> selected = recipeManager.getRecipeFor((RecipeType<Recipe<CraftingInput>>) (RecipeType<?>) RecipeType.CRAFTING, input, level);
                if (selected.isEmpty()) {
                        failures.add(missingRecipeDetails(scenario, recipeManager, level, input));
            return;
        }

                RecipeHolder<Recipe<CraftingInput>> holder = selected.get();
                Recipe<CraftingInput> craftingRecipe = holder.value();
                Identifier selectedId = recipeId(holder);

        if (!craftingRecipe.matches(input, level)) {
                        failures.add(scenario + " selected recipe " + selectedId + " but matches() returned false for sample input");
            return;
        }

        ItemStack directResult = assemble(craftingRecipe, input, registries);
        if (!BitsAndBalanceCommon.MOD_ID.equals(selectedId.getNamespace())
                && validatesExpectedRecipeDirectly(recipeManager, registries, level, scenario, input, expected)) {
            return;
        }

        if (!sameResult(directResult, expected) && !isEquivalentCompatResult(scenario, input, directResult, expected.getCount())) {
                        failures.add(scenario + " selected recipe " + selectedId + " assembled " + describe(directResult) + " expected " + describe(expected));
            return;
        }

                String selectedNamespace = selectedId.getNamespace();
                if (!BitsAndBalanceCommon.MOD_ID.equals(selectedNamespace) && !isEquivalentCompatResult(scenario, input, directResult, expected.getCount())) {
                        failures.add(scenario + " selected unexpected recipe " + selectedId);
        }
    }

    private static boolean validatesExpectedRecipeDirectly(RecipeManager recipeManager,
                                                           HolderLookup.Provider registries,
                                                           Level level,
                                                           String scenario,
                                                           CraftingInput input,
                                                           ItemStack expected) {
        for (String recipeId : expectedRecipeIds(scenario)) {
            RecipeHolder<?> holder = findRecipe(recipeManager, Identifier.parse(recipeId));
            if (holder == null) {
                continue;
            }

            Recipe<?> rawRecipe = holder.value();
            @SuppressWarnings("unchecked")
            Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) rawRecipe;
            if (!craftingRecipe.matches(input, level)) {
                continue;
            }

            if (sameResult(assemble(craftingRecipe, input, registries), expected)) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static void validateVanillaResult(List<String> failures,
                                              RecipeManager recipeManager,
                                              HolderLookup.Provider registries,
                                              Level level,
                                              String scenario,
                                              CraftingInput input,
                                              String expectedItemId,
                                              int expectedCount) {
        ItemStack expected = stack(expectedItemId, expectedCount);
        if (expected.isEmpty()) {
            failures.add(scenario + " expected item missing: " + expectedItemId);
            return;
        }

        Optional<RecipeHolder<Recipe<CraftingInput>>> selected =
                recipeManager.getRecipeFor((RecipeType<Recipe<CraftingInput>>) (RecipeType<?>) RecipeType.CRAFTING, input, level);
        if (selected.isEmpty()) {
            failures.add(scenario + " did not select any crafting recipe");
            return;
        }

        RecipeHolder<Recipe<CraftingInput>> holder = selected.get();
        Identifier id = recipeId(holder);
        if (!"minecraft".equals(id.getNamespace())) {
            failures.add(scenario + " selected non-vanilla recipe " + id);
            return;
        }

        ItemStack actual = assemble(holder.value(), input, registries);
        if (!sameResult(actual, expected)) {
            failures.add(scenario + " selected " + id + " assembled " + describe(actual)
                    + " expected " + describe(expected));
        }
    }

    private static void validateRecoveryRecipesRemoved(List<String> failures, RecipeManager recipeManager) {
        for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
            Identifier id = recipeId(holder);
            if (!BitsAndBalanceCommon.MOD_ID.equals(id.getNamespace())) {
                continue;
            }

            String path = id.getPath();
            if ("base_block_recovery".equals(path)
                    || path.contains("base_block_from_")
                    || path.contains("base_blocks/from_")) {
                failures.add("obsolete slab-to-full-block recipe still loaded: " + id);
            }
        }
    }

    private static ItemStack stack(String id) {
        return stack(id, 1);
    }

    private static ItemStack stack(String id, int count) {
        Identifier itemId = Identifier.parse(id);
        @Nullable Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) return ItemStack.EMPTY;
        return new ItemStack(item, count);
    }

    private static boolean sameResult(ItemStack actual, ItemStack expected) {
        return !actual.isEmpty()
                && !expected.isEmpty()
                && ItemStack.isSameItemSameComponents(actual, expected)
                && actual.getCount() == expected.getCount();
    }

    private static boolean isEquivalentCompatResult(String scenario, CraftingInput input, ItemStack actual, int expectedCount) {
        if (actual == null || actual.isEmpty() || input == null) {
            return false;
        }
        if (actual.getCount() != expectedCount) {
            return false;
        }

        return switch (scenario) {
            case "vertical_slab_convert slab->vertical" -> isVerticalSlabForInputSlab(singleInputBlock(input), block(actual));
            case "vertical_slab_convert vertical->slab" -> isSlabForInputVerticalSlab(singleInputBlock(input), block(actual));
            case "vertical_slab_from_planks planks->vertical_slab" -> isVerticalSlabForInputFamilyItem(firstInputItem(input), block(actual));
            default -> false;
        };
    }

    private static boolean isVerticalSlabForInputSlab(@Nullable Block inputSlab, @Nullable Block outputVerticalSlab) {
        if (inputSlab == null || outputVerticalSlab == null) {
            return false;
        }
        return EnhancedSlabRecipeLookup.verticalForSlab(inputSlab) == outputVerticalSlab
                || EnhancedSlabRecipeLookup.slabForVertical(outputVerticalSlab) == inputSlab
                || idsMatchSlabToVertical(blockId(inputSlab), blockId(outputVerticalSlab));
    }

    private static boolean isSlabForInputVerticalSlab(@Nullable Block inputVerticalSlab, @Nullable Block outputSlab) {
        if (inputVerticalSlab == null || outputSlab == null) {
            return false;
        }
        return EnhancedSlabRecipeLookup.slabForVertical(inputVerticalSlab) == outputSlab
                || EnhancedSlabRecipeLookup.verticalForSlab(outputSlab) == inputVerticalSlab
                || idsMatchSlabToVertical(blockId(outputSlab), blockId(inputVerticalSlab));
    }

    private static boolean isVerticalSlabForInputFamilyItem(@Nullable Item inputItem, @Nullable Block outputVerticalSlab) {
        if (inputItem == null || outputVerticalSlab == null) {
            return false;
        }

        Block sourceSlab = EnhancedSlabRecipeLookup.slabForVertical(outputVerticalSlab);
        if (sourceSlab == null) {
            return idsMatchFamilyInputToVertical(itemId(inputItem), blockId(outputVerticalSlab));
        }

        return VerticalSlabRecipeInputs.displayInputForSlab(sourceSlab) == inputItem
                || idsMatchFamilyInputToVertical(itemId(inputItem), blockId(outputVerticalSlab));
    }

    private static boolean idsMatchSlabToVertical(@Nullable Identifier slabId, @Nullable Identifier verticalId) {
        if (slabId == null || verticalId == null) {
            return false;
        }

        String slabBase = slabBase(slabId.getPath());
        String verticalBase = verticalSlabBase(verticalId.getPath());
        return slabBase != null && slabBase.equals(verticalBase);
    }

    private static boolean idsMatchFamilyInputToVertical(@Nullable Identifier inputId, @Nullable Identifier verticalId) {
        if (inputId == null || verticalId == null) {
            return false;
        }

        String inputBase = familyInputBase(inputId.getPath());
        String verticalBase = verticalSlabBase(verticalId.getPath());
        return inputBase != null && inputBase.equals(verticalBase);
    }

    private static @Nullable String slabBase(String path) {
        return path.endsWith("_slab") ? path.substring(0, path.length() - "_slab".length()) : null;
    }

    private static @Nullable String verticalSlabBase(String path) {
        if (path.startsWith("vertical_") && path.endsWith("_slab")) {
            return path.substring("vertical_".length(), path.length() - "_slab".length());
        }
        if (path.endsWith("_vertical_slab")) {
            return path.substring(0, path.length() - "_vertical_slab".length());
        }
        return null;
    }

    private static @Nullable String familyInputBase(String path) {
        if (path.endsWith("_planks")) {
            return path.substring(0, path.length() - "_planks".length());
        }
        return switch (path) {
            case "bamboo_mosaic" -> "bamboo_mosaic";
            default -> null;
        };
    }

    private static @Nullable Identifier blockId(Block block) {
        return BuiltInRegistries.BLOCK.getKey(block);
    }

    private static @Nullable Identifier itemId(Item item) {
        return BuiltInRegistries.ITEM.getKey(item);
    }

    private static @Nullable Block singleInputBlock(CraftingInput input) {
        Item item = firstInputItem(input);
        if (item instanceof BlockItem blockItem) {
            return blockItem.getBlock();
        }
        return null;
    }

    private static @Nullable Item firstInputItem(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                return stack.getItem();
            }
        }
        return null;
    }

    private static @Nullable Block block(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
    }

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + " x" + stack.getCount();
    }

        private static String missingRecipeDetails(String scenario, RecipeManager recipeManager, Level level, CraftingInput input) {
                List<String> candidateIds = expectedRecipeIds(scenario);

                if (candidateIds.isEmpty()) {
                        return scenario + " did not select any crafting recipe";
                }

                return scenario + " did not select any crafting recipe; "
                        + candidateIds.stream()
                        .map(recipeId -> describeCandidate(recipeManager, level, input, recipeId))
                        .reduce((left, right) -> left + "; " + right)
                        .orElse("no candidate ids");
        }

        private static List<String> expectedRecipeIds(String scenario) {
                return switch (scenario) {
                        case "vertical_slab_convert slab->vertical", "vertical_slab_convert vertical->slab" ->
                                List.of("bitsandbalance:vertical_slab_convert");
                        case "step_from_slab slab->step", "step_from_slab step->slab" ->
                                List.of("bitsandbalance:step_from_slab");
                        case "step_convert step->vertical_step" ->
                                List.of("bitsandbalance:step_convert");
                        case "step_convert vertical_step->step" ->
                                List.of("bitsandbalance:vertical_step_to_step");
                        case "vertical_step_from_vertical_slab slab->step" ->
                                List.of("bitsandbalance:vertical_step_from_vertical_slab");
                        case "vertical_step_from_vertical_slab step->slab" ->
                                List.of("bitsandbalance:vertical_slab_from_vertical_step");
                        case "vertical_slab_from_planks planks->vertical_slab" ->
                                List.of("bitsandbalance:vertical_slab_from_planks");
                        default -> List.of();
                };
        }

        @SuppressWarnings("unchecked")
        private static String describeCandidate(RecipeManager recipeManager, Level level, CraftingInput input, String recipeId) {
                Identifier id = Identifier.parse(recipeId);
                RecipeHolder<?> holder = findRecipe(recipeManager, id);
                if (holder != null) {

                        Recipe<?> rawRecipe = holder.value();
                        if (!(rawRecipe instanceof Recipe<?> recipe)) {
                                return recipeId + " present but not a crafting recipe";
                        }

                        Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) recipe;
                        boolean matches = craftingRecipe.matches(input, level);
                        ItemStack result = assemble(craftingRecipe, input, level.registryAccess());
                        return recipeId + " present matches=" + matches + " result=" + describe(result);
                }

                return recipeId + " missing";
        }

        private static @Nullable RecipeHolder<?> findRecipe(RecipeManager recipeManager, Identifier id) {
                for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                        if (recipeId(holder).equals(id)) {
                                return holder;
                        }
                }
                return null;
        }

        private static ItemStack assemble(Recipe<CraftingInput> recipe, CraftingInput input, HolderLookup.Provider registries) {
                //? if mc == 26.1 || mc == 26.2 || mc == 26.3
                return recipe.assemble(input);
                //? else
                return recipe.assemble(input, registries);
                //? endif
        }

        private static Identifier recipeId(RecipeHolder<?> holder) {
                //? if mc == 1.21.1
                return holder.id();
                //? else
                return holder.id().identifier();
                //? endif
        }

        private static CraftingInput grid(int width, int height, ItemStack... stacks) {
                NonNullList<ItemStack> items = NonNullList.withSize(width * height, ItemStack.EMPTY);
                for (int i = 0; i < stacks.length && i < items.size(); i++) {
                        items.set(i, stacks[i]);
                }
                return CraftingInput.of(width, height, items);
        }
}
