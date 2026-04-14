package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.ReputationEventType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.onenonly.bitsandbalance.fabric.config.FabricBalanceConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Nerfed Discounts: Remove zombie curing discounts and cap maximum discounts.
 */
@Mixin(Villager.class)
public class VillagerTradingMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void bitsandbalance$capDiscounts(CallbackInfo ci) {
        if (!FabricBalanceConfig.enableNerfedDiscounts) return;

        Villager self = (Villager) (Object) this;
        MerchantOffers offers = self.getOffers();
        if (offers == null || offers.isEmpty()) return;

        try {
            for (MerchantOffer offer : offers) {
                applyDiscountCap(offer);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "onReputationEventFrom", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$removeZombieCureDiscount(ReputationEventType reputationEventType, Entity entity, CallbackInfo ci) {
        if (!FabricBalanceConfig.removeZombieCureDiscounts) return;

        try {
            if (reputationEventType == ReputationEventType.ZOMBIE_VILLAGER_CURED) {
                ci.cancel();
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyDiscountCap(MerchantOffer offer) {
        try {
            int originalBaseCostA = offer.getBaseCostA().getCount();

            double maxDiscount = FabricBalanceConfig.maxVillagerDiscount;
            if (Double.isNaN(maxDiscount)) maxDiscount = 0.0D;
            if (maxDiscount < 0.0D) maxDiscount = 0.0D;
            if (maxDiscount > 1.0D) maxDiscount = 1.0D;

            double minMultiplier = 1.0D - maxDiscount;
            int minCostA = Math.max(1, (int) Math.ceil(originalBaseCostA * minMultiplier));

            int currentCostA = offer.getCostA().getCount();
            if (currentCostA < minCostA) {
                offer.getCostA().setCount(minCostA);
            }

            if (!offer.getCostB().isEmpty()) {
                int currentCostB = offer.getCostB().getCount();
                int minCostB = Math.max(1, (int) Math.ceil(currentCostB / (1.0D - maxDiscount)));
                if (currentCostB < minCostB) {
                    offer.getCostB().setCount(minCostB);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
