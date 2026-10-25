package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class EnhancedSlabParticleStateCache {

    private static final long RETENTION_TICKS = 40L;
    private static final Map<Level, Map<Long, Entry>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    private EnhancedSlabParticleStateCache() {
    }

    public static void remember(@Nullable Level level, BlockPos pos, @Nullable BlockState slabState) {
        if (level == null || slabState == null) {
            return;
        }

        synchronized (CACHE) {
            CACHE.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos.asLong(), new Entry(slabState, level.getGameTime() + RETENTION_TICKS));
        }
    }

    public static @Nullable BlockState peek(@Nullable BlockGetter level, BlockPos pos) {
        if (!(level instanceof Level actualLevel)) {
            return null;
        }

        synchronized (CACHE) {
            Map<Long, Entry> byPos = CACHE.get(actualLevel);
            if (byPos == null) {
                return null;
            }

            long posLong = pos.asLong();
            Entry entry = byPos.get(posLong);
            if (entry == null) {
                return null;
            }

            if (entry.expiresAt < actualLevel.getGameTime()) {
                byPos.remove(posLong);
                if (byPos.isEmpty()) {
                    CACHE.remove(actualLevel);
                }
                return null;
            }

            return entry.slabState;
        }
    }

    private record Entry(BlockState slabState, long expiresAt) {
    }
}