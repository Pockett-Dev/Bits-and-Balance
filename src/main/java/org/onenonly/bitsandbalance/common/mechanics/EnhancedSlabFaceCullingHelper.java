package org.onenonly.bitsandbalance.common.mechanics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blocks.MixedSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalSlabBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;

public final class EnhancedSlabFaceCullingHelper {
    private static final double EPSILON = 1.0E-4D;

    private EnhancedSlabFaceCullingHelper() {
    }

    public static boolean shouldForceRenderFace(BlockGetter level,
                                                BlockPos pos,
                                                BlockState state,
                                                Direction direction,
                                                BlockPos adjacentPos) {
        if (level == null || pos == null || state == null || direction == null || adjacentPos == null) {
            return false;
        }

        BlockState ownerState = state;
        BlockState adjacentState = level.getBlockState(adjacentPos);
        return isEnhancedSlabFamily(ownerState)
                || isEnhancedSlabFamily(adjacentState)
                || hasVisualOffset(level, pos, state)
                || hasHangBelowRenderOffset(level, pos, state)
                || hasVisualOffset(level, adjacentPos, adjacentState)
                || hasHangBelowRenderOffset(level, adjacentPos, adjacentState);
    }

    public static boolean shouldForceRenderFaceWithoutLevel(BlockState state, BlockState adjacentState) {
        return mayUseEnhancedVisualOffset(state) || mayUseEnhancedVisualOffset(adjacentState);
    }

    private static boolean hasVisualOffset(BlockGetter level, BlockPos pos, BlockState state) {
        return state != null && !state.isAir()
                && Math.abs(EnhancedSlabHelper.getVisualYOffset(level, pos, state)) > EPSILON;
    }

    private static boolean hasHangBelowRenderOffset(BlockGetter level, BlockPos pos, BlockState state) {
        if (level == null || pos == null || state == null || state.isAir()) {
            return false;
        }
        if (!EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state)) {
            return false;
        }
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        return EnhancedSlabHelper.hasHangBelowOffsetSurface(level, abovePos, aboveState);
    }

    private static boolean mayUseEnhancedVisualOffset(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        return EnhancedSlabHelper.isVisualOffsetEligibleNonSlab(state)
                || isEnhancedSlabFamily(state);
    }

    private static boolean isEnhancedSlabFamily(BlockState state) {
        if (state == null || state.isAir()) {
            return false;
        }
        Object block = state.getBlock();
        return block instanceof MixedSlabBlock
                || block instanceof VerticalSlabBlock
                || block instanceof StepBlock
                || block instanceof VerticalStepBlock
                || block instanceof QuadStepBlock
                || block instanceof QuadVerticalStepBlock;
    }
}
