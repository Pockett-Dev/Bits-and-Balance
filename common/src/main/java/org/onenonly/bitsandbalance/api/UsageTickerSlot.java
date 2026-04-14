package org.onenonly.bitsandbalance.api;

/**
 * Logical slot types that Usage Ticker can display.
 */
public enum UsageTickerSlot {
    MAIN_HAND,
    OFF_HAND,
    ARMOR_HEAD,
    ARMOR_CHEST,
    ARMOR_LEGS,
    ARMOR_FEET;

    public boolean isArmor() {
        return this == ARMOR_HEAD || this == ARMOR_CHEST || this == ARMOR_LEGS || this == ARMOR_FEET;
    }
}
