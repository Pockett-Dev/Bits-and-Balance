package org.onenonly.bitsandbalance.common.candle;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;

public final class CandleBundleColorClientCache {
    private static final Long2IntOpenHashMap[] COLORS = new Long2IntOpenHashMap[]{
        new Long2IntOpenHashMap(),
        new Long2IntOpenHashMap(),
        new Long2IntOpenHashMap(),
        new Long2IntOpenHashMap()
    };

    static {
        for (Long2IntOpenHashMap map : COLORS) {
            map.defaultReturnValue(-1);
        }
    }

    private CandleBundleColorClientCache() {
    }

    public static void clear() {
        for (Long2IntOpenHashMap map : COLORS) {
            map.clear();
        }
    }

    public static void apply(long posLong, int c0, int c1, int c2, int c3) {
        applyIndex(0, posLong, c0);
        applyIndex(1, posLong, c1);
        applyIndex(2, posLong, c2);
        applyIndex(3, posLong, c3);
    }

    public static void apply(long posLong, int[] colors) {
        if (colors == null || colors.length < 4) {
            apply(posLong, -1, -1, -1, -1);
            return;
        }
        apply(posLong, colors[0], colors[1], colors[2], colors[3]);
    }

    private static void applyIndex(int index, long posLong, int rgb) {
        if (index < 0 || index >= COLORS.length) return;
        if (rgb == -1) {
            COLORS[index].remove(posLong);
        } else {
            COLORS[index].put(posLong, rgb & 0xFFFFFF);
        }
    }

    public static int rawColor(BlockPos pos, int tintIndex) {
        if (pos == null) return -1;
        if (tintIndex < 0 || tintIndex >= COLORS.length) return -1;
        return COLORS[tintIndex].get(pos.asLong());
    }

    public static int rawColor(long posLong, int tintIndex) {
        if (tintIndex < 0 || tintIndex >= COLORS.length) return -1;
        return COLORS[tintIndex].get(posLong);
    }
}
