package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import org.onenonly.bitsandbalance.Config;
/**
 * Tweaks: Double Door Opening
 * When a wooden door is toggled by right-click, toggle its paired adjacent door to match.
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    private static final ThreadLocal<Boolean> rebalance$ironMirrorGuard = ThreadLocal.withInitial(() -> false);

    // Inject at HEAD to toggle both doors simultaneously and avoid visible delay, canceling vanilla logic
    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void rebalance$doubleDoorImmediate(BlockState stateAtCall, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!Config.enableDoubleDoorOpening) return;
        if (level.isClientSide()) return; // only handle on server; client will receive updates
        if (Config.doubleDoorCrouchSingle && player != null && player.isShiftKeyDown()) return;

        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DoorBlock)) return;
            // Do not enable right-click opening on iron doors or copper doors
            if (state.getBlock() == Blocks.IRON_DOOR || 
                state.getBlock() == Blocks.COPPER_DOOR || 
                state.getBlock() == Blocks.EXPOSED_COPPER_DOOR || 
                state.getBlock() == Blocks.WEATHERED_COPPER_DOOR || 
                state.getBlock() == Blocks.OXIDIZED_COPPER_DOOR) return;

            // Normalize to lower half
            if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.below();
                state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof DoorBlock)) return;
            }

            // Determine the target state (toggle)
            boolean currentOpen = state.getValue(DoorBlock.OPEN);
            boolean targetOpen = !currentOpen;
            Direction facing = state.getValue(DoorBlock.FACING);
            DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);

            // Find the paired door first before making any changes
            BlockPos pairedDoorPos = null;
            BlockState pairedDoorState = null;
            Direction[] candidates = new Direction[] { facing.getClockWise(), facing.getCounterClockWise() };
            
            for (Direction side : candidates) {
                BlockPos neighborPosLower = pos.relative(side);
                BlockState neighborLower = level.getBlockState(neighborPosLower);
                if (!(neighborLower.getBlock() instanceof DoorBlock)) continue;
                if (!rebalance$matchesDoorType(state, neighborLower)) continue;

                // Normalize neighbor to lower half
                if (neighborLower.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    if (neighborLower.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                        neighborPosLower = neighborPosLower.below();
                        neighborLower = level.getBlockState(neighborPosLower);
                        if (!(neighborLower.getBlock() instanceof DoorBlock)) continue;
                    } else {
                        continue;
                    }
                }
                // Validate same facing and opposite hinge
                if (neighborLower.getValue(DoorBlock.FACING) != facing) continue;
                if (neighborLower.getValue(DoorBlock.HINGE) == hinge) continue;

                // Found the paired door
                pairedDoorPos = neighborPosLower;
                pairedDoorState = neighborLower;
                break; // only toggle one neighbor
            }

            // Toggle both doors simultaneously to ensure perfect sync
            ((DoorBlock) (Object) this).setOpen(player, level, state, pos, targetOpen);
            if (pairedDoorPos != null && pairedDoorState != null) {
                // Only toggle paired door if it's not already in the target state
                if (pairedDoorState.getValue(DoorBlock.OPEN) != targetOpen) {
                    ((DoorBlock) (Object) this).setOpen(player, level, pairedDoorState, pairedDoorPos, targetOpen);
                }
            }

            // Consume interaction and prevent vanilla from running to avoid delay
            cir.setReturnValue(InteractionResult.CONSUME);
        } catch (Throwable ignored) {
            // If anything goes wrong, let vanilla handle it
        }
    }


    // Mirror iron doors when opened/closed via redstone by listening after neighborChanged updates state
    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void rebalance$ironDoubleRedstone(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!Config.enableDoubleDoorOpening || !Config.doubleDoorsWithRedstone) return;
        if (level.isClientSide()) return;
        if (rebalance$ironMirrorGuard.get()) return;

        try {
            // Always re-fetch latest state from world after vanilla may have updated it
            BlockState current = level.getBlockState(pos);
            if (!(current.getBlock() instanceof DoorBlock)) return;

            // Normalize to lower half
            if (current.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.below();
                current = level.getBlockState(pos);
                if (!(current.getBlock() instanceof DoorBlock)) return;
            }

            Block selfBlock = current.getBlock();
            // If iron door and iron mirroring disabled, skip
            if (selfBlock == Blocks.IRON_DOOR && !Config.doubleDoorRedstoneIncludeIron) return;
            
            // If copper door and iron mirroring disabled, skip (copper doors follow iron door rules)
            if ((selfBlock == Blocks.COPPER_DOOR || 
                 selfBlock == Blocks.EXPOSED_COPPER_DOOR || 
                 selfBlock == Blocks.WEATHERED_COPPER_DOOR || 
                 selfBlock == Blocks.OXIDIZED_COPPER_DOOR) && !Config.doubleDoorRedstoneIncludeIron) return;

            boolean open = current.getValue(DoorBlock.OPEN);
            Direction facing = current.getValue(DoorBlock.FACING);
            DoorHingeSide hinge = current.getValue(DoorBlock.HINGE);

            Direction[] candidates = new Direction[] { facing.getClockWise(), facing.getCounterClockWise() };
            for (Direction side : candidates) {
                BlockPos neighborPosLower = pos.relative(side);
                BlockState neighborLower = level.getBlockState(neighborPosLower);
                if (!(neighborLower.getBlock() instanceof DoorBlock)) continue;
                if (!rebalance$matchesDoorType(current, neighborLower)) continue;

                if (neighborLower.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) {
                    if (neighborLower.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                        neighborPosLower = neighborPosLower.below();
                        neighborLower = level.getBlockState(neighborPosLower);
                        if (!(neighborLower.getBlock() instanceof DoorBlock)) continue;
                    } else {
                        continue;
                    }
                }
                if (neighborLower.getValue(DoorBlock.FACING) != facing) continue;
                if (neighborLower.getValue(DoorBlock.HINGE) == hinge) continue;

                if (neighborLower.getValue(DoorBlock.OPEN) == open) break; // already correct

                rebalance$ironMirrorGuard.set(true);
                try {
                    ((DoorBlock) (Object) this).setOpen(null, level, neighborLower, neighborPosLower, open);
                } finally {
                    rebalance$ironMirrorGuard.set(false);
                }
                break;
            }
        } catch (Throwable ignored) {
        }
    }

    // Villager mirroring removed to avoid POI errors and lag. Double-door feature remains for players/redstone only.

    private static boolean rebalance$matchesDoorType(BlockState originState, BlockState candidateState) {
        if (!(candidateState.getBlock() instanceof DoorBlock)) return false;
        if (Config.doubleDoorSameBlockOnly) {
            return candidateState.getBlock() == originState.getBlock();
        }
        return candidateState.getBlock().getClass() == originState.getBlock().getClass();
    }
}
