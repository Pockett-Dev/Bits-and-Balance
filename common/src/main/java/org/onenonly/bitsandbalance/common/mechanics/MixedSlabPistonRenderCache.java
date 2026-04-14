package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.TimeUnit;

/**
 * Client-side cache used to render mixed slabs while they are being moved by pistons.
 *
 * <p>Mixed slabs render via a block entity renderer and their {@code MixedSlabBlock}
 * is {@code RenderShape.INVISIBLE}. While a piston is moving a mixed slab, the world
 * contains a {@code PistonMovingBlockEntity} rather than a {@code MixedSlabBlockEntity},
 * so we must carry the bottom/top slab states through the animation.
 *
 * <p>This cache is intentionally separate from {@link MixedSlabPistonMoveCache} to avoid
 * conflicts in singleplayer (integrated server) where client + server share a JVM.
 */
public final class MixedSlabPistonRenderCache {
    private static final long FINALIZE_LINGER_NANOS = TimeUnit.MILLISECONDS.toNanos(250L);

    private MixedSlabPistonRenderCache() {
    }

    public record Entry(long sourcePosLong, long destPosLong, BlockState bottomSlab, BlockState topSlab) {
    }

    private static final Long2ObjectOpenHashMap<Entry> BY_DEST_POS_LONG = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Entry> BY_SOURCE_POS_LONG = new Long2ObjectOpenHashMap<>();
    private static final Long2LongOpenHashMap LINGER_UNTIL_BY_DEST_POS_LONG = new Long2LongOpenHashMap();
    private static final Long2LongOpenHashMap LINGER_UNTIL_BY_SOURCE_POS_LONG = new Long2LongOpenHashMap();

    static {
        BY_DEST_POS_LONG.defaultReturnValue(null);
        BY_SOURCE_POS_LONG.defaultReturnValue(null);
        LINGER_UNTIL_BY_DEST_POS_LONG.defaultReturnValue(0L);
        LINGER_UNTIL_BY_SOURCE_POS_LONG.defaultReturnValue(0L);
    }

    public static void putMove(long sourcePosLong, long destPosLong, BlockState bottomSlab, BlockState topSlab) {
        if (bottomSlab == null || topSlab == null) return;

        Entry entry = new Entry(sourcePosLong, destPosLong, bottomSlab, topSlab);
        BY_DEST_POS_LONG.put(destPosLong, entry);
        BY_SOURCE_POS_LONG.put(sourcePosLong, entry);
        LINGER_UNTIL_BY_DEST_POS_LONG.remove(destPosLong);
        LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(sourcePosLong);
    }

    /** Like {@code takeForMove} but does not remove the entry (used during animation frames). */
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
        LINGER_UNTIL_BY_SOURCE_POS_LONG.put(entry.sourcePosLong(), lingerUntil);
    }

    public static @Nullable Entry takeForMove(long destPosLong, long sourcePosLong) {
        Entry entry = bitsandbalance$getLiveEntry(BY_DEST_POS_LONG.remove(destPosLong));
        if (entry == null) {
            entry = bitsandbalance$getLiveEntry(BY_SOURCE_POS_LONG.remove(sourcePosLong));
            if (entry == null) return null;
            BY_DEST_POS_LONG.remove(entry.destPosLong());
            LINGER_UNTIL_BY_DEST_POS_LONG.remove(entry.destPosLong());
            LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
            return entry;
        }

        BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        LINGER_UNTIL_BY_DEST_POS_LONG.remove(entry.destPosLong());
        LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        return entry;
    }

    private static @Nullable Entry bitsandbalance$getLiveEntry(@Nullable Entry entry) {
        if (entry == null) {
            return null;
        }
        if (!bitsandbalance$isExpired(entry)) {
            return entry;
        }

        BY_DEST_POS_LONG.remove(entry.destPosLong());
        BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        LINGER_UNTIL_BY_DEST_POS_LONG.remove(entry.destPosLong());
        LINGER_UNTIL_BY_SOURCE_POS_LONG.remove(entry.sourcePosLong());
        return null;
    }

    private static boolean bitsandbalance$isExpired(Entry entry) {
        long lingerUntilByDest = LINGER_UNTIL_BY_DEST_POS_LONG.get(entry.destPosLong());
        long lingerUntilBySource = LINGER_UNTIL_BY_SOURCE_POS_LONG.get(entry.sourcePosLong());
        long lingerUntil = Math.max(lingerUntilByDest, lingerUntilBySource);
        return lingerUntil != 0L && System.nanoTime() > lingerUntil;
    }
}
