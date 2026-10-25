package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public final class VerticalSlabStonecuttingRecipeGenerator {
    private VerticalSlabStonecuttingRecipeGenerator() {
    }

    public static Map<String, String> generate(RecipeManager recipeManager, RegistryAccess registryAccess) {
        //? if mc == 26.1 || mc == 26.2 || mc == 26.3
        return new HashMap<>(generateStonecuttingMirrorRecipes(recipeManager, registryAccess));
        //? else
        Map<String, String> result = new HashMap<>(EnhancedSlabCraftingRecipeGenerator.generateVerticalSlabRecipes());
        result.putAll(generateStonecuttingMirrorRecipes(recipeManager, registryAccess));
        return result;
        //? endif
    }

    public static Map<String, String> generateStonecuttingMirrorRecipes(RecipeManager recipeManager, RegistryAccess registryAccess) {
        Map<String, String> result = new HashMap<>();
        try {
            for (RecipeHolder<?> holder : recipeManager.getRecipes()) {
                if (!(holder.value() instanceof StonecutterRecipe stonecutting)) continue;

                //? if mc == 26.1 || mc == 26.2 || mc == 26.3
                ItemStack output = RecipeCompat.assemble(stonecutting, new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
                //? else
                ItemStack output = stonecutting.assemble(new SingleRecipeInput(ItemStack.EMPTY), registryAccess);
                //? endif
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
                Identifier singleIngredientId = singleIngredientId(stonecutting);
                if (isStaticOwnedMirror(outputBlock, singleIngredientId)) continue;

                //? if mc == 1.21.1
                Identifier recipeId = holder.id();
                //? else
                Identifier recipeId = holder.id().identifier();
                //? endif
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

    private static boolean isStaticOwnedMirror(Block slabBlock, Identifier singleIngredientId) {
        if (singleIngredientId == null || slabBlock.builtInRegistryHolder().is(BlockTags.WOODEN_SLABS)) {
            return false;
        }

        Identifier staticInputId = staticFamilyInputId(slabBlock);
        return singleIngredientId.equals(staticInputId);
    }

    private static Identifier staticFamilyInputId(Block slabBlock) {
        Item inputItem = VerticalSlabRecipeInputs.displayInputForSlab(slabBlock);
        if (inputItem == null || inputItem == Items.AIR) {
            Block baseBlock = SlabFamilyBlockLookup.getBaseBlockForSlab(slabBlock);
            inputItem = baseBlock == null ? Items.AIR : baseBlock.asItem();
        }
        return inputItem == null || inputItem == Items.AIR ? null : BuiltInRegistries.ITEM.getKey(inputItem);
    }

    private static Identifier singleIngredientId(StonecutterRecipe recipe) {
        try {
            //? if mc == 1.21.1
            var items = new ArrayList<>(java.util.Arrays.asList(recipe.getIngredients().getFirst().getItems()));
            //? else
            var items = new ArrayList<>(recipe.input().items().toList());
            //? endif
            if (items.size() != 1) return null;
            //? if mc == 1.21.1
            return BuiltInRegistries.ITEM.getKey(items.get(0).getItem());
            //? else
            return BuiltInRegistries.ITEM.getKey(items.get(0).value());
            //? endif
        } catch (Exception e) {
            return null;
        }
    }

    private static String buildIngredientJson(StonecutterRecipe recipe) {
        try {
            //? if mc == 1.21.1
            var ingredient = recipe.getIngredients().getFirst();
            var items = new ArrayList<>(java.util.Arrays.asList(ingredient.getItems()));
            if (items.isEmpty()) return null;

            if (items.size() == 1) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(items.get(0).getItem());
                return itemId != null ? itemIngredient(itemId) : null;
            }

            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (var h : items) {
                Identifier itemId = BuiltInRegistries.ITEM.getKey(h.getItem());
                if (itemId != null) {
                    if (!first) sb.append(", ");
                    sb.append(itemIngredient(itemId));
                    first = false;
                }
            }
            //? else
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
            //? endif
            sb.append("]");
            return first ? null : sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    //? if mc == 1.21.1
    private static String itemIngredient(Identifier itemId) {
        return "{ \"item\": \"" + itemId + "\" }";
    }
    //? endif
}
