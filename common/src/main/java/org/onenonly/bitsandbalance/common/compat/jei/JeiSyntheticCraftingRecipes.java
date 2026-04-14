package org.onenonly.bitsandbalance.common.compat.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.recipe.SlabFamilyBlockLookup;
import org.onenonly.bitsandbalance.common.recipe.VerticalSlabRecipeInputs;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JEI-only synthetic crafting recipes for dynamic/custom crafting behavior that does not
 * exist as normal loaded datapack recipe entries.
 */
public final class JeiSyntheticCraftingRecipes {

    private JeiSyntheticCraftingRecipes() {
    }

    public static List<RecipeHolder<CraftingRecipe>> buildAll(boolean includeVerticalSlabs, boolean includeSteps) {
        List<RecipeHolder<CraftingRecipe>> recipes = new ArrayList<>();

        if (includeVerticalSlabs) {
            addVerticalSlabRecipes(recipes);
        }

        if (includeSteps) {
            addStepRecipes(recipes);
        }

        return recipes;
    }

    private static void addVerticalSlabRecipes(List<RecipeHolder<CraftingRecipe>> recipes) {
        Set<String> seenRecipes = new LinkedHashSet<>();

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
            if (verticalBlock == null) continue;
            if (!shouldExposeEnhancedSlabJei(slabBlock, verticalBlock)) continue;

            Item slabItem = slabBlock.asItem();
            Item verticalItem = verticalBlock.asItem();
            if (!isRealItem(slabItem) || !isRealItem(verticalItem)) continue;

            Identifier slabId = BuiltInRegistries.ITEM.getKey(slabItem);
            Identifier verticalId = BuiltInRegistries.ITEM.getKey(verticalItem);
            if (slabId == null || verticalId == null) continue;

            Item displayInput = VerticalSlabRecipeInputs.displayInputForSlab(slabBlock);
            Identifier displayInputId = displayInput == null ? null : BuiltInRegistries.ITEM.getKey(displayInput);
            if (isRealItem(displayInput) && displayInputId != null
                    && seenRecipes.add("vertical_slab_from_input:" + displayInputId + "->" + verticalId)) {
                recipes.add(holder(
                        "vertical_slabs/from_input/" + sanitize(displayInputId) + "/to/" + sanitize(verticalId),
                        shaped(
                                "bitsandbalance.jei.vertical_slab_from_input",
                                new ItemStack(verticalItem, 6),
                                Map.of('A', Ingredient.of(displayInput)),
                                "A",
                                "A",
                                "A"
                        )
                ));
            }

            if (seenRecipes.add("vertical_slab_convert:" + slabId + "->" + verticalId)) {
                recipes.add(holder(
                        "vertical_slabs/from_slab/" + sanitize(slabId) + "/to/" + sanitize(verticalId),
                        shapeless(
                                "bitsandbalance.jei.vertical_slab_convert",
                                new ItemStack(verticalItem),
                                Ingredient.of(slabItem)
                        )
                ));
                recipes.add(holder(
                        "slabs/from_vertical_slab/" + sanitize(verticalId) + "/to/" + sanitize(slabId),
                        shapeless(
                                "bitsandbalance.jei.vertical_slab_convert",
                                new ItemStack(slabItem),
                                Ingredient.of(verticalItem)
                        )
                ));
            }
        }
    }

    private static void addStepRecipes(List<RecipeHolder<CraftingRecipe>> recipes) {
        Set<String> seenRecipes = new LinkedHashSet<>();

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
            if (stepBlock == null) continue;
            if (!shouldExposeEnhancedSlabJei(slabBlock, stepBlock)) continue;

            Item slabItem = slabBlock.asItem();
            Item stepItem = stepBlock.asItem();
            if (!isRealItem(slabItem) || !isRealItem(stepItem)) continue;

            Identifier slabId = BuiltInRegistries.ITEM.getKey(slabItem);
            Identifier stepId = BuiltInRegistries.ITEM.getKey(stepItem);
            if (slabId == null || stepId == null) continue;

            if (seenRecipes.add("step_from_slab:" + slabId + "->" + stepId)) {
                recipes.add(holder(
                        "steps/from_slab/" + sanitize(slabId) + "/to/" + sanitize(stepId),
                        shaped(
                                "bitsandbalance.jei.step_from_slab",
                                new ItemStack(stepItem, 4),
                                Map.of('A', Ingredient.of(slabItem)),
                                "AA"
                        )
                ));
                recipes.add(holder(
                        "slabs/from_step/" + sanitize(stepId) + "/to/" + sanitize(slabId),
                        shaped(
                                "bitsandbalance.jei.step_from_slab",
                                new ItemStack(slabItem),
                                Map.of('A', Ingredient.of(stepItem)),
                                "AA"
                        )
                ));
            }

            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(slabBlock);
            if (baseBlock != null) {
                Item baseItem = baseBlock.asItem();
                Identifier baseId = BuiltInRegistries.ITEM.getKey(baseItem);
                if (isRealItem(baseItem) && baseId != null && seenRecipes.add("base_block_recovery:" + slabId + "->" + baseId)) {
                    recipes.add(holder(
                            "base_blocks/from_slab/" + sanitize(slabId) + "/to/" + sanitize(baseId),
                            shaped(
                                    "bitsandbalance.jei.base_block_recovery",
                                    new ItemStack(baseItem),
                                    Map.of('A', Ingredient.of(slabItem)),
                                    "A",
                                    "A"
                            )
                    ));
                }
            }

            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
            if (verticalBlock == null) continue;
            Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalBlock);
            if (verticalStepBlock == null) continue;
            if (!shouldExposeEnhancedSlabJei(slabBlock, verticalBlock, verticalStepBlock)) continue;

            Item verticalItem = verticalBlock.asItem();
            Item verticalStepItem = verticalStepBlock.asItem();
            if (!isRealItem(verticalItem) || !isRealItem(verticalStepItem)) continue;

            Identifier verticalId = BuiltInRegistries.ITEM.getKey(verticalItem);
            Identifier verticalStepId = BuiltInRegistries.ITEM.getKey(verticalStepItem);
            if (verticalId == null || verticalStepId == null) continue;

            if (seenRecipes.add("vertical_step_from_vertical_slab:" + verticalId + "->" + verticalStepId)) {
                recipes.add(holder(
                        "vertical_steps/from_vertical_slab/" + sanitize(verticalId) + "/to/" + sanitize(verticalStepId),
                        shaped(
                                "bitsandbalance.jei.vertical_step_from_vertical_slab",
                                new ItemStack(verticalStepItem, 4),
                                Map.of('A', Ingredient.of(verticalItem)),
                                "AA"
                        )
                ));
                recipes.add(holder(
                        "vertical_slabs/from_vertical_step/" + sanitize(verticalStepId) + "/to/" + sanitize(verticalId),
                        shaped(
                                "bitsandbalance.jei.vertical_step_from_vertical_slab",
                                new ItemStack(verticalItem),
                                Map.of('A', Ingredient.of(verticalStepItem)),
                                "AA"
                        )
                ));
            }

            if (seenRecipes.add("step_convert:" + stepId + "->" + verticalStepId)) {
                recipes.add(holder(
                        "vertical_steps/from_step/" + sanitize(stepId) + "/to/" + sanitize(verticalStepId),
                        shapeless(
                                "bitsandbalance.jei.step_convert",
                                new ItemStack(verticalStepItem),
                                Ingredient.of(stepItem)
                        )
                ));
                recipes.add(holder(
                        "steps/from_vertical_step/" + sanitize(verticalStepId) + "/to/" + sanitize(stepId),
                        shapeless(
                                "bitsandbalance.jei.step_convert",
                                new ItemStack(stepItem),
                                Ingredient.of(verticalStepItem)
                        )
                ));
            }
        }
    }

    private static RecipeHolder<CraftingRecipe> holder(String path, CraftingRecipe recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("bitsandbalance", "jei/" + path);
        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        return new RecipeHolder<>(key, recipe);
    }

    private static ShapedRecipe shaped(String group, ItemStack result,
                                       Map<Character, Ingredient> keys,
                                       String... patternRows) {
        ShapedRecipePattern pattern = ShapedRecipePattern.of(keys, patternRows);

        try {
            return instantiateModernShapedRecipe(group, result, pattern);
        } catch (ReflectiveOperationException modernFailure) {
            try {
                return instantiateLegacyShapedRecipe(group, result, pattern);
            } catch (ReflectiveOperationException legacyFailure) {
                modernFailure.addSuppressed(legacyFailure);
                throw new IllegalStateException("Unable to create synthetic shaped JEI recipe", modernFailure);
            }
        }
    }

    private static ShapelessRecipe shapeless(String group, ItemStack result, Ingredient... ingredients) {
        List<Ingredient> ingredientList = List.of(ingredients);

        try {
            return instantiateModernShapelessRecipe(group, result, ingredientList);
        } catch (ReflectiveOperationException modernFailure) {
            try {
                return instantiateLegacyShapelessRecipe(group, result, ingredientList);
            } catch (ReflectiveOperationException legacyFailure) {
                modernFailure.addSuppressed(legacyFailure);
                throw new IllegalStateException("Unable to create synthetic shapeless JEI recipe", modernFailure);
            }
        }
    }

    private static ShapedRecipe instantiateModernShapedRecipe(String group, ItemStack result, ShapedRecipePattern pattern)
            throws ReflectiveOperationException {
        Class<?> commonInfoClass = Class.forName("net.minecraft.world.item.crafting.Recipe$CommonInfo");
        Class<?> craftingBookInfoClass = Class.forName("net.minecraft.world.item.crafting.CraftingRecipe$CraftingBookInfo");
        Class<?> itemStackTemplateClass = Class.forName("net.minecraft.world.item.ItemStackTemplate");
        Object commonInfo = commonInfoClass.getConstructor(boolean.class).newInstance(false);
        Object craftingBookInfo = craftingBookInfoClass.getConstructor(CraftingBookCategory.class, String.class)
                .newInstance(CraftingBookCategory.BUILDING, group);
        Object itemStackTemplate = itemStackTemplateClass.getMethod("fromNonEmptyStack", ItemStack.class).invoke(null, result);

        return ShapedRecipe.class.getConstructor(commonInfoClass, craftingBookInfoClass, ShapedRecipePattern.class, itemStackTemplateClass)
                .newInstance(commonInfo, craftingBookInfo, pattern, itemStackTemplate);
    }

    private static ShapedRecipe instantiateLegacyShapedRecipe(String group, ItemStack result, ShapedRecipePattern pattern)
            throws ReflectiveOperationException {
        try {
            return ShapedRecipe.class.getConstructor(String.class, CraftingBookCategory.class, ShapedRecipePattern.class, ItemStack.class, boolean.class)
                    .newInstance(group, CraftingBookCategory.BUILDING, pattern, result, false);
        } catch (NoSuchMethodException ignored) {
            return ShapedRecipe.class.getConstructor(String.class, CraftingBookCategory.class, ShapedRecipePattern.class, ItemStack.class)
                    .newInstance(group, CraftingBookCategory.BUILDING, pattern, result);
        }
    }

    private static ShapelessRecipe instantiateModernShapelessRecipe(String group, ItemStack result, List<Ingredient> ingredients)
            throws ReflectiveOperationException {
        Class<?> commonInfoClass = Class.forName("net.minecraft.world.item.crafting.Recipe$CommonInfo");
        Class<?> craftingBookInfoClass = Class.forName("net.minecraft.world.item.crafting.CraftingRecipe$CraftingBookInfo");
        Class<?> itemStackTemplateClass = Class.forName("net.minecraft.world.item.ItemStackTemplate");
        Object commonInfo = commonInfoClass.getConstructor(boolean.class).newInstance(false);
        Object craftingBookInfo = craftingBookInfoClass.getConstructor(CraftingBookCategory.class, String.class)
                .newInstance(CraftingBookCategory.BUILDING, group);
        Object itemStackTemplate = itemStackTemplateClass.getMethod("fromNonEmptyStack", ItemStack.class).invoke(null, result);

        return ShapelessRecipe.class.getConstructor(commonInfoClass, craftingBookInfoClass, itemStackTemplateClass, List.class)
                .newInstance(commonInfo, craftingBookInfo, itemStackTemplate, ingredients);
    }

    private static ShapelessRecipe instantiateLegacyShapelessRecipe(String group, ItemStack result, List<Ingredient> ingredients)
            throws ReflectiveOperationException {
        return ShapelessRecipe.class.getConstructor(String.class, CraftingBookCategory.class, ItemStack.class, List.class)
                .newInstance(group, CraftingBookCategory.BUILDING, result, ingredients);
    }

    private static boolean isRealItem(@Nullable Item item) {
        return item != null && item != Items.AIR;
    }

    private static boolean shouldExposeEnhancedSlabJei(Block... blocks) {
        for (Block block : blocks) {
            if (block == null) continue;
        }

        return true;
    }

    private static String sanitize(Identifier id) {
        return id.getNamespace() + "/" + id.getPath();
    }
}