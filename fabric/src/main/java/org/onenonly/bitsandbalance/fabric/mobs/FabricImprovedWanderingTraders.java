package org.onenonly.bitsandbalance.fabric.mobs;

import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.Items;
import org.onenonly.bitsandbalance.fabric.config.FabricMobsConfig;

import java.util.ArrayList;
import java.util.List;

/** Fabric port: Improved Wandering Traders (trade pool). */
public final class FabricImprovedWanderingTraders {
    private FabricImprovedWanderingTraders() {
    }

    public static boolean enabled() {
        return FabricMobsConfig.enableImprovedWanderingTraders;
    }

    public static boolean replaceVanillaTrades() {
        return FabricMobsConfig.replaceVanillaWanderingTraderTrades;
    }

    public static List<VillagerTrades.ItemListing> enabledGenericTrades() {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        if (!enabled()) return out;

        if (FabricMobsConfig.enableBuildingMaterialsTrades) {
            out.add(new VillagerTrades.ItemsForEmeralds(Items.COBBLESTONE, 1, 16, 3, 2));
            out.add(new VillagerTrades.ItemsForEmeralds(Items.STONE_BRICKS, 2, 8, 2, 3));
        }

        if (FabricMobsConfig.enableUtilityItemsTrades) {
            out.add(new VillagerTrades.ItemsForEmeralds(Items.TORCH, 1, 8, 5, 1));
        }

        if (FabricMobsConfig.enableDecorativeItemsTrades) {
            out.add(new VillagerTrades.ItemsForEmeralds(Items.POPPY, 1, 4, 3, 2));
        }

        return out;
    }

    public static List<VillagerTrades.ItemListing> enabledRareTrades() {
        List<VillagerTrades.ItemListing> out = new ArrayList<>();
        if (!enabled()) return out;

        if (FabricMobsConfig.enableRareItemsTrades) {
            out.add(new VillagerTrades.ItemsForEmeralds(Items.NAME_TAG, 8, 1, 1, 10));
        }

        return out;
    }
}
