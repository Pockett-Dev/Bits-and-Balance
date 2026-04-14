package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public final class PistonBlockEntityMoveCache {
    private PistonBlockEntityMoveCache() {
    }

    public record Entry(long sourcePosLong, long destPosLong, CompoundTag tag) {
        public Entry {
            tag = tag.copy();
        }
    }

    private static final long UNKNOWN_SOURCE_POS_LONG = BlockPos.ZERO.asLong();

    private static final ConcurrentHashMap<Long, Entry> BY_DEST_POS_LONG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Entry> BY_SOURCE_POS_LONG = new ConcurrentHashMap<>();

    public static void putMove(long sourcePosLong, long destPosLong, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return;

        Entry entry = new Entry(sourcePosLong, destPosLong, tag);
        BY_DEST_POS_LONG.put(destPosLong, entry);
        if (sourcePosLong != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.put(sourcePosLong, entry);
        }
    }

    public static @Nullable Entry takeByDest(long destPosLong) {
        Entry entry = BY_DEST_POS_LONG.remove(destPosLong);
        if (entry != null && entry.sourcePosLong() != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        }
        return entry;
    }

    public static @Nullable Entry takeBySource(long sourcePosLong) {
        Entry entry = BY_SOURCE_POS_LONG.remove(sourcePosLong);
        if (entry != null) {
            BY_DEST_POS_LONG.remove(entry.destPosLong());
        }
        return entry;
    }

    public static @Nullable Entry takeForMove(long destPosLong, long sourcePosLong) {
        Entry entry = takeByDest(destPosLong);
        if (entry != null) return entry;
        return takeBySource(sourcePosLong);
    }

    public static void clear() {
        BY_DEST_POS_LONG.clear();
        BY_SOURCE_POS_LONG.clear();
    }
}