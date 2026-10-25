package org.onenonly.bitsandbalance.fabric.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.onenonly.bitsandbalance.common.blockentity.EnhancedSlabMaterialSavedData;
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
        Object actualSlabs = bitsandbalance$bestSlots(blockEntity, blockEntity.getAllSlabs());
        if (prediction != null && bitsandbalance$countSlabs(actualSlabs) < 2) {
            VerticalStepBlockEntityRenderer.renderQuadPrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), prediction.quadState(), prediction.slabs(), poseStack, buffer, packedLight, packedOverlay);
            return;
        }
        BlockState quadState = blockEntity.getLevel().getBlockState(blockEntity.getBlockPos());
        if (!(quadState.getBlock() instanceof QuadVerticalStepBlock)) {
            return;
        }
        if (prediction != null) {
            EnhancedSlabPlacementPredictionCache.clearQuadVerticalStep(blockEntity.getLevel(), blockEntity.getBlockPos());
        }

        BlockState fullBlockState = SlabFamilyModelSourceSelector.fullBlockRenderStateForCompleteFamily(
                bitsandbalance$slot(actualSlabs, 0),
                bitsandbalance$slot(actualSlabs, 1),
                bitsandbalance$slot(actualSlabs, 2),
                bitsandbalance$slot(actualSlabs, 3));
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

        VerticalStepBlockEntityRenderer.renderQuadPrediction(this.blockRenderDispatcher, blockEntity.getLevel(), blockEntity.getBlockPos(), quadState, actualSlabs, poseStack, buffer, packedLight, packedOverlay);
    }

    private static Object bitsandbalance$bestSlots(QuadVerticalStepBlockEntity blockEntity, BlockState[] blockEntitySlots) {
        EnhancedSlabMaterialSavedData.MaterialView savedSlots =
                EnhancedSlabMaterialSavedData.getView(blockEntity.getLevel(), blockEntity.getBlockPos().asLong());
        if (bitsandbalance$countSlabs(savedSlots) > bitsandbalance$countSlabs(blockEntitySlots)) {
            return savedSlots;
        }
        return blockEntitySlots;
    }

    private static int bitsandbalance$countSlabs(Object slabs) {
        int count = 0;
        for (int index = 0; index < 4; index++) {
            if (bitsandbalance$slot(slabs, index) != null) {
                count++;
            }
        }
        return count;
    }

    private static BlockState bitsandbalance$slot(Object slabs, int index) {
        if (slabs instanceof EnhancedSlabMaterialSavedData.MaterialView view) {
            return view.slot(index);
        }
        return slabs instanceof BlockState[] states && index >= 0 && index < states.length ? states[index] : null;
    }
}
