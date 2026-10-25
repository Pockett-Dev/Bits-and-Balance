package org.onenonly.bitsandbalance.common.mechanics;

import org.onenonly.bitsandbalance.common.config.DoubleDoorFeatureFlags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.TickTask;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Half;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TrapDoorInteractionFallbackCore {
    private static final int MAX_ADDITIONAL_TRAPDOORS = 4;
    private static final ConcurrentMap<Class<?>, Boolean> OVERRIDE_FALLBACK_CACHE = new ConcurrentHashMap<>();
    private static volatile Method trapDoorGetTypeMethod;
    private static volatile boolean trapDoorGetTypeMethodResolved;

    private TrapDoorInteractionFallbackCore() {
    }

    public static boolean shouldUseInteractionFallback(BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock)) {
            return false;
        }
        return OVERRIDE_FALLBACK_CACHE.computeIfAbsent(state.getBlock().getClass(), TrapDoorInteractionFallbackCore::computeInteractionFallback);
    }

    public static void schedulePostInteractionTrapdoorChain(Level level, BlockPos pos, Player player, BlockState stateBefore) {
        if (level.isClientSide()) return;
        if (!shouldUseInteractionFallback(stateBefore)) return;
        if (!isPlayerChainableTrapdoor(stateBefore)) return;

        var server = level.getServer();
        if (server == null) return;

        BlockPos capturedPos = pos.immutable();
        server.execute(new TickTask(1, () -> chainTrapdoorsAfterObservedToggle(level, capturedPos, player, stateBefore)));
    }

    public static boolean chainTrapdoorsAfterObservedToggle(Level level, BlockPos pos, Player player, BlockState stateBefore) {
        if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled() || !DoubleDoorFeatureFlags.doubleDoorChainTrapdoorsEnabled()) return false;
        if (DoubleDoorFeatureFlags.doubleDoorCrouchSingleEnabled() && player != null && player.isShiftKeyDown()) return false;

        try {
            if (!(stateBefore.getBlock() instanceof TrapDoorBlock)) return false;

            BlockState stateAfter = level.getBlockState(pos);
            if (!(stateAfter.getBlock() instanceof TrapDoorBlock)) return false;
            if (!isPlayerChainableTrapdoor(stateAfter)) return false;

            boolean wasOpen = stateBefore.getValue(TrapDoorBlock.OPEN);
            boolean targetOpen = stateAfter.getValue(TrapDoorBlock.OPEN);
            if (wasOpen == targetOpen) return false;

            Half half = stateAfter.getValue(TrapDoorBlock.HALF);
            chainTrapdoorNeighbors(level, pos, stateAfter, half, targetOpen);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void chainTrapdoorNeighbors(Level level, BlockPos originPos, BlockState originState, Half half, boolean targetOpen) {
        int remaining = MAX_ADDITIONAL_TRAPDOORS;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        visited.add(originPos);
        for (Direction direction : Direction.values()) {
            queue.add(originPos.relative(direction));
        }

        while (remaining > 0 && !queue.isEmpty()) {
            BlockPos currentPos = queue.poll();
            if (!visited.add(currentPos)) continue;

            BlockState currentState = level.getBlockState(currentPos);
            if (!canChainTrapdoor(originState, currentState, half)) continue;

            if (currentState.getValue(TrapDoorBlock.OPEN) != targetOpen) {
                setTrapdoorOpen(level, currentPos, currentState, targetOpen);
                remaining--;
            }

            if (remaining > 0) {
                for (Direction direction : Direction.values()) {
                    queue.add(currentPos.relative(direction));
                }
            }
        }
    }

    private static boolean canChainTrapdoor(BlockState originState, BlockState candidateState, Half half) {
        if (!(candidateState.getBlock() instanceof TrapDoorBlock)) return false;
        if (candidateState.getValue(TrapDoorBlock.HALF) != half) return false;
        return matchesTrapdoorType(originState, candidateState);
    }

    private static boolean isPlayerChainableTrapdoor(BlockState state) {
        return canOpenByHand(state);
    }

    private static boolean matchesTrapdoorType(BlockState originState, BlockState candidateState) {
        if (DoubleDoorFeatureFlags.doubleDoorSameBlockOnly()) {
            return candidateState.getBlock() == originState.getBlock();
        }
        return canOpenByHand(candidateState) == canOpenByHand(originState);
    }

    private static boolean canOpenByHand(BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock trapDoorBlock)) {
            return false;
        }

        Method method = resolveTrapDoorGetTypeMethod();
        if (method == null) {
            return false;
        }

        try {
            Object result = method.invoke(trapDoorBlock);
            return result instanceof BlockSetType blockSetType && blockSetType.canOpenByHand();
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Method resolveTrapDoorGetTypeMethod() {
        if (trapDoorGetTypeMethodResolved) {
            return trapDoorGetTypeMethod;
        }

        try {
            Method method = TrapDoorBlock.class.getDeclaredMethod("getType");
            method.setAccessible(true);
            trapDoorGetTypeMethod = method;
        } catch (ReflectiveOperationException ignored) {
            trapDoorGetTypeMethod = null;
        }

        trapDoorGetTypeMethodResolved = true;
        return trapDoorGetTypeMethod;
    }

    private static boolean computeInteractionFallback(Class<?> blockClass) {
        Class<?> cursor = blockClass;
        while (cursor != null && cursor != TrapDoorBlock.class) {
            try {
                cursor.getDeclaredMethod("useWithoutItem", BlockState.class, Level.class, BlockPos.class, Player.class, net.minecraft.world.phys.BlockHitResult.class);
                return true;
            } catch (NoSuchMethodException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return false;
    }

    private static void setTrapdoorOpen(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), 10);
    }
}
