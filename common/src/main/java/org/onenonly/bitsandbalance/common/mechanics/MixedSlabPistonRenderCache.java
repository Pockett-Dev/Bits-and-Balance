package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

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

    private MixedSlabPistonRenderCache() {
    }

    public record Entry(long sourcePosLong, long destPosLong, BlockState bottomSlab, BlockState topSlab) {
    }

    private static final Long2ObjectOpenHashMap<Entry> BY_DEST_POS_LONG = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Entry> BY_SOURCE_POS_LONG = new Long2ObjectOpenHashMap<>();

    static {
        BY_DEST_POS_LONG.defaultReturnValue(null);
        BY_SOURCE_POS_LONG.defaultReturnValue(null);
    }

    public static void putMove(long sourcePosLong, long destPosLong, BlockState bottomSlab, BlockState topSlab) {
        if (bottomSlab == null || topSlab == null) return;

        Entry entry = new Entry(sourcePosLong, destPosLong, bottomSlab, topSlab);
        BY_DEST_POS_LONG.put(destPosLong, entry);
        BY_SOURCE_POS_LONG.put(sourcePosLong, entry);
    }

    /** Like {@code takeForMove} but does not remove the entry (used during animation frames). */
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
