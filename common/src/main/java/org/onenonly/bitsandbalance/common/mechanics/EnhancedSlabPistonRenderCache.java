package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import org.jetbrains.annotations.Nullable;

public final class EnhancedSlabPistonRenderCache {
    private EnhancedSlabPistonRenderCache() {
    }

    public record Entry(long sourcePosLong, long destPosLong, EnhancedSlabPistonData data) {
    }

    private static final Long2ObjectOpenHashMap<Entry> BY_DEST_POS_LONG = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Entry> BY_SOURCE_POS_LONG = new Long2ObjectOpenHashMap<>();

    static {
        BY_DEST_POS_LONG.defaultReturnValue(null);
        BY_SOURCE_POS_LONG.defaultReturnValue(null);
    }

    public static void putMove(long sourcePosLong, long destPosLong, EnhancedSlabPistonData data) {
        if (data == null) return;

        Entry entry = new Entry(sourcePosLong, destPosLong, data);
        BY_DEST_POS_LONG.put(destPosLong, entry);
        BY_SOURCE_POS_LONG.put(sourcePosLong, entry);
    }

    public static @Nullable Entry peekForMove(long destPosLong, long sourcePosLong) {
        Entry entry = BY_DEST_POS_LONG.get(destPosLong);
        if (entry != null) return entry;
        return BY_SOURCE_POS_LONG.get(sourcePosLong);
    }

    public static @Nullable Entry takeForMove(long destPosLong, long sourcePosLong) {
        Entry entry = BY_DEST_POS_LONG.remove(destPosLong);
        if (entry == null) {
            entry = BY_SOURCE_POS_LONG.remove(sourcePosLong);
            if (entry == null) return null;
            BY_DEST_POS_LONG.remove(entry.destPosLong());
            return entry;
        }

        BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        return entry;
    }
}