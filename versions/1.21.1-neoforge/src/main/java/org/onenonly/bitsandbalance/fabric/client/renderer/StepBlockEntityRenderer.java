package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.StepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadStepBlock;
import org.onenonly.bitsandbalance.common.blocks.StepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;

public class StepBlockEntityRenderer implements BlockEntityRenderer<StepBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;
    private static final int ALL_DIRECTIONS = LegacyEnhancedSlabRenderHelper.allDirectionMask();

    public StepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public int getViewDistance() {
        return 1024;
    }

    @Override
    public void render(StepBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) {
            return;
        }
        if (blockEntity.getLevel() == null) {
            return;
        }
        EnhancedSlabPlacementPredictionCache.QuadStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekQuadStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (prediction != null) {
            buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);
            renderQuadPrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), prediction.quadState(), prediction.slabs(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        EnhancedSlabPlacementPredictionCache.SingleStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (singlePrediction != null) {
            buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);
            renderSinglePrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), singlePrediction.stepState(), singlePrediction.slabState(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        if (blockEntity.getSlabState() == null) {
            return;
        }
        if (!(blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getBlock() instanceof StepBlock)) {
            return;
        }

        buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);

        Direction facing = Direction.EAST;
        boolean top = false;
        if (blockEntity.getBlockState().getBlock() instanceof StepBlock) {
            facing = blockEntity.getBlockState().getValue(StepBlock.FACING);
            top = blockEntity.getBlockState().getValue(StepBlock.TOP);
        }

        LegacyEnhancedSlabRenderHelper.renderStep(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getSlabState(), facing, top, poseStack, buffer, packedLight, packedOverlay);
    }

    static void renderSinglePrediction(BlockRenderDispatcher dispatcher, Level level, net.minecraft.core.BlockPos pos, BlockState stepState, BlockState slabState, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = Direction.EAST;
        boolean top = false;
        if (stepState.getBlock() instanceof StepBlock) {
            facing = stepState.getValue(StepBlock.FACING);
            top = stepState.getValue(StepBlock.TOP);
        }

        LegacyEnhancedSlabRenderHelper.renderStep(dispatcher, level, pos, slabState, facing, top, poseStack, buffer, packedLight, packedOverlay);
    }

    static void renderQuadPrediction(BlockRenderDispatcher dispatcher, Level level, net.minecraft.core.BlockPos pos, BlockState quadState, BlockState[] slabs, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction.Axis axis = Direction.Axis.X;
        if (quadState.getBlock() instanceof QuadStepBlock) {
            axis = quadState.getValue(QuadStepBlock.AXIS);
        }

        for (int index = 0; index < slabs.length; index++) {
            BlockState slab = slabs[index];
            if (slab == null) {
                continue;
            }

            boolean positive = index == 1 || index == 3;
            Direction facing = axis == Direction.Axis.Z
                ? (positive ? Direction.SOUTH : Direction.NORTH)
                : (positive ? Direction.EAST : Direction.WEST);
            boolean top = index >= 2;

            LegacyEnhancedSlabRenderHelper.renderStep(dispatcher, level, pos, slab, facing, top, poseStack, buffer, packedLight, packedOverlay, quadStepVisibleMask(axis, index, slabs));
        }
    }

    private static int quadStepVisibleMask(Direction.Axis axis, int index, BlockState[] slabs) {
        int mask = ALL_DIRECTIONS;
        boolean positive = index == 1 || index == 3;
        boolean top = index >= 2;
        int horizontalNeighbor = top ? (positive ? 2 : 3) : (positive ? 0 : 1);
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, horizontalNeighbor))) {
            Direction internal = axis == Direction.Axis.Z
                    ? (positive ? Direction.NORTH : Direction.SOUTH)
                    : (positive ? Direction.WEST : Direction.EAST);
            mask = LegacyEnhancedSlabRenderHelper.without(mask, internal);
        }
        int verticalNeighbor = top ? index - 2 : index + 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, verticalNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, top ? Direction.DOWN : Direction.UP);
        }
        return mask;
    }

    private static BlockState slot(BlockState[] slabs, int index) {
        return slabs != null && index >= 0 && index < slabs.length ? slabs[index] : null;
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }
}
