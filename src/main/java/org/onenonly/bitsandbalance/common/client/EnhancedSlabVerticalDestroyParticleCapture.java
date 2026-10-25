package org.onenonly.bitsandbalance.common.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Keeps the real material and vertical geometry alive across client-side block removal.
 * Generated Vertical Slab owners intentionally have no useful particle texture of their own.
 */
public final class EnhancedSlabVerticalDestroyParticleCapture {
    private static final long RETENTION_TICKS = 40L;
    private static final VoxelShape NORTH_HALF = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 8.0);
    private static final VoxelShape SOUTH_HALF = Block.box(0.0, 0.0, 8.0, 16.0, 16.0, 16.0);
    private static final VoxelShape WEST_HALF = Block.box(0.0, 0.0, 0.0, 8.0, 16.0, 16.0);
    private static final VoxelShape EAST_HALF = Block.box(8.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final VoxelShape FULL = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);
    private static final Map<ClientLevel, Map<Long, Capture>> CAPTURES =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static DebugTrace debugTrace;

    private EnhancedSlabVerticalDestroyParticleCapture() {
    }

    public static void capture(Level level,
                               BlockPos pos,
                               BlockState ownerState,
                               BlockState materialState,
                               Direction targetedHalf,
                               boolean wholeDouble) {
        if (!(level instanceof ClientLevel clientLevel) || pos == null || ownerState == null
                || materialState == null || targetedHalf == null) {
            return;
        }

        VoxelShape shape = wholeDouble ? FULL : halfShape(targetedHalf);
        synchronized (CAPTURES) {
            Map<Long, Capture> byPos = CAPTURES.computeIfAbsent(clientLevel, ignored -> new HashMap<>());
            prune(clientLevel, byPos);
            byPos.put(pos.asLong(), new Capture(ownerState, materialState, shape,
                    clientLevel.getGameTime() + RETENTION_TICKS));
        }
        if (debugEnabled()) {
            synchronized (EnhancedSlabVerticalDestroyParticleCapture.class) {
                DebugTrace trace = ensureDebugTrace();
                trace.captures++;
                trace.details.add("capture=" + materialState.getBlock() + ",whole=" + wholeDouble);
            }
        }
    }

    /**
     * Handles both the real-material event and a late generated-owner event. Returning true means
     * the caller must cancel vanilla particles; the level event's sound has already been handled.
     */
    public static boolean handleDestroyEffect(ClientLevel level,
                                              @Nullable Player player,
                                              BlockPos pos,
                                              BlockState eventState) {
        if (level == null || pos == null || eventState == null) return false;

        Capture capture = get(level, pos);
        if (capture != null && (capture.materialState.getBlock() == eventState.getBlock()
                || eventState.getBlock() instanceof VerticalSlabBlock)) {
            return spawnOrSuppress(level, pos, capture, "level-event");
        }

        if (eventState.getBlock() instanceof VerticalSlabBlock) {
            if (debugEnabled()) {
                synchronized (EnhancedSlabVerticalDestroyParticleCapture.class) {
                    DebugTrace trace = ensureDebugTrace();
                    trace.suppressedOwnerFallbacks++;
                    trace.details.add("suppressed-owner=" + eventState.getBlock());
                }
            }
            return true;
        }
        return false;
    }

    /** Prevents the client prediction redirect from adding a second debris batch. */
    public static boolean spawnOrSuppressCaptured(ClientLevel level, BlockPos pos, BlockState ownerState) {
        if (level == null || pos == null || ownerState == null
                || !(ownerState.getBlock() instanceof VerticalSlabBlock)) {
            return false;
        }
        Capture capture = get(level, pos);
        if (capture == null || !capture.ownerState.equals(ownerState)) return false;
        return spawnOrSuppress(level, pos, capture, "prediction");
    }

    public static void clear(ClientLevel level, BlockPos pos) {
        if (level == null || pos == null) return;
        synchronized (CAPTURES) {
            Map<Long, Capture> byPos = CAPTURES.get(level);
            if (byPos == null) return;
            byPos.remove(pos.asLong());
            if (byPos.isEmpty()) CAPTURES.remove(level);
        }
    }

    public static void resetDebugTrace() {
        synchronized (EnhancedSlabVerticalDestroyParticleCapture.class) {
            debugTrace = new DebugTrace();
        }
    }

    public static DebugSnapshot debugSnapshot() {
        synchronized (EnhancedSlabVerticalDestroyParticleCapture.class) {
            DebugTrace trace = ensureDebugTrace();
            return new DebugSnapshot(trace.captures, trace.spawns, trace.duplicateSuppressions,
                    trace.suppressedOwnerFallbacks, List.copyOf(trace.spawnedStates),
                    List.copyOf(trace.spawnedParticleCounts), List.copyOf(trace.details));
        }
    }

    private static @Nullable Capture get(ClientLevel level, BlockPos pos) {
        synchronized (CAPTURES) {
            Map<Long, Capture> byPos = CAPTURES.get(level);
            if (byPos == null) return null;
            prune(level, byPos);
            Capture capture = byPos.get(pos.asLong());
            if (byPos.isEmpty()) CAPTURES.remove(level);
            return capture;
        }
    }

    private static boolean spawnOrSuppress(ClientLevel level, BlockPos pos, Capture capture, String route) {
        synchronized (CAPTURES) {
            if (capture.spawned) {
                if (debugEnabled()) {
                    synchronized (EnhancedSlabVerticalDestroyParticleCapture.class) {
                        DebugTrace trace = ensureDebugTrace();
                        trace.duplicateSuppressions++;
                        trace.details.add("duplicate=" + route);
                    }
                }
                return true;
            }
            capture.spawned = true;
        }

        int particleCount = spawn(level, pos, capture.materialState, capture.shape);
        if (debugEnabled()) {
            synchronized (EnhancedSlabVerticalDestroyParticleCapture.class) {
                DebugTrace trace = ensureDebugTrace();
                trace.spawns++;
                trace.spawnedStates.add(capture.materialState);
                trace.spawnedParticleCounts.add(particleCount);
                trace.details.add("spawn=" + route + ",state=" + capture.materialState.getBlock()
                        + ",particles=" + particleCount);
            }
        }
        return true;
    }

    private static int spawn(ClientLevel level, BlockPos pos, BlockState materialState, VoxelShape shape) {
        // NeoForge uses the source position to resolve model-data-aware particle sprites.
        //? if project == 1.21.1-neoforge
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, materialState).setPos(pos);
        //? else
        //? if neoforge
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, materialState, pos);
        //? else
        BlockParticleOption option = new BlockParticleOption(ParticleTypes.BLOCK, materialState);
        //? endif
        //? endif
        return AsyncParticlesCompat.withDestroyLight(level, pos, () ->
                EnhancedSlabDestroyParticleGeometry.emitGrid(pos, shape, (box, motion) ->
                        level.addParticle(
                                option,
                                motion.x(), motion.y(), motion.z(),
                                motion.vx(), motion.vy(), motion.vz()
                        )));
    }

    private static VoxelShape halfShape(Direction direction) {
        return switch (direction) {
            case NORTH -> NORTH_HALF;
            case SOUTH -> SOUTH_HALF;
            case WEST -> WEST_HALF;
            case EAST -> EAST_HALF;
            default -> NORTH_HALF;
        };
    }

    private static void prune(ClientLevel level, Map<Long, Capture> byPos) {
        long gameTime = level.getGameTime();
        byPos.entrySet().removeIf(entry -> entry.getValue().expiresAt < gameTime);
    }

    private static boolean debugEnabled() {
        return "enhanced-slab-vertical-destroy-particles".equals(
                System.getProperty("bitsandbalance.fixtureRunner", ""));
    }

    private static DebugTrace ensureDebugTrace() {
        if (debugTrace == null) debugTrace = new DebugTrace();
        return debugTrace;
    }

    private static final class Capture {
        private final BlockState ownerState;
        private final BlockState materialState;
        private final VoxelShape shape;
        private final long expiresAt;
        private boolean spawned;

        private Capture(BlockState ownerState, BlockState materialState, VoxelShape shape, long expiresAt) {
            this.ownerState = ownerState;
            this.materialState = materialState;
            this.shape = shape;
            this.expiresAt = expiresAt;
        }
    }

    private static final class DebugTrace {
        private int captures;
        private int spawns;
        private int duplicateSuppressions;
        private int suppressedOwnerFallbacks;
        private final List<BlockState> spawnedStates = new ArrayList<>();
        private final List<Integer> spawnedParticleCounts = new ArrayList<>();
        private final List<String> details = new ArrayList<>();
    }

    public record DebugSnapshot(int captures,
                                int spawns,
                                int duplicateSuppressions,
                                int suppressedOwnerFallbacks,
                                List<BlockState> spawnedStates,
                                List<Integer> spawnedParticleCounts,
                                List<String> details) {
    }
}
