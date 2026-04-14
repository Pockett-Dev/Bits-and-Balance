package org.onenonly.bitsandbalance.tweaks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Half;
import org.onenonly.bitsandbalance.Config;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class TrapDoorInteractionFallbackHelper {
    private static final int MAX_ADDITIONAL_TRAPDOORS = 4;
    private static final ConcurrentMap<Class<?>, Boolean> OVERRIDE_FALLBACK_CACHE = new ConcurrentHashMap<>();
    private static volatile Method trapDoorGetTypeMethod;
    private static volatile boolean trapDoorGetTypeMethodResolved;

    private TrapDoorInteractionFallbackHelper() {
    }

    public static boolean shouldUseInteractionFallback(BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock)) {
            return false;
        }
        return OVERRIDE_FALLBACK_CACHE.computeIfAbsent(state.getBlock().getClass(), TrapDoorInteractionFallbackHelper::computeInteractionFallback);
    }

    public static boolean handlePlayerTrapdoorInteraction(Level level, BlockPos pos, Player player) {
        if (!Config.enableDoubleDoorOpening || !Config.doubleDoorChainTrapdoors) return false;
        if (Config.doubleDoorCrouchSingle && player != null && player.isShiftKeyDown()) return false;

        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TrapDoorBlock)) return false;
            if (!isPlayerChainableTrapdoor(state)) return false;

            boolean targetOpen = !state.getValue(TrapDoorBlock.OPEN);
            Half half = state.getValue(TrapDoorBlock.HALF);
            setTrapdoorOpen(level, pos, state, targetOpen);
            chainTrapdoorNeighbors(level, pos, state, half, targetOpen);
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
        if (Config.doubleDoorSameBlockOnly) {
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