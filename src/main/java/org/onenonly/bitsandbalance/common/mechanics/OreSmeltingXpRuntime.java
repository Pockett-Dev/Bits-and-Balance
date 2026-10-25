package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Runtime values for the configurable experience awarded by vanilla ore-block
 * smelting and blasting recipes.
 */
public final class OreSmeltingXpRuntime {
    public static volatile boolean enabled = true;
    public static volatile double coal = 0.5D;
    public static volatile double lapis = 1.0D;
    public static volatile double netherQuartz = 1.0D;
    public static volatile double iron = 3.5D;
    public static volatile double copper = 3.5D;
    public static volatile double redstone = 3.5D;
    public static volatile double gold = 5.0D;
    public static volatile double netherGold = 5.0D;
    public static volatile double diamond = 5.0D;
    public static volatile double emerald = 5.0D;
    public static volatile double ancientDebris = 10.0D;

    private OreSmeltingXpRuntime() {
    }

    /**
     * Returns the configured XP for a supported vanilla ore block, or NaN when
     * the original recipe value should be preserved.
     */
    public static float configuredExperience(Item input) {
        if (!enabled) {
            return Float.NaN;
        }
        if (input == Items.COAL_ORE || input == Items.DEEPSLATE_COAL_ORE) {
            return (float) coal;
        }
        if (input == Items.LAPIS_ORE || input == Items.DEEPSLATE_LAPIS_ORE) {
            return (float) lapis;
        }
        if (input == Items.NETHER_QUARTZ_ORE) {
            return (float) netherQuartz;
        }
        if (input == Items.IRON_ORE || input == Items.DEEPSLATE_IRON_ORE) {
            return (float) iron;
        }
        if (input == Items.COPPER_ORE || input == Items.DEEPSLATE_COPPER_ORE) {
            return (float) copper;
        }
        if (input == Items.REDSTONE_ORE || input == Items.DEEPSLATE_REDSTONE_ORE) {
            return (float) redstone;
        }
        if (input == Items.GOLD_ORE || input == Items.DEEPSLATE_GOLD_ORE) {
            return (float) gold;
        }
        if (input == Items.NETHER_GOLD_ORE) {
            return (float) netherGold;
        }
        if (input == Items.DIAMOND_ORE || input == Items.DEEPSLATE_DIAMOND_ORE) {
            return (float) diamond;
        }
        if (input == Items.EMERALD_ORE || input == Items.DEEPSLATE_EMERALD_ORE) {
            return (float) emerald;
        }
        if (input == Items.ANCIENT_DEBRIS) {
            return (float) ancientDebris;
        }
        return Float.NaN;
    }
}
