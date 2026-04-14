package org.onenonly.bitsandbalance.mixin;

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
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Trapdoor chaining (up to 5 touching trapdoors open together) and redstone mirroring.
 */
@Mixin(TrapDoorBlock.class)
public abstract class TrapDoorBlockMixin {

    @Shadow
    protected abstract BlockSetType getType();

    @Shadow
    protected abstract void playSound(Player player, Level level, BlockPos pos, boolean open);

    private static final ThreadLocal<Boolean> rebalance$trapdoorMirrorGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void rebalance$chainTrapdoors(BlockState stateAtCall, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Config.enableDoubleDoorOpening || !Config.doubleDoorChainTrapdoors) return;
        if (Config.doubleDoorCrouchSingle && player != null && player.isShiftKeyDown()) return;

        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof TrapDoorBlock)) return;
            if (!rebalance$canOpenByHand(state)) return;

            boolean targetOpen = !state.getValue(TrapDoorBlock.OPEN);
            Half half = state.getValue(TrapDoorBlock.HALF);

            if (level.isClientSide()) {
                rebalance$setTrapdoorOpen(level, pos, state, targetOpen, player);
                rebalance$chainTrapdoorNeighbors(level, pos, state, half, targetOpen, false);
                cir.setReturnValue(InteractionResult.CONSUME);
                return;
            }

            rebalance$trapdoorMirrorGuard.set(true);
            try {
                rebalance$setTrapdoorOpen(level, pos, state, targetOpen, player);
                rebalance$chainTrapdoorNeighbors(level, pos, state, half, targetOpen, true);
            } finally {
                rebalance$trapdoorMirrorGuard.set(false);
            }

            cir.setReturnValue(InteractionResult.CONSUME);
        } catch (Throwable ignored) {
        }
    }

    // Mirror trapdoors when toggled via redstone by listening after neighborChanged updates state
    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void rebalance$trapdoorRedstoneChain(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!Config.enableDoubleDoorOpening || !Config.doubleDoorsWithRedstone || !Config.doubleDoorRedstoneIncludeTrapdoors) return;
        if (level.isClientSide()) return;
        if (rebalance$trapdoorMirrorGuard.get()) return;

        try {
            BlockState current = level.getBlockState(pos);
            if (!(current.getBlock() instanceof TrapDoorBlock)) return;

            boolean open = current.getValue(TrapDoorBlock.OPEN);
            Half half = current.getValue(TrapDoorBlock.HALF);
            Block selfBlock = current.getBlock();

            int remaining = 4; // chain up to 4 more to make 5 total
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> q = new ArrayDeque<>();
            visited.add(pos);
            for (Direction d : Direction.values()) {
                q.add(pos.relative(d));
            }

            rebalance$trapdoorMirrorGuard.set(true);
            try {
                while (remaining > 0 && !q.isEmpty()) {
                    BlockPos cur = q.poll();
                    if (!visited.add(cur)) continue;
                    BlockState st = level.getBlockState(cur);
                    if (!rebalance$canChainTrapdoor(current, st, half)) continue;

                    if (st.getValue(TrapDoorBlock.OPEN) != open) {
                        rebalance$setTrapdoorOpen(level, cur, st, open, null);
                        remaining--;
                    }

                    if (remaining > 0) {
                        for (Direction d : Direction.values()) {
                            q.add(cur.relative(d));
                        }
                    }
                }
            } finally {
                rebalance$trapdoorMirrorGuard.set(false);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void rebalance$chainTrapdoorNeighbors(Level level, BlockPos originPos, BlockState originState, Half half, boolean targetOpen, boolean playEffects) {
        int remaining = 4;
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
            if (!rebalance$canChainTrapdoor(originState, st, half)) continue;

            if (st.getValue(TrapDoorBlock.OPEN) != targetOpen) {
                if (playEffects) {
                    rebalance$setTrapdoorOpen(level, cur, st, targetOpen, null);
                } else {
                    rebalance$setTrapdoorOpenState(level, cur, st, targetOpen);
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

    private static boolean rebalance$canChainTrapdoor(BlockState originState, BlockState candidateState, Half half) {
        if (!(candidateState.getBlock() instanceof TrapDoorBlock)) return false;
        if (candidateState.getValue(TrapDoorBlock.HALF) != half) return false;
        return rebalance$matchesTrapdoorType(originState, candidateState);
    }

    private static boolean rebalance$canOpenByHand(BlockState state) {
        if (!(state.getBlock() instanceof TrapDoorBlock)) {
            return false;
        }
        return ((TrapDoorBlockMixin) (Object) state.getBlock()).getType().canOpenByHand();
    }

    private static boolean rebalance$matchesTrapdoorType(BlockState originState, BlockState candidateState) {
        if (Config.doubleDoorSameBlockOnly) {
            return candidateState.getBlock() == originState.getBlock();
        }
        return rebalance$canOpenByHand(candidateState) == rebalance$canOpenByHand(originState);
    }

    private static void rebalance$setTrapdoorOpen(Level level, BlockPos pos, BlockState state, boolean open, Player player) {
        level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), 10);
        TrapDoorBlockMixin self = (TrapDoorBlockMixin) (Object) state.getBlock();
        self.playSound(player, level, pos, open);
    }

    private static void rebalance$setTrapdoorOpenState(Level level, BlockPos pos, BlockState state, boolean open) {
        level.setBlock(pos, state.setValue(TrapDoorBlock.OPEN, open), 10);
    }
}
