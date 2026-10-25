package org.onenonly.bitsandbalance.common.compat.jei;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/** A JEI-only view of one recipe that the Kiln can process. */
public record JeiKilnRecipe(Ingredient input, ItemStack output, float experience, int cookTime) {
}
