package org.onenonly.bitsandbalance.common.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.Collections;
import java.util.List;

public final class RecipeBookDisplayHelper {
    private RecipeBookDisplayHelper() {
    }

    public static PlacementInfo repeatedPlacement(Ingredient ingredient, int count) {
        return PlacementInfo.create(Collections.nCopies(count, ingredient));
    }

    public static RecipeDisplay shapedFilledDisplay(int width, int height, Ingredient ingredient, ItemStack result) {
        return new ShapedCraftingRecipeDisplay(
                width,
                height,
                Collections.nCopies(width * height, ingredient.display()),
                new SlotDisplay.ItemSlotDisplay(result.getItem()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        );
    }

    public static RecipeDisplay shapelessSingleDisplay(Ingredient ingredient, ItemStack result) {
        return new ShapelessCraftingRecipeDisplay(
                List.of(ingredient.display()),
                new SlotDisplay.ItemSlotDisplay(result.getItem()),
                new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)
        );
    }
}