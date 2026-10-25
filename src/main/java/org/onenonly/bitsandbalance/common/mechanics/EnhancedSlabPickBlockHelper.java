package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
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
import org.onenonly.bitsandbalance.common.registry.StepDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;
import org.onenonly.bitsandbalance.common.registry.VerticalStepDynamicRegistry;

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
                BlockEntity blockEntity = level.getBlockEntity(pos);
                VerticalSlabBlockEntity verticalSlab = blockEntity instanceof VerticalSlabBlockEntity vbe ? vbe : null;
                BlockState[] stored = verticalSlab == null ? storedSlabs(level, pos) : null;

                Direction facing = current.getValue(VerticalSlabBlock.FACING);
                Direction targeted = facing;
                if (current.getValue(VerticalSlabBlock.DOUBLE)) {
                    if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
                        targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(pos, hitResult.getLocation(), facing);
                    } else if (player != null) {
                        targeted = VerticalSlabBlock.bitsandbalance$getTargetedHalf(player, pos, facing);
                    }
                }

                BlockState sourceState = targeted == facing
                        ? facingSlab(verticalSlab, stored)
                        : oppositeSlab(verticalSlab, stored);
                if (sourceState == null) {
                    sourceState = targeted == facing
                            ? oppositeSlab(verticalSlab, stored)
                            : facingSlab(verticalSlab, stored);
                }
                if (sourceState == null) {
                    sourceState = verticalSlabSourceFallback(current);
                }
                return result(sourceState, sourceState != null ? VerticalSlabBlock.bitsandbalance$makeHalfDropItem(sourceState) : ItemStack.EMPTY);
            }

            if (current.getBlock() instanceof MixedSlabBlock) {
                BlockState[] stored = storedSlabs(level, pos);
                if (stored == null) return null;

                SlabType targeted = SlabType.BOTTOM;
                if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
                    targeted = EnhancedSlabHelper.getTargetedHalf(pos, hitResult.getLocation());
                } else if (player != null) {
                    targeted = EnhancedSlabHelper.getTargetedHalfFromRayToFullCube(player, pos);
                }
                BlockState sourceState = targeted == SlabType.TOP ? slot(stored, 1) : slot(stored, 0);
                if (sourceState == null) {
                    sourceState = targeted == SlabType.TOP ? slot(stored, 0) : slot(stored, 1);
                }
                return result(sourceState, makeSlabItem(sourceState));
            }

            if (current.getBlock() instanceof FixedStepBlock fixedStep) {
                BlockState sourceState = fixedStep.getSourceSlab().defaultBlockState();
                return result(sourceState, StepBlock.bitsandbalance$makeStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof StepBlock) {
                BlockState sourceState = singleStepSlab(level, pos, current);
                return result(sourceState, StepBlock.bitsandbalance$makeStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof QuadStepBlock) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                QuadStepBlockEntity quadStep = blockEntity instanceof QuadStepBlockEntity qse ? qse : null;
                BlockState[] stored = quadStep == null ? storedSlabs(level, pos) : null;
                if (quadStep == null && stored == null) return null;

                int index = resolveQuadStepIndex(player, pos, hitResult, current.getValue(QuadStepBlock.AXIS), quadStep, stored);
                if (index < 0) return null;

                BlockState sourceState = slabAt(quadStep, stored, index);
                return result(sourceState, sourceState != null ? StepBlock.bitsandbalance$makeStepDropItem(sourceState) : ItemStack.EMPTY);
            }

            if (current.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
                BlockState sourceState = fixedVerticalStep.getSourceVerticalSlab().defaultBlockState();
                return result(sourceState, VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof VerticalStepBlock) {
                BlockState sourceState = singleVerticalStepSlab(level, pos, current);
                return result(sourceState, VerticalStepBlock.bitsandbalance$makeVerticalStepDropItem(sourceState));
            }

            if (current.getBlock() instanceof QuadVerticalStepBlock) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                QuadVerticalStepBlockEntity quadVerticalStep = blockEntity instanceof QuadVerticalStepBlockEntity qvse ? qvse : null;
                BlockState[] stored = quadVerticalStep == null ? storedSlabs(level, pos) : null;
                if (quadVerticalStep == null && stored == null) return null;

                int index = resolveQuadVerticalStepIndex(player, pos, hitResult, quadVerticalStep, stored);
                if (index < 0) return null;

                BlockState sourceState = slabAt(quadVerticalStep, stored, index);
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

    private static @Nullable BlockState[] storedSlabs(LevelReader level, BlockPos pos) {
        return level instanceof Level actualLevel ? EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong()) : null;
    }

    private static @Nullable BlockState slot(@Nullable BlockState[] stored, int index) {
        return stored != null && index >= 0 && index < stored.length ? stored[index] : null;
    }

    private static @Nullable BlockState facingSlab(@Nullable VerticalSlabBlockEntity verticalSlab, @Nullable BlockState[] stored) {
        return verticalSlab != null ? verticalSlab.getFacingSlab() : slot(stored, 0);
    }

    private static @Nullable BlockState oppositeSlab(@Nullable VerticalSlabBlockEntity verticalSlab, @Nullable BlockState[] stored) {
        return verticalSlab != null ? verticalSlab.getOppositeSlab() : slot(stored, 1);
    }


    private static @Nullable BlockState slabAt(@Nullable QuadStepBlockEntity quadStep, @Nullable BlockState[] stored, int index) {
        return quadStep != null ? quadStep.getSlabAt(index) : slot(stored, index);
    }

    private static @Nullable BlockState slabAt(@Nullable QuadVerticalStepBlockEntity quadVerticalStep, @Nullable BlockState[] stored, int index) {
        return quadVerticalStep != null ? quadVerticalStep.getSlabAt(index) : slot(stored, index);
    }

    private static @Nullable BlockState verticalSlabSourceFallback(BlockState current) {
        Block slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(current.getBlock());
        return slabBlock != null ? slabBlock.defaultBlockState() : null;
    }

    private static @Nullable BlockState singleStepSlab(LevelReader level, BlockPos pos, BlockState current) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof StepBlockEntity step && step.getSlabState() != null) {
            return step.getSlabState();
        }
        BlockState stored = slot(storedSlabs(level, pos), 0);
        if (stored != null) return stored;
        Block slabBlock = StepDynamicRegistry.getSlabForStep(current.getBlock());
        return slabBlock != null ? slabBlock.defaultBlockState() : null;
    }

    private static @Nullable BlockState singleVerticalStepSlab(LevelReader level, BlockPos pos, BlockState current) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
            return verticalStep.getSlabState();
        }
        BlockState stored = slot(storedSlabs(level, pos), 0);
        if (stored != null) return stored;
        Block verticalSlabBlock = VerticalStepDynamicRegistry.getVerticalSlabForVerticalStep(current.getBlock());
        if (verticalSlabBlock == null) return null;
        Block slabBlock = VerticalSlabDynamicRegistry.getSlabForVertical(verticalSlabBlock);
        return slabBlock != null ? slabBlock.defaultBlockState() : verticalSlabBlock.defaultBlockState();
    }

    private static int resolveQuadStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            Direction.Axis axis,
            @Nullable QuadStepBlockEntity quadStep,
            @Nullable BlockState[] stored
    ) {
        int clippedIndex = clipQuadStepIndex(player, pos, hitResult, axis, quadStep, stored);
        if (clippedIndex >= 0) return clippedIndex;

        if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
            return resolveOccupiedIndex(
                    quadStep,
                    stored,
                    QuadStepBlock.bitsandbalance$getTargetedIndex(
                            pos,
                            hitResult.getLocation(),
                            hitResult.getDirection(),
                            axis
                    )
            );
        }

        if (player != null) {
            return resolveOccupiedIndex(
                    quadStep,
                    stored,
                    QuadStepBlock.bitsandbalance$getTargetedIndex(player, pos, axis)
            );
        }

        return firstOccupiedIndex(quadStep, stored);
    }

    private static int resolveQuadVerticalStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            @Nullable QuadVerticalStepBlockEntity quadVerticalStep,
            @Nullable BlockState[] stored
    ) {
        int clippedIndex = clipQuadVerticalStepIndex(player, pos, hitResult, quadVerticalStep, stored);
        if (clippedIndex >= 0) return clippedIndex;

        if (hitResult != null && pos.equals(hitResult.getBlockPos())) {
            return resolveOccupiedIndex(
                    quadVerticalStep,
                    stored,
                    QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(
                            pos,
                            hitResult.getLocation(),
                            hitResult.getDirection()
                    )
            );
        }

        if (player != null) {
            return resolveOccupiedIndex(
                    quadVerticalStep,
                    stored,
                    QuadVerticalStepBlock.bitsandbalance$getTargetedIndex(player, pos)
            );
        }

        return firstOccupiedIndex(quadVerticalStep, stored);
    }

    private static int clipQuadStepIndex(
            @Nullable Player player,
            BlockPos pos,
            @Nullable BlockHitResult hitResult,
            Direction.Axis axis,
            @Nullable QuadStepBlockEntity quadStep,
            @Nullable BlockState[] stored
    ) {
        if (player == null || hitResult == null || !pos.equals(hitResult.getBlockPos())) return -1;

        Vec3 start = EnhancedSlabHelper.getInteractionRay(player, pos).start();
        Vec3 end = extendPastHit(start, hitResult.getLocation());
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < 4; i++) {
            if (slabAt(quadStep, stored, i) == null) continue;

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
            @Nullable QuadVerticalStepBlockEntity quadVerticalStep,
            @Nullable BlockState[] stored
    ) {
        if (player == null || hitResult == null || !pos.equals(hitResult.getBlockPos())) return -1;

        Vec3 start = EnhancedSlabHelper.getInteractionRay(player, pos).start();
        Vec3 end = extendPastHit(start, hitResult.getLocation());
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;

        for (int i = 0; i < 4; i++) {
            if (slabAt(quadVerticalStep, stored, i) == null) continue;

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

    private static int resolveOccupiedIndex(@Nullable QuadStepBlockEntity quadStep, @Nullable BlockState[] stored, int preferred) {
        if (slabAt(quadStep, stored, preferred) != null) return preferred;
        return firstOccupiedIndex(quadStep, stored);
    }

    private static int resolveOccupiedIndex(@Nullable QuadVerticalStepBlockEntity quadVerticalStep, @Nullable BlockState[] stored, int preferred) {
        if (slabAt(quadVerticalStep, stored, preferred) != null) return preferred;
        return firstOccupiedIndex(quadVerticalStep, stored);
    }

    private static int firstOccupiedIndex(@Nullable QuadStepBlockEntity quadStep, @Nullable BlockState[] stored) {
        for (int i = 0; i < 4; i++) {
            if (slabAt(quadStep, stored, i) != null) return i;
        }
        return -1;
    }

    private static int firstOccupiedIndex(@Nullable QuadVerticalStepBlockEntity quadVerticalStep, @Nullable BlockState[] stored) {
        for (int i = 0; i < 4; i++) {
            if (slabAt(quadVerticalStep, stored, i) != null) return i;
        }
        return -1;
    }
}
