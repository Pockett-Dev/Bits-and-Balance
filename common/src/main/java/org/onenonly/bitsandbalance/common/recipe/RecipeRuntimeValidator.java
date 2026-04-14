package org.onenonly.bitsandbalance.common.recipe;

import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
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

    public static void validate(MinecraftServer server) {
        if (server == null) return;

        Level level = server.overworld();
        if (level == null) return;

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

        validate(failures, recipeManager, registries, level,
                "base_block_recovery slab->base",
                grid(2, 2, stack("minecraft:oak_slab"), ItemStack.EMPTY, stack("minecraft:oak_slab"), ItemStack.EMPTY),
                "minecraft:oak_planks",
                1);

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
            return;
        }

        for (String failure : failures) {
            LOGGER.warn("[{}] Recipe runtime validation failed: {}", BitsAndBalanceCommon.MOD_ID, failure);
        }
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
                        if ("base_block_recovery slab->base".equals(scenario)) {
                                failures.add(scenario + " did not select any crafting recipe; "
                                        + describeCandidate(recipeManager, level, input, "bitsandbalance:base_block_recovery") + "; "
                                        + describeCandidate(recipeManager, level, input, "bitsandbalance:base_block_from_slab__minecraft__oak_slab__to__minecraft__oak_planks") + "; "
                                        + describeCandidate(recipeManager, level, input, "bitsandbalance:step_from_slab__minecraft__oak_slab__to__bitsandbalance__oak_step") + "; "
                                        + describeCandidate(recipeManager, level, input, "bitsandbalance:vertical_slab_from_planks__minecraft__oak_planks__to__bitsandbalance__vertical_oak_slab") + "; "
                                        + describeCandidate(recipeManager, level, input, "bitsandbalance:50_step_step_sc_minecraft_cobblestone__bitsandbalance_cobblestone_step"));
                                return;
                        }
                        failures.add(scenario + " did not select any crafting recipe");
            return;
        }

                RecipeHolder<Recipe<CraftingInput>> holder = selected.get();
                Recipe<CraftingInput> craftingRecipe = holder.value();
                Identifier selectedId = holder.id().identifier();

        if (!craftingRecipe.matches(input, level)) {
                        failures.add(scenario + " selected recipe " + selectedId + " but matches() returned false for sample input");
            return;
        }

        ItemStack directResult = craftingRecipe.assemble(input, registries);
        if (!sameResult(directResult, expected)) {
                        failures.add(scenario + " selected recipe " + selectedId + " assembled " + describe(directResult) + " expected " + describe(expected));
            return;
        }

                String selectedNamespace = selectedId.getNamespace();
                if (!BitsAndBalanceCommon.MOD_ID.equals(selectedNamespace)) {
                        failures.add(scenario + " selected unexpected recipe " + selectedId);
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

    private static String describe(ItemStack stack) {
        if (stack.isEmpty()) return "empty";
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id + " x" + stack.getCount();
    }

        @SuppressWarnings("unchecked")
        private static String describeCandidate(RecipeManager recipeManager, Level level, CraftingInput input, String recipeId) {
                Identifier id = Identifier.parse(recipeId);
                for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                        if (!holder.id().identifier().equals(id)) {
                                continue;
                        }

                        Recipe<?> rawRecipe = holder.value();
                        if (!(rawRecipe instanceof Recipe<?> recipe)) {
                                return recipeId + " present but not a crafting recipe";
                        }

                        Recipe<CraftingInput> craftingRecipe = (Recipe<CraftingInput>) recipe;
                        boolean matches = craftingRecipe.matches(input, level);
                        ItemStack result = craftingRecipe.assemble(input, level.registryAccess());
                        return recipeId + " present matches=" + matches + " result=" + describe(result);
                }

                return recipeId + " missing";
        }

        private static CraftingInput grid(int width, int height, ItemStack... stacks) {
                NonNullList<ItemStack> items = NonNullList.withSize(width * height, ItemStack.EMPTY);
                for (int i = 0; i < stacks.length && i < items.size(); i++) {
                        items.set(i, stacks[i]);
                }
                return CraftingInput.of(width, height, items);
        }
}