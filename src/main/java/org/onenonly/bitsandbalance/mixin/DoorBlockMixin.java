package org.onenonly.bitsandbalance.mixin;

import org.onenonly.bitsandbalance.common.config.DoubleDoorFeatureFlags;
import org.onenonly.bitsandbalance.common.config.EnhancedSlabFeatureFlags;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
//? if mc != 1.21.1
import net.minecraft.world.level.redstone.Orientation;
//? endif
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
        if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled()) return;
        if (DoubleDoorFeatureFlags.doubleDoorCrouchSingleEnabled() && player != null && player.isShiftKeyDown()) return;

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

            if (level.isClientSide()) {
                bitsandbalance$setDoorOpenState(level, pos, state, targetOpen);
                bitsandbalance$playDoorSound(level, pos, state, targetOpen, player);
                if (pairedDoorPos != null && pairedDoorState != null && pairedDoorState.getValue(DoorBlock.OPEN) != targetOpen) {
                    bitsandbalance$setDoorOpenState(level, pairedDoorPos, pairedDoorState, targetOpen);
                    bitsandbalance$playDoorSound(level, pairedDoorPos, pairedDoorState, targetOpen, player);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            ((DoorBlock) (Object) this).setOpen(player, level, state, pos, targetOpen);
            if (pairedDoorPos != null && pairedDoorState != null && pairedDoorState.getValue(DoorBlock.OPEN) != targetOpen) {
                ((DoorBlock) (Object) this).setOpen(player, level, pairedDoorState, pairedDoorPos, targetOpen);
            }

            cir.setReturnValue(InteractionResult.SUCCESS);
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "neighborChanged", at = @At("TAIL"))
    private void bitsandbalance$ironDoubleRedstone(BlockState state, Level level, BlockPos pos, Block block,
//? if mc == 1.21.1
                                                   BlockPos neighborPos,
//? else
                                                   Orientation orientation,
//? endif
                                                   boolean isMoving, CallbackInfo ci) {
        if (!DoubleDoorFeatureFlags.doubleDoorOpeningEnabled() || !DoubleDoorFeatureFlags.doubleDoorsWithRedstoneEnabled()) return;
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
            if (selfBlock == Blocks.IRON_DOOR && !DoubleDoorFeatureFlags.doubleDoorRedstoneIncludesIron()) return;
            if ((selfBlock == Blocks.COPPER_DOOR ||
                    selfBlock == Blocks.EXPOSED_COPPER_DOOR ||
                    selfBlock == Blocks.WEATHERED_COPPER_DOOR ||
                    selfBlock == Blocks.OXIDIZED_COPPER_DOOR) && !DoubleDoorFeatureFlags.doubleDoorRedstoneIncludesIron()) return;

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
        if (DoubleDoorFeatureFlags.doubleDoorSameBlockOnly()) {
            return candidateState.getBlock() == originState.getBlock();
        }
        return candidateState.getBlock().getClass() == originState.getBlock().getClass();
    }

    private static void bitsandbalance$setDoorOpenState(Level level, BlockPos lowerPos, BlockState lowerState, boolean open) {
        level.setBlock(lowerPos, lowerState.setValue(DoorBlock.OPEN, open), 10);

        BlockPos upperPos = lowerPos.above();
        BlockState upperState = level.getBlockState(upperPos);
        if (!(upperState.getBlock() instanceof DoorBlock)) return;
        if (upperState.getValue(DoorBlock.HALF) != DoubleBlockHalf.UPPER) return;

        level.setBlock(upperPos, upperState.setValue(DoorBlock.OPEN, open), 10);
    }

    private static void bitsandbalance$playDoorSound(Level level, BlockPos pos, BlockState state, boolean open, Player player) {
        if (!(state.getBlock() instanceof DoorBlock doorBlock)) return;

        BlockSetType type = doorBlock.type();
        level.playSound(
                player,
                pos,
                open ? type.doorOpen() : type.doorClose(),
                SoundSource.BLOCKS,
                1.0F,
                level.getRandom().nextFloat() * 0.1F + 0.9F
        );
    }

    @Inject(method = "canSurvive", at = @At("RETURN"), cancellable = true)
    private void bitsandbalance$requireDirectEnhancedSlabSupport(BlockState state, LevelReader level, BlockPos pos,
                                                                 CallbackInfoReturnable<Boolean> cir) {
        if (!EnhancedSlabFeatureFlags.enabled()) return;
        if (state.getValue(DoorBlock.HALF) != DoubleBlockHalf.LOWER) return;

        BlockPos supportPos = pos.below();
        BlockState supportState = level.getBlockState(supportPos);
        if (!bitsandbalance$requiresDirectDoorSupport(supportState)) return;

        cir.setReturnValue(cir.getReturnValue()
                && bitsandbalance$doorHasDirectEnhancedSlabSupport(state, level, pos, supportState, supportPos));
    }

    private static boolean bitsandbalance$requiresDirectDoorSupport(BlockState supportState) {
        if (supportState.getBlock() instanceof VerticalSlabBlock) {
            return !supportState.getValue(VerticalSlabBlock.DOUBLE);
        }
        return supportState.getBlock() instanceof StepBlock
                || supportState.getBlock() instanceof QuadStepBlock
                || supportState.getBlock() instanceof VerticalStepBlock
                || supportState.getBlock() instanceof QuadVerticalStepBlock;
    }

    private static boolean bitsandbalance$doorHasDirectEnhancedSlabSupport(BlockState state, LevelReader level, BlockPos pos,
                                                                           BlockState supportState, BlockPos supportPos) {
        BlockState closedState = state.getValue(DoorBlock.OPEN) ? state.setValue(DoorBlock.OPEN, false) : state;
        return bitsandbalance$hasHorizontalOverlap(closedState.getShape(level, pos), supportState.getCollisionShape(level, supportPos));
    }

    private static boolean bitsandbalance$hasHorizontalOverlap(VoxelShape first, VoxelShape second) {
        for (AABB firstBox : first.toAabbs()) {
            for (AABB secondBox : second.toAabbs()) {
                if (firstBox.maxX > secondBox.minX + 1.0E-6D
                        && secondBox.maxX > firstBox.minX + 1.0E-6D
                        && firstBox.maxZ > secondBox.minZ + 1.0E-6D
                        && secondBox.maxZ > firstBox.minZ + 1.0E-6D) {
                    return true;
                }
            }
        }
        return false;
    }
}
