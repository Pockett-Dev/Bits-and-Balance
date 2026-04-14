package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class VerticalSlabStonecuttingRecipeGenerator {
    private VerticalSlabStonecuttingRecipeGenerator() {
    }

    public static Map<String, String> generate(RecipeManager recipeManager, RegistryAccess registryAccess) {
        Map<String, String> result = new HashMap<>(EnhancedSlabCraftingRecipeGenerator.generateVerticalSlabRecipes());
        result.putAll(generateStonecuttingMirrorRecipes(recipeManager, registryAccess));
        return result;
    }

    public static Map<String, String> generateStonecuttingMirrorRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        Map<String, String> result = new HashMap<>();
        try {
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (!(holder.value() instanceof StonecutterRecipe stonecutting)) continue;

                ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
                if (output == null || output.isEmpty()) continue;

                Item outputItem = output.getItem();
                Block outputBlock = (outputItem instanceof BlockItem bi) ? bi.getBlock() : null;
                if (outputBlock == null) continue;

                Block verticalBlock = VerticalSlabDynamicRegistry.getVerticalForSlab(outputBlock);
                if (verticalBlock == null) continue;

                Item verticalItem = verticalBlock.asItem();
                if (verticalItem == null) continue;

                Identifier verticalItemId = BuiltInRegistries.ITEM.getKey(verticalItem);
                if (verticalItemId == null) continue;

                String ingredientJson = buildIngredientJson(stonecutting);
                if (ingredientJson == null) continue;

                Identifier recipeId = holder.id().identifier();
                if (recipeId == null || recipeId.getPath().contains("_wood_sc_")) {
                    continue;
                }

                String safeName = StonecutterOutputSortHelper.buildDerivedRecipeName("vertical_slab_sc", recipeId, verticalItem);

                String json = "{\n"
                        + "  \"type\": \"minecraft:stonecutting\",\n"
                        + "  \"ingredient\": " + ingredientJson + ",\n"
                        + "  \"result\": {\n"
                        + "    \"id\": \"" + verticalItemId + "\",\n"
                        + "    \"count\": " + output.getCount() + "\n"
                        + "  }\n"
                        + "}";

                result.put(safeName, json);
            }
        } catch (Exception ignored) {
        }
        return result;
    }

    private static String buildIngredientJson(StonecutterRecipe recipe) {
        try {
            var ingredient = recipe.input();
            var items = new ArrayList<>(ingredient.items().toList());
            if (items.isEmpty()) return null;

            if (items.size() == 1) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(items.get(0).value());
                return itemId != null ? "\"" + itemId + "\"" : null;
            }

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (var h : items) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(h.value());
                if (itemId != null) {
                    if (!first) sb.append(", ");
                    sb.append("\"").append(itemId).append("\"");
                    first = false;
                }
            }
            sb.append("]");
            return first ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}