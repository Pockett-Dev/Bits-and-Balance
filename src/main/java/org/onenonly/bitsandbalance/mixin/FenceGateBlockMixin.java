package org.onenonly.bitsandbalance.mixin;

import org.onenonly.bitsandbalance.common.config.DoubleDoorFeatureFlags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;
//? if mc != 1.21.1
import net.minecraft.world.level.redstone.Orientation;
//? endif
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * Tweaks: Double Door Opening
 * When a fence gate is toggled by right-click, also toggle up to 4 touching matching gates.
 */
@Mixin(FenceGateBlock.class)
public abstract class FenceGateBlockMixin {

    private static final ThreadLocal<Boolean> bitsandbalance$fenceGateMirrorGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$chainFenceGates(BlockState stateAtCall, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled()) return;
        if (level.isClientSide()) return;
        if (DoubleDoorFeatureFlags.doubleDoorCrouchSingleEnabled() && player != null && player.isShiftKeyDown()) return;

        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FenceGateBlock)) return;

            boolean currentOpen = state.getValue(FenceGateBlock.OPEN);
            boolean targetOpen = !currentOpen;
            Direction.Axis axis = state.getValue(FenceGateBlock.FACING).getAxis();
            Block selfBlock = state.getBlock();

            BlockState newClickedState = bitsandbalance$setFenceGateOpen(state, targetOpen, player);
            level.setBlock(pos, newClickedState, 10);

            // BFS over touching neighbors up to 4 additional gates (5 total)
            int remaining = 4;
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> q = new ArrayDeque<>();
            visited.add(pos);
            for (Direction d : Direction.values()) {
                q.add(pos.relative(d));
            }

            while (remaining > 0 && !q.isEmpty()) {
                BlockPos cur = q.poll();
                if (!visited.add(cur)) continue;

                BlockState st = level.getBlockState(cur);
                if (!(st.getBlock() instanceof FenceGateBlock)) continue;
                if (st.getBlock() != selfBlock) continue; // exact same gate type
                if (st.getValue(FenceGateBlock.FACING).getAxis() != axis) continue; // must be aligned

                if (st.getValue(FenceGateBlock.OPEN) != targetOpen) {
                    BlockState updated = bitsandbalance$setFenceGateOpen(st, targetOpen, player);
                    level.setBlock(cur, updated, 10);
                    remaining--;
                }

                if (remaining > 0) {
                    for (Direction d : Direction.values()) {
                        q.add(cur.relative(d));
                    }
                }
            }

            cir.setReturnValue(InteractionResult.CONSUME);
        } catch (Throwable ignored) {
        }
    }

    private static BlockState bitsandbalance$setFenceGateOpen(BlockState state, boolean open, Player player) {
        BlockState updated = state;
        if (open && player != null) {
            Direction playerDirection = player.getDirection();
            if (updated.getValue(FenceGateBlock.FACING) == playerDirection.getOpposite()) {
                updated = updated.setValue(FenceGateBlock.FACING, playerDirection);
            }
        }
        return updated.setValue(FenceGateBlock.OPEN, open);
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void bitsandbalance$fenceGateRedstoneMirror(BlockState state, Level level, BlockPos pos, Block block,
//? if mc == 1.21.1
                                                        BlockPos neighborPos,
//? else
                                                        Orientation orientation,
//? endif
                                                        boolean isMoving, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled() || !DoubleDoorFeatureFlags.doubleDoorsWithRedstoneEnabled()) return;
        if (level.isClientSide()) return;
        if (bitsandbalance$fenceGateMirrorGuard.get()) return;

        try {
            BlockState current = level.getBlockState(pos);
            if (!(current.getBlock() instanceof FenceGateBlock)) return;

            Block selfBlock = current.getBlock();
            boolean open = current.getValue(FenceGateBlock.OPEN);
            boolean powered = current.getValue(FenceGateBlock.POWERED);
            Direction.Axis axis = current.getValue(FenceGateBlock.FACING).getAxis();

            int remaining = 4; // chain up to 4 more to make 5 total
            Set<BlockPos> visited = new HashSet<>();
            Queue<BlockPos> q = new ArrayDeque<>();
            visited.add(pos);
            for (Direction d : Direction.values()) {
                q.add(pos.relative(d));
            }

            bitsandbalance$fenceGateMirrorGuard.set(true);
            try {
                while (remaining > 0 && !q.isEmpty()) {
                    BlockPos cur = q.poll();
                    if (!visited.add(cur)) continue;

                    BlockState st = level.getBlockState(cur);
                    if (!(st.getBlock() instanceof FenceGateBlock)) continue;
                    if (st.getBlock() != selfBlock) continue;
                    if (st.getValue(FenceGateBlock.FACING).getAxis() != axis) continue;

                    boolean needsOpen = st.getValue(FenceGateBlock.OPEN) != open;
                    boolean needsPowered = st.getValue(FenceGateBlock.POWERED) != powered;
                    if (needsOpen || needsPowered) {
                        BlockState updated = st;
                        if (needsOpen) updated = updated.setValue(FenceGateBlock.OPEN, open);
                        if (needsPowered) updated = updated.setValue(FenceGateBlock.POWERED, powered);
                        level.setBlock(cur, updated, 10);
                        remaining--;
                    }

                    if (remaining > 0) {
                        for (Direction d : Direction.values()) {
                            q.add(cur.relative(d));
                        }
                    }
                }
            } finally {
                bitsandbalance$fenceGateMirrorGuard.set(false);
            }
        } catch (Throwable ignored) {
        }
    }
}
