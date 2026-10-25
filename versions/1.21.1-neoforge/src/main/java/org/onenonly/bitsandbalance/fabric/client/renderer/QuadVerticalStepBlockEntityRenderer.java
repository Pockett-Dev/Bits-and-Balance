package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.QuadVerticalStepBlockEntity;
import org.onenonly.bitsandbalance.common.blocks.QuadVerticalStepBlock;
import org.onenonly.bitsandbalance.common.client.SlabFamilyModelSourceSelector;
import org.onenonly.bitsandbalance.common.mechanics.EnhancedSlabPlacementPredictionCache;

public class QuadVerticalStepBlockEntityRenderer implements BlockEntityRenderer<QuadVerticalStepBlockEntity> {
    private final BlockRenderDispatcher blockRenderDispatcher;

    public QuadVerticalStepBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockRenderDispatcher = context.getBlockRenderDispatcher();
    }

    @Override
    public int getViewDistance() {
        return 1024;
    }

    @Override
    public void render(QuadVerticalStepBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (EnhancedSlabTerrainRenderHelper.isEnabled()) {
            return;
        }
        if (blockEntity.getLevel() == null) {
            return;
        }
        buffer = EnhancedSlabBlockEntityRenderBuffers.withoutBlockEntityCrumbling(buffer);

        EnhancedSlabPlacementPredictionCache.SingleVerticalStepPrediction singlePrediction = EnhancedSlabPlacementPredictionCache.peekSingleVerticalStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        if (singlePrediction != null) {
            VerticalStepBlockEntityRenderer.renderSinglePrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), singlePrediction.stepState(), singlePrediction.slabState(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }

        EnhancedSlabPlacementPredictionCache.QuadVerticalStepPrediction prediction = EnhancedSlabPlacementPredictionCache.peekQuadVerticalStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        BlockState[] actualSlabs = blockEntity.getAllSlabs();
        if (prediction != null && bitsandbalance$countSlabs(actualSlabs) < 2) {
            VerticalStepBlockEntityRenderer.renderQuadPrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), prediction.quadState(), prediction.slabs(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        if (!(blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getBlock() instanceof QuadVerticalStepBlock)) {
            return;
        }
        if (prediction != null) {
            EnhancedSlabPlacementPredictionCache.clearQuadVerticalStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        }

        BlockState[] slabs = actualSlabs;
        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(slabs);
        if (fullBlockState != null) {
            LegacyEnhancedSlabRenderHelper.renderSourceModel(
                    this.blockRenderDispatcher,
                    blockEntity.getLevel(),
                    blockEntity.getBlockPos(),
                    fullBlockState,
                    poseStack,
                    buffer,
                    packedOverlay
            );
            return;
        }

        for (int index = 0; index < slabs.length; index++) {
            BlockState slab = slabs[index];
            if (slab == null) {
                continue;
            }

            Direction facing = index == 2 || index == 3 ? Direction.SOUTH : Direction.NORTH;
            Direction side = index == 1 || index == 3 ? Direction.EAST : Direction.WEST;
            LegacyEnhancedSlabRenderHelper.renderVerticalStep(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), slab, facing, side, poseStack, buffer, packedLight, packedOverlay);
        }
    }

    private static int bitsandbalance$countSlabs(BlockState[] slabs) {
        int count = 0;
        for (BlockState slab : slabs) {
            if (slab != null) {
                count++;
            }
        }
        return count;
    }
}
