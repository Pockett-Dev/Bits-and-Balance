package org.onenonly.bitsandbalance.wanderingtraders;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onenonly.bitsandbalance.Config;
/**
 * Registry for all wandering trader trades.
 * This is where you'll add new trades as they're defined.
 */
public class WanderingTraderRegistry {
    private static final List<WanderingTraderTrade> ALL_TRADES = new ArrayList<>();
    private static final Map<String, List<WanderingTraderTrade>> TRADES_BY_CATEGORY = new HashMap<>();

    static {
        registerTrades();
    }

    /**
     * Register all wandering trader trades here.
     * This method will be expanded as you provide new trades.
     */
    private static void registerTrades() {
        // Example trades - these will be replaced/expanded with your actual trade definitions
        
        // Building Materials category
        registerTrade(new WanderingTraderTrade(
            "cobblestone_for_emeralds",
            "buildingMaterials",
            () -> new ItemStack(Items.EMERALD, 1),
            () -> new ItemStack(Items.COBBLESTONE, 16),
            3,
            2,
            0.2f,
            () -> Config.enableBuildingMaterialsTrades
        ));

        registerTrade(new WanderingTraderTrade(
            "stone_bricks_for_emeralds", 
            "buildingMaterials",
            () -> new ItemStack(Items.EMERALD, 2),
            () -> new ItemStack(Items.STONE_BRICKS, 8),
            2,
            3,
            0.1f,
            () -> Config.enableBuildingMaterialsTrades
        ));

        // Rare Items category
        registerTrade(new WanderingTraderTrade(
            "name_tag_for_emeralds",
            "rareItems", 
            () -> new ItemStack(Items.EMERALD, 8),
            () -> new ItemStack(Items.NAME_TAG, 1),
            1,
            10,
            0.05f,
            () -> Config.enableRareItemsTrades
        ));

        // Utility Items category
        registerTrade(new WanderingTraderTrade(
            "torch_bundle",
            "utilityItems",
            () -> new ItemStack(Items.EMERALD, 1),
            () -> new ItemStack(Items.TORCH, 8),
            5,
            1,
            0.2f,
            () -> Config.enableUtilityItemsTrades
        ));

        // Decorative Items category
        registerTrade(new WanderingTraderTrade(
            "flower_assortment",
            "decorativeItems",
            () -> new ItemStack(Items.EMERALD, 1),
            () -> new ItemStack(Items.POPPY, 4),
            3,
            2,
            0.15f,
            () -> Config.enableDecorativeItemsTrades
        ));
    }

    /**
     * Register a single trade and organize it by category.
     */
    private static void registerTrade(WanderingTraderTrade trade) {
        ALL_TRADES.add(trade);
        TRADES_BY_CATEGORY.computeIfAbsent(trade.getCategory(), k -> new ArrayList<>()).add(trade);
    }

    /**
     * Get all registered trades.
     */
    public static List<WanderingTraderTrade> getAllTrades() {
        return new ArrayList<>(ALL_TRADES);
    }

    /**
     * Get trades by category.
     */
    public static List<WanderingTraderTrade> getTradesByCategory(String category) {
        return new ArrayList<>(TRADES_BY_CATEGORY.getOrDefault(category, List.of()));
    }

    /**
     * Get all enabled trades.
     */
    public static List<WanderingTraderTrade> getEnabledTrades() {
        return ALL_TRADES.stream()
                .filter(WanderingTraderTrade::isEnabled)
                .toList();
    }

    /**
     * Get enabled trades by category.
     */
    public static List<WanderingTraderTrade> getEnabledTradesByCategory(String category) {
        return TRADES_BY_CATEGORY.getOrDefault(category, List.of()).stream()
                .filter(WanderingTraderTrade::isEnabled)
                .toList();
    }

    /**
     * Check if the wandering trader improvements are enabled.
     */
    public static boolean isFeatureEnabled() {
        return Config.enableImprovedWanderingTraders;
    }

    /**
     * Check if vanilla trades should be replaced or supplemented.
     */
    public static boolean shouldReplaceVanillaTrades() {
        return Config.replaceVanillaTrades;
    }
}
