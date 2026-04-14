package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public final class EnhancedSlabPistonMoveCache {
    private EnhancedSlabPistonMoveCache() {
    }

    public record Entry(long sourcePosLong, long destPosLong, EnhancedSlabPistonData data) {
    }

    private static final ConcurrentHashMap<Long, Entry> BY_DEST_POS_LONG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Entry> BY_SOURCE_POS_LONG = new ConcurrentHashMap<>();

    public static void putMove(long sourcePosLong, long destPosLong, EnhancedSlabPistonData data) {
        if (data == null) return;

        Entry entry = new Entry(sourcePosLong, destPosLong, data);
        BY_DEST_POS_LONG.put(destPosLong, entry);
        BY_SOURCE_POS_LONG.put(sourcePosLong, entry);
    }

    public static @Nullable Entry takeForMove(long destPosLong, long sourcePosLong) {
        Entry entry = BY_DEST_POS_LONG.remove(destPosLong);
        if (entry == null) {
            entry = BY_SOURCE_POS_LONG.remove(sourcePosLong);
            if (entry == null) {
                return null;
            }
            BY_DEST_POS_LONG.remove(entry.destPosLong());
            return entry;
        }

        BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        return entry;
    }

    public static void clear() {
        BY_DEST_POS_LONG.clear();
        BY_SOURCE_POS_LONG.clear();
    }
}