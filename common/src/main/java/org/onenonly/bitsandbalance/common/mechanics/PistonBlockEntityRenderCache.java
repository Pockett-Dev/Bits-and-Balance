package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class PistonBlockEntityRenderCache {
    private static final long FINALIZE_LINGER_NANOS = TimeUnit.MILLISECONDS.toNanos(250L);

    private PistonBlockEntityRenderCache() {
    }

    public record Entry(long sourcePosLong, long destPosLong, CompoundTag tag) {
        public Entry {
            tag = tag.copy();
        }
    }

    private static final long UNKNOWN_SOURCE_POS_LONG = BlockPos.ZERO.asLong();

    private static final ConcurrentHashMap<Long, Entry> BY_DEST_POS_LONG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Entry> BY_SOURCE_POS_LONG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> LINGER_UNTIL_BY_DEST_POS_LONG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Long> LINGER_UNTIL_BY_SOURCE_POS_LONG = new ConcurrentHashMap<>();

    public static void putMove(long sourcePosLong, long destPosLong, CompoundTag tag) {
        if (tag == null || tag.isEmpty()) return;

        Entry entry = new Entry(sourcePosLong, destPosLong, tag);
        BY_DEST_POS_LONG.put(destPosLong, entry);
        if (sourcePosLong != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.put(sourcePosLong, entry);
        }
        LINGER_UNTIL_BY_DEST_POS_LONG.remove(destPosLong);
        if (sourcePosLong != UNKNOWN_SOURCE_POS_LONG) {
            LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(sourcePosLong);
        }
    }

    public static @Nullable Entry peekForMove(long destPosLong, long sourcePosLong) {
        Entry entry = bitsandbalance$getLiveEntry(BY_DEST_POS_LONG.get(destPosLong));
        if (entry != null) return entry;
        return bitsandbalance$getLiveEntry(BY_SOURCE_POS_LONG.get(sourcePosLong));
    }

    public static void lingerForMove(long destPosLong, long sourcePosLong) {
        Entry entry = peekForMove(destPosLong, sourcePosLong);
        if (entry == null) {
            return;
        }

        long lingerUntil = System.nanoTime() + FINALIZE_LINGER_NANOS;
        LINGER_UNTIL_BY_DEST_POS_LONG.put(entry.destPosLong(), lingerUntil);
        if (entry.sourcePosLong() != UNKNOWN_SOURCE_POS_LONG) {
            LINGER_UNTIL_BY_SOURCE_POS_LONG.put(entry.sourcePosLong(), lingerUntil);
        }
    }

    public static @Nullable Entry takeByDest(long destPosLong) {
        Entry entry = bitsandbalance$getLiveEntry(BY_DEST_POS_LONG.remove(destPosLong));
        if (entry != null && entry.sourcePosLong() != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
            LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        }
        LINGER_UNTIL_BY_DEST_POS_LONG.remove(destPosLong);
        return entry;
    }

    public static @Nullable Entry takeBySource(long sourcePosLong) {
        Entry entry = bitsandbalance$getLiveEntry(BY_SOURCE_POS_LONG.remove(sourcePosLong));
        if (entry != null) {
            BY_DEST_POS_LONG.remove(entry.destPosLong());
            LINGER_UNTIL_BY_DEST_POS_LONG.remove(entry.destPosLong());
        }
        LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(sourcePosLong);
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
        LINGER_UNTIL_BY_DEST_POS_LONG.clear();
        LINGER_UNTIL_BY_SOURCE_POS_LONG.clear();
    }

    private static @Nullable Entry bitsandbalance$getLiveEntry(@Nullable Entry entry) {
        if (entry == null) {
            return null;
        }
        if (!bitsandbalance$isExpired(entry)) {
            return entry;
        }

        BY_DEST_POS_LONG.remove(entry.destPosLong());
        if (entry.sourcePosLong() != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
            LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        }
        LINGER_UNTIL_BY_DEST_POS_LONG.remove(entry.destPosLong());
        return null;
    }

    private static boolean bitsandbalance$isExpired(Entry entry) {
        long lingerUntilByDest = LINGER_UNTIL_BY_DEST_POS_LONG.getOrDefault(entry.destPosLong(), 0L);
        long lingerUntilBySource = entry.sourcePosLong() == UNKNOWN_SOURCE_POS_LONG
                ? 0L
                : LINGER_UNTIL_BY_SOURCE_POS_LONG.getOrDefault(entry.sourcePosLong(), 0L);
        long lingerUntil = Math.max(lingerUntilByDest, lingerUntilBySource);
        return lingerUntil != 0L && System.nanoTime() > lingerUntil;
    }
}