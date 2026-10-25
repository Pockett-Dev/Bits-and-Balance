package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;

public final class EnhancedSlabHangIntentClientCache {
    private static final LongOpenHashSet EXPLICIT_HANG_POSITIONS = new LongOpenHashSet();

    private EnhancedSlabHangIntentClientCache() {
    }

    public static void clear() {
        EXPLICIT_HANG_POSITIONS.clear();
    }

    public static void apply(long posLong, boolean explicit) {
        if (explicit) {
            EXPLICIT_HANG_POSITIONS.add(posLong);
        } else {
            EXPLICIT_HANG_POSITIONS.remove(posLong);
        }
    }

    public static boolean has(long posLong) {
        return EXPLICIT_HANG_POSITIONS.contains(posLong);
    }

    public static boolean has(BlockPos pos) {
        return pos != null && has(pos.asLong());
    }
}