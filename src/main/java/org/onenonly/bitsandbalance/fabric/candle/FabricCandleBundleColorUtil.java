package org.onenonly.bitsandbalance.fabric.candle;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class FabricCandleBundleColorUtil {
    private FabricCandleBundleColorUtil() {
    }

    public static int[] computeBundleColors(List<ItemStack> stacks) {
        int[] out = new int[]{-1, -1, -1, -1};
        if (stacks == null || stacks.isEmpty()) {
            return out;
        }

        int max = Math.min(4, stacks.size());
        for (int i = 0; i < max; i++) {
            ItemStack stack = stacks.get(i);
            out[i] = stack == null || stack.isEmpty() ? 0xFFFFFF : 0xFFFFFF;
        }
        return out;
    }
}