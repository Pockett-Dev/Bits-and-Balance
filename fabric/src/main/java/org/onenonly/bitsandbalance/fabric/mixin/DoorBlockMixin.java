package org.onenonly.bitsandbalance.fabric.mixin;

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
import org.onenonly.bitsandbalance.fabric.config.FabricTweaksConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tweaks: Double Door Opening
 * When a wooden door is toggled by right-click, toggle its paired adjacent door to match.
 */
@Mixin(DoorBlock.class)
public abstract class DoorBlockMixin {

    private static final ThreadLocal<Boolean> bitsandbalance$ironMirrorGuard = ThreadLocal.withInitial(() -> false);

    @Inject(method = "useWithoutItem", at = @At("HEAD"), cancellable = true)
    private void bitsandbalance$doubleDoorImmediate(BlockState stateAtCall, Level level, BlockPos pos, Player player, net.minecraft.world.phys.BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (!FabricTweaksConfig.enableDoubleDoorOpening) return;
        if (level.isClientSide()) return;
        if (FabricTweaksConfig.doubleDoorCrouchSingle && player != null && player.isShiftKeyDown()) return;

        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof DoorBlock)) return;

            if (state.getBlock() == Blocks.IRON_DOOR ||
                    state.getBlock() == Blocks.COPPER_DOOR ||
                    state.getBlock() == Blocks.EXPOSED_COPPER_DOOR ||
                    state.getBlock() == Blocks.WEATHERED_COPPER_DOOR ||
                    state.getBlock() == Blocks.OXIDIZED_COPPER_DOOR) return;

            if (state.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.below();
                state = level.getBlockState(pos);
                if (!(state.getBlock() instanceof DoorBlock)) return;
            }

            boolean currentOpen = state.getValue(DoorBlock.OPEN);
            boolean targetOpen = !currentOpen;
            Direction facing = state.getValue(DoorBlock.FACING);
            DoorHingeSide hinge = state.getValue(DoorBlock.HINGE);

            BlockPos pairedDoorPos = null;
            BlockState pairedDoorState = null;
            Direction[] candidates = new Direction[]{facing.getClockWise(), facing.getCounterClockWise()};

            for (Direction side : candidates) {
                BlockPos neighborPosLower = pos.relative(side);
                BlockState neighborLower = level.getBlockState(neighborPosLower);
                if (!(neighborLower.getBlock() instanceof DoorBlock)) continue;
                if (!bitsandbalance$matchesDoorType(state, neighborLower)) continue;

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

                pairedDoorPos = neighborPosLower;
                pairedDoorState = neighborLower;
                break;
            }

            ((DoorBlock) (Object) this).setOpen(player, level, state, pos, targetOpen);
            if (pairedDoorPos != null && pairedDoorState != null) {
                if (pairedDoorState.getValue(DoorBlock.OPEN) != targetOpen) {
                    ((DoorBlock) (Object) this).setOpen(player, level, pairedDoorState, pairedDoorPos, targetOpen);
                }
            }

            cir.setReturnValue(InteractionResult.CONSUME);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void bitsandbalance$ironDoubleRedstone(BlockState state, Level level, BlockPos pos, Block block, Orientation orientation, boolean isMoving, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        if (!FabricTweaksConfig.enableDoubleDoorOpening || !FabricTweaksConfig.doubleDoorsWithRedstone) return;
        if (level.isClientSide()) return;
        if (bitsandbalance$ironMirrorGuard.get()) return;

        try {
            BlockState current = level.getBlockState(pos);
            if (!(current.getBlock() instanceof DoorBlock)) return;

            if (current.getValue(DoorBlock.HALF) == DoubleBlockHalf.UPPER) {
                pos = pos.below();
                current = level.getBlockState(pos);
                if (!(current.getBlock() instanceof DoorBlock)) return;
            }

            Block selfBlock = current.getBlock();
            if (selfBlock == Blocks.IRON_DOOR && !FabricTweaksConfig.doubleDoorRedstoneIncludeIron) return;
            if ((selfBlock == Blocks.COPPER_DOOR ||
                    selfBlock == Blocks.EXPOSED_COPPER_DOOR ||
                    selfBlock == Blocks.WEATHERED_COPPER_DOOR ||
                    selfBlock == Blocks.OXIDIZED_COPPER_DOOR) && !FabricTweaksConfig.doubleDoorRedstoneIncludeIron) return;

            boolean open = current.getValue(DoorBlock.OPEN);
            Direction facing = current.getValue(DoorBlock.FACING);
            DoorHingeSide hinge = current.getValue(DoorBlock.HINGE);

            Direction[] candidates = new Direction[]{facing.getClockWise(), facing.getCounterClockWise()};
            for (Direction side : candidates) {
                BlockPos neighborPosLower = pos.relative(side);
                BlockState neighborLower = level.getBlockState(neighborPosLower);
                if (!(neighborLower.getBlock() instanceof DoorBlock)) continue;
                if (!bitsandbalance$matchesDoorType(current, neighborLower)) continue;

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

                if (neighborLower.getValue(DoorBlock.OPEN) == open) break;

                bitsandbalance$ironMirrorGuard.set(true);
                try {
                    ((DoorBlock) (Object) this).setOpen(null, level, neighborLower, neighborPosLower, open);
                } finally {
                    bitsandbalance$ironMirrorGuard.set(false);
                }
                break;
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean bitsandbalance$matchesDoorType(BlockState originState, BlockState candidateState) {
        if (!(candidateState.getBlock() instanceof DoorBlock)) return false;
        if (FabricTweaksConfig.doubleDoorSameBlockOnly) {
            return candidateState.getBlock() == originState.getBlock();
        }
        return candidateState.getBlock().getClass() == originState.getBlock().getClass();
    }
}
