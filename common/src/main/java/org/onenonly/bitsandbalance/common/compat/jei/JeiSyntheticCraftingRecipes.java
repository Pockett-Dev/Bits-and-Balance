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
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.recipe.SlabFamilyBlockLookup;
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
            if (verticalBlock == null) continue;
            if (!shouldExposeEnhancedSlabJei(slabBlock, verticalBlock)) continue;

            Item slabItem = slabBlock.asItem();
            Item verticalItem = verticalBlock.asItem();
            if (!isRealItem(slabItem) || !isRealItem(verticalItem)) continue;
        }
    }

    private static void addStepRecipes(List<RecipeHolder<CraftingRecipe>> recipes) {
        Set<Block> seenVerticalSteps = new LinkedHashSet<>();

        for (Block slabBlock : BuiltInRegistries.BLOCK) {
            Block stepBlock = StepDynamicRegistry.getStepForSlab(slabBlock);
            if (stepBlock == null) continue;
            if (!shouldExposeEnhancedSlabJei(slabBlock, stepBlock)) continue;

            Item slabItem = slabBlock.asItem();
            Item stepItem = stepBlock.asItem();
            if (!isRealItem(slabItem) || !isRealItem(stepItem)) continue;

            Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(slabBlock);
            if (verticalBlock == null) continue;
            Block verticalStepBlock = VerticalStepDynamicRegistry.getVerticalStepForVerticalSlab(verticalBlock);
            if (verticalStepBlock == null || !seenVerticalSteps.add(verticalStepBlock)) continue;
            if (!shouldExposeEnhancedSlabJei(slabBlock, verticalBlock, verticalStepBlock)) continue;

            Item verticalStepItem = verticalStepBlock.asItem();
            Item verticalItem = verticalBlock.asItem();
            if (!isRealItem(verticalItem) || !isRealItem(verticalStepItem)) continue;

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
        return new ShapedRecipe(
                group,
                CraftingBookCategory.BUILDING,
                ShapedRecipePattern.of(keys, patternRows),
                result,
                false
        );
    }

    private static ShapelessRecipe shapeless(String group, ItemStack result, Ingredient... ingredients) {
        return new ShapelessRecipe(group, CraftingBookCategory.BUILDING, result, List.of(ingredients));
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

    private static @Nullable Item findRepresentativeVerticalSlabCraftingInput(Block slabBlock) {
        Identifier slabId = BuiltInRegistries.BLOCK.getKey(slabBlock);
        if (slabId == null) return null;

        String namespace = slabId.getNamespace();
        String path = slabId.getPath();
        if (!path.endsWith("_slab")) return null;

        String base = path.substring(0, path.length() - "_slab".length());
        List<Identifier> candidates = new ArrayList<>();

        candidates.add(Identifier.fromNamespaceAndPath(namespace, base + "_planks"));
        if (base.endsWith("_brick")) {
            candidates.add(Identifier.fromNamespaceAndPath(namespace, base.substring(0, base.length() - "_brick".length()) + "_bricks"));
        }
        candidates.add(Identifier.fromNamespaceAndPath(namespace, base + "_block"));
        candidates.add(Identifier.fromNamespaceAndPath(namespace, base));

        for (Identifier candidate : candidates) {
            Optional<net.minecraft.core.Holder.Reference<Item>> holder = BuiltInRegistries.ITEM.get(candidate);
            if (holder.isPresent()) {
                Item item = holder.get().value();
                if (isRealItem(item)) {
                    return item;
                }
            }
        }

        return null;
    }
}