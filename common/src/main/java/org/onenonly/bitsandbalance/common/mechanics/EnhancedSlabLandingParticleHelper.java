package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalSlabBlockEntity;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.FixedStepBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.FixedVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.registry.VerticalSlabDynamicRegistry;

/**
 * Resolves the block state vanilla should use for enhanced-slab landing particles.
 */
public final class EnhancedSlabLandingParticleHelper {

    private EnhancedSlabLandingParticleHelper() {
    }

    public static BlockState resolveLandingParticleState(LivingEntity entity, BlockState state, BlockPos pos) {
        if (state == null) return null;

        Level level = entity.level();

        try {
            if (state.getBlock() instanceof FixedVerticalSlabBlock fixedVerticalSlab) {
                return fixedVerticalSlab.getSourceSlab().defaultBlockState();
            }

            if (state.getBlock() instanceof VerticalSlabBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VerticalSlabBlockEntity verticalSlab) {
                    BlockState resolved = bitsandbalance$resolveVerticalSlabState(entity, pos, state, verticalSlab);
                    if (resolved != null) return resolved;
                }
                return state;
            }

            if (state.getBlock() instanceof FixedStepBlock fixedStep) {
                return fixedStep.getSourceSlab().defaultBlockState();
            }

            if (state.getBlock() instanceof StepBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof StepBlockEntity step && step.getSlabState() != null) {
                    return step.getSlabState();
                }
                return state;
            }

            if (state.getBlock() instanceof QuadStepBlock quadStepBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof QuadStepBlockEntity quad) {
                    int preferred = QuadStepBlock.toIndex(
                            bitsandbalance$isPositive(entity, pos, state.getValue(QuadStepBlock.AXIS)),
                            bitsandbalance$isTop(entity, pos)
                    );
                    int resolvedIndex = QuadStepBlock.bitsandbalance$resolveOccupiedIndex(quad, preferred);
                    BlockState resolved = quad.getSlabAt(resolvedIndex);
                    if (resolved != null) return resolved;
                }
                return state;
            }

            if (state.getBlock() instanceof FixedVerticalStepBlock fixedVerticalStep) {
                return bitsandbalance$sourceRenderState(fixedVerticalStep.getSourceVerticalSlab());
            }

            if (state.getBlock() instanceof VerticalStepBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof VerticalStepBlockEntity verticalStep && verticalStep.getSlabState() != null) {
                    return verticalStep.getSlabState();
                }
                return state;
            }

            if (state.getBlock() instanceof QuadVerticalStepBlock) {
                BlockEntity be = level.getBlockEntity(pos);
                if (be instanceof QuadVerticalStepBlockEntity quad) {
                    int preferred = QuadVerticalStepBlock.toIndex(
                            bitsandbalance$isEast(entity, pos),
                            bitsandbalance$isSouth(entity, pos)
                    );
                    int resolvedIndex = QuadVerticalStepBlock.bitsandbalance$resolveOccupiedIndex(quad, preferred);
                    BlockState resolved = quad.getSlabAt(resolvedIndex);
                    if (resolved != null) return resolved;
                }
                return state;
            }
        } catch (Throwable ignored) {
        }

        return state;
    }

    private static @Nullable BlockState bitsandbalance$resolveVerticalSlabState(LivingEntity entity,
                                                                                BlockPos pos,
                                                                                BlockState state,
                                                                                VerticalSlabBlockEntity verticalSlab) {
        BlockState facingState = verticalSlab.getFacingSlab();
        BlockState oppositeState = verticalSlab.getOppositeSlab();

        if (!state.getValue(VerticalSlabBlock.DOUBLE)) {
            return facingState != null ? facingState : oppositeState;
        }

        Direction facing = state.getValue(VerticalSlabBlock.FACING);
        boolean onFacingHalf = switch (facing) {
            case NORTH -> !bitsandbalance$isSouth(entity, pos);
            case SOUTH -> bitsandbalance$isSouth(entity, pos);
            case WEST -> !bitsandbalance$isEast(entity, pos);
            case EAST -> bitsandbalance$isEast(entity, pos);
            default -> true;
        };

        BlockState preferred = onFacingHalf ? facingState : oppositeState;
        if (preferred != null) return preferred;
        return onFacingHalf ? oppositeState : facingState;
    }

    private static BlockState bitsandbalance$sourceRenderState(Block sourceVerticalSlab) {
        Block sourceSlab = VerticalSlabDynamicRegistry.getSlabForVertical(sourceVerticalSlab);
        if (sourceSlab != null) return sourceSlab.defaultBlockState();
        return sourceVerticalSlab.defaultBlockState();
    }

    private static boolean bitsandbalance$isTop(LivingEntity entity, BlockPos pos) {
        return (entity.getY() - pos.getY()) > 0.5D;
    }

    private static boolean bitsandbalance$isPositive(LivingEntity entity, BlockPos pos, Direction.Axis axis) {
        return axis == Direction.Axis.Z
                ? bitsandbalance$isSouth(entity, pos)
                : bitsandbalance$isEast(entity, pos);
    }

    private static boolean bitsandbalance$isEast(LivingEntity entity, BlockPos pos) {
        return (entity.getX() - pos.getX()) > 0.5D;
    }

    private static boolean bitsandbalance$isSouth(LivingEntity entity, BlockPos pos) {
        return (entity.getZ() - pos.getZ()) > 0.5D;
    }
}