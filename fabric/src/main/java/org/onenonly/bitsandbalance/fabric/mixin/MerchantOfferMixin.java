package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Nerfed Discounts: extra enforcement at the offer level.
 */
@Mixin(MerchantOffer.class)
public class MerchantOfferMixin {

    @Inject(method = "updateDemand", at = @At("TAIL"))
    private void bitsandbalance$enforceDiscountCap(CallbackInfo ci) {
        if (!FabricBalanceConfig.enableNerfedDiscounts) return;

        MerchantOffer self = (MerchantOffer) (Object) this;
        try {
            enforceDiscountLimits(self);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "getCostA", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$capFinalCostA(CallbackInfoReturnable<ItemStack> cir) {
        if (!FabricBalanceConfig.enableNerfedDiscounts) return;

        try {
            MerchantOffer self = (MerchantOffer) (Object) this;
            ItemStack result = cir.getReturnValue();

            int baseCost = self.getBaseCostA().getCount();
            double maxDiscount = FabricBalanceConfig.maxVillagerDiscount;
            if (Double.isNaN(maxDiscount)) maxDiscount = 0.0D;
            if (maxDiscount < 0.0D) maxDiscount = 0.0D;
            if (maxDiscount > 1.0D) maxDiscount = 1.0D;

            double minMultiplier = 1.0D - maxDiscount;
            int minCost = Math.max(1, (int) Math.ceil(baseCost * minMultiplier));

            if (result.getCount() < minCost) {
                ItemStack adjustedStack = result.copy();
                adjustedStack.setCount(minCost);
                cir.setReturnValue(adjustedStack);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "getCostB", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$capFinalCostB(CallbackInfoReturnable<ItemStack> cir) {
        if (!FabricBalanceConfig.enableNerfedDiscounts) return;

        try {
            ItemStack result = cir.getReturnValue();
            if (result.isEmpty()) return;

            double maxDiscount = FabricBalanceConfig.maxVillagerDiscount;
            if (Double.isNaN(maxDiscount)) maxDiscount = 0.0D;
            if (maxDiscount < 0.0D) maxDiscount = 0.0D;
            if (maxDiscount > 1.0D) maxDiscount = 1.0D;

            int currentCount = result.getCount();
            double minMultiplier = 1.0D - maxDiscount;
            int minCost = Math.max(1, (int) Math.ceil(currentCount / minMultiplier));

            if (currentCount < minCost) {
                ItemStack adjustedStack = result.copy();
                adjustedStack.setCount(minCost);
                cir.setReturnValue(adjustedStack);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void enforceDiscountLimits(MerchantOffer offer) {
        try {
            int baseCostA = offer.getCostA().getCount();

            double maxDiscountPercent = FabricBalanceConfig.maxVillagerDiscount;
            if (Double.isNaN(maxDiscountPercent)) maxDiscountPercent = 0.0D;
            if (maxDiscountPercent < 0.0D) maxDiscountPercent = 0.0D;
            if (maxDiscountPercent > 1.0D) maxDiscountPercent = 1.0D;

            int maxDiscountAmount = (int) Math.floor(baseCostA * maxDiscountPercent);

            int currentDemand = offer.getDemand();
            int specialPriceDiff = offer.getSpecialPriceDiff();

            int totalDiscount = Math.abs(Math.min(0, currentDemand + specialPriceDiff));

            if (totalDiscount > maxDiscountAmount) {
                int adjustedSpecialDiff = Math.max(0, maxDiscountAmount - Math.abs(Math.min(0, currentDemand)));
                offer.setSpecialPriceDiff(-adjustedSpecialDiff);
            }
        } catch (Throwable ignored) {
        }
    }
}
