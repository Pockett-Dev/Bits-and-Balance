package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.util.RandomSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.onenonly.bitsandbalance.fabric.mobs.FabricImprovedWanderingTraders;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(WanderingTrader.class)
public abstract class WanderingTraderMixin {
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void bitsandbalance$improvedWanderingTraderTrades(CallbackInfo ci) {
        if (!FabricImprovedWanderingTraders.enabled()) return;

        WanderingTrader trader = (WanderingTrader) (Object) this;
        if (!(trader.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        MerchantOffers offers;
        try {
            offers = trader.getOffers();
        } catch (Throwable t) {
            return;
        }
        if (offers == null) return;

        if (FabricImprovedWanderingTraders.replaceVanillaTrades()) {
            try {
                offers.clear();
            } catch (Throwable ignored) {
            }
        }

        RandomSource random;
        try {
            random = trader.getRandom();
        } catch (Throwable t) {
            return;
        }

        // Add a small, wandering-trader-like selection from our enabled pools.
        addRandomOffers(serverLevel, trader, random, offers, FabricImprovedWanderingTraders.enabledGenericTrades(), 5);
        addRandomOffers(serverLevel, trader, random, offers, FabricImprovedWanderingTraders.enabledRareTrades(), 1);
    }

    private static void addRandomOffers(
            ServerLevel serverLevel,
            WanderingTrader trader,
            RandomSource random,
            MerchantOffers offers,
            List<VillagerTrades.ItemListing> pool,
            int count
    ) {
        if (pool == null || pool.isEmpty()) return;

        List<VillagerTrades.ItemListing> remaining = new ArrayList<>(pool);
        int tries = Math.min(count, remaining.size());
        for (int i = 0; i < tries; i++) {
            int idx;
            try {
                idx = random.nextInt(remaining.size());
            } catch (Throwable t) {
                idx = 0;
            }
            VillagerTrades.ItemListing listing = remaining.remove(idx);
            if (listing == null) continue;

            MerchantOffer offer;
            try {
                offer = listing.getOffer(serverLevel, trader, random);
            } catch (Throwable t) {
                offer = null;
            }

            if (offer != null) {
                try {
                    offers.add(offer);
                } catch (Throwable ignored) {
                }
            }
        }
    }
}
