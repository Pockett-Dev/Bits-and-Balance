package org.onenonly.bitsandbalance.common.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Server-side helper for "Automatic Tool Restock".
 */
public final class AutomaticToolRestock {
    private AutomaticToolRestock() {
    }

    /**
     * Attempts to restock a broken hotbar tool with a matching item from the main inventory.
     *
     * @param player The server-side player.
     * @param hotbarSlot 0-8.
     * @param brokenOriginal A copy of the stack before it broke.
     * @return true if a replacement was moved into the hotbar slot.
     */
    public static boolean tryRestockBrokenHotbarTool(ServerPlayer player, int hotbarSlot, ItemStack brokenOriginal) {
        if (player == null) return false;
        if (hotbarSlot < 0 || hotbarSlot > 8) return false;
        if (brokenOriginal == null || brokenOriginal.isEmpty()) return false;

        Inventory inv;
        try {
            inv = player.getInventory();
        } catch (Throwable ignored) {
            return false;
        }

        ItemStack current;
        try {
            current = inv.getItem(hotbarSlot);
        } catch (Throwable ignored) {
            return false;
        }

        // Only fill an empty slot; never overwrite.
        if (current != null && !current.isEmpty()) return false;

        Item item;
        try {
            item = brokenOriginal.getItem();
        } catch (Throwable ignored) {
            return false;
        }
        if (item == null) return false;

        int bestIdx = -1;
        long bestScore = Long.MIN_VALUE;

        // Search main inventory only (exclude hotbar to avoid disrupting it).
        for (int i = 9; i <= 35; i++) {
            ItemStack candidate;
            try {
                candidate = inv.getItem(i);
            } catch (Throwable ignored) {
                continue;
            }
            if (candidate == null || candidate.isEmpty()) continue;
            try {
                if (candidate.getItem() != item) continue;
            } catch (Throwable ignored) {
                continue;
            }

            long score = similarityScore(brokenOriginal, candidate);
            if (score > bestScore) {
                bestScore = score;
                bestIdx = i;
            }
        }

        if (bestIdx < 0) return false;

        try {
            ItemStack replacement = inv.getItem(bestIdx);
            if (replacement == null || replacement.isEmpty()) return false;

            inv.setItem(hotbarSlot, replacement);
            inv.setItem(bestIdx, ItemStack.EMPTY);
            InventorySyncUtil.syncPlayerInventory(player);

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static long similarityScore(ItemStack a, ItemStack b) {
        // Higher is better.
        long score = 0L;

        // Prefer more remaining durability as a secondary tie-break.
        score += (long) remainingDurability(b);

        ItemEnchantments ea = a.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        ItemEnchantments eb = b.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);

        int aSilk = enchantLevel(ea, Enchantments.SILK_TOUCH);
        int bSilk = enchantLevel(eb, Enchantments.SILK_TOUCH);
        int aFortune = enchantLevel(ea, Enchantments.FORTUNE);
        int bFortune = enchantLevel(eb, Enchantments.FORTUNE);

        // Big preference for matching Silk Touch / Fortune state.
        if (aSilk > 0) {
            score += (bSilk > 0) ? 10_000 : -8_000;
        } else if (bSilk > 0) {
            score -= 2_000;
        }

        if (aFortune > 0) {
            score += (bFortune > 0) ? 9_000 : -7_000;
        } else if (bFortune > 0) {
            score -= 1_500;
        }

        // Moderate preference for matching some common "tool quality" enchants.
        score += matchBonus(ea, eb, Enchantments.MENDING, 600);
        score += matchBonus(ea, eb, Enchantments.UNBREAKING, 250);
        score += matchBonus(ea, eb, Enchantments.EFFICIENCY, 250);

        // Generic enchantment overlap score.
        try {
            for (var holder : ea.keySet()) {
                int la = ea.getLevel(holder);
                int lb = eb.getLevel(holder);
                if (lb > 0) {
                    score += 100L;
                    score += 20L * Math.min(la, lb);
                    if (la == lb) score += 60L;
                } else {
                    score -= 35L;
                }
            }
            for (var holder : eb.keySet()) {
                if (ea.getLevel(holder) > 0) continue;
                score -= 5L;
            }
        } catch (Throwable ignored) {
        }

        return score;
    }

    private static int remainingDurability(ItemStack stack) {
        try {
            if (stack == null || stack.isEmpty() || !stack.isDamageableItem()) return 0;
            return Math.max(0, stack.getMaxDamage() - stack.getDamageValue());
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int enchantLevel(ItemEnchantments ench, net.minecraft.resources.ResourceKey<Enchantment> key) {
        if (ench == null) return 0;
        try {
            for (var holder : ench.keySet()) {
                if (holder.is(key)) {
                    return ench.getLevel(holder);
                }
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static long matchBonus(ItemEnchantments a, ItemEnchantments b, net.minecraft.resources.ResourceKey<Enchantment> key, long weight) {
        int la = enchantLevel(a, key);
        int lb = enchantLevel(b, key);
        if (la <= 0) return 0L;
        if (lb <= 0) return -weight;
        if (la == lb) return weight;
        return (long) (weight / 2);
    }
}
