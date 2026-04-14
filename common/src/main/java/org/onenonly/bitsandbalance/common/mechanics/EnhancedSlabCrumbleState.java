package org.onenonly.bitsandbalance.common.mechanics;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Holds per-position crumble-overlay state used by both the
 * {@code LevelRendererEnhancedSlabCrumblingMixin} (reads) and the
 * {@code ClientLevelEnhancedSlabCrumbleLockMixin} (writes).
 *
 * <p>Extracted from the mixin to avoid non-private static methods inside
 * Mixin classes (the Mixin transformer rejects those).
 */
public final class EnhancedSlabCrumbleState {

    private static final Object LOCK = new Object();
    private static final Long2ObjectOpenHashMap<BlockState> LOCKED =
            new Long2ObjectOpenHashMap<>();

    private EnhancedSlabCrumbleState() {}

    public static void set(BlockPos pos, BlockState state) {
        if (pos == null || state == null) return;
        synchronized (LOCK) {
            LOCKED.put(pos.asLong(), state);
        }
    }

    public static void clear(BlockPos pos) {
        if (pos == null) return;
        synchronized (LOCK) {
            LOCKED.remove(pos.asLong());
        }
    }

    public static boolean has(BlockPos pos) {
        if (pos == null) return false;
        synchronized (LOCK) {
            return LOCKED.containsKey(pos.asLong());
        }
    }

    public static BlockState get(BlockPos pos) {
        if (pos == null) return null;
        synchronized (LOCK) {
            return LOCKED.get(pos.asLong());
        }
    }
}
