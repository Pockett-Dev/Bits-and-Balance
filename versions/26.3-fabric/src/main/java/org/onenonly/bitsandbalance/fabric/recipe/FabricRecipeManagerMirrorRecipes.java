package org.onenonly.bitsandbalance.fabric.recipe;

import com.mojang.logging.LogUtils;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.BitsAndBalanceCommon;
import org.onenonly.bitsandbalance.common.recipe.RecipeCompat;
import org.onenonly.bitsandbalance.common.recipe.StonecutterOutputSortHelper;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;
import org.onenonly.bitsandbalance.fabric.config.FabricBuildingConfig;
import org.onenonly.bitsandbalance.fabric.mixin.RecipeMapAccessor;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FabricRecipeManagerMirrorRecipes {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FabricRecipeManagerMirrorRecipes() {
    }

    public static RecipeMap appendMirrorRecipes(RecipeMap recipeMap, HolderLookup.Provider registries) {
        if (!FabricBuildingConfig.enhancedSlabsVerticalSlabs && !FabricBuildingConfig.enhancedSlabsSteps) {
            return recipeMap;
        }

        List<RecipeHolder<?>> recipes = new ArrayList<>(recipeMap.values());
        Set<ResourceKey<Recipe<?>>> existingRecipeIds = new HashSet<>();
        for (RecipeHolder<?> holder : recipes) {
            existingRecipeIds.add(holder.id());
        }

        int verticalSlabRecipes = FabricBuildingConfig.enhancedSlabsVerticalSlabs
                ? appendVerticalSlabMirrors(recipeMap.values(), registries, recipes, existingRecipeIds)
                : 0;
        int stepRecipes = FabricBuildingConfig.enhancedSlabsSteps
                ? appendStepMirrors(recipeMap.values(), registries, recipes, existingRecipeIds)
                : 0;

        int added = verticalSlabRecipes + stepRecipes;
        if (added == 0) {
            return recipeMap;
        }

        LOGGER.info(
                "[{}] Added {} in-load mirror stonecutting recipes (vertical slabs: {}, steps: {})",
                BitsAndBalanceCommon.MOD_ID,
                added,
                verticalSlabRecipes,
                stepRecipes
        );
        updateRecipeMap(recipeMap, recipes);
        return recipeMap;
    }

    private static void updateRecipeMap(RecipeMap recipeMap, List<RecipeHolder<?>> recipes) {
        ImmutableMultimap.Builder<RecipeType<?>, RecipeHolder<?>> byType = ImmutableMultimap.builder();
        ImmutableMap.Builder<ResourceKey<Recipe<?>>, RecipeHolder<?>> byKey = ImmutableMap.builder();
        for (RecipeHolder<?> holder : recipes) {
            byType.put(holder.value().getType(), holder);
            byKey.put(holder.id(), holder);
        }
        RecipeMapAccessor accessor = (RecipeMapAccessor) recipeMap;
        accessor.bitsandbalance$setByType(byType.build());
        accessor.bitsandbalance$setByKey(byKey.build());
    }

    private static int appendVerticalSlabMirrors(Iterable<RecipeHolder<?>> sourceRecipes,
                                                 HolderLookup.Provider registries,
                                                 List<RecipeHolder<?>> out,
                                                 Set<ResourceKey<Recipe<?>>> existingRecipeIds) {
        int added = 0;
        for (RecipeHolder<?> holder : sourceRecipes) {
            if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (shouldSkipMirroredRecipe(recipeId)) {
                continue;
            }

            ItemStack output = assemble(stonecutting, registries);
            if (output.isEmpty()) {
                continue;
            }

            Block outputBlock = output.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (outputBlock == null) {
                continue;
            }

            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(outputBlock);
            if (verticalBlock == null) {
                continue;
            }

            Item verticalItem = verticalBlock.asItem();
            if (verticalItem == null) {
                continue;
            }

            String recipeName = StonecutterOutputSortHelper.buildDerivedRecipeName("vertical_slab_sc", recipeId, verticalItem);
            if (addStonecutterRecipe(out, existingRecipeIds, recipeName, stonecutting.input(), verticalItem, output.getCount())) {
                added++;
            }
        }
        return added;
    }

    private static int appendStepMirrors(Iterable<RecipeHolder<?>> sourceRecipes,
                                         HolderLookup.Provider registries,
                                         List<RecipeHolder<?>> out,
                                         Set<ResourceKey<Recipe<?>>> existingRecipeIds) {
        int added = 0;
        Set<String> seenSimpleConversions = new HashSet<>();
        for (RecipeHolder<?> holder : sourceRecipes) {
            if (!(holder.value() instanceof StonecutterRecipe stonecutting)) {
                continue;
            }

            Identifier recipeId = holder.id().identifier();
            if (shouldSkipMirroredRecipe(recipeId)) {
                continue;
            }

            ItemStack output = assemble(stonecutting, registries);
            if (output.isEmpty()) {
                continue;
            }

            Block outputBlock = output.getItem() instanceof BlockItem blockItem ? blockItem.getBlock() : null;
            if (outputBlock == null) {
                continue;
            }

            Block stepBlock = StepDynamicRegistry.getStepForSlab(outputBlock);
            if (stepBlock == null) {
                continue;
            }

            Identifier singleIngredientId = singleIngredientId(stonecutting);
            Item stepItem = stepBlock.asItem();
            if (addStepMirror(out, existingRecipeIds, seenSimpleConversions, recipeId, stonecutting.input(), singleIngredientId, stepItem, output.getCount() * 2, "step")) {
                added++;
            }

            Block verticalSlab = VerticalSlabDynamicRegistry.getVerticalForSlab(outputBlock);
            if (verticalSlab == null) {
                continue;
            }
            Block verticalStep = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalSlab);
            if (verticalStep == null) {
                continue;
            }

            Item verticalStepItem = verticalStep.asItem();
            if (addStepMirror(out, existingRecipeIds, seenSimpleConversions, recipeId, stonecutting.input(), singleIngredientId, verticalStepItem, output.getCount() * 2, "vertical_step")) {
                added++;
            }
        }
        return added;
    }

    private static boolean addStepMirror(List<RecipeHolder<?>> out,
                                         Set<ResourceKey<Recipe<?>>> existingRecipeIds,
                                         Set<String> seenSimpleConversions,
                                         Identifier recipeId,
                                         Ingredient ingredient,
                                         Identifier singleIngredientId,
                                         Item outputItem,
                                         int count,
                                         String suffix) {
        Identifier outputId = BuiltInRegistries.ITEM.getKey(outputItem);
        if (outputId == null) {
            return false;
        }

        if (singleIngredientId != null && !seenSimpleConversions.add(singleIngredientId + "->" + outputId + "@" + count)) {
            return false;
        }

        String recipeName = StonecutterOutputSortHelper.buildDerivedRecipeName("step_sc_" + suffix, recipeId, outputItem);
        return addStonecutterRecipe(out, existingRecipeIds, recipeName, ingredient, outputItem, count);
    }

    private static boolean addStonecutterRecipe(List<RecipeHolder<?>> out,
                                                Set<ResourceKey<Recipe<?>>> existingRecipeIds,
                                                String recipeName,
                                                Ingredient ingredient,
                                                Item outputItem,
                                                int count) {
        if (outputItem == null || count <= 0) {
            return false;
        }

        ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(BitsAndBalanceCommon.MOD_ID, recipeName));
        if (!existingRecipeIds.add(key)) {
            return false;
        }

        StonecutterRecipe recipe = new StonecutterRecipe(
                new Recipe.CommonInfo(true),
                ingredient,
                new ItemStackTemplate(outputItem, count)
        );
        out.add(new RecipeHolder<>(key, recipe));
        return true;
    }

    private static ItemStack assemble(StonecutterRecipe recipe, HolderLookup.Provider registries) {
        try {
            ItemStack output = RecipeCompat.assemble(recipe, new SingleRecipeInput(ItemStack.EMPTY), registries);
            return output == null ? ItemStack.EMPTY : output;
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static Identifier singleIngredientId(StonecutterRecipe recipe) {
        try {
            var items = new ArrayList<>(recipe.input().items().toList());
            if (items.size() != 1) {
                return null;
            }
            return BuiltInRegistries.ITEM.getKey(items.get(0).value());
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean shouldSkipMirroredRecipe(Identifier recipeId) {
        return recipeId == null
                || recipeId.getPath().contains("_wood_sc_")
                || (recipeId.getNamespace().equals(BitsAndBalanceCommon.MOD_ID)
                && (recipeId.getPath().contains("vertical_slab_sc_") || recipeId.getPath().contains("step_sc_")));
    }
}
