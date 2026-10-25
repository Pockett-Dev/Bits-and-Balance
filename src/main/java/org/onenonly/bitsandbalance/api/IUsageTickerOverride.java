package org.onenonly.bitsandbalance.api;

import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

/**
 * Allow items to customize how the Usage Ticker displays and counts them.
 */
public interface IUsageTickerOverride {
    /**
     * Return the logical item to display for the given stack (e.g., bow -> arrows).
     * If you return ItemStack.EMPTY, the ticker will hide for this slot.
     */
    ItemStack getUsageTickerItem(ItemStack stack);

    /**
     * Contribute a custom count for this stack when counting total matches in inventory.
     * Implementations should evaluate the predicate to know what constitutes a match.
     */
    int getUsageTickerCountForItem(ItemStack stack, Predicate<ItemStack> targetPredicate);

    /**
     * If true, the ticker hides when the aggregated total exactly matches the held stack's own count.
     */
    boolean shouldUsageTickerCheckMatchSize(ItemStack stack);
}
