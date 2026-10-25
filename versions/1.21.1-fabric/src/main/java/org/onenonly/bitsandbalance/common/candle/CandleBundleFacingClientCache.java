package org.onenonly.bitsandbalance.common.candle;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.Direction;

/**
 * Client-only cache for candle bundle facing.
 *
 * <p>Used to bridge the first-render gap where the bundle block reaches the client
 * before the block entity has received its oriented facing from the server.</p>
 */
public final class CandleBundleFacingClientCache {
    private static final Long2ObjectOpenHashMap<Direction> BY_POS = new Long2ObjectOpenHashMap<>();

    private CandleBundleFacingClientCache() {
    }

    public static void clear() {
        BY_POS.clear();
    }

    public static void remove(long posLong) {
        BY_POS.remove(posLong);
    }

    public static void apply(long posLong, Direction facing) {
        BY_POS.put(posLong, normalize(facing));
    }

    public static Direction get(long posLong) {
        Direction facing = BY_POS.get(posLong);
        return facing == null ? Direction.SOUTH : normalize(facing);
    }

    private static Direction normalize(Direction facing) {
        if (facing == null || !facing.getAxis().isHorizontal()) {
            return Direction.SOUTH;
        }
        return facing;
    }
}