package org.onenonly.bitsandbalance.common.candle;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.item.ItemStack;

/**
 * Client-only cache for candle bundle contents.
 *
 * <p>Used to bridge timing gaps where the server sends a contents payload before the
 * client has created the block entity for that position.</p>
 */
public final class CandleBundleContentsClientCache {
    private static final Long2ObjectOpenHashMap<ItemStack[]> BY_POS = new Long2ObjectOpenHashMap<>();

    private CandleBundleContentsClientCache() {
    }

    public static void clear() {
        BY_POS.clear();
    }

    public static void remove(long posLong) {
        BY_POS.remove(posLong);
    }

    public static void apply(long posLong, ItemStack s0, ItemStack s1, ItemStack s2, ItemStack s3) {
        ItemStack[] arr = BY_POS.get(posLong);
        if (arr == null) {
            arr = new ItemStack[] { ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY };
            BY_POS.put(posLong, arr);
        }

        arr[0] = normalize(s0);
        arr[1] = normalize(s1);
        arr[2] = normalize(s2);
        arr[3] = normalize(s3);
    }

    public static ItemStack get(long posLong, int index) {
        ItemStack[] arr = BY_POS.get(posLong);
        if (arr == null || index < 0 || index >= arr.length) {
            return ItemStack.EMPTY;
        }
        return arr[index] == null ? ItemStack.EMPTY : arr[index];
    }

    private static ItemStack normalize(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(1);
    }
}
