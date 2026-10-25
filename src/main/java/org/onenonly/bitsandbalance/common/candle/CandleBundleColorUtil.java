package org.onenonly.bitsandbalance.common.candle;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class CandleBundleColorUtil {
    private CandleBundleColorUtil() {
    }

    public static int effectiveCandleRgb(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0xFFFFFF;
        }

        return 0xFFFFFF;
    }

    /**
     * Returns up to 4 per-candle RGB colors, where indices 0..3 map to model tintIndex 0..3.
     * Missing candles are represented as -1.
     */
    public static int[] computeBundleColors(List<ItemStack> stacks) {
        int[] out = new int[]{-1, -1, -1, -1};
        if (stacks == null || stacks.isEmpty()) {
            return out;
        }

        int max = Math.min(4, stacks.size());
        for (int i = 0; i < max; i++) {
            ItemStack s = stacks.get(i);
            if (s == null || s.isEmpty()) {
                out[i] = 0xFFFFFF;
            } else {
                out[i] = effectiveCandleRgb(s);
            }
        }
        return out;
    }
}
