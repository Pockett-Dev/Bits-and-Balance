package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
import org.onenonly.bitsandbalance.common.blockentity.VerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.blocks.VerticalStepBlock;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;

public class VerticalStepBlockEntityRenderer implements BlockEntityRenderer<VerticalStepBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;
    private static final int ALL_DIRECTIONS = LegacyEnhancedSlabRenderHelper.allDirectionMask();

    public VerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public int getViewDistance() {
        return 1024;
    }

    @Override
    public void render(VerticalStepBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) {
            return;
        }
        if (blockEntity.getLevel() == null) {
            return;
        }
        EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (prediction != null) {
            buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);
            renderQuadPrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), prediction.quadState(), prediction.slabs(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (singlePrediction != null) {
            buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);
            renderSinglePrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), singlePrediction.stepState(), singlePrediction.slabState(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        if (blockEntity.getSlabState() == null) {
            return;
        }
        if (!(blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getBlock() instanceof VerticalStepBlock)) {
            return;
        }

        buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);

        Direction facing = Direction.NORTH;
        Direction side = Direction.WEST;
        if (blockEntity.getBlockState().getBlock() instanceof VerticalStepBlock) {
            facing = blockEntity.getBlockState().getValue(VerticalStepBlock.FACING);
            side = blockEntity.getBlockState().getValue(VerticalStepBlock.SIDE);
        }

        LegacyEnhancedSlabRenderHelper.renderVerticalStep(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), blockEntity.getSlabState(), facing, side, poseStack, buffer, packedLight, packedOverlay);
    }

    static void renderSinglePrediction(BlockRenderDispatcher dispatcher, Level level, net.minecraft.core.BlockPos pos, BlockState stepState, BlockState slabState, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        Direction facing = Direction.NORTH;
        Direction side = Direction.WEST;
        if (stepState.getBlock() instanceof VerticalStepBlock) {
            facing = stepState.getValue(VerticalStepBlock.FACING);
            side = stepState.getValue(VerticalStepBlock.SIDE);
        }

        LegacyEnhancedSlabRenderHelper.renderVerticalStep(dispatcher, level, pos, slabState, facing, side, poseStack, buffer, packedLight, packedOverlay);
    }

    static void renderQuadPrediction(BlockRenderDispatcher dispatcher, Level level, net.minecraft.core.BlockPos pos, BlockState quadState, Object slabs, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        for (int index = 0; index < 4; index++) {
            BlockState slab = slot(slabs, index);
            if (slab == null) {
                continue;
            }

            Direction facing = index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH;
            Direction side = index == 1 || index == 3 ? Direction.EAST : Direction.WEST;
            LegacyEnhancedSlabRenderHelper.renderVerticalStep(dispatcher, level, pos, slab, facing, side, poseStack, buffer, packedLight, packedOverlay, quadVerticalStepVisibleMask(index, slabs));
        }
    }

    private static int quadVerticalStepVisibleMask(int index, Object slabs) {
        int mask = ALL_DIRECTIONS;
        int eastWestNeighbor = (index == 0 || index == 2) ? index + 1 : index - 1;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, eastWestNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, (index == 0 || index == 2) ? Direction.EAST : Direction.WEST);
        }
        int northSouthNeighbor = (index == 0 || index == 1) ? index + 2 : index - 2;
        if (sameRenderMaterial(slot(slabs, index), slot(slabs, northSouthNeighbor))) {
            mask = LegacyEnhancedSlabRenderHelper.without(mask, (index == 0 || index == 1) ? Direction.SOUTH : Direction.NORTH);
        }
        return mask;
    }

    private static BlockState slot(Object slabs, int index) {
        if (slabs instanceof EnhancedSlabMaterialSavedData.MaterialView view) {
            return view.slot(index);
        }
        return slabs instanceof BlockState[] states && index >= 0 && index < states.length ? states[index] : null;
    }

    private static boolean sameRenderMaterial(BlockState first, BlockState second) {
        return first != null && first.equals(second);
    }
}
