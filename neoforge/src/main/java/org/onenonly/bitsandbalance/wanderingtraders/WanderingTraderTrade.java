package org.onenonly.bitsandbalance.wanderingtraders;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.function.Supplier;

/**
 * Represents a custom wandering trader trade with configuration support.
 */
public class WanderingTraderTrade {
    private final String id;
    private final String category;
    private final Supplier<ItemStack> buyItem1;
    private final Supplier<ItemStack> buyItem2; // Can be null for single-item trades
    private final Supplier<ItemStack> sellItem;
    private final int maxUses;
    private final int xpReward;
    private final float priceMultiplier;
    private final Supplier<Boolean> enabledCheck;

    public WanderingTraderTrade(String id, String category, 
                               Supplier<ItemStack> buyItem1, 
                               Supplier<ItemStack> buyItem2,
                               Supplier<ItemStack> sellItem,
                               int maxUses, 
                               int xpReward, 
                               float priceMultiplier,
                               Supplier<Boolean> enabledCheck) {
        this.id = id;
        this.category = category;
        this.buyItem1 = buyItem1;
        this.buyItem2 = buyItem2;
        this.sellItem = sellItem;
        this.maxUses = maxUses;
        this.xpReward = xpReward;
        this.priceMultiplier = priceMultiplier;
        this.enabledCheck = enabledCheck;
    }

    /**
     * Creates a trade with only one buy item (most common case)
     */
    public WanderingTraderTrade(String id, String category,
                               Supplier<ItemStack> buyItem1,
                               Supplier<ItemStack> sellItem,
                               int maxUses,
                               int xpReward,
                               float priceMultiplier,
                               Supplier<Boolean> enabledCheck) {
        this(id, category, buyItem1, null, sellItem, maxUses, xpReward, priceMultiplier, enabledCheck);
    }

    public String getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public boolean isEnabled() {
        try {
            return enabledCheck.get();
        } catch (Exception e) {
            return false;
        }
    }

    public Supplier<ItemStack> getBuyItem1() {
        return buyItem1;
    }

    public Supplier<ItemStack> getBuyItem2() {
        return buyItem2;
    }

    public Supplier<ItemStack> getSellItem() {
        return sellItem;
    }

    public int getMaxUses() {
        return maxUses;
    }

    public int getXpReward() {
        return xpReward;
    }

    /**
     * Creates a MerchantOffer from this trade definition
     */
    public MerchantOffer createMerchantOffer() {
        if (!isEnabled()) {
            return null;
        }

        try {
            ItemStack buy1 = buyItem1.get();
            ItemStack buy2 = buyItem2 != null ? buyItem2.get() : ItemStack.EMPTY;
            ItemStack sell = sellItem.get();

            if (buy1.isEmpty() || sell.isEmpty()) {
                return null;
            }

            // TODO: Create MerchantOffer with correct constructor
            // For now, return null - this will be fixed once we determine the correct constructor signature
            // The framework is in place, just need to fix this constructor call
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
