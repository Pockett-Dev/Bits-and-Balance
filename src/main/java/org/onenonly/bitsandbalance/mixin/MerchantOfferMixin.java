package org.onenonly.bitsandbalance.mixin;

import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Additional protection for merchant offers to ensure discount caps are enforced
 * at the offer level, providing a safety net for any discount applications.
 */
@Mixin(MerchantOffer.class)
public class MerchantOfferMixin {

    /**
     * Intercept price updates to ensure they don't violate our discount cap.
     * This provides an additional layer of protection beyond the villager-level checks.
     */
    @Inject(method = "updateDemand", at = @At("TAIL"))
    private void rebalance$enforceDiscountCap(CallbackInfo ci) {
        if (!Config.enableNerfedDiscounts) return;
        
        MerchantOffer self = (MerchantOffer) (Object) this;
        
        try {
            // Enforce discount cap after any demand updates
            enforceDiscountLimits(self);
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes if mappings change
        }
    }

    /**
     * Intercept the getCostA method to ensure Hero of the Village discounts don't exceed our cap.
     * This is where the final price calculation happens, including Hero of the Village effects.
     */
    @Inject(method = "getCostA", at = @At("RETURN"), cancellable = true)
    private void rebalance$capFinalCostA(CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (!Config.enableNerfedDiscounts) return;
        
        try {
            MerchantOffer self = (MerchantOffer) (Object) this;
            net.minecraft.world.item.ItemStack result = cir.getReturnValue();
            
            // Get the base cost and calculate minimum allowed
            int baseCost = self.getBaseCostA().getCount();
            double minMultiplier = 1.0D - Config.maxVillagerDiscount;
            int minCost = Math.max(1, (int) Math.ceil(baseCost * minMultiplier));
            
            // If the final cost is below our minimum, adjust it
            if (result.getCount() < minCost) {
                net.minecraft.world.item.ItemStack adjustedStack = result.copy();
                adjustedStack.setCount(minCost);
                cir.setReturnValue(adjustedStack);
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }

    /**
     * Intercept the getCostB method to ensure Hero of the Village discounts don't exceed our cap.
     */
    @Inject(method = "getCostB", at = @At("RETURN"), cancellable = true)
    private void rebalance$capFinalCostB(CallbackInfoReturnable<net.minecraft.world.item.ItemStack> cir) {
        if (!Config.enableNerfedDiscounts) return;
        
        try {
            net.minecraft.world.item.ItemStack result = cir.getReturnValue();
            
            if (result.isEmpty()) return;
            
            // For cost B, we calculate minimum based on a proportional approach
            // since there's no direct getBaseCostB method available
            int currentCount = result.getCount();
            double minMultiplier = 1.0D - Config.maxVillagerDiscount;
            int minCost = Math.max(1, (int) Math.ceil(currentCount / minMultiplier));
            
            // If the current count suggests the discount is too high, adjust it
            if (currentCount < minCost) {
                net.minecraft.world.item.ItemStack adjustedStack = result.copy();
                adjustedStack.setCount(minCost);
                cir.setReturnValue(adjustedStack);
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }

    /**
     * Enforce discount limits on a merchant offer.
     * Ensures discounts don't exceed the maximum threshold defined by maxVillagerDiscount.
     */
    private void enforceDiscountLimits(MerchantOffer offer) {
        try {
            // Get the base cost of the primary item
            int baseCostA = offer.getCostA().getCount();
            
            // Calculate the maximum allowed discount
            double maxDiscountPercent = Config.maxVillagerDiscount;
            int maxDiscountAmount = (int) Math.floor(baseCostA * maxDiscountPercent);
            
            // Get current price modifiers
            int currentDemand = offer.getDemand();
            int specialPriceDiff = offer.getSpecialPriceDiff();
            
            // Calculate total discount being applied (negative values are discounts)
            int totalDiscount = Math.abs(Math.min(0, currentDemand + specialPriceDiff));
            
            // If discount exceeds our cap, adjust it
            if (totalDiscount > maxDiscountAmount) {
                // Reset special price diff to prevent excessive discounts
                int adjustedSpecialDiff = Math.max(0, maxDiscountAmount - Math.abs(Math.min(0, currentDemand)));
                offer.setSpecialPriceDiff(-adjustedSpecialDiff);
            }
        } catch (Throwable ignored) {
            // Fail silently to avoid crashes
        }
    }
}
