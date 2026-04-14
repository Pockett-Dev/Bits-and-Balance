package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Temporary server-side cache to carry {@link net.minecraft.world.level.block.entity.BlockEntity}
 * data for {@code MixedSlabBlock} through vanilla piston movement.
 *
 * <p>Vanilla pistons normally refuse to move blocks with block entities. We selectively
 * allow moving mixed slabs, then store their slab-half data keyed by the destination
 * position long. When the destination {@code MixedSlabBlockEntity} loads, it consumes
 * this entry.</p>
 */
public final class MixedSlabPistonMoveCache {
    private MixedSlabPistonMoveCache() {
    }

    private static final long UNKNOWN_SOURCE_POS_LONG = BlockPos.ZERO.asLong();

    public record Entry(long sourcePosLong, long destPosLong, BlockState bottomSlab, BlockState topSlab) {
    }

    private static final ConcurrentHashMap<Long, Entry> BY_DEST_POS_LONG = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, Entry> BY_SOURCE_POS_LONG = new ConcurrentHashMap<>();

    /**
     * Stages a piston move for a mixed slab.
     *
     * <p>We store by destination (normal lookup at final placement time) and also
     * optionally by source so we can recover when a destination lookup misses on
     * certain piston code paths.</p>
     */
    public static void putMove(long sourcePosLong, long destPosLong, BlockState bottomSlab, BlockState topSlab) {
        if (bottomSlab == null || topSlab == null) return;
        Entry e = new Entry(sourcePosLong, destPosLong, bottomSlab, topSlab);

        BY_DEST_POS_LONG.put(destPosLong, e);
        if (sourcePosLong != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.put(sourcePosLong, e);
        }
    }

    /**
     * Backwards-compatible convenience: stages only by destination.
     * Prefer {@link #putMove(long, long, BlockState, BlockState)}.
     */
    public static void put(long destPosLong, BlockState bottomSlab, BlockState topSlab) {
        putMove(UNKNOWN_SOURCE_POS_LONG, destPosLong, bottomSlab, topSlab);
    }

    /** Backwards-compatible alias for destination lookup. */
    public static @Nullable Entry take(long destPosLong) {
        return takeByDest(destPosLong);
    }
    public static @Nullable Entry takeByDest(long destPosLong) {
        Entry e = BY_DEST_POS_LONG.remove(destPosLong);
        if (e != null && e.sourcePosLong() != UNKNOWN_SOURCE_POS_LONG) {
            BY_SOURCE_POS_LONG.remove(e.sourcePosLong());
        }
        return e;
    }

    public static @Nullable Entry takeBySource(long sourcePosLong) {
        Entry e = BY_SOURCE_POS_LONG.remove(sourcePosLong);
        if (e != null) {
            BY_DEST_POS_LONG.remove(e.destPosLong());
        }
        return e;
    }

    /**
     * Preferred lookup: try destination first, then source.
     */
    public static @Nullable Entry takeForMove(long destPosLong, long sourcePosLong) {
        Entry e = takeByDest(destPosLong);
        if (e != null) return e;
        return takeBySource(sourcePosLong);
    }

    public static void clear() {
        BY_DEST_POS_LONG.clear();
        BY_SOURCE_POS_LONG.clear();
    }
}
