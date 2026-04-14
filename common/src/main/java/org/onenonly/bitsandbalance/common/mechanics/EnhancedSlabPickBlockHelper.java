package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.MixedSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;

public final class EnhancedSlabPickBlockHelper {
    private EnhancedSlabPickBlockHelper() {
    }

    public record PickTarget(BlockState sourceState, ItemStack stack) {
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, BlockPos pos) {
        return resolve(level, player, pos, level.getBlockState(pos));
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, @Nullable BlockHitResult hitResult) {
        if (hitResult == null) return null;
        BlockPos pos = hitResult.getBlockPos();
        return resolve(level, player, pos, level.getBlockState(pos), hitResult);
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, BlockPos pos, BlockState current) {
        return resolve(level, player, pos, current, null);
    }

    public static @Nullable PickTarget resolve(LevelReader level, @Nullable Player player, BlockPos pos, BlockState current, @Nullable BlockHitResult hitResult) {
        try {
            if (current.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
                BlockState sourceState = null;
                if (level.getBlockEntity(pos) instanceof VerticalSlabBlockEntity verticalSlab) {
                    sourceState = verticalSlab.getFacingSlab();
                    if (sourceState == null) {
                        sourceState = verticalSlab.getOppositeSlab();
                    }
                }
                if (sourceState == null) {
                    sourceState = fixedVerticalSlab.getSourceSlab().defaultBlockState();
                }
                return result(sourceState, VerticalSlabBlock.bitsandbalance$makeHalfDropItem(sourceState));
            }

            if (current.getBlock() instanceof VerticalSlabBlock) {
                if (!(level.getBlockEntity(pos) instanceof VerticalSlabBlockEntity verticalSlab)) return null;

                Direction facing = current.getValue(VerticalSlabBlock.FACING);
                Direction targeted = facing;
                if (current.getValue(VerticalSlabBlock.DOUBLE)) {
                    if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
                        targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, hitResult.getLocation(), facing);
                    } else if (player != null) {
                        targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                    }
                }

                BlockState sourceState = targeted == facing ? verticalSlab.getFacingSlab() : verticalSlab.getOppositeSlab();
                if (sourceState == null) {
                    sourceState = targeted == facing ? verticalSlab.getOppositeSlab() : verticalSlab.getFacingSlab();
                }
                return result(sourceState, sourceState != null ? VerticalSlabBlock.bitsandbalance$makeHalfDropItem(sourceState) : ItemStack.EMPTY);
            }

            if (current.getBlock() instanceof MixedSlabBlock) {
                if (!(level.getBlockEntity(pos) instanceof MixedSlabBlockEntity mixedSlab)) return null;

                SlabType targeted = SlabType.BOTTOM;
                if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
                    targeted = EnhancedSlabHelper.getTargetedHalf(pos, hitResult.getLocation());
                } else if (player != null) {
                    targeted = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
                }
                BlockState sourceState = targeted == SlabType.TOP ? mixedSlab.getTopSlab() : mixedSlab.getBottomSlab();
                if (sourceState == null) {
                    sourceState = targeted == SlabType.TOP ? mixedSlab.getBottomSlab() : mixedSlab.getTopSlab();
                }
                return result(sourceState, makeSlabItem(sourceState));
            }

            if (current.getBlock() instanceof FixedStepBlock fixedStep) {
                BlockState sourceState = fixedStep.getSourceSlab().defaultBlockState();
                return result(sourceState, StepBlock.bitsandbalance$makeStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof StepBlock) {
                if (!(level.getBlockEntity(pos) instanceof StepBlockEntity step) || step.getSlabState() == null) return null;
                BlockState sourceState = step.getSlabState();
                return result(sourceState, StepBlock.bitsandbalance$makeStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof QuadStepBlock) {
                if (!(level.getBlockEntity(pos) instanceof QuadStepBlockEntity quadStep)) return null;

                int index = resolveQuadStepIndex(player, pos, hitResult, current.getValue(QuadStepBlock.AXIS), quadStep);
                if (index < 0) return null;

                BlockState sourceState = quadStep.getSlabAt(index);
                return result(sourceState, sourceState != null ? StepBlock.bitsandbalance$makeStepDropItem(sourceState) : ItemStack.EMPTY);
            }

            if (current.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
                BlockState sourceState = fixedVerticalStep.getSourceVerticalSlab().defaultBlockState();
                return result(sourceState, VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof VerticalStepBlock) {
                if (!(level.getBlockEntity(pos) instanceof VerticalStepBlockEntity verticalStep) || verticalStep.getSlabState() == null) return null;
                BlockState sourceState = verticalStep.getSlabState();
                return result(sourceState, VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof QuadVerticalStepBlock) {
                if (!(level.getBlockEntity(pos) instanceof QuadVerticalStepBlockEntity quadVerticalStep)) return null;

                int index = resolveQuadVerticalStepIndex(player, pos, hitResult, quadVerticalStep);
                if (index < 0) return null;

                BlockState sourceState = quadVerticalStep.getSlabAt(index);
                return result(sourceState, sourceState != null ? VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(sourceState) : ItemStack.EMPTY);
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    private static @Nullable PickTarget result(@Nullable BlockState sourceState, ItemStack stack) {
        if (sourceState == null || stack.isEmpty()) return null;
        return new PickTarget(sourceState, stack);
    }

    private static ItemStack makeSlabItem(@Nullable BlockState sourceState) {
        if (sourceState == null) return ItemStack.EMPTY;
        return new ItemStack(sourceState.getBlock().asItem());
    }

    private static int resolveQuadStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            Direction.Axis axis,
            QuadStepBlockEntity quadStep
    ) {
        int clippedIndex = clipQuadStepIndex(player, pos, hitResult, axis, quadStep);
        if (clippedIndex >= 0) return clippedIndex;

        if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
            return QuadStepBlock.bitsandbalance$resolveOccupiedIndex(
                    quadStep,
                    QuadStepBlock.bitsandbalance$getTargetedIndex(
                            pos,
                            hitResult.getLocation(),
                            hitResult.getDirection(),
                            axis
                    )
            );
        }

        if (player != null) {
            return QuadStepBlock.bitsandbalance$resolveOccupiedIndex(
                    quadStep,
                    QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis)
            );
        }

        return firstOccupiedIndex(quadStep);
    }

    private static int resolveQuadVerticalStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            QuadVerticalStepBlockEntity quadVerticalStep
    ) {
        int clippedIndex = clipQuadVerticalStepIndex(player, pos, hitResult, quadVerticalStep);
        if (clippedIndex >= 0) return clippedIndex;

        if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
            return QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(
                    quadVerticalStep,
                    QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(
                            pos,
                            hitResult.getLocation(),
                            hitResult.getDirection()
                    )
            );
        }

        if (player != null) {
            return QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(
                    quadVerticalStep,
                    QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos)
            );
        }

        return firstOccupiedIndex(quadVerticalStep);
    }

    private static int clipQuadStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            Direction.Axis axis,
            QuadStepBlockEntity quadStep
    ) {
        if (player == null || hitResult == null || !pos.equals(hitResult.getBlockPos())) return -1;

        Vec3 start = player.getEyePosition();
        Vec3 end = extendPastHit(start, hitResult.getLocation());
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < 4; i++) {
            if (quadStep.getSlabAt(i) == null) continue;

            VoxelShape shape = QuadStepBlock.shapeForIndex(axis, i);
            BlockHitResult clipped = shape.clip(start, end, pos);
            if (clipped == null) continue;

            double distance = start.distanceToSqr(clipped.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static int clipQuadVerticalStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            QuadVerticalStepBlockEntity quadVerticalStep
    ) {
        if (player == null || hitResult == null || !pos.equals(hitResult.getBlockPos())) return -1;

        Vec3 start = player.getEyePosition();
        Vec3 end = extendPastHit(start, hitResult.getLocation());
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < 4; i++) {
            if (quadVerticalStep.getSlabAt(i) == null) continue;

            VoxelShape shape = QuadVerticalStepBlock.shapeForIndex(i);
            BlockHitResult clipped = shape.clip(start, end, pos);
            if (clipped == null) continue;

            double distance = start.distanceToSqr(clipped.getLocation());
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static Vec3 extendPastHit(Vec3 start, Vec3 hitLocation) {
        Vec3 delta = hitLocation.subtract(start);
        double lengthSqr = delta.lengthSqr();
        if (lengthSqr < 1.0E-8) {
            return hitLocation;
        }
        return hitLocation.add(delta.scale(0.01 / Math.sqrt(lengthSqr)));
    }

    private static int firstOccupiedIndex(QuadStepBlockEntity quadStep) {
        for (int i = 0; i < 4; i++) {
            if (quadStep.getSlabAt(i) != null) return i;
        }
        return -1;
    }

    private static int firstOccupiedIndex(QuadVerticalStepBlockEntity quadVerticalStep) {
        for (int i = 0; i < 4; i++) {
            if (quadVerticalStep.getSlabAt(i) != null) return i;
        }
        return -1;
    }
}