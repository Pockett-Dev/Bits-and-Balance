package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import org.onenonly.bitsandbalance.Config;
/**
 * Nerfed Discounts: Remove zombie curing discounts and cap maximum discounts at configurable percentage.
 * Intercepts villager trading mechanics to prevent exploitation and balance the economy.
 */
@Mixin(Villager.class)
public class VillagerTradingMixin {

    /**
     * Intercept trade updates to apply discount caps to all merchant offers.
     * This ensures no villager can offer more than the configured maximum discount.
     */
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void rebalance$capDiscounts(CallbackInfo ci) {
        if (!Config.enableNerfedDiscounts) return;
        
        Villager self = (Villager) (Object) this;
        MerchantOffers offers = self.getOffers();
        
        if (offers == null || offers.isEmpty()) return;
        
        try {
            // Apply discount cap to all offers
            for (MerchantOffer offer : offers) {
                applyDiscountCap(offer);
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes if mappings change
        }
    }

    /**
     * Remove reputation bonuses from zombie villager curing.
     * This prevents the major discount that comes from curing zombie villagers.
     */
    @Inject(method = "onReputationEventFrom", at = @At("HEAD"), cancellable = true)
    private void rebalance$removeZombieCureDiscount(ReputationEventType reputationEventType, Entity entity, CallbackInfo ci) {
        if (!Config.removeZombieCureDiscounts) return;
        
        try {
            // Cancel reputation events related to zombie villager curing
            if (reputationEventType == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
                // This is the reputation event given when curing zombie villagers
                // We cancel it to prevent the associated discount
                ci.cancel();
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes if mappings change
        }
    }

    /**
     * Apply the configured discount cap to a merchant offer.
     * Ensures the offer price doesn't go below the minimum allowed by the max discount setting.
     */
    private void applyDiscountCap(MerchantOffer offer) {
        try {
            // Get base cost from the first cost item
            int originalBaseCostA = offer.getBaseCostA().getCount();
            
            // Calculate minimum allowed costs based on max discount
            double minMultiplier = 1.0D - Config.maxVillagerDiscount;
            int minCostA = Math.max(1, (int) Math.ceil(originalBaseCostA * minMultiplier));
            
            // Get current cost (after all discounts are applied)
            int currentCostA = offer.getCostA().getCount();
            
            // If current cost is below minimum, adjust it to the minimum
            if (currentCostA < minCostA) {
                offer.getCostA().setCount(minCostA);
            }
            
            // Handle second cost item if it exists
            if (!offer.getCostB().isEmpty()) {
                // For costB, we need to use a different approach since there's no getBaseCostB
                // We'll calculate based on the current costB and apply proportional limits
                int currentCostB = offer.getCostB().getCount();
                // Apply the same discount cap logic proportionally
                int minCostB = Math.max(1, (int) Math.ceil(currentCostB / (1.0D - Config.maxVillagerDiscount)));
                
                if (currentCostB < minCostB) {
                    offer.getCostB().setCount(minCostB);
                }
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes if the offer structure changes
        }
    }
}
