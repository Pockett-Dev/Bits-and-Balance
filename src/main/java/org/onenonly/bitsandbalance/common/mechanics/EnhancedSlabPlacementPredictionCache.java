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

public final class EnhancedSlabPlacementPredictionCache {
    private static final long RETENTION_TICKS = 20L;
    private static final Map<Level, Map<Long, QuadStepPrediction>> QUAD_STEPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, Map<Long, QuadVerticalStepPrediction>> QUAD_VERTICAL_STEPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, Map<Long, SingleStepPrediction>> SINGLE_STEPS = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<Level, Map<Long, SingleVerticalStepPrediction>> SINGLE_VERTICAL_STEPS = Collections.synchronizedMap(new WeakHashMap<>());

    private EnhancedSlabPlacementPredictionCache() {
    }

    public static void rememberQuadStep(@Nullable Level level, BlockPos pos, BlockState quadState, BlockState[] slabs) {
        if (level == null) {
            return;
        }

        clearSingleStep(level, pos);
        synchronized (QUAD_STEPS) {
            QUAD_STEPS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos.asLong(), new QuadStepPrediction(quadState, slabs, level.getGameTime() + RETENTION_TICKS));
        }
    }

    public static @Nullable QuadStepPrediction peekQuadStep(@Nullable BlockGetter level, BlockPos pos) {
        return peek(level, pos, QUAD_STEPS);
    }

    public static void clearQuadStep(@Nullable Level level, BlockPos pos) {
        clear(level, pos, QUAD_STEPS);
    }

    public static void rememberSingleStep(@Nullable Level level, BlockPos pos, BlockState stepState, BlockState slabState) {
        if (level == null || stepState == null || slabState == null) {
            return;
        }

        clearQuadStep(level, pos);
        synchronized (SINGLE_STEPS) {
            SINGLE_STEPS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos.asLong(), new SingleStepPrediction(stepState, slabState, level.getGameTime() + RETENTION_TICKS));
        }
    }

    public static @Nullable SingleStepPrediction peekSingleStep(@Nullable BlockGetter level, BlockPos pos) {
        return peek(level, pos, SINGLE_STEPS);
    }

    public static void clearSingleStep(@Nullable Level level, BlockPos pos) {
        clear(level, pos, SINGLE_STEPS);
    }

    public static void rememberQuadVerticalStep(@Nullable Level level, BlockPos pos, BlockState quadState, BlockState[] slabs) {
        if (level == null) {
            return;
        }

        clearSingleVerticalStep(level, pos);
        synchronized (QUAD_VERTICAL_STEPS) {
            QUAD_VERTICAL_STEPS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos.asLong(), new QuadVerticalStepPrediction(quadState, slabs, level.getGameTime() + RETENTION_TICKS));
        }
    }

    public static @Nullable QuadVerticalStepPrediction peekQuadVerticalStep(@Nullable BlockGetter level, BlockPos pos) {
        return peek(level, pos, QUAD_VERTICAL_STEPS);
    }

    public static void clearQuadVerticalStep(@Nullable Level level, BlockPos pos) {
        clear(level, pos, QUAD_VERTICAL_STEPS);
    }

    public static void rememberSingleVerticalStep(@Nullable Level level, BlockPos pos, BlockState stepState, BlockState slabState) {
        if (level == null || stepState == null || slabState == null) {
            return;
        }

        clearQuadVerticalStep(level, pos);
        synchronized (SINGLE_VERTICAL_STEPS) {
            SINGLE_VERTICAL_STEPS.computeIfAbsent(level, ignored -> new HashMap<>())
                    .put(pos.asLong(), new SingleVerticalStepPrediction(stepState, slabState, level.getGameTime() + RETENTION_TICKS));
        }
    }

    public static @Nullable SingleVerticalStepPrediction peekSingleVerticalStep(@Nullable BlockGetter level, BlockPos pos) {
        return peek(level, pos, SINGLE_VERTICAL_STEPS);
    }

    public static void clearSingleVerticalStep(@Nullable Level level, BlockPos pos) {
        clear(level, pos, SINGLE_VERTICAL_STEPS);
    }

    private static <T extends Prediction> @Nullable T peek(@Nullable BlockGetter level, BlockPos pos, Map<Level, Map<Long, T>> cache) {
        if (!(level instanceof Level actualLevel)) {
            return null;
        }

        synchronized (cache) {
            Map<Long, T> byPos = cache.get(actualLevel);
            if (byPos == null) {
                return null;
            }

            long posLong = pos.asLong();
            T entry = byPos.get(posLong);
            if (entry == null) {
                return null;
            }

            if (entry.expiresAt() < actualLevel.getGameTime()) {
                byPos.remove(posLong);
                if (byPos.isEmpty()) {
                    cache.remove(actualLevel);
                }
                return null;
            }

            return entry;
        }
    }

    private static <T extends Prediction> void clear(@Nullable Level level, BlockPos pos, Map<Level, Map<Long, T>> cache) {
        if (level == null) {
            return;
        }

        synchronized (cache) {
            Map<Long, T> byPos = cache.get(level);
            if (byPos == null) {
                return;
            }

            byPos.remove(pos.asLong());
            if (byPos.isEmpty()) {
                cache.remove(level);
            }
        }
    }

    private interface Prediction {
        long expiresAt();
    }

    public record QuadStepPrediction(BlockState quadState, BlockState[] slabs, long expiresAt) implements Prediction {
        public QuadStepPrediction {
            slabs = slabs == null ? new BlockState[4] : slabs.clone();
        }

        @Override
        public BlockState[] slabs() {
            return slabs.clone();
        }
    }

    public record QuadVerticalStepPrediction(BlockState quadState, BlockState[] slabs, long expiresAt) implements Prediction {
        public QuadVerticalStepPrediction {
            slabs = slabs == null ? new BlockState[4] : slabs.clone();
        }

        @Override
        public BlockState[] slabs() {
            return slabs.clone();
        }
    }

    public record SingleStepPrediction(BlockState stepState, BlockState slabState, long expiresAt) implements Prediction {
    }

    public record SingleVerticalStepPrediction(BlockState stepState, BlockState slabState, long expiresAt) implements Prediction {
    }
}