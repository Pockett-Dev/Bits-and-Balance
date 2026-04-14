package org.onenonly.bitsandbalance.fabric.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.Half;
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Tweaks: Trapdoor chaining (up to 5 touching trapdoors open together) and redstone mirroring.
 */
@Mixin(TrapDoorBlock.class)
public abstract class TrapDoorBlockMixin {

    @Shadow
    protected abstract BlockSetType getType();

    @Shadow
    protected abstract void playSound(Player player, Level level, BlockPos pos, boolean open);

    private static final int bitsandbalance$maxAdditionalTrapdoors = 4;
    private static final ThreadLocal<Boolean> bitsandbalance$trapdoorMirrorGuard = ThreadLocal.withInitial(() -> false);
    private static final ConcurrentMap<Class<?>, Boolean> bitsandbalance$overrideFallbackCache = new ConcurrentHashMap<>();

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$chainTrapdoors(BlockState stateAtCall, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (bitsandbalance$handlePlayerTrapdoorInteraction(level, pos, player)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void bitsandbalance$trapdoorRedstoneChain(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!FabricTweaksConfig.enableDoubleDoorOpening || !FabricTweaksConfig.doubleDoorsWithRedstone || !FabricTweaksConfig.doubleDoorRedstoneIncludeTrapdoors)
            return;
        if (level.isClientSide()) return;
        if (bitsandbalance$trapdoorMirrorGuard.get()) return;

        try {
            BlockState current = level.getBlockState(pos);
            if (!(current.getBlock() instanceof TrapDoorBlock)) return;

            boolean open = current.getValue(TrapDoorBlock.OPEN);
            Half half = current.getValue(TrapDoorBlock.HALF);

            int remaining = bitsandbalance$maxAdditionalTrapdoors;
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> q = new ArrayDeque<>();
            visited.add(pos);
            for (Direction d : Direction.values()) {
                q.add(pos.relative(d));
            }

            bitsandbalance$trapdoorMirrorGuard.set(true);
            try {
                while (remaining > 0 && !q.isEmpty()) {
                    BlockPos cur = q.poll();
                    if (!visited.add(cur)) continue;
                    BlockState st = level.getBlockState(cur);
                    if (!bitsandbalance$canChainTrapdoor(current, st, half)) continue;

                    if (st.getValue(TrapDoorBlock.OPEN) != open) {
                        bitsandbalance$setTrapdoorOpen(level, cur, st, open, null);
                        remaining--;
                    }

                    if (remaining > 0) {
                        for (Direction d : Direction.values()) {
                            q.add(cur.relative(d));
                        }
                    }
                }
            } finally {
                bitsandbalance$trapdoorMirrorGuard.set(false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void bitsandbalance$chainTrapdoorNeighbors(Level level, BlockPos originPos, BlockState originState, Half half, boolean targetOpen) {
        bitsandbalance$chainTrapdoorNeighbors(level, originPos, originState, half, targetOpen, !level.isClientSide());
    }

    private static void bitsandbalance$chainTrapdoorNeighbors(Level level, BlockPos originPos, BlockState originState, Half half, boolean targetOpen, boolean playEffects) {
        int remaining = bitsandbalance$maxAdditionalTrapdoors;
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> q = new ArrayDeque<>();
        visited.add(originPos);
        for (Direction d : Direction.values()) {
            q.add(originPos.relative(d));
        }

        while (remaining > 0 && !q.isEmpty()) {
            BlockPos cur = q.poll();
            if (!visited.add(cur)) continue;
            BlockState st = level.getBlockState(cur);
            if (!bitsandbalance$canChainTrapdoor(originState, st, half)) continue;

            if (st.getValue(TrapDoorBlock.OPEN) != targetOpen) {
                if (playEffects) {
                    bitsandbalance$setTrapdoorOpen(level, cur, st, targetOpen, null);
                } else {
                    bitsandbalance$setTrapdoorOpenState(level, cur, st, targetOpen);
                }
                remaining--;
            }

            if (remaining > 0) {
                for (Direction d : Direction.values()) {
                    q.add(cur.relative(d));
                }
            }
        }
    }

    private static boolean bitsandbalance$canChainTrapdoor(BlockState originState, BlockState candidateState, Half half) {
        if (!(candidateState.getBlock() instanceof TrapDoorBlock)) return false;
        if (candidateState.getValue(TrapDoorBlock.HALF) != half) return false;
        return bitsandbalance$matchesTrapdoorType(originState, candidateState);
    }

    private static boolean bitsandbalance$isPlayerChainableTrapdoor(BlockState state) {
        return bitsandbalance$canOpenByHand(state);
    }

    private static boolean bitsandbalance$canOpenByHand(BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock)) {
            return false;
        }
        return ((TrapDoorBlockMixin) (Object) state.getBlock()).getType().canOpenByHand();
    }

    private static boolean bitsandbalance$matchesTrapdoorType(BlockState originState, BlockState candidateState) {
        if (FabricTweaksConfig.doubleDoorSameBlockOnly) {
            return candidateState.getBlock() == originState.getBlock();
        }
        return bitsandbalance$canOpenByHand(candidateState) == bitsandbalance$canOpenByHand(originState);
    }

    private static boolean bitsandbalance$shouldUseInteractionFallback(BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock)) {
            return false;
        }
        return bitsandbalance$overrideFallbackCache.computeIfAbsent(state.getBlock().getClass(), TrapDoorBlockMixin::bitsandbalance$computeInteractionFallback);
    }

    private static boolean bitsandbalance$handlePlayerTrapdoorInteraction(Level level, BlockPos pos, Player player) {
        if (!FabricTweaksConfig.enableDoubleDoorOpening || !FabricTweaksConfig.doubleDoorChainTrapdoors) return false;
        if (FabricTweaksConfig.doubleDoorCrouchSingle && player != null && player.isShiftKeyDown()) return false;

        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TrapDoorBlock)) return false;
            if (!bitsandbalance$isPlayerChainableTrapdoor(state)) return false;

            boolean targetOpen = !state.getValue(TrapDoorBlock.OPEN);
            Half half = state.getValue(TrapDoorBlock.HALF);

            if (level.isClientSide()) {
                bitsandbalance$setTrapdoorOpen(level, pos, state, targetOpen, player);
                bitsandbalance$chainTrapdoorNeighbors(level, pos, state, half, targetOpen, false);
                return true;
            }

            bitsandbalance$trapdoorMirrorGuard.set(true);
            try {
                bitsandbalance$setTrapdoorOpen(level, pos, state, targetOpen, player);
                bitsandbalance$chainTrapdoorNeighbors(level, pos, state, half, targetOpen);
            } finally {
                bitsandbalance$trapdoorMirrorGuard.set(false);
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean bitsandbalance$computeInteractionFallback(Class<?> blockClass) {
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

    private static void bitsandbalance$setTrapdoorOpen(Level level, BlockPos pos, BlockState state, boolean open, Player player) {
        level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), 10);
        TrapDoorBlockMixin self = (TrapDoorBlockMixin) (Object) state.getBlock();
        self.playSound(player, level, pos, open);
    }

    private static void bitsandbalance$setTrapdoorOpenState(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), 10);
    }
}
