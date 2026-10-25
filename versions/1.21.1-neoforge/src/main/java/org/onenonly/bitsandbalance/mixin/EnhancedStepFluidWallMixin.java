package org.onenonly.bitsandbalance.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.QuadStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.Config;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FlowingFluid.class)
public abstract class EnhancedStepFluidWallMixin {
    private static final double FACE_EPSILON = 1.0E-7;

    @ModifyReturnValue(
            method = "canPassThroughWall(Lnet/minecraft/core/Direction;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            at = @At("RETURN")
    )
    private static boolean bitsandbalance$blockEnhancedStepWalls(boolean original,
                                                                 Direction direction,
                                                                 BlockGetter level,
                                                                 BlockPos fromPos,
                                                                 BlockState fromState,
                                                                 BlockPos toPos,
                                                                 BlockState toState) {
        if (!original) {
            return false;
        }
        if (!Config.enableEnhancedSlabs || !Config.enhancedSlabsSteps) {
            return original;
        }
        if (direction == null || !direction.getAxis().isHorizontal()) {
            return original;
        }
        if (bitsandbalance$hasClosedFluidWall(level, fromPos, fromState, direction)
                || bitsandbalance$hasClosedFluidWall(level, toPos, toState, direction.getOpposite())) {
            return false;
        }
        return original;
    }

    private static boolean bitsandbalance$hasClosedFluidWall(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!bitsandbalance$isEnhancedFluidBarrier(state)) {
            return false;
        }

        if (org.onenonly.bitsandbalance.common.mechanics.EnhancedStepFluidWallGeometry.blocksAxisFlow(
                level, pos, state, face)) {
            return true;
        }

        VoxelShape collisionShape = state.getCollisionShape(level, pos);
        if (Block.isFaceFull(collisionShape, face)) {
            return true;
        }

        return bitsandbalance$shapeTouchesFace(collisionShape, face)
                || bitsandbalance$isQuadStepStairBack(level, pos, state, face);
    }

    private static boolean bitsandbalance$isEnhancedFluidBarrier(BlockState state) {
        if (state == null) {
            return false;
        }
        return state.getBlock() instanceof StepBlock
                || state.getBlock() instanceof VerticalStepBlock
                || state.getBlock() instanceof VerticalSlabBlock
                || state.getBlock() instanceof MixedSlabBlock
                || state.getBlock() instanceof QuadStepBlock
                || state.getBlock() instanceof QuadVerticalStepBlock;
    }

    private static boolean bitsandbalance$shapeTouchesFace(VoxelShape shape, Direction face) {
        if (shape == null || shape.isEmpty()) {
            return false;
        }

        Direction.Axis axis = face.getAxis();
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE
                ? shape.max(axis) >= 1.0 - FACE_EPSILON
                : shape.min(axis) <= FACE_EPSILON;
    }

    private static boolean bitsandbalance$isQuadStepStairBack(BlockGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!(state.getBlock() instanceof QuadStepBlock) || face.getAxis() != state.getValue(QuadStepBlock.AXIS)) {
            return false;
        }

        BlockState[] slots = bitsandbalance$getQuadStepSlots(level, pos);
        if (slots == null) {
            return false;
        }

        boolean positiveFace = face == Direction.EAST || face == Direction.SOUTH;
        int topOnFace = QuadStepBlock.toIndex(positiveFace, true);
        int bottomOppositeFace = QuadStepBlock.toIndex(!positiveFace, false);
        return bitsandbalance$slot(slots, topOnFace) != null
                && bitsandbalance$slot(slots, bottomOppositeFace) != null;
    }

    private static BlockState[] bitsandbalance$getQuadStepSlots(BlockGetter level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof QuadStepBlockEntity quad) {
            return quad.getAllSlabs();
        }
        return level instanceof Level actualLevel
                ? EnhancedSlabMaterialSavedData.get(actualLevel, pos.asLong())
                : null;
    }

    private static BlockState bitsandbalance$slot(BlockState[] slots, int index) {
        return index >= 0 && index < slots.length ? slots[index] : null;
    }
}
