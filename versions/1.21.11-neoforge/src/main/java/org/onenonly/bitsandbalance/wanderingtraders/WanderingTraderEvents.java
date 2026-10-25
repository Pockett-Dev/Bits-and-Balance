package org.onenonly.bitsandbalance.wanderingtraders;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import org.onenonly.bitsandbalance.BitsAndBalance;

import java.util.List;

/**
 * Handles wandering trader trade modifications using the proper NeoForge WandererTradesEvent.
 * This is the correct and recommended approach for modifying wandering trader trades.
 * 
 * Note: WandererTradesEvent is fired on the NeoForge.EVENT_BUS during reload by TagsUpdatedEvent.
 */
@EventBusSubscriber(modid = BitsAndBalance.MODID)
public class WanderingTraderEvents {

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        // Check if the feature is enabled
        if (!WanderingTraderRegistry.isFeatureEnabled()) {
            return;
        }

        try {
            // Get the trade lists from the event
            List<VillagerTrades.ItemListing> genericTrades = event.getGenericTrades();
            List<VillagerTrades.ItemListing> rareTrades = event.getRareTrades();

            // Clear vanilla trades if replacement is enabled
            if (WanderingTraderRegistry.shouldReplaceVanillaTrades()) {
                genericTrades.clear();
                rareTrades.clear();
            }

            // Add our custom trades
            addCustomTrades(genericTrades, rareTrades);

        } catch (Exception e) {
            BitsAndBalance.LOGGER.warn("Failed to modify wandering trader trades: {}", e.getMessage());
        }
    }

    /**
     * Adds custom trades to the wandering trader's trade lists.
     */
    private static void addCustomTrades(List<VillagerTrades.ItemListing> genericTrades, 
                                       List<VillagerTrades.ItemListing> rareTrades) {
        
        // Get all enabled trades from our registry
        List<WanderingTraderTrade> enabledTrades = WanderingTraderRegistry.getEnabledTrades();
        
        for (WanderingTraderTrade trade : enabledTrades) {
            // Create ItemListing using lambda expression (as shown in NeoForge docs)
            VillagerTrades.ItemListing itemListing = (level, entity, random) -> {
                if (!trade.isEnabled()) {
                    return null;
                }
                
                try {
                    ItemStack buy1 = trade.getBuyItem1().get();
                    ItemStack buy2 = trade.getBuyItem2() != null ? trade.getBuyItem2().get() : ItemStack.EMPTY;
                    ItemStack sell = trade.getSellItem().get();

                    if (buy1.isEmpty() || sell.isEmpty()) {
                        return null;
                    }

                    // Offer construction is not implemented by this listing.
                    // Returning null leaves this entry out of the available offers.
                    // The version-specific MerchantOffer constructor is still required.
                    return null;
                } catch (Exception e) {
                    return null;
                }
            };

            // Decide whether this trade goes in generic or rare list
            if (isRareTrade(trade)) {
                rareTrades.add(itemListing);
            } else {
                genericTrades.add(itemListing);
            }
        }
    }

    /**
     * Determines if a trade should be considered "rare" (single trade selected) 
     * or "generic" (multiple trades selected).
     */
    private static boolean isRareTrade(WanderingTraderTrade trade) {
        // The rareItems category selects the rare trade pool.
        // All other categories use the generic pool.
        String category = trade.getCategory();
        
        // Consider "rareItems" category trades as rare
        return "rareItems".equals(category);
    }
}
