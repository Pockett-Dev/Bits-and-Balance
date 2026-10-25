package org.onenonly.bitsandbalance.common.inventory;

import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Server-side helper for "Automatic Block Restock".
 */
public final class AutomaticBlockRestock {
    private AutomaticBlockRestock() {
    }

    /**
     * Attempts to restock an empty hotbar slot with a matching block from the main inventory.
     *
     * @param player The server-side player.
     * @param hotbarSlot 0-8.
     * @param emptyOriginal A copy of the stack before it emptied.
     * @return true if a replacement was moved into the hotbar slot.
     */
    public static boolean tryRestockEmptyHotbarBlock(ServerPlayer player, int hotbarSlot, ItemStack emptyOriginal) {
        if (player == null) return false;
        if (hotbarSlot < 0 || hotbarSlot > 8) return false;
        if (emptyOriginal == null || emptyOriginal.isEmpty()) return false;

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
            item = emptyOriginal.getItem();
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

            long score = similarityScore(emptyOriginal, candidate);
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
        // For blocks, we prefer exact matches in NBT/components.
        // Higher is better.
        long score = 0L;

        // Base preference for stack size (more blocks = better).
        score += (long) b.getCount() * 10L;

        // Check if both have the same custom data (e.g., BlockEntityTag for spawners, shulker boxes, etc.).
        CustomData customA = a.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CustomData customB = b.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);

        try {
            if (customA.isEmpty() && customB.isEmpty()) {
                // Both have no custom data - perfect match bonus.
                score += 10_000L;
            } else if (!customA.isEmpty() && !customB.isEmpty()) {
                // Both have custom data - compare them.
                if (customA.equals(customB)) {
                    // Exact match - highest preference.
                    score += 50_000L;
                } else {
                    // Different custom data - moderate penalty.
                    score -= 5_000L;
                }
            } else {
                // One has custom data, the other doesn't - penalty.
                score -= 15_000L;
            }
        } catch (Throwable ignored) {
        }

        // For blocks, also check custom name similarity (e.g., renamed shulker boxes).
        try {
            var nameA = a.get(DataComponents.CUSTOM_NAME);
            var nameB = b.get(DataComponents.CUSTOM_NAME);

            if (nameA == null && nameB == null) {
                score += 1_000L;
            } else if (nameA != null && nameB != null && nameA.equals(nameB)) {
                score += 5_000L;
            } else if (nameA != null || nameB != null) {
                score -= 2_000L;
            }
        } catch (Throwable ignored) {
        }

        return score;
    }
}
